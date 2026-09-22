package com.aitutor.chatbot.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.spacing

/**
 * A slow, quiet alpha pulse — not a sweeping shimmer gradient — because the
 * only job here is to say "this is loading," not to entertain.
 */
@Composable
private fun skeletonAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    return alpha
}

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    val alpha = skeletonAlpha()
    Box(
        modifier = modifier
            .height(height)
            .background(color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), shape = shape)
    )
}

/** A title + description shaped placeholder, matching [ChalkRow]'s rhythm. */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.md)
            .semantics { contentDescription = "Loading" },
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.55f))
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.8f), height = 12.dp)
    }
}
