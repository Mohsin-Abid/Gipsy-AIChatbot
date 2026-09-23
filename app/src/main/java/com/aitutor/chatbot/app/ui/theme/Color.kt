package com.aitutor.chatbot.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Minimal black/white/grey palette per the design spec — no dynamic color.
 * One restrained accent is spent only on CTAs, selected states, and active
 * mode cards. Category tints on the Home mode grid are a separate, muted
 * use of color for at-a-glance scanning, not the CTA accent.
 */

// ---- Light ----
val LightBackground = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF5F5F5)
val LightBorder = Color(0xFFEEEEEE)

val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF6B7280)
val LightTextTertiary = Color(0xFF9CA3AF)

val LightPrimary = Color(0xFF2856E0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE4EAFC)
val LightOnPrimaryContainer = Color(0xFF1A3FBF)

val LightError = Color(0xFFDC2626)
val LightErrorContainer = Color(0xFFFDECEC)

// ---- Dark ----
val DarkBackground = Color(0xFF121212)
val DarkSurfaceVariant = Color(0xFF1E1E1E)
val DarkBorder = Color(0xFF2C2C2C)

val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFA0A3AA)
val DarkTextTertiary = Color(0xFF74777E)

val DarkPrimary = Color(0xFF6D93FF)
val DarkOnPrimary = Color(0xFF0B1636)
val DarkPrimaryContainer = Color(0xFF22345E)
val DarkOnPrimaryContainer = Color(0xFFB9CCFF)

val DarkError = Color(0xFFF87171)
val DarkErrorContainer = Color(0xFF3A1F1F)

// ---- Mode-category tints (Home grid icon accents only — muted, not CTA color) ----
val CategoryChatTint = Color(0xFF2856E0)
val CategoryProblemSolvingTint = Color(0xFF9333EA)
val CategoryWritingTint = Color(0xFFD97706)
val CategoryStudyAidsTint = Color(0xFF059669)

/**
 * Premium signifiers only — PRO badges, the upgrade surfaces, the plan row in Settings. Kept apart
 * from the CTA accent so "this costs money" never reads the same as "this is the next action".
 */
val PremiumGold = Color(0xFFB8860B)
val PremiumGoldBright = Color(0xFFE8B931)
val PremiumGoldSoft = Color(0xFFFBF1D8)
val PremiumInkStart = Color(0xFF1B1D26)
val PremiumInkEnd = Color(0xFF32243A)

// ---- Hero gradient (the one large "ask anything" surface on the dashboard) ----
val HeroGradientStart = Color(0xFF2856E0)
val HeroGradientEnd = Color(0xFF6D3BD4)
val HeroGradientStartDark = Color(0xFF2B3C7A)
val HeroGradientEndDark = Color(0xFF442C6B)
