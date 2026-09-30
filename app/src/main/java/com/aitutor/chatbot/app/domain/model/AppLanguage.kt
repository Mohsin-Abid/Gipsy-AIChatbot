package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import com.aitutor.chatbot.app.R

/**
 * The languages the app offers, in the order the design lists them.
 *
 * [nativeName] is deliberately a hardcoded literal rather than a string resource: it must always
 * render in its own script, whatever locale the app is currently running in. [englishNameRes] is
 * the translatable half — it's a description of the language, so it follows the user's locale.
 */
enum class AppLanguage(
    val tag: String,
    val nativeName: String,
    @param:StringRes val englishNameRes: Int,
) {
    English("en", "English", R.string.lang_english),
    Spanish("es", "Español", R.string.lang_spanish),
    French("fr", "Français", R.string.lang_french),
    German("de", "Deutsch", R.string.lang_german),
    Portuguese("pt", "Português", R.string.lang_portuguese),
    Turkish("tr", "Türkçe", R.string.lang_turkish),
    Indonesian("id", "Bahasa Indonesia", R.string.lang_indonesian),
    Hindi("hi", "हिन्दी", R.string.lang_hindi),
    Urdu("ur", "اردو", R.string.lang_urdu),
    Arabic("ar", "العربية", R.string.lang_arabic),
    Bengali("bn", "বাংলা", R.string.lang_bengali),
    Chinese("zh", "中文", R.string.lang_chinese);

    /** English is shown as the device default rather than repeating its own name twice. */
    val isDeviceDefault: Boolean get() = this == English
}
