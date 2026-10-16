package com.askylix.app.data.session

import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.ConfigDto
import com.askylix.app.data.remote.MeDto
import com.askylix.app.data.remote.QuotaDto
import com.askylix.app.data.remote.ToolFlagDto
import com.askylix.app.domain.model.ChatSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The chat list as History shows it, and the paging, search and filter that produced it. */
data class ChatListState(
    val items: List<ChatSummary> = emptyList(),
    val nextCursor: String? = null,
    /** The `category` last asked for, or null for all. */
    val category: String? = null,
    val query: String = "",
    val loadedAt: Long = 0L,
    /** The first page is being fetched. Whatever is in [items] is still shown meanwhile. */
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: ApiErrorCode? = null,
) {
    val hasMore: Boolean get() = nextCursor != null
}

/** What Home needs: the newest chats, and the name the greeting uses. */
data class DashboardState(
    val name: String? = null,
    val recent: List<ChatSummary> = emptyList(),
    val loadedAt: Long = 0L,
    val loading: Boolean = false,
    val error: ApiErrorCode? = null,
)

/** One conversation, as far as it has been read. */
data class ThreadState(
    val chatId: String,
    val title: String = "",
    val tool: com.askylix.app.domain.model.StudyTool? = null,
    val messages: List<StoredMessage> = emptyList(),
    /** The cursor for older messages, or null when the whole conversation is here. */
    val olderCursor: String? = null,
    val loadedAt: Long = 0L,
    val loading: Boolean = false,
    val loadingOlder: Boolean = false,
    val error: ApiErrorCode? = null,
    /** The server says this chat is gone: deleted, or never the signed-in person's. */
    val gone: Boolean = false,
    /** A question or a regenerate is on its way, even if the screen that asked has been left. */
    val sending: Boolean = false,
) {
    val hasOlder: Boolean get() = olderCursor != null
}

/**
 * Everything the app has read from the server, held in memory.
 *
 * There is nothing on disk and nothing here outlives the process, which is the point: the server is
 * the only copy of a conversation, so there is no second copy to fall out of step with it. A
 * conversation deleted on another phone is simply absent from the next list that is fetched.
 *
 * The flows are what screens watch. Repositories are the only writers.
 */
class SessionStore {

    private val _me = MutableStateFlow<MeDto?>(null)
    val me: StateFlow<MeDto?> = _me.asStateFlow()

    private val _quota = MutableStateFlow<QuotaDto?>(null)
    val quota: StateFlow<QuotaDto?> = _quota.asStateFlow()

    private val _config = MutableStateFlow<ConfigDto?>(null)
    val config: StateFlow<ConfigDto?> = _config.asStateFlow()

    private val _toolFlags = MutableStateFlow<Map<String, ToolFlagDto>>(emptyMap())
    val toolFlags: StateFlow<Map<String, ToolFlagDto>> = _toolFlags.asStateFlow()

    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard.asStateFlow()

    private val _history = MutableStateFlow(ChatListState())
    val history: StateFlow<ChatListState> = _history.asStateFlow()

    private val threads = HashMap<String, MutableStateFlow<ThreadState>>()

    /** The `me` fetch time, kept apart from the value so "is it fresh?" never needs the value. */
    @Volatile var meLoadedAt: Long = 0L

    fun setMe(value: MeDto?, at: Long) {
        _me.value = value
        meLoadedAt = if (value == null) 0L else at
        if (value != null) _quota.value = value.quota
    }

    fun setQuota(value: QuotaDto?) {
        if (value != null) _quota.value = value
    }

    fun setConfig(value: ConfigDto) {
        _config.value = value
        if (value.tools.isNotEmpty()) mergeToolFlags(value.tools)
    }

    fun mergeToolFlags(flags: List<ToolFlagDto>) {
        _toolFlags.update { current -> current + flags.associateBy { it.tool } }
    }

    fun updateDashboard(change: (DashboardState) -> DashboardState) = _dashboard.update(change)

    fun updateHistory(change: (ChatListState) -> ChatListState) = _history.update(change)

    /** The flow for one conversation, created empty the first time it is asked for. */
    fun thread(chatId: String): MutableStateFlow<ThreadState> = synchronized(threads) {
        threads.getOrPut(chatId) { MutableStateFlow(ThreadState(chatId)) }
    }

    /** Takes a deleted chat out of every place it is listed, and forgets its messages. */
    fun forgetChat(chatId: String) {
        _history.update { it.copy(items = it.items.filterNot { chat -> chat.id == chatId }) }
        _dashboard.update { it.copy(recent = it.recent.filterNot { chat -> chat.id == chatId }) }
        // A screen still watching the thread holds the flow it was given, so it is told the chat is gone
        // rather than being left with a stale one.
        synchronized(threads) { threads.remove(chatId) }?.update { it.copy(gone = true, sending = false) }
    }

    /**
     * Gives a chat a new title everywhere it is listed. Returns the title it had, so a rename that the
     * server then refuses can be put back.
     */
    fun renameChat(chatId: String, title: String): String? {
        val previous = _history.value.items.firstOrNull { it.id == chatId }?.title
            ?: _dashboard.value.recent.firstOrNull { it.id == chatId }?.title
            ?: threads[chatId]?.value?.title
        fun List<ChatSummary>.renamed() = map { if (it.id == chatId) it.copy(title = title) else it }
        _history.update { it.copy(items = it.items.renamed()) }
        _dashboard.update { it.copy(recent = it.recent.renamed()) }
        synchronized(threads) { threads[chatId] }?.update { it.copy(title = title) }
        return previous
    }

    /** Marks the lists as out of date, so the next screen to open them fetches again. */
    fun markListsStale() {
        _history.update { it.copy(loadedAt = 0L) }
        _dashboard.update { it.copy(loadedAt = 0L) }
    }

    /**
     * Forgets everything about the signed-in account: on log-out, on a deleted account, and when the
     * server stops accepting the session. App configuration is not the account's, so it stays.
     */
    fun clearAccount() {
        _me.value = null
        meLoadedAt = 0L
        _quota.value = null
        _dashboard.value = DashboardState()
        _history.value = ChatListState()
        synchronized(threads) { threads.clear() }
    }
}
