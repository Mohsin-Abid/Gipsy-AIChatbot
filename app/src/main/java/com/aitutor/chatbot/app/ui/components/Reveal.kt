package com.aitutor.chatbot.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * Fades content in while lifting it a little. Staggering a few of these down a page is what makes
 * a screen feel like it arrived rather than blinked into existence.
 *
 * [visible] lets a caller re-run the entrance — a pager page replays it each time it becomes
 * current, so swiping forward and back always feels alive.
 */
@Composable
fun Reveal(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    lift: Dp = 24.dp,
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis.toLong())
            progress.animateTo(1f, Motion.emphasized(durationMillis = Motion.Reveal))
        } else {
            progress.snapTo(0f)
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * lift.toPx()
        }
    ) {
        content()
    }
}
