package com.aitutor.chatbot.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.data.repository.ChatHeader
import com.aitutor.chatbot.app.data.repository.ChatRepository
import com.aitutor.chatbot.app.domain.model.HistoryEntry
import com.aitutor.chatbot.app.domain.model.HistoryFilter
import com.aitutor.chatbot.app.domain.model.HistoryGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit

/** What the History screen draws. [loading] is true only before the first read returns. */
data class HistoryUiState(
    val entries: List<HistoryEntry> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.All,
    val loading: Boolean = true,
) {
    val visible: List<HistoryEntry> get() = entries.filter(filter::matches)

    /** True when there is no history at all, as opposed to none matching the current filter. */
    val isEmpty: Boolean get() = !loading && entries.isEmpty()

    fun inGroup(group: HistoryGroup): List<HistoryEntry> = visible.filter { it.group == group }
}

class HistoryViewModel(
    private val chats: ChatRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.All)

    val state: StateFlow<HistoryUiState> =
        combine(chats.observeHeaders(), filter) { headers, selected ->
            HistoryUiState(
                entries = headers.map { it.toEntry(now()) },
                filter = selected,
                loading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HistoryUiState(),
        )

    fun setFilter(value: HistoryFilter) {
        filter.value = value
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * A stored chat as a history row. The detail line is built here rather than saved, so it stays
 * correct as messages are added.
 */
internal fun ChatHeader.toEntry(nowMillis: Long): HistoryEntry = HistoryEntry(
    chatId = id,
    title = title,
    detail = subject,
    tool = tool,
    updatedAt = updatedAt,
    group = groupFor(updatedAt, nowMillis),
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
