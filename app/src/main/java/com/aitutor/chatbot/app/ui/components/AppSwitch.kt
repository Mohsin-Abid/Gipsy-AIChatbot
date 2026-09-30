package com.aitutor.chatbot.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.appColors

/**
 * The design's switch, rather than Material's — the track is the accent at full chroma and the
 * thumb is plain white, which Material's palette can't express without fighting its own tonal
 * rules. Both the track colour and the thumb's travel animate, so a toggle reads as movement.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 50.dp,
    height: Dp = 30.dp,
) {
    val colors = MaterialTheme.appColors
    val inset = 3.dp
    val thumbSize = height - inset * 2
    val track by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.switchTrackOff,
        animationSpec = Motion.medium(),
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - inset else inset,
        animationSpec = Motion.emphasized(durationMillis = Motion.Medium),
        label = "switchThumb",
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(AppShapes.Pill)
            .background(track)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(AppShapes.Pill)
                .background(Color.White)
        )
    }
}
