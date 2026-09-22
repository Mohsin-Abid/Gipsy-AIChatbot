package com.aitutor.chatbot.app.ui.state

import com.aitutor.chatbot.app.model.ChatEntry
import com.aitutor.chatbot.app.model.ToolCategory

sealed interface ChatHistoryUiState {

    data object Loading : ChatHistoryUiState

    data class Error(val message: String) : ChatHistoryUiState

    /** Distinguishes "nothing yet" from "nothing matches this tab." */
    data class Empty(val title: String, val message: String) : ChatHistoryUiState

    data class Success(val entries: List<ChatEntry>) : ChatHistoryUiState
}

/** `null` is the "All" tab; every other entry is a real [ToolCategory]. */
val ChatHistoryTabs: List<ToolCategory?> = listOf(null) + ToolCategory.entries

fun ToolCategory?.tabLabel(): String = this?.label ?: "All"
