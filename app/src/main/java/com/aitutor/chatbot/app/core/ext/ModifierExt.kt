package com.aitutor.chatbot.app.core.ext

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.AppShapes

/** The app's one recurring bordered-card look, applied as a modifier instead of copy-pasted per screen. */
fun Modifier.cardStyle(shape: Shape = AppShapes.Card): Modifier = composed {
    this
        .background(MaterialTheme.colorScheme.background, shape)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
}

/**
 * The raised variant, for the few surfaces that should sit above the page rather than sit in it —
 * the dashboard hero and the upgrade card. Shadow is deliberately soft and low-contrast so the
 * minimal palette still reads flat everywhere else.
 */
fun Modifier.elevatedCardStyle(shape: Shape = AppShapes.CardLarge, elevation: Dp = 10.dp): Modifier = composed {
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
        spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
    )
}

/** A quiet alpha-pulse "this is loading" background, for any element standing in for real content. */
fun Modifier.shimmerLoading(shape: Shape = AppShapes.Row): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerLoading")
    val alpha by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerLoadingAlpha"
    )
    this.background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), shape)
}
