package com.aitutor.chatbot.app.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Every screen carries this so Android Studio renders it in both themes at once, at the exact
 * frame the design was drawn on (390 × 844).
 */
@Preview(name = "Light", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(
    name = "Dark",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ScreenPreviews

/** For screens taller than one viewport — the design draws these at up to 1700dp. */
@Preview(name = "Light", widthDp = 390, heightDp = 1700, showBackground = true)
@Preview(
    name = "Dark",
    widthDp = 390,
    heightDp = 1700,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class TallScreenPreviews
