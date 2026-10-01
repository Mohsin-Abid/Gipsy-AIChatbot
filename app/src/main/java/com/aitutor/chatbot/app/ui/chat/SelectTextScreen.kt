package com.aitutor.chatbot.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.SampleContent
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.bottomSafePadding
import com.aitutor.chatbot.app.ui.theme.appColors

private val CardShape = RoundedCornerShape(22.dp)
private val NoteShape = RoundedCornerShape(14.dp)
private val ToolbarShape = RoundedCornerShape(14.dp)
private val ShadowTint = Color(0xFF080C28)

/** Placeholder until the selection is driven by real drag handles. */
private const val SELECTED_WORDS = 8

/**
 * Text selection over one answer, as its own screen rather than a system context menu — that is
 * what lets "Ask about this" feed the selection straight back into the conversation.
 *
 * The highlight here marks one whole block. Dragging the handles is not wired up: it needs a text
 * layout the selection can be measured against, which arrives with the real message content.
 */
@Composable
fun SelectTextScreen(
    tool: StudyTool,
    subject: String,
    timestamp: String,
    blocks: List<AnswerBlock>,
    onClose: () -> Unit,
    onAskAboutSelection: () -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = blocks.lastIndex,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
    // A plain surface sits under the status bar, so the icons follow the theme.
    SystemBarIcons(lightStatusBarIcons = colors.isDark)

        SelectHeader(onClose = onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = Dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
        ) {
            HintBanner()
            AnswerAttribution(tool = tool, subject = subject, timestamp = timestamp)
            SelectableAnswer(
                blocks = blocks,
                selectedIndex = selectedIndex,
                modifier = Modifier.padding(top = 50.dp, bottom = Dimens.spaceXxl),
            )
        }

        SelectFooter(onAskAboutSelection = onAskAboutSelection)
    }
}

@Composable
private fun SelectHeader(onClose: () -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .background(colors.surface)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = Dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppShapes.Pill)
                    .background(colors.accentTint)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = colors.accentText,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = stringResource(R.string.chat_select_text),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.select_all),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.accentText,
                modifier = Modifier
                    .clip(AppShapes.Pill)
                    .clickable { }
                    .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.cardBorder)
        )
    }
}

@Composable
private fun HintBanner() {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NoteShape)
            .background(colors.accentTint)
            .padding(horizontal = Dimens.spaceLg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Icon(
            imageVector = AppIcons.Info,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.select_hint),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accentText,
        )
    }
}

@Composable
private fun AnswerAttribution(tool: StudyTool, subject: String, timestamp: String) {
    val colors = MaterialTheme.appColors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssistantAvatar()
        Column {
            Text(
                text = stringResource(R.string.chat_answer_from, stringResource(tool.labelRes)),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.select_answer_meta, subject, timestamp),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.textTertiary,
            )
        }
    }
}

/**
 * The answer at reading size, with one block highlighted and the action bar floating above it.
 * The bar is positioned against the highlighted block rather than the card, so it stays attached
 * to whatever is selected.
 */
@Composable
private fun SelectableAnswer(
    blocks: List<AnswerBlock>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, CardShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(CardShape)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, CardShape)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg + 2.dp),
    ) {
        blocks.forEachIndexed { index, block ->
            val selected = index == selectedIndex
            if (selected) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectionToolbar()
                    AnswerLine(block = block, selected = true)
                }
            } else {
                AnswerLine(block = block, selected = false)
            }
        }
    }
}

@Composable
private fun AnswerLine(block: AnswerBlock, selected: Boolean) {
    val colors = MaterialTheme.appColors
    val style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 28.sp)
    val highlight = if (selected) colors.selectionHighlight else Color.Transparent

    when (block) {
        is AnswerBlock.Paragraph -> Text(
            text = block.text,
            style = style.copy(background = highlight),
            color = colors.textPrimary,
        )
        is AnswerBlock.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            Box(
                modifier = Modifier
                    .padding(top = 11.dp)
                    .size(6.dp)
                    .clip(AppShapes.Pill)
                    .background(colors.accent)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, background = highlight)) {
                        append(block.term)
                    }
                    withStyle(SpanStyle(background = highlight)) {
                        append(" ")
                        append(block.text)
                    }
                },
                style = style,
                color = colors.textPrimary,
            )
        }
    }
}

/** The dark floating bar over a selection: copy, share, explain, ask. */
@Composable
private fun SelectionToolbar() {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .shadow(14.dp, ToolbarShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(ToolbarShape)
            .background(colors.selectionBar)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            AppIcons.Copy to R.string.chat_action_copy,
            AppIcons.Share to R.string.chat_action_share,
            AppIcons.Idea to R.string.select_explain,
            AppIcons.Spark to R.string.select_ask_ai,
        ).forEachIndexed { index, (icon, labelRes) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                )
            }
            ToolbarAction(icon = icon, label = stringResource(labelRes))
        }
    }
}

@Composable
private fun ToolbarAction(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun SelectFooter(onAskAboutSelection: () -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .bottomSafePadding()
            .padding(start = 20.dp, end = 20.dp, top = Dimens.spaceLg, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.select_words_selected, SELECTED_WORDS),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(AppShapes.Pill)
                    .background(colors.pageTint)
                    .border(1.dp, colors.cardBorder, AppShapes.Pill)
                    .clickable { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = AppIcons.Copy,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(R.string.chat_action_copy),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
            }
            Row(
                modifier = Modifier
                    .weight(1.4f)
                    .height(52.dp)
                    .shadow(12.dp, AppShapes.Pill, ambientColor = colors.accent, spotColor = colors.accent)
                    .clip(AppShapes.Pill)
                    .background(colors.accent)
                    .clickable(onClick = onAskAboutSelection),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm, Alignment.CenterHorizontally),
            ) {
                Icon(
                    imageVector = AppIcons.Spark,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.select_ask_about_this),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.onAccent,
                )
            }
        }
    }
}

@ScreenPreviews
@Composable
private fun SelectTextScreenPreview() {
    AITutorTheme {
        SelectTextScreen(
            tool = StudyTool.AiTutor,
            subject = "Biology",
            timestamp = "10:22 AM",
            blocks = SampleContent.answerBlocks(),
            onClose = {},
            onAskAboutSelection = {},
            selectedIndex = 2,
        )
    }
}
