package com.askylix.app

import com.askylix.app.data.remote.ApiBodies
import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.ApiClient
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.ChatOutcome
import com.askylix.app.data.remote.TokenProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The network layer against a fake server: what goes out, and what the app makes of what comes back.
 * The replies are the shapes the backend's own collection shows.
 */
class ApiClientTest {

    private lateinit var server: MockWebServer

    /** "OLD" for a normal request, "NEW" when a refused token is being replaced. */
    private val tokens = TokenProvider { forceRefresh -> if (forceRefresh) "NEW" else "OLD" }

    private lateinit var client: ApiClient
    private var unauthorizedReports = 0
    private val caller = ApiCaller(onUnauthorized = { unauthorizedReports++ })

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        client = ApiClient(baseUrl = server.url("/v1/").toString(), tokens = tokens)
    }

    @After
    fun stop() {
        runCatching { server.shutdown() }
    }

    private fun json(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun failure(code: Int, errorCode: String, vararg headers: Pair<String, String>) =
        json("""{"error":{"code":"$errorCode","message":"x"},"requestId":"req_1"}""", code).apply {
            headers.forEach { (k, v) -> setHeader(k, v) }
        }

    private val me = """{"userId":"u1","setupCompleted":true,"plan":{"plan":"FREE"},"quota":{"unlimited":false,"limit":15,"remaining":11}}"""

    // ---- Tokens ----

    private val configJson =
        """{"minAppVersion":"1.0.0","latestAppVersion":"1.0.0","maintenance":{"active":false,"message":null},"urls":{},"freeDailyLimit":15,"tools":[{"tool":"CHATBOT","enabled":true,"isNew":false}]}"""

    private fun clientWith(provider: TokenProvider) =
        ApiClient(baseUrl = server.url("/v1/").toString(), tokens = provider)

    @Test
    fun `config carries the token too when someone is signed in, and never the marker header`() = runBlocking {
        server.enqueue(json(configJson))

        val config = caller.call(endsSessionOn401 = false) { client.api.config() }
        val request = server.takeRequest()

        assertEquals("/v1/config", request.path)
        assertEquals("Bearer OLD", request.getHeader("Authorization"))
        assertNull(request.getHeader("X-Auth-Optional"))
        assertEquals(15, config.freeDailyLimit)
    }

    @Test
    fun `config goes out with no token when nobody is signed in`() = runBlocking {
        server.enqueue(json(configJson))
        val signedOut = clientWith { null }

        val config = caller.call(endsSessionOn401 = false) { signedOut.api.config() }

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(15, config.freeDailyLimit)
    }

    @Test
    fun `config still goes out when the token cannot be read`() = runBlocking {
        server.enqueue(json(configJson))
        val broken = clientWith { error("Firebase could not be reached") }

        val config = caller.call(endsSessionOn401 = false) { broken.api.config() }

        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(15, config.freeDailyLimit)
    }

    @Test
    fun `a refused config is neither retried nor the end of the session`() = runBlocking {
        server.enqueue(failure(401, "INVALID_ID_TOKEN"))

        val error = runCatching { caller.call(endsSessionOn401 = false) { client.api.config() } }
            .exceptionOrNull() as ApiError

        assertTrue(error.isUnauthorized)
        assertEquals("a public call has no token to renew", 1, server.requestCount)
        assertEquals("and says nothing about the session", 0, unauthorizedReports)
    }

    @Test
    fun `a call that needs the token fails when the token cannot be read`() {
        val broken = clientWith { error("Firebase could not be reached") }

        val failed = runCatching { runBlocking { caller.call { broken.api.me() } } }

        assertTrue("it must not go out without the token", failed.isFailure)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `every call carries the token`() = runBlocking {
        server.enqueue(json(me))

        caller.call { client.api.me() }

        assertEquals("Bearer OLD", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `a 401 is retried once with a fresh token`() = runBlocking {
        server.enqueue(failure(401, "TOKEN_EXPIRED"))
        server.enqueue(json(me))

        val result = caller.call { client.api.me() }

        assertEquals("u1", result.userId)
        assertEquals("Bearer OLD", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer NEW", server.takeRequest().getHeader("Authorization"))
        assertEquals(0, unauthorizedReports)
    }

    @Test
    fun `a second 401 is given up on and reported as the end of the session`() = runBlocking {
        server.enqueue(failure(401, "UNAUTHENTICATED"))
        server.enqueue(failure(401, "UNAUTHENTICATED"))

        val error = runCatching { caller.call { client.api.me() } }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.Unauthenticated, error.code)
        assertTrue(error.isUnauthorized)
        assertEquals("exactly one retry, not a loop", 2, server.requestCount)
        assertEquals(1, unauthorizedReports)
    }

    // ---- Errors ----

    @Test
    fun `an error reply is read for its code, request id and retry delay`() = runBlocking {
        server.enqueue(failure(429, "QUOTA_EXCEEDED", "Retry-After" to "30"))

        val error = runCatching { caller.call { client.api.me() } }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.QuotaExceeded, error.code)
        assertEquals(429, error.status)
        assertEquals("req_1", error.requestId)
        assertEquals(30L, error.retryAfterSeconds)
    }

    @Test
    fun `after a rate limit every call waits out the delay without going to the server`() = runBlocking {
        var clock = 1_000_000L
        val limited = ApiCaller(now = { clock })
        server.enqueue(failure(429, "RATE_LIMITED", "Retry-After" to "20"))
        server.enqueue(json(me))

        runCatching { limited.call { client.api.me() } }
        clock += 5_000
        val during = runCatching { limited.call { client.api.me() } }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.RateLimited, during.code)
        assertEquals("15 seconds are left of the 20", 15L, during.retryAfterSeconds)
        assertEquals("the second call never left the phone", 1, server.requestCount)

        clock += 15_000
        val after = limited.call { client.api.me() }
        assertEquals("u1", after.userId)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `using up the day's questions is not a delay and does not pause anything`() = runBlocking {
        server.enqueue(failure(429, "QUOTA_EXCEEDED"))
        server.enqueue(json(me))

        runCatching { caller.call { client.api.me() } }
        val next = caller.call { client.api.me() }

        assertEquals("u1", next.userId)
    }

    @Test
    fun `a pause is capped, so a mistaken header cannot lock the app up for an hour`() = runBlocking {
        var clock = 1_000_000L
        val limited = ApiCaller(now = { clock })
        server.enqueue(failure(429, "RATE_LIMITED", "Retry-After" to "3600"))
        server.enqueue(json(me))

        runCatching { limited.call { client.api.me() } }
        clock += 61_000

        assertEquals("u1", limited.call { client.api.me() }.userId)
    }

    @Test
    fun `an error with no readable body falls back to the status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("<html>nope</html>"))

        val error = runCatching { caller.call { client.api.openChat("cht_gone") } }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.NotFound, error.code)
    }

    @Test
    fun `a server that cannot be reached is a connection problem`() = runBlocking {
        server.shutdown()

        val error = runCatching { caller.call { client.api.me() } }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.NoConnection, error.code)
        assertTrue(error.isConnectionProblem)
        assertFalse(error.isUnauthorized)
    }

    @Test
    fun `statuses map to codes`() {
        assertEquals(ApiErrorCode.FileTooLarge, ApiErrorCode.fromStatus(413))
        assertEquals(ApiErrorCode.UnsupportedFileType, ApiErrorCode.fromStatus(415))
        assertEquals(ApiErrorCode.FileUnreadable, ApiErrorCode.fromStatus(422))
        assertEquals(ApiErrorCode.UpstreamUnavailable, ApiErrorCode.fromStatus(503))
        assertEquals(ApiErrorCode.Internal, ApiErrorCode.fromStatus(500))
        assertEquals(ApiErrorCode.AccountRequired, ApiErrorCode.fromStatus(403))
    }

    // ---- Reading replies ----

    @Test
    fun `unknown fields are ignored and a null term is accepted`() = runBlocking {
        server.enqueue(json("""{"chat":{"chatId":"cht_1","tool":"MATH_SOLVER","title":"T","lastMessageAt":"2026-10-15T09:47:55Z","somethingNew":1},
            "messages":[{"messageId":"msg_1","role":"ASSISTANT","createdAt":"2026-10-15T09:47:45Z",
            "blocks":[{"type":"PARAGRAPH","term":null,"text":"Hello"},{"type":"BULLET","term":"Step","text":"Do it"}],"rating":null}],"nextCursor":null}"""))

        val chat = caller.call { client.api.openChat("cht_1") }

        val blocks = chat.messages.single().blocks!!
        assertNull(blocks[0].term)
        assertEquals("Step", blocks[1].term)
        assertEquals("MATH_SOLVER", chat.chat.tool)
    }

    // ---- Chat ----

    private val answered = """{"chatId":"cht_1","title":"Solving","userMessage":{"messageId":"msg_1","text":"2x=4"},
        "answer":{"messageId":"msg_2","blocks":[{"type":"PARAGRAPH","term":null,"text":"x is 2"}]},
        "quota":{"unlimited":false,"limit":15,"remaining":14,"resetsAt":"2026-10-16T00:00:00Z"}}"""

    @Test
    fun `a new chat is asked with an explicit null chat id and the key header`() = runBlocking {
        server.enqueue(json(answered))

        caller.chat { client.api.ask("key-123", ApiBodies.chat("MATH_SOLVER", chatId = null, message = "2x=4")) }
        val request = server.takeRequest()

        assertEquals("key-123", request.getHeader("Idempotency-Key"))
        assertEquals("""{"tool":"MATH_SOLVER","chatId":null,"message":"2x=4"}""", request.body.readUtf8())
    }

    @Test
    fun `a regenerate carries the answer id`() = runBlocking {
        server.enqueue(json(answered))

        caller.chat {
            client.api.ask("k", ApiBodies.chat("MATH_SOLVER", "cht_1", message = "", regenerateMessageId = "msg_2"))
        }

        assertTrue(server.takeRequest().body.readUtf8().contains(""""regenerateMessageId":"msg_2""""))
    }

    @Test
    fun `200 is answered`() = runBlocking {
        server.enqueue(json(answered))

        val outcome = caller.chat { client.api.ask("k", ApiBodies.chat("MATH_SOLVER", null, "2x=4")) }

        outcome as ChatOutcome.Answered
        assertEquals("cht_1", outcome.response.chatId)
        assertEquals("msg_2", outcome.response.answer?.messageId)
        assertEquals(14, outcome.response.quota?.remaining)
    }

    @Test
    fun `207 is unanswered, with the question saved and no answer`() = runBlocking {
        server.enqueue(
            json(
                """{"chatId":"cht_1","title":"Hello","userMessage":{"messageId":"msg_1","text":"Hello"},
                "error":{"code":"UPSTREAM_UNAVAILABLE","message":"No answer could be produced."}}""",
                code = 207,
            )
        )

        val outcome = caller.chat { client.api.ask("k", ApiBodies.chat("CHATBOT", null, "Hello")) }

        outcome as ChatOutcome.Unanswered
        assertEquals("msg_1", outcome.response.userMessage?.messageId)
        assertNull(outcome.response.answer)
        assertEquals("UPSTREAM_UNAVAILABLE", outcome.response.error?.code)
    }

    @Test
    fun `a chat refused for the daily limit is an error, not an outcome`() = runBlocking {
        server.enqueue(failure(429, "QUOTA_EXCEEDED"))

        val error = runCatching {
            caller.chat { client.api.ask("k", ApiBodies.chat("CHATBOT", null, "Hi")) }
        }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.QuotaExceeded, error.code)
    }

    @Test
    fun `a question with a file is multipart with a payload part and a file part`() = runBlocking {
        server.enqueue(json(answered))
        val payload = ApiBodies.chat("HOMEWORK_SOLVER", null, "What is this?").toString()
            .toRequestBody("text/plain; charset=utf-8".toMediaType())
        val file = MultipartBody.Part.createFormData(
            "file", "tiny.jpg", "bytes".toRequestBody("image/jpeg".toMediaType()),
        )

        caller.chat { client.api.askWithFile("k2", payload, file) }
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        assertTrue(body.contains("""name="payload""""))
        assertTrue(body.contains(""""tool":"HOMEWORK_SOLVER""""))
        assertTrue(body.contains("""name="file"; filename="tiny.jpg""""))
        assertEquals("k2", request.getHeader("Idempotency-Key"))
    }

    // ---- Writes ----

    @Test
    fun `a PATCH carries only the keys it was given`() = runBlocking {
        server.enqueue(json(me))
        val settings: JsonObject = buildJsonObject { put("language", "ur") }

        caller.call { client.api.updateMe(ApiBodies.me(settings = settings)) }

        assertEquals("""{"settings":{"language":"ur"}}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `deleting a chat and rating an answer accept an empty 204`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        caller.call { client.api.deleteChat("cht_1") }
        caller.call { client.api.feedback("msg_2", ApiBodies.rating("HELPFUL")) }

        assertEquals("DELETE", server.takeRequest().method)
        val rating = server.takeRequest()
        assertEquals("/v1/messages/msg_2/feedback", rating.path)
        assertEquals("""{"rating":"HELPFUL"}""", rating.body.readUtf8())
    }

    @Test
    fun `the chat list sends its paging, search and filter`() = runBlocking {
        server.enqueue(json("""{"items":[],"nextCursor":null}"""))

        caller.call { client.api.chats(cursor = "abc", limit = 20, query = "solve", category = "SOLVED") }

        assertEquals("/v1/chats?cursor=abc&limit=20&q=solve&category=SOLVED", server.takeRequest().path)
    }

    // ---- Files ----

    @Test
    fun `a signed file link is fetched without the token`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("file-bytes"))

        val body = client.files.download(server.url("/v1/files/1?expires=1&signature=abc").toString())
        val request = server.takeRequest()

        assertEquals("file-bytes", body.string())
        assertNull("the token must not travel to a signed link", request.getHeader("Authorization"))
        assertEquals("/v1/files/1?expires=1&signature=abc", request.path)
    }

    @Test
    fun `any other failure still becomes an ApiError`() {
        val error = runCatching {
            runBlocking { caller.call<Unit> { throw IllegalStateException("boom") } }
        }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.Unknown, error.code)
    }
}
