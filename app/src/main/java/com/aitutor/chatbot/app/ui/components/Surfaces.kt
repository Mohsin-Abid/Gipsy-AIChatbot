package com.aitutor.chatbot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.appColors

/**
 * The design's one card treatment: a hairline border plus a long, soft drop shadow. The shadow is
 * what lifts these cards off the accent hero — without it they read as flat cut-outs.
 */
@Composable
fun Modifier.designCard(
    shape: Shape = AppShapes.CardLarge,
    elevation: Dp = 14.dp,
    background: Color = MaterialTheme.appColors.surface,
): Modifier {
    val colors = MaterialTheme.appColors
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (colors.isDark) Color.Black else Color(0xFF080C28),
            spotColor = if (colors.isDark) Color.Black else Color(0xFF080C28),
        )
        .background(background, shape)
        .border(1.dp, colors.cardBorder, shape)
}
