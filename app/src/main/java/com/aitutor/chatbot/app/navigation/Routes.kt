package com.aitutor.chatbot.app.navigation

import kotlinx.serialization.Serializable

/** Type-safe Navigation Compose routes. */
sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object Onboarding : Route
    @Serializable data class LanguageSelect(val fromSettings: Boolean = false) : Route
    /** The 3-tab shell: Home (tools), Chat History, Settings. */
    @Serializable data object Main : Route
    @Serializable data class ChatDetail(val modeId: String, val chatId: String? = null) : Route
    @Serializable data object Premium : Route
}
