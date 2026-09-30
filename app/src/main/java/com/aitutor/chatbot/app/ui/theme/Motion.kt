package com.aitutor.chatbot.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * One motion vocabulary for the whole app, so nothing has to invent a duration.
 *
 * [Emphasized] is the decelerating curve things use when they arrive on screen — fast out of the
 * gate, settling gently. [Standard] handles state changes that are already on screen, where a
 * showy curve would just look fussy.
 */
object Motion {
    const val Fast = 180
    const val Medium = 300
    const val Slow = 480
    const val Reveal = 520

    /** Stagger between siblings entering together. Long enough to read, short enough not to wait. */
    const val Stagger = 70

    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard: Easing = FastOutSlowInEasing

    fun <T> fast() = tween<T>(durationMillis = Fast, easing = Standard)
    fun <T> medium() = tween<T>(durationMillis = Medium, easing = Standard)
    fun <T> emphasized(durationMillis: Int = Slow, delayMillis: Int = 0) =
        tween<T>(durationMillis = durationMillis, delayMillis = delayMillis, easing = Emphasized)
}
