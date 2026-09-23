package com.aitutor.chatbot.app.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.data.ocr.EmptyExtractionException
import com.aitutor.chatbot.app.data.ocr.ImageTextExtractor
import com.aitutor.chatbot.app.data.chat.ChatRepository
import com.aitutor.chatbot.app.data.chat.ExpandDirection
import com.aitutor.chatbot.app.data.firebase.ReportRepository
import com.aitutor.chatbot.app.data.firebase.UserProfileRepository
import com.aitutor.chatbot.app.data.tts.TtsManager
import com.aitutor.chatbot.app.domain.model.UserPlan
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.ReportReason
import com.aitutor.chatbot.app.domain.model.modes
import com.aitutor.chatbot.app.domain.model.starterPromptsFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatDetailArgs(val modeId: String, val chatId: String?)

sealed interface AttachmentState {
    data object None : AttachmentState
    data class Extracting(val label: String) : AttachmentState
    data class Extracted(val label: String, val text: String) : AttachmentState
    data class Failed(val label: String, val uri: Uri, val message: String) : AttachmentState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatDetailViewModel(
    args: ChatDetailArgs,
    private val chatRepository: ChatRepository,
    private val reportRepository: ReportRepository,
    private val userProfileRepository: UserProfileRepository,
    private val imageTextExtractor: ImageTextExtractor,
    val ttsManager: TtsManager,
) : ViewModel() {

    val mode: Mode = modes.firstOrNull { it.id.name == args.modeId } ?: modes.first()

    private val chatIdState = MutableStateFlow(args.chatId)
    val chatId: StateFlow<String?> = chatIdState.asStateFlow()

    private val draft = MutableStateFlow("")
    val draftText: StateFlow<String> = draft.asStateFlow()

    private val sending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = sending.asStateFlow()

    private val sendError = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = sendError.asStateFlow()

    private val paywallVisible = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = paywallVisible.asStateFlow()

    private val _attachment = MutableStateFlow<AttachmentState>(AttachmentState.None)
    val attachment: StateFlow<AttachmentState> = _attachment.asStateFlow()

    val starterPrompts: List<String> = starterPromptsFor(mode.id)

    val plan: StateFlow<UserPlan> = userProfileRepository.plan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPlan())

    val chatTitle: StateFlow<String> = chatIdState.filterNotNull()
        .flatMapLatest { chatRepository.chatTitle(it) }
        .map { it ?: mode.title }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), mode.title)

    val messages: StateFlow<UiState<List<Message>>> = chatIdState.filterNotNull()
        .flatMapLatest { id -> chatRepository.messages(id) }
        .map<List<Message>, UiState<List<Message>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Couldn't load this chat.", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        val existingId = args.chatId
        if (existingId == null) {
            viewModelScope.launch {
                val newId = chatRepository.createChat(mode.id)
                chatIdState.value = newId
                chatRepository.startSync(newId)
            }
        } else {
            chatRepository.startSync(existingId)
        }
    }

    fun onDraftChange(text: String) {
        draft.value = text
    }

    fun onSend() {
        val text = draft.value
        val id = chatIdState.value
        if (text.isBlank() || id == null || sending.value) return
        if (!plan.value.canAskQuestion) {
            paywallVisible.value = true
            return
        }
        draft.value = ""
        sendError.value = null
        viewModelScope.launch {
            sending.value = true
            chatRepository.sendMessage(id, mode, text)
                .onSuccess { userProfileRepository.recordQuestionAsked() }
                .onFailure { sendError.value = it.message ?: "Couldn't reach the AI. Check your connection and try again." }
            sending.value = false
        }
    }

    fun onPaywallDismissed() {
        paywallVisible.value = false
    }

    fun onAttachmentRequested(): Boolean {
        if (!plan.value.isPremium) {
            paywallVisible.value = true
            return false
        }
        return true
    }

    /** Runs on-device OCR, then hands the text back for review rather than inserting it blindly. */
    fun onImagePicked(uri: Uri, label: String) {
        _attachment.value = AttachmentState.Extracting(label)
        viewModelScope.launch {
            imageTextExtractor.extract(uri)
                .onSuccess { text -> _attachment.value = AttachmentState.Extracted(label, text) }
                .onFailure { error ->
                    _attachment.value = AttachmentState.Failed(
                        label = label,
                        uri = uri,
                        message = if (error is EmptyExtractionException) {
                            "No readable text found. Try a clearer, better-lit photo."
                        } else {
                            "Couldn't read that file. Try again or pick a different one."
                        },
                    )
                }
        }
    }

    fun onRetryExtraction() {
        val failed = _attachment.value as? AttachmentState.Failed ?: return
        onImagePicked(failed.uri, failed.label)
    }

    /** Appends to whatever is already typed, so a scan supplements the question instead of replacing it. */
    fun onExtractedTextConfirmed(text: String) {
        val existing = draft.value
        draft.value = if (existing.isBlank()) text else "$existing\n\n$text"
        _attachment.value = AttachmentState.None
    }

    fun onAttachmentCleared() {
        _attachment.value = AttachmentState.None
    }

    fun onStarterPromptSelected(prompt: String) {
        draft.value = prompt
    }

    fun onRetry() {
        val id = chatIdState.value ?: return
        if (sending.value) return
        sendError.value = null
        viewModelScope.launch {
            sending.value = true
            chatRepository.regenerate(id, mode)
                .onFailure { sendError.value = it.message ?: "Couldn't reach the AI. Check your connection and try again." }
            sending.value = false
        }
    }

    fun onRegenerate() = onRetry()

    fun onMakeShorter(message: Message) = expand(message, ExpandDirection.Shorter)
    fun onMakeLonger(message: Message) = expand(message, ExpandDirection.Longer)

    private fun expand(message: Message, direction: ExpandDirection) {
        val id = chatIdState.value ?: return
        if (sending.value) return
        viewModelScope.launch {
            sending.value = true
            chatRepository.expand(id, mode, message, direction)
                .onFailure { sendError.value = it.message ?: "Couldn't reach the AI. Check your connection and try again." }
            sending.value = false
        }
    }

    fun onFeedback(message: Message, liked: Boolean, disliked: Boolean) {
        val id = chatIdState.value ?: return
        viewModelScope.launch { chatRepository.setFeedback(id, message.id, liked, disliked) }
    }

    fun onReport(message: Message, reason: ReportReason, detail: String?) {
        val id = chatIdState.value ?: return
        viewModelScope.launch { reportRepository.submit(id, message.id, reason, detail) }
    }

    fun onClearChat() {
        val id = chatIdState.value ?: return
        viewModelScope.launch { chatRepository.clearChat(id) }
    }

    fun onRenameChat(title: String) {
        val id = chatIdState.value ?: return
        viewModelScope.launch { chatRepository.renameChat(id, title) }
    }

    fun onDeleteChat(onDone: () -> Unit) {
        val id = chatIdState.value ?: return
        viewModelScope.launch {
            chatRepository.deleteChat(id)
            onDone()
        }
    }

    fun speak(message: Message) {
        if (ttsManager.speakingMessageId.value == message.id) {
            ttsManager.stop()
        } else {
            ttsManager.speak(message.id, message.text)
        }
    }

    override fun onCleared() {
        ttsManager.stop()
        super.onCleared()
    }
}
