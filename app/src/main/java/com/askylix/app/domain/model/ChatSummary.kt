package com.askylix.app.domain.model

/** A conversation in a list: enough to draw a row and to open it. The messages are fetched when it opens. */
data class ChatSummary(
    val id: String,
    val tool: StudyTool,
    val title: String,
    /** When it was last used, in epoch milliseconds. */
    val lastMessageAt: Long,
)
