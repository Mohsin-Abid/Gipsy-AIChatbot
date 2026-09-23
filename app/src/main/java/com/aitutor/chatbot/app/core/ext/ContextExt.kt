package com.aitutor.chatbot.app.core.ext

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

fun Context.toast(message: String, longDuration: Boolean = false) {
    Toast.makeText(this, message, if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Context.toast(@StringRes messageRes: Int, longDuration: Boolean = false) {
    Toast.makeText(this, messageRes, if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

/** Applies a per-app locale (e.g. "ur", "ur-Latn", "ar") without touching the device's system language. */
fun Context.applyAppLocale(languageTag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
}

fun Context.currentAppLocaleTag(): String? =
    AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }
