package com.aitutor.chatbot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.elevatedCardStyle
import com.aitutor.chatbot.app.ui.theme.HeroGradientEnd
import com.aitutor.chatbot.app.ui.theme.HeroGradientEndDark
import com.aitutor.chatbot.app.ui.theme.HeroGradientStart
import com.aitutor.chatbot.app.ui.theme.HeroGradientStartDark

/**
 * The brand gradient, resolved for the active theme. Every gradient surface in the first-run flow
 * goes through here so none of them can end up showing the light values on a dark page.
 */
@Composable
fun heroBrush(): Brush {
    val dark = isSystemInDarkTheme()
    return Brush.linearGradient(
        colors = if (dark) {
            listOf(HeroGradientStartDark, HeroGradientEndDark)
        } else {
            listOf(HeroGradientStart, HeroGradientEnd)
        }
    )
}

/**
 * Lighting for a gradient surface — a highlight off the top-left, a deepening toward the
 * bottom-right, and two hairline orbit rings. A flat two-stop gradient reads like a placeholder;
 * this is what makes the same colors look like a lit object. Applied after `.background()`, so it
 * paints over the gradient but still underneath the panel's own content.
 */
fun Modifier.heroSheen(rings: Boolean = true): Modifier = this.drawWithCache {
    val highlight = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
        center = Offset(size.width * 0.16f, size.height * 0.06f),
        radius = size.maxDimension * 0.85f,
    )
    val shade = Brush.radialGradient(
        colors = listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent),
        center = Offset(size.width * 1.02f, size.height * 1.10f),
        radius = size.maxDimension * 0.95f,
    )
    val ring = Color.White.copy(alpha = 0.10f)
    val hairline = Stroke(width = 1.dp.toPx())
    onDrawBehind {
        drawRect(shade)
        drawRect(highlight)
        if (rings) {
            drawCircle(
                color = ring,
                radius = size.minDimension * 0.60f,
                center = Offset(size.width * 0.86f, size.height * 0.14f),
                style = hairline,
            )
            drawCircle(
                color = ring,
                radius = size.minDimension * 0.92f,
                center = Offset(size.width * 0.10f, size.height * 0.98f),
                style = hairline,
            )
        }
    }
}

/**
 * A wide, very low-opacity wash of the accent behind an otherwise empty screen. Keeps the splash
 * from reading as a blank page without putting any actual color on it.
 */
@Composable
fun ambientGlowBrush(alpha: Float = 0.10f): Brush {
    val dark = isSystemInDarkTheme()
    val tint = if (dark) HeroGradientStartDark else HeroGradientStart
    return Brush.radialGradient(
        colors = listOf(tint.copy(alpha = alpha), Color.Transparent),
        radius = 900f,
    )
}

/** The app mark: one object, used at every size, so the splash and the store icon agree. */
@Composable
fun AppLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    elevation: Dp = 18.dp,
) {
    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier = modifier
            .size(size)
            .elevatedCardStyle(shape = shape, elevation = elevation)
            .clip(shape)
            .background(heroBrush())
            .heroSheen(rings = false),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.70f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}
