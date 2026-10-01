package com.aitutor.chatbot.app.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitutor.chatbot.app.di.AppViewModelFactory

/**
 * Connects [ChatScreen] to its ViewModel.
 *
 * The screen itself takes a state and a set of callbacks and knows nothing about ViewModels, which
 * is what lets every preview render it without any of this.
 */
@Composable
fun ChatRoute(
    onBack: () -> Unit,
    onSelectText: () -> Unit,
    viewModelKey: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: ChatViewModel = viewModel(
        key = viewModelKey,
        factory = AppViewModelFactory.chat,
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        onBack = onBack,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::send,
        onSelectText = onSelectText,
        onClearMessages = viewModel::clearMessages,
        onErrorShown = viewModel::onErrorShown,
        onFilePicked = viewModel::onFilePicked,
        onAttachmentRemoved = viewModel::onAttachmentRemoved,
        onNoCameraApp = viewModel::onNoCameraApp,
        modifier = modifier,
    )
}
