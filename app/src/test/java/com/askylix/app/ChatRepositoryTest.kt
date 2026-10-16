package com.askylix.app

import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.ApiClient
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.QuotaDto
import com.askylix.app.data.remote.TokenProvider
import com.askylix.app.data.repository.ChatRepository
import com.askylix.app.data.repository.Question
import com.askylix.app.data.repository.SendResult
import com.askylix.app.data.session.SessionStore
import com.askylix.app.domain.model.ChatMessage
import com.askylix.app.domain.model.ChatSummary
import com.askylix.app.domain.model.Rating
import com.askylix.app.domain.model.SendStatus
import com.askylix.app.domain.model.StudyTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * The chat repository against a fake server: what it asks for, what it keeps in memory, and what it
 * does when a question is slow, refused, or left behind by the screen that asked it.
 */
class ChatRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var scope: CoroutineScope
    private val session = SessionStore()
    private var clock = 1_000_000L
    private lateinit var repo: ChatRepository

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val client = ApiClient(server.url("/v1/").toString(), TokenProvider { "T" })
        repo = ChatRepository(client.api, ApiCaller(), session, scope, now = { clock })
    }

    @After
    fun stop() {
        scope.cancel()
        runCatching { server.shutdown() }
    }

    private fun summary(id: String) = ChatSummary(id, StudyTool.MathSolver, "Chat $id", 1L)
    private fun ask(chatId: String?, key: String = "key-1", text: String = "q", regenerate: String? = null) =
        Question(StudyTool.MathSolver, chatId, text, idempotencyKey = key, regenerateMessageId = regenerate)

    // ---- History ----

    @Test
    fun `the first page of history replaces what was held and keeps its cursor`() = runBlocking {
        server.enqueue(jsonResponse(chatPage("a", "b", next = "CUR")))

        repo.loadHistory(category = null, query = "", force = false).getOrThrow()

        val state = session.history.value
        assertEquals(listOf("a", "b"), state.items.map { it.id })
        assertEquals("CUR", state.nextCursor)
        assertTrue(state.hasMore)
        assertFalse(state.loading)
        assertEquals(clock, state.loadedAt)
        assertEquals("/v1/chats?limit=20", server.takeRequest().path)
    }

    @Test
    fun `a reading under thirty seconds old is reused, an older one is fetched again`() = runBlocking {
        server.enqueue(jsonResponse(chatPage("a")))
        server.enqueue(jsonResponse(chatPage("a", "b")))
        server.enqueue(jsonResponse(chatPage("a", "b", "c")))

        repo.loadHistory(null, "", force = false)
        clock += 10_000
        repo.loadHistory(null, "", force = false)
        assertEquals("fresh enough: no second call", 1, server.requestCount)

        repo.loadHistory(null, "", force = true)
        assertEquals("a pull to refresh always asks", 2, server.requestCount)

        clock += ChatRepository.FRESH_MS + 1
        repo.loadHistory(null, "", force = false)
        assertEquals("stale: asks again", 3, server.requestCount)
        assertEquals(3, session.history.value.items.size)
    }

    @Test
    fun `a chat deleted on another phone is simply missing from the next list`() = runBlocking {
        server.enqueue(jsonResponse(chatPage("a", "b")))
        server.enqueue(jsonResponse(chatPage("b")))

        repo.loadHistory(null, "", force = false)
        repo.loadHistory(null, "", force = true)

        assertEquals(listOf("b"), session.history.value.items.map { it.id })
    }

    @Test
    fun `a filter and a search are the server's, so a different one starts clean`() = runBlocking {
        server.enqueue(jsonResponse(chatPage("a")))
        server.enqueue(jsonResponse(chatPage("s", tool = "HOMEWORK_SOLVER")))

        repo.loadHistory(null, "", force = false)
        repo.loadHistory("SOLVED", "solve", force = false)

        server.takeRequest()
        assertEquals("/v1/chats?limit=20&q=solve&category=SOLVED", server.takeRequest().path)
        assertEquals(listOf("s"), session.history.value.items.map { it.id })
        assertEquals("SOLVED", session.history.value.category)
    }

    @Test
    fun `the next page is added to the list without repeating a chat`() = runBlocking {
        server.enqueue(jsonResponse(chatPage("a", "b", next = "CUR")))
        server.enqueue(jsonResponse(chatPage("b", "c", next = null)))

        repo.loadHistory(null, "", force = false)
        repo.loadMoreHistory()

        assertEquals(listOf("a", "b", "c"), session.history.value.items.map { it.id })
        assertFalse(session.history.value.hasMore)
        server.takeRequest()
        assertEquals("/v1/chats?cursor=CUR&limit=20", server.takeRequest().path)
    }

    @Test
    fun `a failed read keeps what was shown and says why`() = runBlocking {
        server.enqueue(jsonResponse(chatPage("a")))
        server.enqueue(errorResponse(500, "INTERNAL"))

        repo.loadHistory(null, "", force = false)
        val result = repo.loadHistory(null, "", force = true)

        assertTrue(result.isFailure)
        assertEquals(listOf("a"), session.history.value.items.map { it.id })
        assertEquals(ApiErrorCode.Internal, session.history.value.error)
        assertFalse(session.history.value.loading)
    }

    @Test
    fun `the dashboard fills Home, the quota and the tool flags in one call`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """{"user":{"name":"Sara","plan":"FREE","setupCompleted":true},
                "quota":{"unlimited":false,"limit":15,"remaining":11},
                "tools":[{"tool":"CHATBOT","enabled":true,"isNew":false},{"tool":"MATH_SOLVER","enabled":false,"isNew":true}],
                "recentChats":[{"chatId":"cht_1","tool":"MATH_SOLVER","title":"T","lastMessageAt":"2026-10-15T09:47:55Z"}]}"""
            )
        )

        repo.loadDashboard(force = false).getOrThrow()

        assertEquals("Sara", session.dashboard.value.name)
        assertEquals(listOf("cht_1"), session.dashboard.value.recent.map { it.id })
        assertEquals(11, session.quota.value?.remaining)
        assertFalse(session.toolFlags.value.getValue("MATH_SOLVER").enabled)
    }

    // ---- One conversation ----

    @Test
    fun `opening a chat reads its messages and the cursor for older ones`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody(older = "OLD")))

        repo.loadThread("cht_1", force = false).getOrThrow()

        val thread = session.thread("cht_1").value
        assertEquals(listOf("msg_u1", "msg_a1"), thread.messages.map { it.id })
        assertEquals("Solving", thread.title)
        assertEquals(StudyTool.MathSolver, thread.tool)
        assertEquals("OLD", thread.olderCursor)
        assertTrue(thread.hasOlder)
    }

    @Test
    fun `older messages are added above the ones held`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody(older = "OLD")))
        server.enqueue(
            jsonResponse(
                """{"chat":{"chatId":"cht_1","tool":"MATH_SOLVER","title":"Solving"},"messages":[
                {"messageId":"msg_old","role":"USER","createdAt":"2026-10-14T09:00:00Z","text":"earlier"}],"nextCursor":null}"""
            )
        )

        repo.loadThread("cht_1", force = false)
        repo.loadOlder("cht_1")

        assertEquals(listOf("msg_old", "msg_u1", "msg_a1"), session.thread("cht_1").value.messages.map { it.id })
        assertFalse(session.thread("cht_1").value.hasOlder)
        server.takeRequest()
        assertEquals("/v1/chats/cht_1?before=OLD", server.takeRequest().path)
    }

    @Test
    fun `a chat the server says is gone is removed from the lists and told so`() = runBlocking {
        session.updateHistory { it.copy(items = listOf(summary("cht_1"), summary("cht_2"))) }
        val watching = session.thread("cht_1")
        server.enqueue(errorResponse(404, "NOT_FOUND"))

        val result = repo.loadThread("cht_1", force = true)

        assertTrue(result.isFailure)
        assertTrue(watching.value.gone)
        assertEquals(listOf("cht_2"), session.history.value.items.map { it.id })
    }

    // ---- Asking ----

    @Test
    fun `a question appears at once, then takes the server's ids with its answer and the quota`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        session.updateHistory { it.copy(loadedAt = 99L) }
        server.enqueue(jsonResponse(answeredBody(), 200).setBodyDelay(400, TimeUnit.MILLISECONDS))

        val sending = scope.async { repo.send(ask("cht_1", text = "Why?")) }

        waitUntil(what = "the question to show as sending") { session.thread("cht_1").value.sending }
        val during = session.thread("cht_1").value.messages.last().message as ChatMessage.User
        assertEquals(SendStatus.Sending, during.status)
        assertEquals("Why?", during.text)
        assertTrue(during.id.startsWith(ChatRepository.LOCAL_PREFIX))

        val result = sending.await() as SendResult.Answered

        assertEquals("cht_1", result.chatId)
        val thread = session.thread("cht_1").value
        assertFalse(thread.sending)
        assertEquals(listOf("msg_u1", "msg_a1", "msg_u2", "msg_a2"), thread.messages.map { it.id })
        assertEquals(SendStatus.Sent, (thread.messages[2].message as ChatMessage.User).status)
        assertEquals(14, session.quota.value?.remaining)
        assertEquals("the lists will be fetched again", 0L, session.history.value.loadedAt)
    }

    @Test
    fun `a failed question is taken back out and the error thrown`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        server.enqueue(errorResponse(500, "INTERNAL"))

        val error = runCatching { repo.send(ask("cht_1")) }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.Internal, error.code)
        val thread = session.thread("cht_1").value
        assertFalse(thread.sending)
        assertEquals("nothing was left behind", listOf("msg_u1", "msg_a1"), thread.messages.map { it.id })
    }

    @Test
    fun `a daily limit is an error the screen can act on`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        server.enqueue(errorResponse(429, "QUOTA_EXCEEDED"))

        val error = runCatching { repo.send(ask("cht_1")) }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.QuotaExceeded, error.code)
    }

    @Test
    fun `no answer produced keeps the saved question, marked, with no quota used`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        session.setQuota(QuotaDto(limit = 15, remaining = 11))
        server.enqueue(
            jsonResponse(
                """{"chatId":"cht_1","title":"Solving","userMessage":{"messageId":"msg_u2","text":"Why?"},
                "error":{"code":"UPSTREAM_UNAVAILABLE","message":"No answer could be produced."}}""",
                code = 207,
            )
        )

        val result = repo.send(ask("cht_1", text = "Why?")) as SendResult.Unanswered

        assertEquals("msg_u2", result.questionId)
        val saved = session.thread("cht_1").value.messages.last().message as ChatMessage.User
        assertEquals(SendStatus.Unanswered, saved.status)
        assertEquals("msg_u2", saved.id)
        assertEquals("no quota was used", 11, session.quota.value?.remaining)
        assertFalse(session.thread("cht_1").value.sending)
    }

    @Test
    fun `the first question of a new chat creates its thread, named by the server`() = runBlocking {
        server.enqueue(jsonResponse(answeredBody(chatId = "cht_new", title = "A fresh topic")))

        val result = repo.send(ask(chatId = null)) as SendResult.Answered

        assertEquals("cht_new", result.chatId)
        val thread = session.thread("cht_new").value
        assertEquals("A fresh topic", thread.title)
        assertEquals(listOf("msg_u2", "msg_a2"), thread.messages.map { it.id })
        assertEquals(StudyTool.MathSolver, thread.tool)
        assertTrue(server.takeRequest().body.readUtf8().contains(""""chatId":null"""))
    }

    @Test
    fun `regenerating swaps the answer where it was, and adds no question`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        server.enqueue(jsonResponse(answeredBody(answerId = "msg_a9")))

        repo.send(ask("cht_1", text = "", regenerate = "msg_a1"))

        assertEquals(listOf("msg_u1", "msg_a9"), session.thread("cht_1").value.messages.map { it.id })
        server.takeRequest()
        assertTrue(server.takeRequest().body.readUtf8().contains(""""regenerateMessageId":"msg_a1""""))
    }

    @Test
    fun `the key a question is sent with is the one it was given`() = runBlocking {
        server.enqueue(jsonResponse(answeredBody(chatId = "cht_new")))

        repo.send(ask(null, key = "the-key-from-before"))

        assertEquals("the-key-from-before", server.takeRequest().getHeader("Idempotency-Key"))
    }

    @Test
    fun `leaving the screen does not cancel the question, and the answer is there on return`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        server.enqueue(jsonResponse(answeredBody()).setBodyDelay(500, TimeUnit.MILLISECONDS))

        // The screen's own coroutine, cancelled while the answer is still being prepared.
        val screen = launchScreen { repo.send(ask("cht_1")) }
        waitUntil(what = "the question to be on its way") { session.thread("cht_1").value.sending }
        screen.cancel()

        waitUntil(what = "the answer to land after the screen was left") {
            session.thread("cht_1").value.messages.any { it.id == "msg_a2" }
        }
        assertFalse(session.thread("cht_1").value.sending)
        assertEquals(14, session.quota.value?.remaining)
    }

    private fun CoroutineScope.launchScreen(block: suspend () -> Unit) = launch { block() }

    // ---- Changing a chat ----

    @Test
    fun `a rename shows at once, and the server's title is the one kept`() = runBlocking {
        session.updateHistory { it.copy(items = listOf(summary("cht_1"))) }
        server.enqueue(jsonResponse("""{"chatId":"cht_1","tool":"MATH_SOLVER","title":"Quadratics","lastMessageAt":"2026-10-15T09:47:55Z"}"""))

        repo.rename("cht_1", "Quadratics")

        assertEquals("Quadratics", session.history.value.items.single().title)
        assertEquals("""{"title":"Quadratics"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `a rename the server refuses is put back`() = runBlocking {
        session.updateHistory { it.copy(items = listOf(summary("cht_1"))) }
        server.enqueue(errorResponse(400, "VALIDATION_FAILED"))

        val error = runCatching { repo.rename("cht_1", "x") }.exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.ValidationFailed, error.code)
        assertEquals("Chat cht_1", session.history.value.items.single().title)
    }

    @Test
    fun `deleting removes the chat once the server has deleted it`() = runBlocking {
        session.updateHistory { it.copy(items = listOf(summary("cht_1"), summary("cht_2"))) }
        server.enqueue(MockResponse().setResponseCode(204))

        repo.delete("cht_1")

        assertEquals(listOf("cht_2"), session.history.value.items.map { it.id })
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1/chats/cht_1", request.path)
    }

    @Test
    fun `a chat that is already gone counts as deleted`() = runBlocking {
        session.updateHistory { it.copy(items = listOf(summary("cht_1"))) }
        server.enqueue(errorResponse(404, "NOT_FOUND"))

        repo.delete("cht_1")

        assertTrue(session.history.value.items.isEmpty())
    }

    @Test
    fun `a delete the server refuses leaves the chat where it was`() = runBlocking {
        session.updateHistory { it.copy(items = listOf(summary("cht_1"))) }
        server.enqueue(errorResponse(500, "INTERNAL"))

        runCatching { repo.delete("cht_1") }

        assertEquals(listOf("cht_1"), session.history.value.items.map { it.id })
    }

    @Test
    fun `deleting a chat stops a question still being answered for it`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        server.enqueue(jsonResponse(answeredBody()).setBodyDelay(1_500, TimeUnit.MILLISECONDS))
        server.enqueue(MockResponse().setResponseCode(204))

        val asking = scope.async { runCatching { repo.send(ask("cht_1")) } }
        waitUntil(what = "the question to be on its way") { session.thread("cht_1").value.sending }

        repo.delete("cht_1")
        asking.await()

        assertTrue("the reply was never written into a chat that no longer exists",
            session.thread("cht_1").value.messages.none { it.id == "msg_a2" })
    }

    @Test
    fun `rating an answer shows at once and tapping it again takes it back`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        repo.rate("cht_1", "msg_a1", Rating.Helpful)
        assertEquals(Rating.Helpful, ratingOf("msg_a1"))
        repo.rate("cht_1", "msg_a1", null)
        assertNull(ratingOf("msg_a1"))

        server.takeRequest()
        assertEquals("""{"rating":"HELPFUL"}""", server.takeRequest().body.readUtf8())
        assertEquals("""{"rating":"NONE"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun `a rating the server refuses is undone`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))
        repo.loadThread("cht_1", force = false)
        server.enqueue(errorResponse(500, "INTERNAL"))

        runCatching { repo.rate("cht_1", "msg_a1", Rating.NotHelpful) }

        assertNull(ratingOf("msg_a1"))
    }

    private fun ratingOf(id: String): Rating? =
        (session.thread("cht_1").value.messages.first { it.id == id }.message as ChatMessage.Assistant).rating

    // ---- Files ----

    @Test
    fun `an expired file link is replaced by reading the chat again`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody(attachmentUrl = "https://files/old?sig=1")))
        repo.loadThread("cht_1", force = false)
        server.enqueue(jsonResponse(openChatBody(attachmentUrl = "https://files/new?sig=2")))

        val fresh = repo.refreshAttachmentLink("cht_1", "msg_u1")

        assertEquals("https://files/new?sig=2", fresh)
    }

    @Test
    fun `no link comes back for a message that has no file`() = runBlocking {
        server.enqueue(jsonResponse(openChatBody()))

        assertNull(repo.refreshAttachmentLink("cht_1", "msg_u1"))
        assertNotNull(session.thread("cht_1").value.messages.firstOrNull())
    }
}
