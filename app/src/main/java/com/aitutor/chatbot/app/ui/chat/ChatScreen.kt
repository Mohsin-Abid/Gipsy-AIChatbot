package com.aitutor.chatbot.app.ui.chat

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.ChatMessage
import com.aitutor.chatbot.app.domain.model.ScanSource
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.SampleContent
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

private val ComposerShape = RoundedCornerShape(28.dp)
private val HeaderIconShape = RoundedCornerShape(12.dp)

/** Which overlay, if any, is open over the thread. Only one can be at a time. */
internal enum class ChatOverlay { None, Scan, AnswerOptions, ChatMenu, Voice }

/**
 * The conversation.
 *
 * The screen owns only which overlay is showing and what is typed; the messages come in and the
 * edits go out, so the whole thing drops onto a ViewModel unchanged once the API client exists.
 */
@Composable
fun ChatScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSelectText: () -> Unit,
    onClearMessages: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    var overlay by rememberSaveable { mutableStateOf(ChatOverlay.None) }
    val listState = rememberLazyListState()
    // A plain surface sits under the status bar, so the icons follow the theme.
    SystemBarIcons(lightStatusBarIcons = colors.isDark)


    // A new message should be visible without the student scrolling for it.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(
                tool = state.tool,
                subject = state.subject.ifBlank { stringResource(state.tool.taglineRes) },
                onBack = onBack,
                onOptions = { overlay = ChatOverlay.ChatMenu },
            )

            if (state.isEmpty) {
                EmptyThread(
                    tool = state.tool,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Dimens.spaceLg + 2.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(items = state.messages, key = ChatMessage::listKey) { message ->
                        MessageRow(
                            message = message,
                            onMore = { overlay = ChatOverlay.AnswerOptions },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            ErrorBanner(errorRes = state.errorRes, onDismiss = onErrorShown)

            Composer(
                draft = state.draft,
                canSend = state.canSend,
                onDraftChange = onDraftChange,
                onScan = { overlay = ChatOverlay.Scan },
                onVoice = { overlay = ChatOverlay.Voice },
                onSend = onSend,
            )
        }

        ScanSheet(
            visible = overlay == ChatOverlay.Scan,
            onDismiss = { overlay = ChatOverlay.None },
            onPick = { _: ScanSource -> overlay = ChatOverlay.None },
        )
        AnswerOptionsSheet(
            visible = overlay == ChatOverlay.AnswerOptions,
            tool = state.tool,
            onDismiss = { overlay = ChatOverlay.None },
            onSelectText = {
                overlay = ChatOverlay.None
                onSelectText()
            },
        )
        ChatOptionsMenu(
            visible = overlay == ChatOverlay.ChatMenu,
            onDismiss = { overlay = ChatOverlay.None },
            onClearMessages = {
                overlay = ChatOverlay.None
                onClearMessages()
            },
        )
        VoiceInputPanel(
            visible = overlay == ChatOverlay.Voice,
            onDismiss = { overlay = ChatOverlay.None },
            onAccept = { overlay = ChatOverlay.None },
        )
    }
}

@Composable
private fun MessageRow(
    message: ChatMessage,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (message) {
        is ChatMessage.DayMarker -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            DayMarker(label = dayLabel(message.timestamp))
        }

        is ChatMessage.User -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            message.attachment?.let { AttachmentChip(attachment = it) }
            UserBubble(text = message.text)
        }

        is ChatMessage.Assistant -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AssistantAvatar()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
            ) {
                AssistantBubble(blocks = message.blocks)
                if (message.showActions) AnswerActions(onMore = onMore)
            }
        }

        ChatMessage.Typing -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AssistantAvatar()
            TypingIndicator()
        }
    }
}

// ---- Header ----

/** Back, the tutor mark, the mode switcher, and the chat's own menu. */
@Composable
private fun ChatHeader(
    tool: StudyTool,
    subject: String,
    onBack: () -> Unit,
    onOptions: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The header's surface reaches the top of the display; its row sits under the clock.
            .background(colors.surface)
            .statusBarsPadding()
            .height(68.dp)
            .padding(start = Dimens.spaceLg, end = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(AppShapes.Pill)
                .background(colors.accentTint)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.ChevronLeft,
                contentDescription = stringResource(R.string.cd_back),
                tint = colors.accentText,
                modifier = Modifier.size(20.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(HeaderIconShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Spark,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(AppShapes.CardSmall)
                .clickable { }
                .padding(vertical = Dimens.spaceXxs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
            ) {
                Text(
                    text = stringResource(tool.labelRes),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.32).sp),
                    color = colors.textPrimary,
                )
                Icon(
                    imageVector = AppIcons.ChevronDown,
                    contentDescription = stringResource(R.string.cd_switch_mode),
                    tint = colors.textTertiary,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = subject,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.textTertiary,
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(AppShapes.Pill)
                .clickable(onClick = onOptions),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.DotsVertical,
                contentDescription = stringResource(R.string.cd_chat_options),
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.cardBorder)
    )
}

// ---- Composer ----

@Composable
private fun Composer(
    draft: String,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onScan: () -> Unit,
    onVoice: () -> Unit,
    onSend: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            // The keyboard lifts the composer; without this it would sit behind it. `imePadding`
            // already accounts for the navigation bar while the keyboard is up, so the two do not
            // stack — which is why the nav bar is applied first and the IME on top.
            .navigationBarsPadding()
            .imePadding()
            .padding(start = Dimens.spaceLg + 2.dp, end = Dimens.spaceLg + 2.dp, top = 10.dp, bottom = Dimens.spaceXxl),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(ComposerShape)
                .background(colors.pageTint)
                .border(1.dp, colors.cardBorder, ComposerShape)
                .padding(horizontal = Dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
        ) {
            ComposerButton(
                icon = AppIcons.ScanText,
                contentDescription = stringResource(R.string.cd_scan_text),
                onClick = onScan,
                tinted = true,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.spaceXs),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_composer_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textTertiary,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            ComposerButton(
                icon = AppIcons.Mic,
                contentDescription = stringResource(R.string.cd_speak_question),
                onClick = onVoice,
            )
            ComposerButton(
                icon = AppIcons.SendUp,
                contentDescription = stringResource(R.string.cd_send_question),
                onClick = onSend,
                filled = true,
                enabled = canSend,
            )
        }
    }
}

@Composable
private fun ComposerButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tinted: Boolean = false,
    filled: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(AppShapes.Pill)
            .background(
                when {
                    // Dimmed rather than hidden: the button has to hold its place in the row.
                    filled && !enabled -> colors.accent.copy(alpha = 0.4f)
                    filled -> colors.accent
                    tinted -> colors.accentTint
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                filled -> colors.onAccent
                tinted -> colors.accentText
                else -> colors.textSecondary
            },
            modifier = Modifier.size(if (filled) 19.dp else 20.dp),
        )
    }
}

/**
 * A stable identity for the list. Messages have real ids; the day marker and the typing row are
 * singletons the ViewModel inserts, so they get constant keys of their own.
 */
private fun ChatMessage.listKey(): Any = when (this) {
    is ChatMessage.DayMarker -> "day:$timestamp"
    is ChatMessage.User -> "user:$id"
    is ChatMessage.Assistant -> "ai:$id"
    ChatMessage.Typing -> "typing"
}

/**
 * "Today", "Yesterday", or a date once it is older than that — from the platform, so it arrives
 * already translated for whatever locale the app is running in.
 */
@Composable
private fun dayLabel(timestamp: Long): String = DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.DAY_IN_MILLIS,
    DateUtils.FORMAT_SHOW_DATE,
).toString()

/** The opening state of a conversation: what this tool is for, before anything has been asked. */
@Composable
private fun EmptyThread(tool: StudyTool, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = stringResource(tool.labelRes),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = stringResource(tool.taglineRes),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.spaceSm),
        )
        Text(
            text = stringResource(R.string.chat_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.spaceXl),
        )
    }
}

/**
 * Shown when a request failed. It says so plainly instead of the thread quietly going nowhere —
 * with no API configured this is what every send does, and pretending otherwise would be worse.
 */
@Composable
private fun ErrorBanner(@StringRes errorRes: Int?, onDismiss: () -> Unit) {
    val colors = MaterialTheme.appColors
    AnimatedVisibility(
        visible = errorRes != null,
        enter = fadeIn(Motion.medium()),
        exit = fadeOut(Motion.fast()),
    ) {
        // Held after errorRes clears so the exit transition has something to draw.
        val shown = remember(errorRes) { errorRes }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = Dimens.spaceLg + 2.dp, vertical = Dimens.spaceSm)
                .clip(AppShapes.CardSmall)
                .background(colors.dangerTint)
                .clickable(onClick = onDismiss)
                .padding(horizontal = Dimens.spaceMd, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            Icon(
                imageVector = AppIcons.Info,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = shown?.let { stringResource(it) }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.danger,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = stringResource(R.string.action_close),
                tint = colors.danger,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun ChatScreenPreview() {
    AITutorTheme {
        ChatScreen(
            state = ChatUiState(
                tool = StudyTool.AiTutor,
                subject = "Biology",
                storedMessages = SampleContent.conversation(),
                // Mid-request, which is the moment the artboard draws: the typing row is showing
                // and the send button is correspondingly dimmed.
                sending = true,
            ),
            onBack = {},
            onDraftChange = {},
            onSend = {},
            onSelectText = {},
            onClearMessages = {},
            onErrorShown = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun ChatEmptyPreview() {
    AITutorTheme {
        ChatScreen(
            state = ChatUiState(tool = StudyTool.MathSolver, subject = "Mathematics"),
            onBack = {},
            onDraftChange = {},
            onSend = {},
            onSelectText = {},
            onClearMessages = {},
            onErrorShown = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun ChatScanSheetPreview() {
    AITutorTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ScanSheet(visible = true, onDismiss = {}, onPick = {})
        }
    }
}

@ScreenPreviews
@Composable
private fun ChatVoicePreview() {
    AITutorTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            VoiceInputPanel(visible = true, onDismiss = {}, onAccept = {})
        }
    }
}

@ScreenPreviews
@Composable
private fun ChatAnswerOptionsPreview() {
    AITutorTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AnswerOptionsSheet(
                visible = true,
                tool = StudyTool.AiTutor,
                onDismiss = {},
                onSelectText = {},
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun ChatOptionsMenuPreview() {
    AITutorTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatOptionsMenu(visible = true, onDismiss = {}, onClearMessages = {})
        }
    }
}
