package com.askylix.app.ui.chat

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.askylix.app.R
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.domain.model.Attachment
import com.askylix.app.domain.model.ChatMessage
import com.askylix.app.domain.model.ChatTextSize
import com.askylix.app.domain.model.Rating
import com.askylix.app.domain.model.SendStatus
import com.askylix.app.domain.model.ScanSource
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.navigation.ChatStart
import kotlinx.coroutines.delay
import com.askylix.app.ui.components.ConfirmDialog
import com.askylix.app.ui.components.ConfirmTone
import com.askylix.app.ui.components.OfflineNote
import com.askylix.app.ui.components.rememberLeaveGuard
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.theme.AITutorTheme
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.SampleContent
import com.askylix.app.ui.theme.ScreenPreviews
import com.askylix.app.ui.theme.SystemBarIcons
import com.askylix.app.ui.theme.bottomSafePadding
import com.askylix.app.ui.theme.appColors

private val ComposerShape = RoundedCornerShape(28.dp)

/**
 * How many lines the field grows to before it scrolls instead.
 *
 * Five keeps a long question fully readable while leaving the conversation most of the screen — a
 * composer that could grow without limit would push the thread off the display on a small phone.
 */
private const val ComposerMaxLines = 5

/** The composer at rest: one line of text between two 6dp insets, around 44dp buttons. */
private val ComposerMinHeight = 56.dp

/**
 * Space above and below the text, so a single line sits centred against the 44dp buttons.
 *
 * It is (44dp button - 22sp line) / 2. Holding it constant rather than centring the field is what
 * stops the first line jumping up as the second one appears.
 */
private val ComposerTextInset = 11.dp
private val HeaderIconShape = RoundedCornerShape(12.dp)

/** How close to the top of the messages held the scroll must be before the page above them is fetched. */
private const val LoadOlderThreshold = 3

/** How long an answer takes before the typing row says it is still being worked on. */
private const val SlowAnswerAfterMillis = 15_000L

/** How long a chat is read before the placeholder admits it is still working. */
private const val StillLoadingAfterMillis = 3_000L

/** Which overlay, if any, is open over the thread. Only one can be at a time. */
internal enum class ChatOverlay { None, Scan, AnswerOptions, ChatMenu, Voice, RenameChat, ConfirmDelete }

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
    onNewChat: () -> Unit,
    onRename: (String) -> Unit,
    onDeleteChat: () -> Unit,
    onTextSizeChange: (ChatTextSize) -> Unit,
    onReadAloud: (String) -> Unit,
    onErrorShown: () -> Unit,
    onFilePicked: (Uri, ScanSource) -> Unit,
    onAttachmentRemoved: () -> Unit,
    onNoCameraApp: () -> Unit,
    onVoiceStarted: () -> Unit,
    onVoiceToggled: () -> Unit,
    onVoiceAccepted: () -> Unit,
    onVoiceCancelled: () -> Unit,
    onVoicePermissionDenied: (askableAgain: Boolean) -> Unit,
    voiceInputEnabled: Boolean,
    modifier: Modifier = Modifier,
    start: ChatStart = ChatStart.None,
    onRegenerate: (String) -> Unit = {},
    onRate: (String, Rating) -> Unit = { _, _ -> },
    onRetry: (String) -> Unit = {},
    onOpenAttachment: (String, Attachment) -> Unit = { _, _ -> },
    onLoadOlder: () -> Unit = {},
    onReload: () -> Unit = {},
    online: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    var overlay by rememberSaveable { mutableStateOf(ChatOverlay.None) }
    val composerFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val pickers = rememberAttachmentPickers(onPicked = onFilePicked, onNoCameraApp = onNoCameraApp)
    val context = LocalContext.current
    // A chat with no saved name yet is shared under the name of the tool it was opened in.
    val toolLabel = stringResource(state.tool.labelRes)

    // Asked for when the mic is tapped, not at launch — at the moment its purpose is obvious.
    val requestMicrophone = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onVoiceStarted()
        } else {
            // After a refusal, a false rationale means the system will not ask again — so the
            // message has to point at Settings rather than invite another tap that does nothing.
            val activity = context as? Activity
            val askableAgain = activity != null && ActivityCompat
                .shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            onVoicePermissionDenied(askableAgain)
        }
    }
    // A plain surface sits under the status bar, so the icons follow the theme.
    SystemBarIcons(lightStatusBarIcons = colors.isDark)

    // How this chat was opened, acted on once: turning the phone must not reopen the sheet or raise
    // the keyboard again over whatever the student has since done.
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (started) return@LaunchedEffect
        started = true
        when (start) {
            ChatStart.None -> Unit
            ChatStart.Compose -> composerFocus.requestFocus()
            ChatStart.Scan -> overlay = ChatOverlay.Scan
        }
    }

    // Back and the header arrow ask first only while a file is still being added: that is lost if the
    // screen closes. A question on its way is not — it keeps running, and its answer is in the chat on return.
    val requestLeave = rememberLeaveGuard(
        busy = state.isBusy,
        bodyRes = R.string.leave_attach_body,
        onLeave = onBack,
    )
    // After the guard, so it is asked first: an open sheet, menu or dictation panel is closed by
    // Back before the screen is treated as leaving.
    BackHandler(enabled = overlay != ChatOverlay.None || state.voice != null) {
        if (state.voice != null) onVoiceCancelled() else overlay = ChatOverlay.None
    }


    // A new message should be visible without the student scrolling for it. Keyed on the newest item,
    // not on how many there are: older messages loading in above must not drag the thread to the bottom.
    val newestKey = state.messages.lastOrNull()?.listKey()
    LaunchedEffect(newestKey) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
    }

    // Scrolling up to the first messages held asks for the page before them.
    LaunchedEffect(listState, state.hasOlder) {
        if (!state.hasOlder) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index -> if (index <= LoadOlderThreshold) onLoadOlder() }
    }

    // Keeps the bottom of the thread where it was when the list is resized beneath it.
    //
    // The composer grows as a question gets longer, and the keyboard opening and closing does the
    // same, and both take room from the list. Left alone, the newest message slides under the
    // composer. The scroll is the exact amount the viewport changed by, so the bottom edge stays put
    // whatever the heights of the items are — and a student who has scrolled up to reread something
    // is left exactly where they are.
    LaunchedEffect(listState) {
        var viewport = 0
        var pinned = true
        snapshotFlow { listState.layoutInfo.viewportSize.height to !listState.canScrollForward }
            .collect { (height, atEnd) ->
                if (height == viewport) {
                    // A real scroll rather than a resize: this is the student's own position.
                    pinned = atEnd
                } else {
                    if (pinned && viewport != 0 && height != 0) {
                        listState.scrollBy((viewport - height).toFloat())
                    }
                    viewport = height
                }
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(
                tool = state.tool,
                subject = stringResource(state.tool.taglineRes),
                onBack = requestLeave,
                onOptions = { overlay = ChatOverlay.ChatMenu },
            )

            when {
                state.isEmpty -> EmptyThread(
                    tool = state.tool,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                // An existing chat is on its way from the server, with nothing held to show yet.
                state.loading && state.storedMessages.isEmpty() -> ThreadPlaceholder(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                state.loadError != null && state.storedMessages.isEmpty() -> ThreadLoadError(
                    offline = state.loadError == ApiErrorCode.NoConnection || state.loadError == ApiErrorCode.Timeout,
                    onRetry = onReload,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                else -> CompositionLocalProvider(LocalChatTextSize provides state.textSize) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = Dimens.spaceLg + 2.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (state.loadingOlder) {
                            item(key = "older") {
                                Text(
                                    text = stringResource(R.string.chat_loading_older),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textTertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        items(items = state.messages, key = ChatMessage::listKey) { message ->
                            MessageRow(
                                message = message,
                                onMore = { overlay = ChatOverlay.AnswerOptions },
                                onReadAloud = onReadAloud,
                                speakingMessageId = state.speakingMessageId,
                                onRegenerate = onRegenerate,
                                onRate = onRate,
                                onRetry = onRetry,
                                onOpenAttachment = onOpenAttachment,
                                downloadingMessageId = state.downloadingMessageId,
                                downloadPercent = state.downloadPercent,
                                sending = state.sending,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // No room is taken while the phone is online. Offline, it says why a question will not go through.
            if (!online) {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.spaceSm), contentAlignment = Alignment.Center) {
                    OfflineNote(online = false)
                }
            }
            ErrorBanner(errorRes = state.errorRes, onDismiss = onErrorShown)

            Composer(
                draft = state.draft,
                canSend = state.canSend,
                pendingAttachment = state.pendingAttachment,
                attaching = state.attaching,
                onAttachmentRemoved = onAttachmentRemoved,
                onDraftChange = onDraftChange,
                composerFocus = composerFocus,
                onScan = { overlay = ChatOverlay.Scan },
                voiceInputEnabled = voiceInputEnabled,
                onVoice = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        onVoiceStarted()
                    } else {
                        requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSend = onSend,
            )
        }

        ScanSheet(
            visible = overlay == ChatOverlay.Scan,
            onDismiss = { overlay = ChatOverlay.None },
            onPick = { source ->
                overlay = ChatOverlay.None
                pickers.pick(source)
            },
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
            hasChat = state.hasChat,
            canStartNew = state.hasChat && !state.isBusy,
            textSize = state.textSize,
            onNewChat = {
                overlay = ChatOverlay.None
                onNewChat()
            },
            onRename = { overlay = ChatOverlay.RenameChat },
            onShare = {
                overlay = ChatOverlay.None
                context.shareChat(
                    title = state.title.ifBlank { toolLabel },
                    messages = state.storedMessages,
                )
            },
            onTextSizeChange = onTextSizeChange,
            onDelete = { overlay = ChatOverlay.ConfirmDelete },
        )
        if (overlay == ChatOverlay.RenameChat) {
            RenameChatDialog(
                initialTitle = state.title,
                onSave = { title ->
                    overlay = ChatOverlay.None
                    onRename(title)
                },
                onDismiss = { overlay = ChatOverlay.None },
            )
        }
        if (overlay == ChatOverlay.ConfirmDelete) {
            ConfirmDialog(
                icon = AppIcons.Trash,
                title = stringResource(R.string.chat_delete_title),
                body = stringResource(R.string.chat_delete_body),
                stayLabel = stringResource(R.string.action_cancel),
                confirmLabel = stringResource(R.string.chat_delete_confirm),
                tone = ConfirmTone.Destructive,
                onStay = { overlay = ChatOverlay.None },
                onConfirm = {
                    overlay = ChatOverlay.None
                    onDeleteChat()
                },
            )
        }
        VoiceInputPanel(
            state = state.voice,
            onDismiss = onVoiceCancelled,
            onAccept = onVoiceAccepted,
            onToggleListening = onVoiceToggled,
        )
    }
}

@Composable
private fun MessageRow(
    message: ChatMessage,
    onMore: () -> Unit,
    onReadAloud: (String) -> Unit,
    modifier: Modifier = Modifier,
    speakingMessageId: String? = null,
    onRegenerate: (String) -> Unit = {},
    onRate: (String, Rating) -> Unit = { _, _ -> },
    onRetry: (String) -> Unit = {},
    onOpenAttachment: (String, Attachment) -> Unit = { _, _ -> },
    downloadingMessageId: String? = null,
    downloadPercent: Int? = null,
    sending: Boolean = false,
) {
    val context = LocalContext.current
    when (message) {
        is ChatMessage.DayMarker -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            DayMarker(label = dayLabel(message.timestamp))
        }

        is ChatMessage.User -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
        ) {
            message.attachment?.let { attachment ->
                AttachmentChip(
                    attachment = attachment,
                    // A question still on its way has not got a server id, so there is nothing to open yet.
                    onClick = if (message.status == SendStatus.Sending) null else {
                        { onOpenAttachment(message.id, attachment) }
                    },
                    downloading = downloadingMessageId == message.id,
                    downloadPercent = downloadPercent,
                )
            }
            // A file sent on its own is the whole message; an empty bubble beside it would just
            // read as a rendering fault.
            if (message.text.isNotBlank()) UserBubble(text = message.text)
            // The server saved the question but could not answer it. Asking again is one tap.
            if (message.status == SendStatus.Unanswered && !sending) {
                TryAgain(onClick = { onRetry(message.id) })
            }
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
                if (message.showActions) {
                    AnswerActions(
                        onMore = onMore,
                        onReadAloud = { onReadAloud(message.id) },
                        isSpeaking = speakingMessageId == message.id,
                        rating = message.rating,
                        onCopy = { context.copyToClipboard(ChatTranscript.answerText(message.blocks)) },
                        onRate = { rating -> onRate(message.id, rating) },
                        onRegenerate = if (sending) null else { { onRegenerate(message.id) } },
                    )
                }
            }
        }

        ChatMessage.Typing -> TypingRow(modifier = modifier)
    }
}

/**
 * The tutor's typing dots, and — if the answer is taking a while — a line saying it still is, so a slow
 * model does not look like a frozen screen. After 90 seconds the request itself gives up and says so.
 */
@Composable
private fun TypingRow(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SlowAnswerAfterMillis)
        slow = true
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssistantAvatar()
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            TypingIndicator()
            if (slow) {
                Text(
                    text = stringResource(R.string.chat_still_working),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

/** "Try again" beneath a question the server could not answer. */
@Composable
private fun TryAgain(onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Text(
        text = stringResource(R.string.chat_try_again),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = colors.accentText,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm),
    )
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
}

/**
 * Shown while an existing chat is being read from the server. Static blocks in the shape of a short
 * exchange, with no shimmer — and after a few seconds a line that says it is still working, so a
 * slow connection does not look like a frozen screen.
 */
@Composable
private fun ThreadPlaceholder(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    var stillLoading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(StillLoadingAfterMillis)
        stillLoading = true
    }
    Column(
        modifier = modifier.padding(horizontal = Dimens.spaceLg + 2.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
    ) {
        PlaceholderBlock(widthFraction = 0.55f, height = 44.dp, alignEnd = true)
        PlaceholderBlock(widthFraction = 0.9f, height = 96.dp)
        PlaceholderBlock(widthFraction = 0.4f, height = 44.dp, alignEnd = true)
        Text(
            text = stringResource(if (stillLoading) R.string.loading_still else R.string.loading),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlaceholderBlock(widthFraction: Float, height: androidx.compose.ui.unit.Dp, alignEnd: Boolean = false) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(height)
                .clip(AppShapes.CardSmall)
                .background(MaterialTheme.appColors.accentTint),
        )
    }
}

/** A chat that could not be read: what went wrong, and a way to try again. */
@Composable
private fun ThreadLoadError(offline: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (offline) R.string.chat_load_offline else R.string.chat_load_error),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        TryAgain(onClick = onRetry)
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
    pendingAttachment: Attachment?,
    attaching: Boolean,
    onAttachmentRemoved: () -> Unit,
    onDraftChange: (String) -> Unit,
    composerFocus: FocusRequester,
    onScan: () -> Unit,
    voiceInputEnabled: Boolean,
    onVoice: () -> Unit,
    onSend: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .bottomSafePadding()
            .padding(start = Dimens.spaceLg + 2.dp, end = Dimens.spaceLg + 2.dp, top = 10.dp, bottom = Dimens.spaceXxl),
    ) {
        // A file waiting to be sent, or the moment of copying it. The design draws this chip only
        // on a sent message; before sending it needs a way back off, hence the ×.
        if (attaching) {
            Text(
                text = stringResource(R.string.attach_preparing),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(bottom = Dimens.spaceSm),
            )
        } else if (pendingAttachment != null) {
            AttachmentChip(
                attachment = pendingAttachment,
                onRemove = onAttachmentRemoved,
                modifier = Modifier.padding(bottom = Dimens.spaceSm),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Grows with the text. The floor is the old fixed height, so an empty composer is
                // exactly as it was drawn.
                .heightIn(min = ComposerMinHeight)
                .clip(ComposerShape)
                .background(colors.pageTint)
                .border(1.dp, colors.cardBorder, ComposerShape)
                .padding(Dimens.spaceXs),
            // Buttons hold the bottom edge as the field gets taller, which is where a thumb is.
            // Centred, they would float off beside the middle of a five-line question.
            verticalAlignment = Alignment.Bottom,
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
                    .padding(start = Dimens.spaceXs, top = ComposerTextInset, bottom = ComposerTextInset),
                contentAlignment = Alignment.TopStart,
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_composer_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textTertiary,
                        // A longer translation must not wrap and make the empty composer two lines.
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    // Enter starts a new line, which is what writing a question wants. Past the cap
                    // the field scrolls inside itself and keeps the cursor in view.
                    maxLines = ComposerMaxLines,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(composerFocus),
                )
            }
            // Hidden rather than disabled: the Profile switch means "I don't dictate".
            if (voiceInputEnabled) {
                ComposerButton(
                    icon = AppIcons.Mic,
                    contentDescription = stringResource(R.string.cd_speak_question),
                    onClick = onVoice,
                )
            }
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
    if (errorRes != null) {
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
                text = stringResource(errorRes),
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

/** Opens the system share sheet with the conversation as text. Nothing is sent until an app is chosen. */
private fun Context.shareChat(title: String, messages: List<ChatMessage>) {
    val text = ChatTranscript.format(
        title = title,
        messages = messages,
        youLabel = getString(R.string.chat_share_you),
        tutorLabel = getString(R.string.app_name),
        attachedLabel = { fileName -> getString(R.string.chat_share_attached, fileName) },
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, getString(R.string.chat_menu_share)))
}

@ScreenPreviews
@Composable
private fun ChatScreenPreview() {
    AITutorTheme {
        ChatScreen(
            state = ChatUiState(
                tool = StudyTool.AiTutor,
                storedMessages = SampleContent.conversation(),
                // Mid-request, which is the moment the artboard draws: the typing row is showing
                // and the send button is correspondingly dimmed.
                sending = true,
            ),
            onBack = {},
            onDraftChange = {},
            onSend = {},
            onSelectText = {},
            onNewChat = {},
            onRename = {},
            onDeleteChat = {},
            onTextSizeChange = {},
            onReadAloud = {},
            onErrorShown = {},
            onFilePicked = { _, _ -> },
            onAttachmentRemoved = {},
            onNoCameraApp = {},
            onVoiceStarted = {},
            onVoiceToggled = {},
            onVoiceAccepted = {},
            onVoiceCancelled = {},
            onVoicePermissionDenied = {},
            voiceInputEnabled = true,
        )
    }
}

@ScreenPreviews
@Composable
private fun ChatLongDraftPreview() {
    AITutorTheme {
        ChatScreen(
            state = ChatUiState(
                tool = StudyTool.AiTutor,
                storedMessages = SampleContent.conversation(),
                // Long enough to hit the five-line cap, so the preview shows the scrolled state.
                draft = List(8) { "Explain how photosynthesis turns light into chemical energy." }
                    .joinToString("\n"),
            ),
            onBack = {},
            onDraftChange = {},
            onSend = {},
            onSelectText = {},
            onNewChat = {},
            onRename = {},
            onDeleteChat = {},
            onTextSizeChange = {},
            onReadAloud = {},
            onErrorShown = {},
            onFilePicked = { _, _ -> },
            onAttachmentRemoved = {},
            onNoCameraApp = {},
            onVoiceStarted = {},
            onVoiceToggled = {},
            onVoiceAccepted = {},
            onVoiceCancelled = {},
            onVoicePermissionDenied = {},
            voiceInputEnabled = true,
        )
    }
}

@ScreenPreviews
@Composable
private fun ChatEmptyPreview() {
    AITutorTheme {
        ChatScreen(
            state = ChatUiState(tool = StudyTool.MathSolver),
            onBack = {},
            onDraftChange = {},
            onSend = {},
            onSelectText = {},
            onNewChat = {},
            onRename = {},
            onDeleteChat = {},
            onTextSizeChange = {},
            onReadAloud = {},
            onErrorShown = {},
            onFilePicked = { _, _ -> },
            onAttachmentRemoved = {},
            onNoCameraApp = {},
            onVoiceStarted = {},
            onVoiceToggled = {},
            onVoiceAccepted = {},
            onVoiceCancelled = {},
            onVoicePermissionDenied = {},
            voiceInputEnabled = true,
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
            VoiceInputPanel(
                state = VoiceUiState(listening = true, transcript = "Explain how photosynthesis", partial = "makes glucose", level = 0.7f),
                onDismiss = {},
                onAccept = {},
                onToggleListening = {},
            )
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
            ChatOptionsMenu(
                visible = true,
                onDismiss = {},
                hasChat = true,
                canStartNew = true,
                textSize = ChatTextSize.Default,
                onNewChat = {},
                onRename = {},
                onShare = {},
                onTextSizeChange = {},
                onDelete = {},
            )
        }
    }
}
