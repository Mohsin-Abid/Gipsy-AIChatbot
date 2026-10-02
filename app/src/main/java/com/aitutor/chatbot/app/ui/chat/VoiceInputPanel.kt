package com.aitutor.chatbot.app.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.components.DismissKeyboard
import com.aitutor.chatbot.app.ui.components.SheetGrip
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Recording
import com.aitutor.chatbot.app.ui.theme.bottomSafePadding
import com.aitutor.chatbot.app.ui.theme.appColors
import kotlin.math.abs
import kotlin.math.sin

private val PanelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private const val BAR_COUNT = 24

/** How much of each bar stays visible in silence, so the meter never collapses to a line. */
private const val SILENT_FLOOR = 0.12f

/**
 * Dictation. Unlike the other overlays this one has no scrim — the thread stays fully visible
 * behind it, because what is being dictated is a reply to what is on screen.
 */
@Composable
fun BoxScope.VoiceInputPanel(
    state: VoiceUiState?,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onToggleListening: () -> Unit,
) {
    val visible = state != null
    val colors = MaterialTheme.appColors
    // Dictation replaces typing, so the keyboard goes away rather than sitting under the panel.
    DismissKeyboard(whenVisible = visible)

    if (visible) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(24.dp, PanelShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(PanelShape)
                .background(colors.surface)
                .bottomSafePadding()
                .padding(start = Dimens.spaceXxl, end = Dimens.spaceXxl, top = Dimens.spaceMd, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetGrip()

            val panel = state ?: return@Column

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.spaceLg + 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                RecordingLabel(listening = panel.listening)
            }

            if (panel.errorRes != null) {
                Text(
                    text = stringResource(panel.errorRes),
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    color = colors.danger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                )
            } else {
                // The grey tail is what the recogniser has not committed to yet; it firms up into
                // the darker text as more audio arrives.
                Text(
                    text = buildAnnotatedString {
                        append(panel.transcript)
                        if (panel.transcript.isNotBlank() && panel.partial.isNotBlank()) append(" ")
                        withStyle(SpanStyle(color = colors.textTertiary)) { append(panel.partial) }
                        if (!panel.canAccept) {
                            withStyle(SpanStyle(color = colors.textTertiary)) {
                                append(stringResource(R.string.voice_prompt))
                            }
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                )
            }

            Waveform(
                level = panel.level,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(48.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundAction(
                    icon = AppIcons.Close,
                    contentDescription = stringResource(R.string.cd_cancel_voice),
                    onClick = onDismiss,
                )
                MicButton(listening = panel.listening, onClick = onToggleListening)
                RoundAction(
                    icon = AppIcons.Check,
                    contentDescription = stringResource(R.string.cd_accept_voice),
                    onClick = onAccept,
                    filled = true,
                    enabled = panel.canAccept,
                )
            }
        }
    }
}

@Composable
private fun RecordingLabel(listening: Boolean) {
    val colors = MaterialTheme.appColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(AppShapes.Pill)
                // The dot is the one thing saying whether the mic is actually open.
                .background(if (listening) Recording else colors.textTertiary)
        )
        Text(
            text = stringResource(if (listening) R.string.voice_listening else R.string.voice_paused),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

/**
 * The level meter, driven by the microphone.
 *
 * Each bar has a fixed share of the full height, so the profile stays the irregular shape the design
 * draws; [level] scales the whole thing. A floor keeps the bars visible at silence — a meter that
 * collapses to nothing reads as broken rather than quiet.
 */
@Composable
private fun Waveform(level: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Canvas(modifier = modifier) {
        val barWidth = 4.dp.toPx()
        val gap = 4.dp.toPx()
        val totalWidth = BAR_COUNT * barWidth + (BAR_COUNT - 1) * gap
        val startX = (size.width - totalWidth) / 2f
        val maxHeight = size.height

        repeat(BAR_COUNT) { index ->
            // A fixed, irregular profile, scaled by how loud the room actually is.
            val shape = abs(sin(index * 0.55f + (index * 37 % 11) / 11f * 2f))
            val wave = shape * (SILENT_FLOOR + (1f - SILENT_FLOOR) * level.coerceIn(0f, 1f))
            val height = (6.dp.toPx() + wave * (maxHeight - 6.dp.toPx())).coerceAtMost(maxHeight)
            drawRoundRect(
                color = colors.accent.copy(alpha = 0.44f + wave * 0.56f),
                topLeft = Offset(startX + index * (barWidth + gap), (maxHeight - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

/** The mic, ringed by its own halo. Tapping it holds and resumes listening. */
@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(brush = SolidColor(colors.accentTint), radius = size.minDimension / 2f)
        }
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(12.dp, AppShapes.Pill, ambientColor = colors.accent, spotColor = colors.accent)
                .clip(AppShapes.Pill)
                .background(colors.accent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Mic,
                contentDescription = stringResource(
                    if (listening) R.string.cd_pause_listening else R.string.cd_resume_listening
                ),
                tint = colors.onAccent,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(AppShapes.Pill)
            .background(
                when {
                    // Dimmed rather than hidden: it keeps its place in the row.
                    filled && !enabled -> colors.accent.copy(alpha = 0.4f)
                    filled -> colors.accent
                    else -> colors.accentTint
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (filled) colors.onAccent else colors.accentText,
            modifier = Modifier.size(if (filled) 24.dp else 22.dp),
        )
    }
}
