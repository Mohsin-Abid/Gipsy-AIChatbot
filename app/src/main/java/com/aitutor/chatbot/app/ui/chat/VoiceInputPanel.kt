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

/**
 * Dictation. Unlike the other overlays this one has no scrim — the thread stays fully visible
 * behind it, because what is being dictated is a reply to what is on screen.
 */
@Composable
fun BoxScope.VoiceInputPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.spaceLg + 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RecordingLabel()
                LanguageChip()
            }

            // The grey tail is what has not been confirmed yet — a real recogniser returns it as
            // a partial result, and it firms up as more audio arrives.
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.voice_transcript_final))
                    append(" ")
                    withStyle(SpanStyle(color = colors.textTertiary)) {
                        append(stringResource(R.string.voice_transcript_partial))
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

            Waveform(
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
                MicButton()
                RoundAction(
                    icon = AppIcons.Check,
                    contentDescription = stringResource(R.string.cd_accept_voice),
                    onClick = onAccept,
                    filled = true,
                )
            }
        }
    }
}

@Composable
private fun RecordingLabel() {
    val colors = MaterialTheme.appColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(AppShapes.Pill)
                .background(Recording)
        )
        Text(
            text = stringResource(R.string.voice_listening),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun LanguageChip() {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(AppShapes.Pill)
            .border(1.dp, colors.cardBorder, AppShapes.Pill)
            .clickable { }
            .padding(horizontal = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        Text(
            text = stringResource(R.string.lang_english),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.textSecondary,
        )
        Icon(
            imageVector = AppIcons.ChevronDown,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(13.dp),
        )
    }
}

/**
 * The level meter, as the design draws it: one frozen frame. It is decorative until a recogniser
 * feeds it real amplitudes, which is why it takes no input.
 */
@Composable
private fun Waveform(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Canvas(modifier = modifier) {
        val barWidth = 4.dp.toPx()
        val gap = 4.dp.toPx()
        val totalWidth = BAR_COUNT * barWidth + (BAR_COUNT - 1) * gap
        val startX = (size.width - totalWidth) / 2f
        val maxHeight = size.height

        repeat(BAR_COUNT) { index ->
            // A fixed, irregular profile — the same shape every time the panel opens.
            val wave = abs(sin(index * 0.55f + (index * 37 % 11) / 11f * 2f))
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

/** The mic, ringed by its own halo. */
@Composable
private fun MicButton() {
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
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Mic,
                contentDescription = stringResource(R.string.cd_pause_listening),
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
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(AppShapes.Pill)
            .background(if (filled) colors.accent else colors.accentTint)
            .clickable(onClick = onClick),
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
