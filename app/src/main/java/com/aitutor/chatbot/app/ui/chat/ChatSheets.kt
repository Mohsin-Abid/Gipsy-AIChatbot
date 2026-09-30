package com.aitutor.chatbot.app.ui.chat

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.ScanSource
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.ui.components.AnchoredMenu
import com.aitutor.chatbot.app.ui.components.BottomSheet
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.appColors

private val TileShape = RoundedCornerShape(20.dp)
private val PanelShape = RoundedCornerShape(20.dp)
private val GlyphShape = RoundedCornerShape(17.dp)
private val NoteShape = RoundedCornerShape(14.dp)

/**
 * Where to pull text from. The tiles are sources, not file pickers — whichever is chosen, only the
 * extracted text becomes the question, which is what the footer promises.
 */
@Composable
fun BoxScope.ScanSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPick: (ScanSource) -> Unit,
) {
    val colors = MaterialTheme.appColors
    BottomSheet(visible = visible, onDismiss = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spaceXl),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
            ) {
                Text(
                    text = stringResource(R.string.scan_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.66).sp,
                    ),
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.scan_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = colors.textSecondary,
                )
            }
            SheetCloseButton(onClick = onDismiss)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            ScanSource.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                    pair.forEach { source ->
                        ScanTile(
                            source = source,
                            onClick = { onPick(source) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spaceLg + 2.dp)
                .clip(NoteShape)
                .background(colors.accentTint)
                .padding(horizontal = Dimens.spaceMd, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Lock,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = stringResource(R.string.scan_privacy_note),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = colors.accentText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScanTile(
    source: ScanSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    // The camera is the one the design fills — it is what most students reach for first.
    val primary = source == ScanSource.Camera
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(colors.pageTint)
            .border(1.dp, colors.cardBorder, TileShape)
            .clickable(onClick = onClick)
            .padding(Dimens.spaceLg + 2.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(GlyphShape)
                .background(if (primary) colors.accent else colors.accentTint)
                .then(if (primary) Modifier else Modifier.border(1.dp, colors.accentRing, GlyphShape)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = source.icon,
                contentDescription = null,
                tint = if (primary) colors.onAccent else colors.accentText,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(source.labelRes),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(source.detailRes),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.textSecondary,
            )
        }
    }
}

private val ScanSource.icon: ImageVector
    get() = when (this) {
        ScanSource.Camera -> AppIcons.Camera
        ScanSource.Gallery -> AppIcons.Gallery
        ScanSource.Pdf -> AppIcons.PdfFile
        ScanSource.Word -> AppIcons.WordFile
    }

/** Everything that can be done to one answer, opened from the "More" pill under it. */
@Composable
fun BoxScope.AnswerOptionsSheet(
    visible: Boolean,
    tool: StudyTool,
    onDismiss: () -> Unit,
    onSelectText: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    BottomSheet(visible = visible, onDismiss = onDismiss, bottomPadding = 28.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spaceLg + 2.dp)
                .clip(AppShapes.CardSmall)
                .background(colors.pageTint)
                .border(1.dp, colors.cardBorder, AppShapes.CardSmall)
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            AssistantAvatar()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.chat_answer_from, stringResource(tool.labelRes)),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colors.textTertiary,
                )
                Text(
                    text = stringResource(R.string.chat_sample_a1_intro),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            listOf(
                AppIcons.Copy to R.string.chat_action_copy,
                AppIcons.Share to R.string.chat_action_share,
                AppIcons.SpeakerWave to R.string.chat_action_read_aloud,
                AppIcons.Refresh to R.string.chat_action_regenerate,
            ).forEach { (icon, labelRes) ->
                QuickAction(
                    icon = icon,
                    label = stringResource(labelRes),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LengthButton(
                icon = AppIcons.Contract,
                label = stringResource(R.string.chat_make_shorter),
                modifier = Modifier.weight(1f),
            )
            LengthButton(
                icon = AppIcons.Expand,
                label = stringResource(R.string.chat_make_longer),
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spaceLg + 2.dp)
                .clip(PanelShape)
                .background(colors.pageTint)
                .border(1.dp, colors.cardBorder, PanelShape)
                .padding(vertical = 2.dp),
        ) {
            SheetRow(
                icon = AppIcons.SelectText,
                label = stringResource(R.string.chat_select_text),
                onClick = onSelectText,
            )
            SheetRowDivider()
            SheetRow(
                icon = AppIcons.VoiceWave,
                label = stringResource(R.string.chat_reading_voice),
                value = stringResource(R.string.chat_reading_voice_value),
                onClick = {},
            )
            SheetRowDivider()
            SheetRow(
                icon = AppIcons.Flag,
                label = stringResource(R.string.chat_report_answer),
                onClick = {},
                destructive = true,
                trailingChevron = false,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.spaceLg + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.chat_was_helpful),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                RatingButton(AppIcons.ThumbUp, stringResource(R.string.chat_action_helpful))
                RatingButton(AppIcons.ThumbDown, stringResource(R.string.chat_action_not_helpful))
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier.clickable { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.accentTint)
                .border(1.dp, colors.accentRing, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LengthButton(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(AppShapes.Pill)
            .background(colors.pageTint)
            .border(1.dp, colors.cardBorder, AppShapes.Pill)
            .clickable { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun RatingButton(icon: ImageVector, contentDescription: String) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(40.dp)
            .clip(AppShapes.Pill)
            .background(colors.pageTint)
            .border(1.dp, colors.cardBorder, AppShapes.Pill)
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.accentText,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    value: String? = null,
    destructive: Boolean = false,
    trailingChevron: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val foreground = if (destructive) colors.danger else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (destructive) colors.dangerTint else colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = foreground,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textTertiary,
            )
        }
        if (trailingChevron) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SheetRowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = Dimens.spaceLg)
            .height(1.dp)
            .background(MaterialTheme.appColors.cardBorder)
    )
}

/** The header's own menu: what can be done to the whole conversation. */
@Composable
fun BoxScope.ChatOptionsMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onClearMessages: () -> Unit,
) {
    AnchoredMenu(
        visible = visible,
        onDismiss = onDismiss,
        modifier = Modifier.padding(top = 60.dp, end = Dimens.spaceMd),
    ) {
        MenuItem(AppIcons.Plus, stringResource(R.string.chat_menu_new))
        MenuItem(AppIcons.Pencil, stringResource(R.string.chat_menu_rename))
        MenuItem(AppIcons.Share, stringResource(R.string.chat_menu_share))
        MenuDivider()
        TextSizeRow()
        MenuDivider()
        MenuItem(AppIcons.Eraser, stringResource(R.string.chat_menu_clear), onClick = onClearMessages)
        MenuItem(AppIcons.Trash, stringResource(R.string.chat_menu_delete), destructive = true)
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = MaterialTheme.appColors
    val foreground = if (destructive) colors.danger else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) colors.danger else colors.accentText,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = foreground,
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceXxs)
            .height(1.dp)
            .background(MaterialTheme.appColors.cardBorder)
    )
}

/** Text size is a segmented pair rather than a slider, so it stays inside the menu's width. */
@Composable
private fun TextSizeRow() {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(start = Dimens.spaceLg, end = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Icon(
            imageVector = AppIcons.TextSize,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = stringResource(R.string.chat_menu_text_size),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accentTint)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
        ) {
            TextSizeButton(stringResource(R.string.chat_text_smaller), 12.sp)
            TextSizeButton(stringResource(R.string.chat_text_larger), 15.sp)
        }
    }
}

@Composable
private fun TextSizeButton(label: String, fontSize: TextUnit) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceRaised)
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            ),
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun SheetCloseButton(onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(AppShapes.Pill)
            .background(colors.accentTint)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Close,
            contentDescription = stringResource(R.string.action_close),
            tint = colors.accentText,
            modifier = Modifier.size(18.dp),
        )
    }
}
