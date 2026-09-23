package com.aitutor.chatbot.app.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.core.ext.createScanCaptureUri
import com.aitutor.chatbot.app.core.ext.toast
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.MessageRole
import com.aitutor.chatbot.app.domain.model.ReportReason
import com.aitutor.chatbot.app.ui.components.EmptyState
import com.aitutor.chatbot.app.ui.components.ErrorState
import com.aitutor.chatbot.app.ui.components.ToolIcon
import com.aitutor.chatbot.app.ui.premium.PaywallSheet
import com.aitutor.chatbot.app.ui.theme.spacing
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatDetailScreen(
    modeId: String,
    chatId: String?,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatDetailViewModel = koinViewModel(parameters = { parametersOf(ChatDetailArgs(modeId, chatId)) }),
) {
    val messagesState by viewModel.messages.collectAsLifecycleAwareState()
    val title by viewModel.chatTitle.collectAsLifecycleAwareState()
    val draft by viewModel.draftText.collectAsLifecycleAwareState()
    val isSending by viewModel.isSending.collectAsLifecycleAwareState()
    val errorMessage by viewModel.errorMessage.collectAsLifecycleAwareState()
    val speakingId by viewModel.ttsManager.speakingMessageId.collectAsLifecycleAwareState()
    val showPaywall by viewModel.showPaywall.collectAsLifecycleAwareState()
    val attachment by viewModel.attachment.collectAsLifecycleAwareState()
    val plan by viewModel.plan.collectAsLifecycleAwareState()

    val messageCount = (messagesState as? UiState.Success<List<Message>>)?.data?.size ?: 0
    val showStarters = messageCount == 0 && draft.isBlank() && attachment is AttachmentState.None

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var actionsSheetFor by remember { mutableStateOf<Message?>(null) }
    var reportDialogFor by remember { mutableStateOf<Message?>(null) }
    var selectableMessageId by remember { mutableStateOf<String?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.onImagePicked(it, "Photo") } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) viewModel.onImagePicked(uri, "Scan") else viewModel.onAttachmentCleared()
        pendingCameraUri = null
    }

    fun launchScan() {
        val uri = context.createScanCaptureUri()
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun launchGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatDetailTopBar(
                title = title,
                icon = viewModel.mode.icon,
                accentColor = viewModel.mode.category.tint,
                onBack = onBack,
                onOverflowClick = { showOverflowMenu = true },
                overflowExpanded = showOverflowMenu,
                onOverflowDismiss = { showOverflowMenu = false },
                onClear = { showOverflowMenu = false; showClearDialog = true },
                onRename = { showOverflowMenu = false; showRenameDialog = true },
                onDelete = { showOverflowMenu = false; showDeleteDialog = true },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(vertical = MaterialTheme.spacing.sm)
            ) {
                if (errorMessage != null) {
                    RetryBanner(message = errorMessage.orEmpty(), onRetry = viewModel::onRetry)
                }
                (attachment as? AttachmentState.Failed)?.let { failed ->
                    RetryBanner(message = failed.message, onRetry = viewModel::onRetryExtraction)
                }
                ChatComposer(
                    value = draft,
                    onValueChange = viewModel::onDraftChange,
                    onSend = viewModel::onSend,
                    onAttachClick = { showAttachmentSheet = true },
                    onScanClick = { if (viewModel.onAttachmentRequested()) launchScan() },
                    onMicClick = { context.toast("Voice input is coming in the next update") },
                    placeholder = viewModel.mode.inputPlaceholder,
                    attachmentLabel = (attachment as? AttachmentState.Extracting)?.label,
                    extracting = attachment is AttachmentState.Extracting,
                    onClearAttachment = viewModel::onAttachmentCleared,
                    starterPrompts = if (showStarters) viewModel.starterPrompts else emptyList(),
                    onStarterPromptClick = viewModel::onStarterPromptSelected,
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (messagesState) {
                UiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is UiState.Error -> ErrorState(
                    message = (messagesState as UiState.Error).message,
                    onRetry = {},
                    modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.xl)
                )
                is UiState.Success -> {
                    val messages = (messagesState as UiState.Success<List<Message>>).data
                    if (messages.isEmpty() && !isSending) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            EmptyState(
                                title = viewModel.mode.title,
                                message = viewModel.mode.description,
                            )
                        }
                    } else {
                        MessageList(
                            messages = messages,
                            isSending = isSending,
                            speakingMessageId = speakingId,
                            selectableMessageId = selectableMessageId,
                            onOpenActions = { actionsSheetFor = it },
                        )
                    }
                }
            }
        }
    }

    actionsSheetFor?.let { message ->
        MessageActionsSheet(
            isSpeaking = speakingId == message.id,
            liked = message.liked,
            disliked = message.disliked,
            callbacks = MessageActionCallbacks(
                onCopy = {
                    clipboard.setText(AnnotatedString(message.text))
                    scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                },
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message.text)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                onToggleSpeak = { viewModel.speak(message) },
                onSelectText = { selectableMessageId = message.id },
                onRegenerate = viewModel::onRegenerate,
                onMakeShorter = { viewModel.onMakeShorter(message) },
                onMakeLonger = { viewModel.onMakeLonger(message) },
                onReport = { reportDialogFor = message },
                onLike = { viewModel.onFeedback(message, liked = !message.liked, disliked = false) },
                onDislike = { viewModel.onFeedback(message, liked = false, disliked = !message.disliked) },
            ),
            onDismiss = { actionsSheetFor = null },
        )
    }

    reportDialogFor?.let { message ->
        ReportDialog(
            onDismiss = { reportDialogFor = null },
            onSubmit = { reason, detail ->
                viewModel.onReport(message, reason, detail)
                reportDialogFor = null
                scope.launch { snackbarHostState.showSnackbar("Thanks — we'll take a look") }
            }
        )
    }

    if (showClearDialog) {
        ConfirmSimpleDialog(
            title = "Clear chat?",
            message = "This deletes every message in this chat. This can't be undone.",
            confirmLabel = "Clear",
            onConfirm = { showClearDialog = false; viewModel.onClearChat() },
            onDismiss = { showClearDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmSimpleDialog(
            title = "Delete chat?",
            message = "This permanently deletes this chat and its messages.",
            confirmLabel = "Delete",
            onConfirm = { showDeleteDialog = false; viewModel.onDeleteChat(onBack) },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            initialValue = title,
            onConfirm = { newTitle -> showRenameDialog = false; viewModel.onRenameChat(newTitle) },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showAttachmentSheet) {
        AttachmentSheet(
            isPremium = plan.isPremium,
            onOptionSelected = { option ->
                showAttachmentSheet = false
                // Free users still get to browse what's here; the gate fires on selection.
                if (viewModel.onAttachmentRequested()) {
                    when (option) {
                        AttachmentOption.Scan -> launchScan()
                        AttachmentOption.Gallery -> launchGallery()
                        AttachmentOption.Pdf, AttachmentOption.Document ->
                            context.toast("Document reading is coming in the next update")
                    }
                }
            },
            onDismiss = { showAttachmentSheet = false },
        )
    }

    (attachment as? AttachmentState.Extracted)?.let { extracted ->
        ExtractedTextSheet(
            extractedText = extracted.text,
            onConfirm = viewModel::onExtractedTextConfirmed,
            onDismiss = viewModel::onAttachmentCleared,
        )
    }

    if (showPaywall) {
        PaywallSheet(
            title = "You've used today's free questions",
            subtitle = "Premium removes the daily cap so you can keep going.",
            onSeePlans = { viewModel.onPaywallDismissed(); onOpenPremium() },
            onDismiss = viewModel::onPaywallDismissed,
        )
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    isSending: Boolean,
    speakingMessageId: String?,
    selectableMessageId: String?,
    onOpenActions: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val reversed = remember(messages, isSending) { messages.reversed() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        if (isSending) {
            item(key = "typing") { TypingIndicator() }
        }
        items(reversed, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                selectable = selectableMessageId == message.id,
                onOpenActions = { if (message.role == MessageRole.Model) onOpenActions(message) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatDetailTopBar(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit,
    onOverflowClick: () -> Unit,
    overflowExpanded: Boolean,
    onOverflowDismiss: () -> Unit,
    onClear: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = spacing.xs, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            ToolIcon(
                icon = icon,
                size = 32.dp,
                iconSize = 16.dp,
                containerColor = accentColor.copy(alpha = 0.14f),
                contentColor = accentColor,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(start = spacing.sm)
            )
            Box {
                IconButton(onClick = onOverflowClick) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Chat options")
                }
                DropdownMenu(expanded = overflowExpanded, onDismissRequest = onOverflowDismiss) {
                    DropdownMenuItem(
                        text = { Text("Clear chat") },
                        leadingIcon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                        onClick = onClear,
                    )
                    DropdownMenuItem(
                        text = { Text("Rename chat") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = onRename,
                    )
                    DropdownMenuItem(
                        text = { Text("Delete chat") },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun RetryBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
            Text("Retry", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun ConfirmSimpleDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RenameDialog(initialValue: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename chat", style = MaterialTheme.typography.titleMedium) },
        text = {
            TextField(value = value, onValueChange = { value = it }, singleLine = true)
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
