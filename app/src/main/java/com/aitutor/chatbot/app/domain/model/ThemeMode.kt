package com.aitutor.chatbot.app.domain.model

enum class ThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: System
    }
}

enum class ExplanationLevel(val label: String) {
    Simple("Simple"),
    Standard("Standard"),
    Advanced("Advanced");

    companion object {
        fun fromName(name: String?): ExplanationLevel = entries.firstOrNull { it.name == name } ?: Standard
    }
}
