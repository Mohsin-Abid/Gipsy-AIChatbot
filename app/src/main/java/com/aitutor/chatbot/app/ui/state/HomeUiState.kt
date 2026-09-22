package com.aitutor.chatbot.app.ui.state

import com.aitutor.chatbot.app.model.ChatEntry
import com.aitutor.chatbot.app.model.StudyTool

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Error(val message: String) : HomeUiState

    /**
     * [recentHistory] empty is "partial data," not a failure — a new
     * student has tools to explore but nothing to resume yet, so Home
     * shows a quiet prompt in that section instead of hiding it.
     */
    data class Success(
        val greeting: String,
        val quickTools: List<StudyTool>,
        val recentHistory: List<ChatEntry>
    ) : HomeUiState
}
