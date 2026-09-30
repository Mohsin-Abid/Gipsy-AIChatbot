package com.aitutor.chatbot.app.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.ScannedText
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.appColors

internal val BubbleMaxWidth = 272.dp
private val AttachmentShape = RoundedCornerShape(16.dp)
private val ShadowTint = Color(0xFF080C28)

/** The 30dp spark that attributes an answer to the tutor. */
@Composable
internal fun AssistantAvatar(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Spark,
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(15.dp),
        )
    }
}

/** The centred day separator. */
@Composable
internal fun DayMarker(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.appColors.textTertiary,
        modifier = modifier,
    )
}

/** The student's own message. Flat accent, and the corner nearest them is the flattened one. */
@Composable
internal fun UserBubble(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        color = colors.onAccent,
        modifier = modifier
            .widthIn(max = BubbleMaxWidth)
            .clip(AppShapes.BubbleUser)
            .background(colors.accent)
            .padding(horizontal = Dimens.spaceLg + 2.dp, vertical = Dimens.spaceMd),
    )
}

/** The chip above a question, naming what was attached and how much of it there is. */
@Composable
internal fun AttachmentChip(attachment: ScannedText, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .clip(AttachmentShape)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, AttachmentShape)
            .padding(start = Dimens.spaceSm, end = Dimens.spaceLg, top = Dimens.spaceSm, bottom = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.ScanText,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(17.dp),
            )
        }
        Column {
            Text(
                text = stringResource(R.string.chat_attachment_title),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(
                    R.string.chat_attachment_detail,
                    stringResource(attachment.source.labelRes),
                    attachment.wordCount,
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.textTertiary,
            )
        }
    }
}

/**
 * The tutor's answer. Bullets set their term in bold and the rest in the secondary grey, which is
 * the whole reason the answer is modelled as blocks rather than as one string.
 */
@Composable
internal fun AssistantBubble(
    blocks: List<AnswerBlock>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, AppShapes.BubbleAi, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(AppShapes.BubbleAi)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, AppShapes.BubbleAi)
            .padding(horizontal = Dimens.spaceLg + 2.dp, vertical = Dimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is AnswerBlock.Paragraph -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                    color = colors.textPrimary,
                )
                is AnswerBlock.Bullet -> BulletLine(term = block.term, text = block.text)
            }
        }
    }
}

@Composable
private fun BulletLine(term: String, text: String) {
    val colors = MaterialTheme.appColors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .padding(top = Dimens.spaceSm)
                .size(6.dp)
                .clip(AppShapes.Pill)
                .background(colors.accent)
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary)) {
                    append(term)
                }
                append(" ")
                append(text)
            },
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
            color = colors.textSecondary,
        )
    }
}

/** The row of quick actions under the newest answer, ending in the "More" pill. */
@Composable
internal fun AnswerActions(
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier.offset(x = (-6).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AnswerAction(AppIcons.Copy, stringResource(R.string.chat_action_copy))
        AnswerAction(AppIcons.SpeakerWave, stringResource(R.string.chat_action_read_aloud))
        AnswerAction(AppIcons.ThumbUp, stringResource(R.string.chat_action_helpful))
        AnswerAction(AppIcons.ThumbDown, stringResource(R.string.chat_action_not_helpful))
        AnswerAction(AppIcons.Refresh, stringResource(R.string.chat_action_regenerate))
        Row(
            modifier = Modifier
                .padding(start = Dimens.spaceXxs)
                .height(30.dp)
                .clip(AppShapes.Pill)
                .background(colors.accentTint)
                .clickable(onClick = onMore)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
        ) {
            Icon(
                imageVector = AppIcons.DotsHorizontal,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.chat_action_more),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.accentText,
            )
        }
    }
}

@Composable
private fun AnswerAction(icon: ImageVector, contentDescription: String) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(AppShapes.Pill)
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.textTertiary,
            modifier = Modifier.size(17.dp),
        )
    }
}

/**
 * The three dots shown while a reply is being generated. The design fixes each dot at a different
 * opacity; here that pattern travels along the row, so the wait reads as progress.
 */
@Composable
internal fun TypingIndicator(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val transition = rememberInfiniteTransition(label = "typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = Motion.Standard),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typingPhase",
    )

    Row(
        modifier = modifier
            .height(36.dp)
            .clip(AppShapes.Pill)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, AppShapes.Pill)
            .padding(horizontal = Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(3) { index ->
            // Distance from the travelling highlight, wrapped so dot 0 follows dot 2.
            val distance = ((phase - index + 3f) % 3f).let { minOf(it, 3f - it) }
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(1f - distance * 0.35f)
                    .clip(AppShapes.Pill)
                    .background(colors.accentText)
            )
        }
    }
}
