package com.aitutor.chatbot.app.ui.settings

import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.ExplanationLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.ThemeMode

data class SettingsData(
    val language: AppLanguage,
    val themeMode: ThemeMode,
    val explanationLevel: ExplanationLevel,
    val fontScale: Float,
    val notificationsEnabled: Boolean,
    val isPremium: Boolean,
    val profile: StudentProfile,
)
