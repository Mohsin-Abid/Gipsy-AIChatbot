package com.askylix.app

import kotlinx.coroutines.delay
import okhttp3.mockwebserver.MockResponse

/** Waits until [condition] holds, or fails the test after [timeoutMs]. For work that happens on another thread. */
internal suspend fun waitUntil(timeoutMs: Long = 4_000, what: String = "the condition", condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        if (System.currentTimeMillis() > deadline) throw AssertionError("Timed out waiting for $what")
        delay(10)
    }
}

internal fun jsonResponse(body: String, code: Int = 200): MockResponse = MockResponse()
    .setResponseCode(code)
    .setHeader("Content-Type", "application/json")
    .setBody(body)

internal fun errorResponse(code: Int, errorCode: String): MockResponse =
    jsonResponse("""{"error":{"code":"$errorCode","message":"x"},"requestId":"req_1"}""", code)

/** A page of chats as the server sends it. */
internal fun chatPage(vararg ids: String, next: String? = null, tool: String = "MATH_SOLVER"): String {
    val items = ids.joinToString(",") { """{"chatId":"$it","tool":"$tool","title":"Chat $it","lastMessageAt":"2026-10-15T09:47:55Z"}""" }
    val cursor = next?.let { "\"$it\"" } ?: "null"
    return """{"items":[$items],"nextCursor":$cursor}"""
}

/** One chat opened, with a question and its answer. */
internal fun openChatBody(
    id: String = "cht_1",
    title: String = "Solving",
    older: String? = null,
    attachmentUrl: String? = null,
): String {
    val attachment = attachmentUrl?.let {
        """{"fileName":"tiny.jpg","mimeType":"image/jpeg","sizeBytes":160,"url":"$it"}"""
    } ?: "null"
    val cursor = older?.let { "\"$it\"" } ?: "null"
    return """{"chat":{"chatId":"$id","tool":"MATH_SOLVER","title":"$title","lastMessageAt":"2026-10-15T09:47:55Z"},
        "messages":[
          {"messageId":"msg_u1","role":"USER","createdAt":"2026-10-15T09:47:45Z","text":"Solve 2x = 4","attachment":$attachment},
          {"messageId":"msg_a1","role":"ASSISTANT","createdAt":"2026-10-15T09:47:45Z","blocks":[{"type":"PARAGRAPH","term":null,"text":"x is 2"}],"rating":null}
        ],"nextCursor":$cursor}"""
}

internal fun answeredBody(
    chatId: String = "cht_1",
    title: String = "Solving",
    userId: String = "msg_u2",
    answerId: String = "msg_a2",
    remaining: Int = 14,
): String = """{"chatId":"$chatId","title":"$title","userMessage":{"messageId":"$userId","text":"q"},
    "answer":{"messageId":"$answerId","blocks":[{"type":"PARAGRAPH","term":null,"text":"an answer"}]},
    "quota":{"unlimited":false,"limit":15,"remaining":$remaining,"resetsAt":"2026-10-16T00:00:00Z"}}"""
