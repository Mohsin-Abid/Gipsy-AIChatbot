package com.askylix.app.data.repository

import com.askylix.app.data.remote.ApiBodies
import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.AskylixApi
import com.askylix.app.data.remote.ChatOutcome
import com.askylix.app.data.remote.ChatResponse
import com.askylix.app.data.remote.toBlock
import com.askylix.app.data.remote.toStudyTool
import com.askylix.app.data.remote.toSummaryOrNull
import com.askylix.app.data.remote.wire
import com.askylix.app.data.session.DashboardState
import com.askylix.app.data.session.SessionStore
import com.askylix.app.data.session.StoredMessage
import com.askylix.app.data.session.ThreadState
import com.askylix.app.data.session.toStoredOrNull
import com.askylix.app.domain.model.Attachment
import com.askylix.app.domain.model.ChatMessage
import com.askylix.app.domain.model.Rating
import com.askylix.app.domain.model.SendStatus
import com.askylix.app.domain.model.StudyTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/** A question on its way: what to ask, where, and the key that makes a repeat of it safe. */
class Question(
    val tool: StudyTool,
    /** Null for a new chat. The server creates it and names it. */
    val chatId: String?,
    val text: String,
    val attachment: Attachment? = null,
    /** A fresh UUID per question, reused when the same question is sent again after a failure. */
    val idempotencyKey: String,
    /** The answer to redo, for the Regenerate button. [text] is empty then. */
    val regenerateMessageId: String? = null,
)

/** How a question ended, when the server replied. Failures are thrown as [ApiError]. */
sealed interface SendResult {
    val chatId: String

    data class Answered(override val chatId: String, val title: String) : SendResult

    /** Saved on the server, no answer produced. [questionId] is the saved question. */
    data class Unanswered(override val chatId: String, val title: String, val questionId: String) : SendResult
}

/**
 * Conversations, read from and written to the server.
 *
 * Nothing here is saved on the phone. Reads fill the [SessionStore] and writes go to the API, so the
 * screens always show what the server says, and a chat deleted on another phone is simply missing
 * the next time the list is fetched.
 *
 * Reads are reused for [FRESH_MS] so that opening screens does not hammer the server, and concurrent
 * reads of the same thing share one call. A question runs in [scope], which outlives any screen: going
 * back while an answer is being prepared does not cancel it, so the answer is in the chat on return.
 */
class ChatRepository(
    private val api: AskylixApi,
    private val caller: ApiCaller,
    private val session: SessionStore,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val historyLock = Mutex()
    private val dashboardLock = Mutex()
    private val threadLocks = HashMap<String, Mutex>()
    private val sends = HashMap<String, Job>()

    // ---- Lists ----

    /**
     * The first page of History. [category] and [query] are the server's own filters, so a narrowed
     * list is a narrowed fetch rather than a filter over whatever pages happen to be loaded.
     */
    suspend fun loadHistory(category: String?, query: String, force: Boolean): Result<Unit> = historyLock.withLock {
        val current = session.history.value
        val sameArgs = current.category == category && current.query == query
        if (!force && sameArgs && current.loadedAt != 0L && now() - current.loadedAt < FRESH_MS) {
            return@withLock Result.success(Unit)
        }

        // Same filter: keep showing the old rows while the new ones arrive. A different one starts clean.
        session.updateHistory {
            (if (sameArgs) it else it.copy(items = emptyList(), nextCursor = null)).copy(
                category = category, query = query, loading = true, error = null,
            )
        }
        runCatching {
            caller.call { api.chats(cursor = null, limit = PAGE_SIZE, query = query.ifBlank { null }, category = category) }
        }.fold(
            onSuccess = { page ->
                if (!stillShowing(category, query)) return@fold Result.success(Unit)
                session.updateHistory {
                    it.copy(
                        items = page.items.mapNotNull { dto -> dto.toSummaryOrNull() },
                        nextCursor = page.nextCursor,
                        loadedAt = now(),
                        loading = false,
                    )
                }
                Result.success(Unit)
            },
            onFailure = { failure ->
                session.updateHistory { it.copy(loading = false, error = failure.errorCode()) }
                Result.failure(failure)
            },
        )
    }

    /** The next page of History, when there is one. */
    suspend fun loadMoreHistory(): Result<Unit> = historyLock.withLock {
        val current = session.history.value
        val cursor = current.nextCursor ?: return@withLock Result.success(Unit)
        session.updateHistory { it.copy(loadingMore = true, error = null) }

        runCatching {
            caller.call {
                api.chats(
                    cursor = cursor,
                    limit = PAGE_SIZE,
                    query = current.query.ifBlank { null },
                    category = current.category,
                )
            }
        }.fold(
            onSuccess = { page ->
                if (!stillShowing(current.category, current.query)) return@fold Result.success(Unit)
                session.updateHistory { state ->
                    val known = state.items.map { it.id }.toSet()
                    state.copy(
                        items = state.items + page.items.mapNotNull { it.toSummaryOrNull() }.filterNot { it.id in known },
                        nextCursor = page.nextCursor,
                        loadingMore = false,
                    )
                }
                Result.success(Unit)
            },
            onFailure = { failure ->
                session.updateHistory { it.copy(loadingMore = false, error = failure.errorCode()) }
                Result.failure(failure)
            },
        )
    }

    /** Home's one call: the greeting name, the quota, tool flags and the newest chats. */
    suspend fun loadDashboard(force: Boolean): Result<Unit> = dashboardLock.withLock {
        val current = session.dashboard.value
        if (!force && current.loadedAt != 0L && now() - current.loadedAt < FRESH_MS) {
            return@withLock Result.success(Unit)
        }
        session.updateDashboard { it.copy(loading = true, error = null) }

        runCatching { caller.call { api.dashboard() } }.fold(
            onSuccess = { dashboard ->
                session.setQuota(dashboard.quota)
                session.mergeToolFlags(dashboard.tools)
                session.updateDashboard {
                    DashboardState(
                        name = dashboard.user.name,
                        recent = dashboard.recentChats.mapNotNull { it.toSummaryOrNull() },
                        loadedAt = now(),
                    )
                }
                Result.success(Unit)
            },
            onFailure = { failure ->
                session.updateDashboard { it.copy(loading = false, error = failure.errorCode()) }
                Result.failure(failure)
            },
        )
    }

    // ---- One conversation ----

    fun thread(chatId: String): StateFlow<ThreadState> = session.thread(chatId)

    /**
     * Reads the newest messages of a chat. The whole page replaces what was held, so anything that
     * changed elsewhere — a rename, a message from another phone — shows.
     *
     * A 404 means the chat is gone, deleted or never this person's, and it is removed from the lists.
     */
    suspend fun loadThread(chatId: String, force: Boolean): Result<Unit> = lockFor(chatId).withLock {
        val flow = session.thread(chatId)
        val current = flow.value
        // A question is on its way: its reply will update the thread, and a read now would race it.
        if (current.sending || current.gone) return@withLock Result.success(Unit)
        if (!force && current.loadedAt != 0L && now() - current.loadedAt < FRESH_MS) {
            return@withLock Result.success(Unit)
        }
        flow.update { it.copy(loading = true, error = null) }

        runCatching { caller.call { api.openChat(chatId) } }.fold(
            onSuccess = { detail ->
                flow.update {
                    it.copy(
                        title = detail.chat.title,
                        tool = detail.chat.tool.toStudyTool(),
                        messages = detail.messages.mapNotNull { dto -> dto.toStoredOrNull() },
                        olderCursor = detail.nextCursor,
                        loadedAt = now(),
                        loading = false,
                    )
                }
                Result.success(Unit)
            },
            onFailure = { failure ->
                val code = failure.errorCode()
                if (code == ApiErrorCode.NotFound) {
                    session.forgetChat(chatId)
                } else {
                    flow.update { it.copy(loading = false, error = code) }
                }
                Result.failure(failure)
            },
        )
    }

    /**
     * A fresh signed link for a message's file. The ones in a chat expire after about ten minutes, so a
     * download that finds its link dead reads the chat again and takes the new one.
     */
    suspend fun refreshAttachmentLink(chatId: String, messageId: String): String? {
        loadThread(chatId, force = true)
        return session.thread(chatId).value.messages
            .firstOrNull { it.id == messageId }
            ?.let { (it.message as? ChatMessage.User)?.attachment?.url }
    }

    /** The page of messages before the oldest one held. */
    suspend fun loadOlder(chatId: String): Result<Unit> = lockFor(chatId).withLock {
        val flow = session.thread(chatId)
        val cursor = flow.value.olderCursor ?: return@withLock Result.success(Unit)
        flow.update { it.copy(loadingOlder = true, error = null) }

        runCatching { caller.call { api.openChat(chatId, before = cursor) } }.fold(
            onSuccess = { detail ->
                flow.update {
                    it.copy(
                        messages = detail.messages.mapNotNull { dto -> dto.toStoredOrNull() } + it.messages,
                        olderCursor = detail.nextCursor,
                        loadingOlder = false,
                    )
                }
                Result.success(Unit)
            },
            onFailure = { failure ->
                flow.update { it.copy(loadingOlder = false, error = failure.errorCode()) }
                Result.failure(failure)
            },
        )
    }

    // ---- Asking ----

    /**
     * Asks a question and waits for the reply.
     *
     * The request runs in [scope], not in the caller's coroutine: if the caller is cancelled — the
     * screen was left — the request carries on, the server saves the answer, and the thread in the
     * store has it when the person comes back.
     *
     * The question appears in its thread at once, marked as sending. If the call fails it is taken
     * out again and the error is thrown, so the caller can put the text back in the composer.
     */
    suspend fun send(question: Question): SendResult {
        // Lazy, so it is registered before it can possibly finish and clear its own entry.
        val job = scope.async(start = CoroutineStart.LAZY) { sendNow(question) }
        question.chatId?.let { id -> synchronized(sends) { sends[id] = job } }
        job.start()
        return job.await()
    }

    private suspend fun sendNow(question: Question): SendResult {
        val chatId = question.chatId
        val localId = LOCAL_PREFIX + question.idempotencyKey

        if (chatId != null) {
            session.thread(chatId).update { thread ->
                val asked = if (question.regenerateMessageId == null) {
                    thread.messages + StoredMessage(
                        ChatMessage.User(localId, question.text, question.attachment, SendStatus.Sending),
                        now(),
                    )
                } else {
                    thread.messages
                }
                thread.copy(messages = asked, sending = true, error = null)
            }
        }

        val outcome = try {
            caller.chat { sendRequest(question) }
        } catch (failure: ApiError) {
            if (chatId != null) {
                session.thread(chatId).update { thread ->
                    thread.copy(messages = thread.messages.filterNot { it.id == localId }, sending = false)
                }
                if (failure.code == ApiErrorCode.NotFound) session.forgetChat(chatId)
            }
            throw failure
        } finally {
            chatId?.let { synchronized(sends) { sends.remove(it) } }
        }

        return when (outcome) {
            is ChatOutcome.Answered -> finishAnswered(question, localId, outcome.response)
            is ChatOutcome.Unanswered -> finishUnanswered(question, localId, outcome.response)
        }
    }

    private suspend fun sendRequest(question: Question): retrofit2.Response<ChatResponse> {
        val body = ApiBodies.chat(question.tool.wire(), question.chatId, question.text, question.regenerateMessageId)
        val file = question.attachment?.localPath?.let(::File)

        return if (file != null && question.attachment != null) {
            val payload = body.toString().toRequestBody("text/plain; charset=utf-8".toMediaType())
            val part = MultipartBody.Part.createFormData(
                "file",
                question.attachment.fileName,
                file.asRequestBody(question.attachment.mimeType.toMediaType()),
            )
            api.askWithFile(question.idempotencyKey, payload, part)
        } else {
            api.ask(question.idempotencyKey, body)
        }
    }

    private fun finishAnswered(question: Question, localId: String, response: ChatResponse): SendResult {
        val answer = response.answer ?: throw ApiError(ApiErrorCode.BadResponse, 200)
        val chatId = response.chatId ?: question.chatId ?: throw ApiError(ApiErrorCode.BadResponse, 200)
        val at = now()
        session.setQuota(response.quota)

        val reply = StoredMessage(
            ChatMessage.Assistant(answer.messageId, answer.blocks.map { it.toBlock() }),
            at,
        )
        val asked = StoredMessage(
            ChatMessage.User(
                id = response.userMessage?.messageId ?: localId,
                text = response.userMessage?.text ?: question.text,
                // Kept so the file opens from this phone's own copy rather than being downloaded again.
                attachment = question.attachment,
            ),
            at,
        )

        session.thread(chatId).update { thread ->
            val messages = if (question.regenerateMessageId != null) {
                // The new answer takes the old one's place, so the thread never shows two answers.
                thread.messages.map { if (it.id == question.regenerateMessageId) reply.copy(createdAt = it.createdAt) else it }
            } else {
                thread.messages.filterNot { it.id == localId } + asked + reply
            }
            thread.copy(
                title = response.title?.ifBlank { null } ?: thread.title,
                tool = question.tool,
                messages = messages,
                sending = false,
                loadedAt = at,
            )
        }
        session.markListsStale()
        return SendResult.Answered(chatId, response.title.orEmpty())
    }

    private fun finishUnanswered(question: Question, localId: String, response: ChatResponse): SendResult {
        val chatId = response.chatId ?: question.chatId ?: throw ApiError(ApiErrorCode.BadResponse, 207)
        val savedId = response.userMessage?.messageId ?: localId
        val at = now()

        session.thread(chatId).update { thread ->
            val saved = StoredMessage(
                ChatMessage.User(savedId, response.userMessage?.text ?: question.text, question.attachment, SendStatus.Unanswered),
                at,
            )
            thread.copy(
                title = response.title?.ifBlank { null } ?: thread.title,
                tool = question.tool,
                messages = thread.messages.filterNot { it.id == localId } + saved,
                sending = false,
                loadedAt = at,
            )
        }
        session.markListsStale()
        return SendResult.Unanswered(chatId, response.title.orEmpty(), savedId)
    }

    /** Takes an unanswered question out of the thread before it is asked again, so it is not shown twice. */
    fun discardUnanswered(chatId: String, questionId: String) {
        session.thread(chatId).update { thread -> thread.copy(messages = thread.messages.filterNot { it.id == questionId }) }
    }

    // ---- Changing a chat ----

    /** Renames a chat. Shown at once everywhere; put back, and the error thrown, if the server refuses. */
    suspend fun rename(chatId: String, title: String) {
        val previous = session.renameChat(chatId, title)
        try {
            val renamed = caller.call { api.rename(chatId, ApiBodies.rename(title)) }
            session.renameChat(chatId, renamed.title)
        } catch (failure: ApiError) {
            previous?.let { session.renameChat(chatId, it) }
            if (failure.code == ApiErrorCode.NotFound) session.forgetChat(chatId)
            throw failure
        }
    }

    /**
     * Deletes a chat. Any question still being answered for it is stopped first, because its reply
     * would be written into a chat that no longer exists. A 404 counts as done: it is already gone.
     */
    suspend fun delete(chatId: String) {
        synchronized(sends) { sends.remove(chatId) }?.cancelAndJoin()
        try {
            caller.call { api.deleteChat(chatId) }
        } catch (failure: ApiError) {
            if (failure.code != ApiErrorCode.NotFound) throw failure
        }
        session.forgetChat(chatId)
    }

    /** Rates an answer, or clears the rating when [rating] is null. Undone if the server refuses. */
    suspend fun rate(chatId: String, messageId: String, rating: Rating?) {
        val previous = setRating(chatId, messageId, rating)
        try {
            caller.call { api.feedback(messageId, ApiBodies.rating(rating.wire())) }
        } catch (failure: ApiError) {
            setRating(chatId, messageId, previous)
            throw failure
        }
    }

    private fun setRating(chatId: String, messageId: String, rating: Rating?): Rating? {
        var previous: Rating? = null
        session.thread(chatId).update { thread ->
            thread.copy(
                messages = thread.messages.map { stored ->
                    val message = stored.message
                    if (message is ChatMessage.Assistant && message.id == messageId) {
                        previous = message.rating
                        stored.copy(message = message.copy(rating = rating))
                    } else {
                        stored
                    }
                },
            )
        }
        return previous
    }

    // ---- Helpers ----

    private fun stillShowing(category: String?, query: String): Boolean {
        val shown = session.history.value
        return shown.category == category && shown.query == query
    }

    private fun lockFor(chatId: String): Mutex = synchronized(threadLocks) { threadLocks.getOrPut(chatId) { Mutex() } }

    private fun Throwable.errorCode(): ApiErrorCode = (this as? ApiError)?.code ?: ApiErrorCode.Unknown

    companion object {
        /** How long a read is reused before a screen asks the server again. */
        const val FRESH_MS = 30_000L

        const val PAGE_SIZE = 20

        /** The prefix of an id given to a question the server has not answered yet. */
        const val LOCAL_PREFIX = "local:"
    }
}
