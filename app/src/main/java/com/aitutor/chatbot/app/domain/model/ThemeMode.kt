package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.icons.AppIcons

/**
 * How the app decides between its light and dark palettes.
 *
 * Three states rather than a switch: a two-way toggle cannot say "follow the phone", so once it is
 * touched the app is pinned to one palette forever and stops tracking the device's night schedule.
 * [System] is the default, and it is a real choice a reader can come back to.
 */
enum class ThemeMode(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Light(R.string.theme_light, AppIcons.Sun),
    Dark(R.string.theme_dark, AppIcons.Moon),
    System(R.string.theme_system, AppIcons.Contrast);

    /** Resolves to a palette. [systemInDark] is what the device currently reports. */
    fun isDark(systemInDark: Boolean): Boolean = when (this) {
        Light -> false
        Dark -> true
        System -> systemInDark
    }

    companion object {
        val Default = System

        /** Reads a stored name back, falling back rather than failing on an unknown value. */
        fun fromNameOrDefault(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
