package com.aitutor.chatbot.app.core.ext

import android.util.Patterns

fun String.isValidEmail(): Boolean =
    isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

/** Collapses runs of whitespace (including newlines from OCR/paste) into single spaces and trims. */
fun String.cleanWhitespace(): String = trim().replace(Regex("\\s+"), " ")

/** Truncates for compact display (chat titles, row subtitles), never splitting mid-word where avoidable. */
fun String.truncate(maxLength: Int, ellipsis: String = "…"): String {
    if (length <= maxLength) return this
    val cut = substring(0, maxLength).trimEnd()
    val lastSpace = cut.lastIndexOf(' ')
    val safeCut = if (lastSpace > maxLength / 2) cut.substring(0, lastSpace) else cut
    return safeCut + ellipsis
}
