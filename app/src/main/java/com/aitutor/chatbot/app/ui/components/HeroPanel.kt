package com.aitutor.chatbot.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.AppInsets
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.appColors

/**
 * The accent panel every onboarding page is built on: a soft glow off the top-right and two
 * oversized hairline rings running out of the bottom-left corner. They are drawn rather than
 * layered as views so they can bleed past the panel's edge and be clipped by its radius.
 */
@Composable
fun OnboardingHero(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Taller by the status bar so the panel reaches the display's top edge, rather than the
            // artwork inside it losing that much room.
            .height(Dimens.heroHeight + AppInsets.top)
            .clip(AppShapes.HeroPanel)
            .background(MaterialTheme.appColors.hero),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width + 50.dp.toPx(), -(140.dp.toPx()) + 170.dp.toPx()),
                    radius = 170.dp.toPx(),
                ),
                radius = 170.dp.toPx(),
                center = Offset(size.width + 50.dp.toPx(), 30.dp.toPx()),
            )
            val hairline = Stroke(width = 1.dp.toPx())
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = 200.dp.toPx(),
                center = Offset(30.dp.toPx(), size.height + 30.dp.toPx()),
                style = hairline,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = 140.dp.toPx(),
                center = Offset(30.dp.toPx(), size.height + 30.dp.toPx()),
                style = hairline,
            )
        }
        // Drawn art bleeds to the panel's edges; everything readable sits below the status bar.
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            content()
        }
    }
}
