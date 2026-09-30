package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import com.aitutor.chatbot.app.R

/**
 * The chips across the top of History. Each stands for a group of tools rather than for a stored
 * flag, so an entry's filter follows from which tool produced it and never has to be kept in step.
 */
enum class HistoryFilter(
    @param:StringRes val labelRes: Int,
    private val category: ToolCategory?,
) {
    All(R.string.history_filter_all, null),
    Chats(R.string.history_filter_chats, ToolCategory.Understand),
    Solved(R.string.history_filter_solved, ToolCategory.Popular),
    Summaries(R.string.history_filter_summaries, ToolCategory.Summarize),
    Writing(R.string.history_filter_writing, ToolCategory.Write);

    fun matches(entry: HistoryEntry): Boolean =
        category == null || entry.tool.category == category
}

/** History groups by how recent an entry is, not by date, which is what the design labels. */
enum class HistoryGroup(@param:StringRes val labelRes: Int) {
    Today(R.string.history_group_today),
    Yesterday(R.string.history_group_yesterday),
    EarlierThisWeek(R.string.history_group_earlier),
    Older(R.string.history_group_older),
}

/**
 * One past conversation, as the History and Home screens list it. [detail] is built from what the
 * conversation contains — its message count and subject — rather than stored alongside it.
 */
data class HistoryEntry(
    val chatId: Long,
    val title: String,
    val detail: String,
    val tool: StudyTool,
    val updatedAt: Long,
    val group: HistoryGroup,
)
