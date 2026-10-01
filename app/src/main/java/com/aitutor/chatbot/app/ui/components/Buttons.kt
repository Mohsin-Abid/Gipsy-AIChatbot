package com.aitutor.chatbot.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.appColors

/** The full-width accent pill — the app's one primary action treatment. */
@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingArrow: Boolean = true,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val fill = if (enabled) colors.accent else colors.accentRing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ctaHeight)
            .clip(AppShapes.Button)
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Dimens.spaceXxl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = colors.onAccent)
        if (trailingArrow) {
            Icon(
                imageVector = AppIcons.ArrowRight,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.padding(start = 10.dp).size(18.dp),
            )
        }
    }
}

/**
 * The circular "next" control: an accent disc inside a ring that fills as the flow advances, so
 * progress and the action are the same object rather than two competing ones.
 */
@Composable
fun CircularNextButton(
    progress: Float,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .size(Dimens.nextButton)
            .clip(AppShapes.Pill)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            val inset = 3.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = colors.accentRing,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        Box(
            modifier = Modifier
                .size(Dimens.nextButtonInner)
                .clip(AppShapes.Pill)
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.ArrowRight,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
