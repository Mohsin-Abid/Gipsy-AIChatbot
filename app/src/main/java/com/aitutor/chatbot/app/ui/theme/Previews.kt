package com.aitutor.chatbot.app.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Light + dark, phone width. The default pair for any screen's primary
 * (Success) state.
 */
@Preview(
    name = "Light",
    group = "Theme",
    showBackground = true,
    backgroundColor = 0xFFFAF6EE,
    device = "spec:width=390dp,height=844dp"
)
@Preview(
    name = "Dark",
    group = "Theme",
    showBackground = true,
    backgroundColor = 0xFF131F1B,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=390dp,height=844dp"
)
annotation class ThemePreviews

/**
 * Normal vs. 200% font scale, light mode. Applied to a screen's primary
 * state to confirm nothing clips or overlaps under accessibility text
 * sizing — kept separate from [ThemePreviews] rather than cross-multiplied
 * with dark mode, which nobody actually reads.
 */
@Preview(
    name = "Text 100%",
    group = "Font scale",
    showBackground = true,
    backgroundColor = 0xFFFAF6EE,
    device = "spec:width=390dp,height=844dp"
)
@Preview(
    name = "Text 200%",
    group = "Font scale",
    showBackground = true,
    backgroundColor = 0xFFFAF6EE,
    fontScale = 2f,
    device = "spec:width=390dp,height=844dp"
)
annotation class FontScalePreviews

/** Same as [ThemePreviews] but sized for a small, component-level preview. */
@Preview(
    name = "Light",
    group = "Theme",
    showBackground = true,
    backgroundColor = 0xFFFAF6EE,
    widthDp = 360
)
@Preview(
    name = "Dark",
    group = "Theme",
    showBackground = true,
    backgroundColor = 0xFF131F1B,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 360
)
annotation class ComponentPreviews
