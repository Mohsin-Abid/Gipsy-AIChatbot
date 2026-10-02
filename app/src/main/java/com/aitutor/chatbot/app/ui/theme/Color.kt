package com.aitutor.chatbot.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every colour in the app, in one place. Screens never hold a literal hex — they read a role off
 * [AppColors] via `MaterialTheme.appColors`, so a palette change happens here and nowhere else.
 *
 * Values are taken from the AI Study Dashboard design. The accent-derived tints are pre-computed
 * from the design's own formulas: light mixes the accent toward white, dark toward the page.
 */

// ---- Brand accent -------------------------------------------------------------------------
val Accent = Color(0xFF3446D1)
val AccentPressed = Color(0xFF2433A8)

/** The design offers these as alternates; kept so a future theme switch has somewhere to point. */
val AccentTeal = Color(0xFF1F6F8B)
val AccentViolet = Color(0xFF5B3FB8)
val AccentGreen = Color(0xFF0F7A6C)

// ---- Light --------------------------------------------------------------------------------
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceMuted = Color(0xFFF7F8FE)
val LightTextPrimary = Color(0xFF0F1222)
val LightTextSecondary = Color(0xFF545A6B)
val LightTextTertiary = Color(0xFF6B7080)
val LightAccentTint = Color(0xFFEBECFA)
val LightAccentRing = Color(0xFFD2D6F5)
val LightOnAccentMuted = Color(0xFFDFE2F8)
val LightCardBorder = Color(0x0F0F1222)
val LightPageTint = Color(0xFFF5F6FD)
val LightFieldBorder = Color(0xFFE7E9FA)
val LightRadioOff = Color(0xFFCDD1DC)
val LightAccentTintStrong = Color(0xFFE3E5F9)
val LightAccentDeep = Color(0xFF1B246D)
val LightSurfaceRaised = Color(0xFFFFFFFF)

// ---- Dark ---------------------------------------------------------------------------------
val DarkBackground = Color(0xFF0B0D17)
val DarkSurface = Color(0xFF151827)
val DarkSurfaceMuted = Color(0xFF151827)
val DarkTextPrimary = Color(0xFFF2F3F8)
val DarkTextSecondary = Color(0xFFA4A9BC)
val DarkTextTertiary = Color(0xFF8A90A6)
val DarkAccentText = Color(0xFF8F99E6)
/** Subtitles on the hero: cooler than the light theme's, since the hero itself is deepened. */
val DarkOnAccentMuted = Color(0xFFD3D7F3)
val DarkHero = Color(0xFF2A38A7)
val DarkAccentTint = Color(0x2E3446D1)
val DarkAccentRing = Color(0x573446D1)
val DarkCardBorder = Color(0x12FFFFFF)
val DarkPageTint = Color(0xFF0B0D17)
val DarkFieldBorder = Color(0x12FFFFFF)
val DarkRadioOff = Color(0x2EFFFFFF)
val DarkAccentTintStrong = Color(0x2E3446D1)
val DarkAccentDeep = Color(0xFF1A2368)
/** Dark mode has two card levels: flat cards sit on [DarkSurface], lifted ones on this. */
val DarkSurfaceRaised = Color(0xFF1B1F33)

// ---- Shared -------------------------------------------------------------------------------
val Coral = Color(0xFFFF6B5E)
val OnAccent = Color(0xFFFFFFFF)

// Destructive actions — "Log out" is the only one so far, and it is a tinted chip plus red label
// rather than a filled red button, so the role needs both a foreground and its wash.
val LightDanger = Color(0xFFC0352B)
val LightDangerTint = Color(0xFFFDECEC)
val DarkDanger = Color(0xFFFF7A70)
val DarkDangerTint = Color(0x24FF7A70)

// Chat sheets, menus and text selection.
/** The drag handle at the top of a bottom sheet. */
val LightSheetGrip = Color(0xFFD9DCE5)
val DarkSheetGrip = Color(0x33FFFFFF)
/** The floating bar above selected text. Near-black in light, a lifted slate in dark. */
val LightSelectionBar = Color(0xFF0F1222)
val DarkSelectionBar = Color(0xFF262B45)

/** What sits behind a sheet or a menu. One colour, two strengths — see [ScrimStrength]. */
val Scrim = Color(0xFF060814)

/** How opaque the scrim is: a full-width sheet dims more than a small anchored menu. */
object ScrimStrength {
    const val Sheet = 0.52f
    const val Menu = 0.28f
}

/** The live-recording dot. Same in both themes — it has to read as a warning, not as a tint. */
val Recording = Color(0xFFF0503C)

/** The off state of a switch track. Deliberately not [LightRadioOff]: a track reads heavier. */
val LightSwitchTrackOff = Color(0xFFD6D9E2)
val DarkSwitchTrackOff = Color(0xFF2E3350)

/** Translucent whites used on top of the accent hero, where the backdrop is the same in both themes. */
object OnHero {
    val Text = Color(0xFFFFFFFF)
    val Fill = Color(0x29FFFFFF)
    val FillSoft = Color(0x24FFFFFF)
    val FillFaint = Color(0x21FFFFFF)
    val Stroke = Color(0x33FFFFFF)
    /** Between [Stroke] and [StrokeSoft] — the hairline around the sign-in hero's pills. */
    val StrokeMedium = Color(0x2EFFFFFF)
    val StrokeSoft = Color(0x1FFFFFFF)
    val RingStrong = Color(0x1FFFFFFF)
    val RingSoft = Color(0x1AFFFFFF)
    /** The widest of the sign-in hero's concentric rings, barely there. */
    val RingFaint = Color(0x14FFFFFF)
}

/**
 * The Google sign-in button's colours, which are **not** this app's to choose.
 *
 * Google's sign-in branding guidelines fix the fill, the stroke and the label for both themes, and
 * a button drawn in anything else is off-brand. So these deliberately sit outside [AppColors]:
 * changing the app's accent must not touch them, and nothing but that one button may read them.
 *
 * Light and dark are picked by argument rather than by theme lookup, because this object has to
 * stay free of Compose — it is data, not a composition local.
 */
object GoogleBrand {
    fun fill(isDark: Boolean): Color = if (isDark) DarkFill else LightFill
    fun stroke(isDark: Boolean): Color = if (isDark) DarkStroke else LightStroke
    fun label(isDark: Boolean): Color = if (isDark) DarkLabel else LightLabel

    private val LightFill = Color(0xFFFFFFFF)
    private val LightStroke = Color(0xFF747775)
    private val LightLabel = Color(0xFF1F1F1F)

    private val DarkFill = Color(0xFF131314)
    private val DarkStroke = Color(0xFF8E918F)
    private val DarkLabel = Color(0xFFE3E3E3)
}
