package com.aitutor.chatbot.app.ui.state

import com.aitutor.chatbot.app.model.StudyTool

sealed interface ToolsUiState {

    data object Loading : ToolsUiState

    data class Success(val tools: List<StudyTool>) : ToolsUiState
}
