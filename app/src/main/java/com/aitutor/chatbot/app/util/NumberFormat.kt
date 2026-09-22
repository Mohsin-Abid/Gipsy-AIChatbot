package com.aitutor.chatbot.app.util

import java.text.NumberFormat
import java.util.Locale

/**
 * "12,480" below 100,000; "128.4K" at or above it. Callers should still put
 * the full, ungrouped value in a contentDescription for TalkBack.
 */
fun formatCompactCount(value: Int): String {
    val locale = Locale.getDefault()
    return if (value < 100_000) {
        NumberFormat.getIntegerInstance(locale).format(value)
    } else {
        String.format(locale, "%.1fK", value / 1000.0)
    }
}

fun formatGroupedCount(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
