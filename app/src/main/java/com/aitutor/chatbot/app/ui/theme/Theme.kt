package com.aitutor.chatbot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The design's colour roles. Material 3's scheme has no slot for "the tint behind an accent chip"
 * or "the secondary body grey", and inventing mappings for them makes screens read as riddles —
 * so those roles live here, named for their job, and both themes fill in the same set.
 */
@Immutable
data class AppColors(
    val accent: Color,
    val accentPressed: Color,
    val accentText: Color,
    val accentTint: Color,
    val accentRing: Color,
    val accentTintStrong: Color,
    /** The darkened accent used for the nav bar and the second featured tool card. */
    val accentDeep: Color,
    val hero: Color,
    val onAccent: Color,
    val onAccentMuted: Color,
    val background: Color,
    val surface: Color,
    /** For cards that sit above the page — the nav bar, the "continue" card. */
    val surfaceRaised: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val cardBorder: Color,
    /** The very faint accent wash some screens use instead of flat white. */
    val pageTint: Color,
    val fieldBorder: Color,
    val radioOff: Color,
    /** The off state of a switch track — heavier than [radioOff], which outlines an empty circle. */
    val switchTrackOff: Color,
    /** Destructive labels and icons, with [dangerTint] as the wash behind them. */
    val danger: Color,
    val dangerTint: Color,
    /** The drag handle on a bottom sheet. */
    val sheetGrip: Color,
    /** The floating action bar over selected text, and the white-on-dark text it carries. */
    val selectionBar: Color,
    /** The wash behind selected text, at the strength each theme needs to stay legible. */
    val selectionHighlight: Color,
    val isDark: Boolean,
)

private val LightAppColors = AppColors(
    accent = Accent,
    accentPressed = AccentPressed,
    accentText = Accent,
    accentTint = LightAccentTint,
    accentRing = LightAccentRing,
    accentTintStrong = LightAccentTintStrong,
    accentDeep = LightAccentDeep,
    hero = Accent,
    onAccent = OnAccent,
    onAccentMuted = LightOnAccentMuted,
    background = LightBackground,
    surface = LightSurface,
    surfaceRaised = LightSurfaceRaised,
    surfaceMuted = LightSurfaceMuted,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    cardBorder = LightCardBorder,
    pageTint = LightPageTint,
    fieldBorder = LightFieldBorder,
    radioOff = LightRadioOff,
    switchTrackOff = LightSwitchTrackOff,
    danger = LightDanger,
    dangerTint = LightDangerTint,
    sheetGrip = LightSheetGrip,
    selectionBar = LightSelectionBar,
    selectionHighlight = Accent.copy(alpha = 0.20f),
    isDark = false,
)

private val DarkAppColors = AppColors(
    accent = Accent,
    accentPressed = AccentPressed,
    // On a near-black page the raw accent is too dark to read as text, so the design lightens it.
    accentText = DarkAccentText,
    accentTint = DarkAccentTint,
    accentRing = DarkAccentRing,
    accentTintStrong = DarkAccentTintStrong,
    accentDeep = DarkAccentDeep,
    // The hero panel is deepened rather than kept at full chroma, so it sits in the page.
    hero = DarkHero,
    onAccent = OnAccent,
    onAccentMuted = DarkOnAccentMuted,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceRaised = DarkSurfaceRaised,
    surfaceMuted = DarkSurfaceMuted,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    cardBorder = DarkCardBorder,
    pageTint = DarkPageTint,
    fieldBorder = DarkFieldBorder,
    radioOff = DarkRadioOff,
    switchTrackOff = DarkSwitchTrackOff,
    danger = DarkDanger,
    dangerTint = DarkDangerTint,
    sheetGrip = DarkSheetGrip,
    selectionBar = DarkSelectionBar,
    // A dark page swallows a 20% wash, so the highlight is more than twice as strong here.
    selectionHighlight = Accent.copy(alpha = 0.45f),
    isDark = true,
)

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = LightAccentTint,
    onPrimaryContainer = Accent,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceMuted,
    onSurfaceVariant = LightTextSecondary,
    outline = LightTextTertiary,
    outlineVariant = LightCardBorder,
    error = Coral,
)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = DarkAccentTint,
    onPrimaryContainer = DarkAccentText,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkTextTertiary,
    outlineVariant = DarkCardBorder,
    error = Coral,
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** The design's colour roles for the active theme: `MaterialTheme.appColors.accentTint`. */
val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

@Composable
fun AITutorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}
