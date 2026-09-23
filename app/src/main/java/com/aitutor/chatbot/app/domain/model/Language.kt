package com.aitutor.chatbot.app.domain.model

/** [languageTag] is a BCP-47 tag applied via the AndroidX per-app language API. */
enum class AppLanguage(val languageTag: String, val nativeLabel: String, val englishLabel: String) {
    English(languageTag = "en", nativeLabel = "English", englishLabel = "English"),
    Urdu(languageTag = "ur", nativeLabel = "اردو", englishLabel = "Urdu"),
    RomanUrdu(languageTag = "ur-Latn", nativeLabel = "Roman Urdu", englishLabel = "Roman Urdu"),
    Arabic(languageTag = "ar", nativeLabel = "العربية", englishLabel = "Arabic"),
    Hindi(languageTag = "hi", nativeLabel = "हिन्दी", englishLabel = "Hindi");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.languageTag == tag } ?: English
    }
}
