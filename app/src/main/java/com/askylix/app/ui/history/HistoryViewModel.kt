package com.askylix.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.askylix.app.data.auth.AuthRepository
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.repository.ChatRepository
import com.askylix.app.data.session.SessionStore
import com.askylix.app.domain.model.ChatSummary
import com.askylix.app.domain.model.HistoryEntry
import com.askylix.app.domain.model.HistoryFilter
import com.askylix.app.domain.model.HistoryGroup
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** What the History screen draws. */
data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.All,
    val query: String = "",
    /** Nothing to show yet and the first page is on its way. */
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    /** Rows are already shown and the server is being asked again: pull to refresh, or coming back to the tab. */
    val refreshing: Boolean = false,
    val hasMore: Boolean = false,
    /** Without an account there are no chats: the screen asks for sign-in instead of listing. */
    val signedIn: Boolean = true,
    val error: ApiErrorCode? = null,
) {
    /** The server has already applied the filter and the search, so every entry loaded is shown. */
    val visible: List<HistoryEntry> get() = entries

    /** True when there is no history at all, as opposed to nothing matching the filter or the search. */
    val isEmpty: Boolean
        get() = signedIn && !loading && error == null && entries.isEmpty() && query.isBlank() && filter == HistoryFilter.All

    fun inGroup(group: HistoryGroup): List<HistoryEntry> = visible.filter { it.group == group }
}

@OptIn(FlowPreview::class)
class HistoryViewModel(
    private val chats: ChatRepository,
    session: SessionStore,
    auth: AuthRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.All)
    private val query = MutableStateFlow("")
    private val signedIn = auth.user.stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser)

    val state: StateFlow<HistoryUiState> = combine(session.history, filter, query, signedIn) { list, selected, text, user ->
        val nothingYet = list.items.isEmpty() && list.loadedAt == 0L && list.error == null
        HistoryUiState(
            entries = list.items.map { it.toEntry(now()) },
            filter = selected,
            query = text,
            loading = user != null && (list.loading && list.items.isEmpty() || nothingYet),
            loadingMore = list.loadingMore,
            refreshing = user != null && list.loading && list.items.isNotEmpty(),
            hasMore = list.hasMore,
            signedIn = user != null,
            error = list.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HistoryUiState(),
    )

    init {
        // The filter applies at once; typing waits for a pause, so a word is one search and not eight.
        viewModelScope.launch {
            combine(filter, query.debounce(SEARCH_PAUSE_MS), signedIn) { f, q, user -> Triple(f, q, user) }
                .distinctUntilChanged()
                .collect { (f, q, user) -> if (user != null) chats.loadHistory(f.category(), q.trim(), force = false) }
        }
    }

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }

    fun setQuery(value: String) {
        query.value = value
    }

    /** Pull to refresh: always asks the server. */
    fun refresh() {
        viewModelScope.launch { chats.loadHistory(filter.value.category(), query.value.trim(), force = true) }
    }

    /** The screen came back to the front: asks again unless it was asked a moment ago. */
    fun onResume() {
        if (signedIn.value == null) return
        viewModelScope.launch { chats.loadHistory(filter.value.category(), query.value.trim(), force = false) }
    }

    fun loadMore() {
        viewModelScope.launch { chats.loadMoreHistory() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val SEARCH_PAUSE_MS = 400L
    }
}

/** The server's name for a filter tab, or null for All. */
internal fun HistoryFilter.category(): String? = when (this) {
    HistoryFilter.All -> null
    HistoryFilter.Chats -> "CHATS"
    HistoryFilter.Solved -> "SOLVED"
    HistoryFilter.Summaries -> "SUMMARIES"
    HistoryFilter.Writing -> "WRITING"
}

/** A listed chat as a history row. */
internal fun ChatSummary.toEntry(nowMillis: Long): HistoryEntry = HistoryEntry(
    chatId = id,
    title = title,
    tool = tool,
    updatedAt = lastMessageAt,
    group = groupFor(lastMessageAt, nowMillis),
)

/**
 * Which heading a timestamp falls under.
 *
 * Deliberately measured in elapsed time rather than by calendar date: comparing days would need a
 * time zone and a locale to be correct, and this only has to decide between four buckets.
 */
internal fun groupFor(updatedAt: Long, nowMillis: Long): HistoryGroup {
    val elapsed = nowMillis - updatedAt
    val day = TimeUnit.DAYS.toMillis(1)
    return when {
        elapsed < day -> HistoryGroup.Today
        elapsed < 2 * day -> HistoryGroup.Yesterday
        elapsed < 7 * day -> HistoryGroup.EarlierThisWeek
        else -> HistoryGroup.Older
    }
}
