package com.aitutor.chatbot.app.ui.state

import com.aitutor.chatbot.app.model.StudentProfile

data class SettingsPreferences(
    val darkMode: Boolean,
    val notifications: Boolean,
    val language: String
)

sealed interface SettingsUiState {

    data object Loading : SettingsUiState

    data class Success(
        val profile: StudentProfile,
        val preferences: SettingsPreferences
    ) : SettingsUiState
}
