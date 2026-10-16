package com.askylix.app.ui.chat

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.askylix.app.R
import com.askylix.app.di.AppViewModelFactory
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.navigation.ChatStart
import com.askylix.app.ui.signin.AuthViewModel
import com.askylix.app.ui.signin.SignInPrompt
import com.askylix.app.ui.signin.SignInReason
import com.askylix.app.ui.signin.findActivity
import kotlinx.coroutines.launch

/**
 * Connects [ChatScreen] to its ViewModel.
 *
 * The screen itself takes a state and a set of callbacks and knows nothing about ViewModels, which
 * is what lets every preview render it without any of this. The things that need a context — opening
 * a downloaded file, signing in from the prompt — are done here, where one is at hand.
 */
@Composable
fun ChatRoute(
    onBack: () -> Unit,
    onNewChat: (StudyTool) -> Unit,
    onSelectText: () -> Unit,
    onOpenPremium: () -> Unit,
    authViewModel: AuthViewModel,
    online: Boolean,
    start: ChatStart,
    viewModelKey: String,
    voiceInputEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val viewModel: ChatViewModel = viewModel(
        key = viewModelKey,
        factory = AppViewModelFactory.chat,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // A chat that changed on another device shows up when this one comes back to the front.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ChatEvent.OpenPremium -> onOpenPremium()
                is ChatEvent.OpenFile -> if (!context.openAttachmentFile(event.file, event.mimeType)) {
                    Toast.makeText(context, R.string.attach_error_no_viewer, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Deleted on another device, or never this person's: the ViewModel has said so, and there is
    // nothing left to show.
    LaunchedEffect(state.gone) { if (state.gone) onBack() }

    ChatScreen(
        state = state,
        onBack = onBack,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::send,
        onSelectText = onSelectText,
        onNewChat = { onNewChat(state.tool) },
        onRename = viewModel::rename,
        // Leaving is what "delete" ends with, and it must wait for the delete — hence a callback.
        onDeleteChat = { viewModel.deleteChat(onDeleted = onBack) },
        onTextSizeChange = viewModel::setTextSize,
        onReadAloud = viewModel::onReadAloud,
        onErrorShown = viewModel::onErrorShown,
        onFilePicked = viewModel::onFilePicked,
        onAttachmentRemoved = viewModel::onAttachmentRemoved,
        onNoCameraApp = viewModel::onNoCameraApp,
        onVoiceStarted = viewModel::onVoiceStarted,
        onVoiceToggled = viewModel::onVoiceToggled,
        onVoiceAccepted = viewModel::onVoiceAccepted,
        onVoiceCancelled = viewModel::onVoiceCancelled,
        onVoicePermissionDenied = viewModel::onVoicePermissionDenied,
        voiceInputEnabled = voiceInputEnabled,
        start = start,
        online = online,
        onRegenerate = viewModel::regenerate,
        onRate = viewModel::rate,
        onRetry = viewModel::retryUnanswered,
        onOpenAttachment = viewModel::openAttachment,
        onLoadOlder = viewModel::loadOlder,
        onReload = { viewModel.load(force = true) },
        modifier = modifier,
    )

    if (state.needsSignIn) {
        SignInPrompt(
            reason = SignInReason.AskQuestion,
            inProgress = authState.inProgress,
            errorRes = authState.errorRes,
            onContinue = {
                context.findActivity()?.let { host ->
                    // Answers saved while skipping set a new account up without asking for them again.
                    scope.launch { authViewModel.signIn(host, alwaysUseSavedAnswers = true) }
                }
            },
            onDismiss = viewModel::onSignInPromptDismissed,
        )
    }
}
