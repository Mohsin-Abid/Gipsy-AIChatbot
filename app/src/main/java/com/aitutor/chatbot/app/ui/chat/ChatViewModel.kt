package com.aitutor.chatbot.app.ui.chat

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.R
import android.net.Uri
import com.aitutor.chatbot.app.data.api.TutorApiNotConfiguredException
import com.aitutor.chatbot.app.data.attachment.AttachmentError
import com.aitutor.chatbot.app.data.attachment.AttachmentException
import com.aitutor.chatbot.app.data.attachment.AttachmentStore
import com.aitutor.chatbot.app.data.voice.VoiceError
import com.aitutor.chatbot.app.data.voice.VoiceEvent
import com.aitutor.chatbot.app.data.voice.VoiceRecognizer
import com.aitutor.chatbot.app.data.prefs.UserPreferencesRepository
import com.aitutor.chatbot.app.data.repository.ChatRepository
import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.Attachment
import com.aitutor.chatbot.app.domain.model.ScanSource
import com.aitutor.chatbot.app.domain.model.ChatMessage
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What the chat screen draws. */
data class ChatUiState(
    val tool: StudyTool = StudyTool.Chatbot,
    val subject: String = "",
    val storedMessages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    /** A file chosen but not yet sent, shown as a chip above the composer. */
    val pendingAttachment: Attachment? = null,
    /** True while a picked file is being copied and downscaled. */
    val attaching: Boolean = false,
    /** Null while the voice panel is closed. */
    val voice: VoiceUiState? = null,
    /** Set when a request failed; cleared as soon as it has been shown and dismissed. */
    @param:StringRes val errorRes: Int? = null,
) {
    /**
     * What the list draws: the stored thread, plus the typing row while a request is in flight. The
     * indicator belongs to the request rather than the conversation, so it is never written down.
     */
    val messages: List<ChatMessage>
        get() = if (sending) storedMessages + ChatMessage.Typing else storedMessages

    /** A thread with nothing in it yet gets the opening prompt instead of an empty list. */
    val isEmpty: Boolean get() = storedMessages.isEmpty() && !sending

    /**
     * An attachment on its own is a question — "what is this?" is implied by a photo of a worksheet,
     * so a typed draft is not required once a file is attached.
     */
    val canSend: Boolean
        get() = (draft.isNotBlank() || pendingAttachment != null) && !sending && !attaching

    val newestAnswerBlocks: List<AnswerBlock>
        get() = storedMessages.filterIsInstance<ChatMessage.Assistant>().lastOrNull()?.blocks.orEmpty()
}

/** What the voice panel draws while someone is dictating. */
data class VoiceUiState(
    val listening: Boolean = false,
    /** Text the recogniser has committed to. */
    val transcript: String = "",
    /** The piece it is still revising, drawn greyed behind the committed text. */
    val partial: String = "",
    /** Microphone level, 0..1, already smoothed. */
    val level: Float = 0f,
    @param:StringRes val errorRes: Int? = null,
) {
    /** Everything heard so far, committed and provisional. */
    val spokenText: String get() = listOf(transcript, partial).filter { it.isNotBlank() }.joinToString(" ")

    val canAccept: Boolean get() = spokenText.isNotBlank()
}

/**
 * One conversation.
 *
 * The chat is created lazily: navigating in with no id opens an empty thread and the row is written
 * the first time a question is sent, so backing out of a tool you opened by mistake leaves no chat
 * behind in the history.
 */
class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    private val chats: ChatRepository,
    private val preferences: UserPreferencesRepository,
    private val attachments: AttachmentStore,
    private val voice: VoiceRecognizer,
) : ViewModel() {

    private val tool: StudyTool = savedStateHandle.get<String>(Route.Chat.TOOL_KEY)
        ?.let { name -> StudyTool.entries.firstOrNull { it.name == name } }
        ?: StudyTool.Chatbot

    private var chatId: Long? = savedStateHandle.get<Long>(Route.Chat.CHAT_ID_KEY)
        ?.takeIf { it > 0 }

    private val _state = MutableStateFlow(ChatUiState(tool = tool))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        chatId?.let(::observe)
    }

    private fun observe(id: Long) = viewModelScope.launch {
        chats.header(id)?.let { header ->
            _state.update { it.copy(tool = header.tool, subject = header.subject) }
        }
        chats.observeMessages(id).collect { messages ->
            _state.update { it.copy(storedMessages = messages) }
        }
    }

    fun onDraftChange(value: String) = _state.update { it.copy(draft = value) }

    /**
     * Copies a picked file into app storage and holds it as the pending attachment.
     *
     * Replacing an existing pending file deletes the old copy — it was never sent, so nothing refers
     * to it and leaving it behind would just accumulate.
     */
    fun onFilePicked(uri: Uri, source: ScanSource) {
        viewModelScope.launch {
            _state.update { it.copy(attaching = true, errorRes = null) }
            val previous = _state.value.pendingAttachment
            val result = attachments.store(uri, source)
            result.getOrNull()?.let { previous?.let { old -> attachments.delete(old) } }
            _state.update { current ->
                current.copy(
                    attaching = false,
                    pendingAttachment = result.getOrNull() ?: current.pendingAttachment,
                    errorRes = result.exceptionOrNull()?.toMessageRes(),
                )
            }
        }
    }

    /** Takes the pending file back off, deleting the copy with it. */
    fun onAttachmentRemoved() {
        val pending = _state.value.pendingAttachment ?: return
        _state.update { it.copy(pendingAttachment = null) }
        viewModelScope.launch { attachments.delete(pending) }
    }

    fun onNoCameraApp() = _state.update { it.copy(errorRes = R.string.attach_error_no_camera) }

    // ---- Dictation ----

    private var listening: Job? = null

    /**
     * Opens the voice panel and starts listening.
     *
     * Availability is checked first so a device with no recognition service says so immediately
     * rather than opening a panel that will never hear anything.
     */
    fun onVoiceStarted() {
        if (!voice.isAvailable()) {
            _state.update { it.copy(voice = VoiceUiState(errorRes = R.string.voice_error_unavailable)) }
            return
        }
        _state.update { it.copy(voice = VoiceUiState(listening = true)) }
        startListening()
    }

    /**
     * The microphone permission was refused.
     *
     * The panel still opens, carrying the reason — refusing silently would leave a student tapping
     * a mic button that appears to do nothing.
     */
    fun onVoicePermissionDenied(askableAgain: Boolean) = _state.update {
        it.copy(
            voice = VoiceUiState(
                errorRes = if (askableAgain) {
                    R.string.voice_error_permission
                } else {
                    R.string.voice_error_permission_settings
                },
            )
        )
    }

    /** Stops listening but keeps the panel, so what was heard can still be read and accepted. */
    fun onVoicePaused() {
        listening?.cancel()
        listening = null
        _state.update { current ->
            current.copy(voice = current.voice?.copy(listening = false, level = 0f))
        }
    }

    /** The mic button: a toggle between listening and held. */
    fun onVoiceToggled() {
        if (_state.value.voice?.listening == true) onVoicePaused() else resumeListening()
    }

    /** The ✓ — the design calls it "Add text to message", so it fills the composer. */
    fun onVoiceAccepted() {
        val spoken = _state.value.voice?.spokenText.orEmpty()
        onVoiceCancelled()
        if (spoken.isBlank()) return
        _state.update { current ->
            current.copy(draft = listOf(current.draft, spoken).filter { it.isNotBlank() }.joinToString(" "))
        }
    }

    /** The × — closes the panel and throws away what was heard. */
    fun onVoiceCancelled() {
        listening?.cancel()
        listening = null
        _state.update { it.copy(voice = null) }
    }

    private fun resumeListening() {
        _state.update { current ->
            current.copy(voice = (current.voice ?: VoiceUiState()).copy(listening = true, errorRes = null))
        }
        startListening()
    }

    private fun startListening() {
        listening?.cancel()
        // Main, not IO: SpeechRecognizer may only be touched from the main thread.
        listening = viewModelScope.launch {
            voice.listen().collect { event ->
                _state.update { current ->
                    val panel = current.voice ?: return@update current
                    current.copy(voice = panel.reduce(event))
                }
            }
        }
    }

    fun onErrorShown() = _state.update { it.copy(errorRes = null) }

    /**
     * Sends the draft.
     *
     * The typing indicator is a local addition to the list, not a stored row — it belongs to this
     * request and disappears with it, whether the request succeeded or failed.
     */
    fun send() {
        val current = _state.value
        if (!current.canSend) return
        val question = current.draft.trim()
        val attachment = current.pendingAttachment

        viewModelScope.launch {
            _state.update {
                it.copy(draft = "", pendingAttachment = null, sending = true, errorRes = null)
            }
            val profile = preferences.settings.first().profile
            val id = chatId ?: chats.createChat(
                tool = tool,
                // A file sent without a question is titled by the file, so history stays readable.
                title = question.ifEmpty { attachment?.fileName.orEmpty() }.toTitle(),
                subject = profile.subjects.firstOrNull()?.name.orEmpty(),
            ).also { created ->
                chatId = created
                observe(created)
            }

            val result = chats.send(
                chatId = id,
                question = question,
                attachment = attachment,
                profile = profile,
            )
            _state.update { current ->
                current.copy(
                    sending = false,
                    errorRes = result.exceptionOrNull()?.toMessageRes(),
                )
            }
        }
    }

    fun clearMessages() {
        val id = chatId ?: return
        viewModelScope.launch { chats.clearMessages(id) }
    }

    fun deleteChat() {
        val id = chatId ?: return
        viewModelScope.launch { chats.delete(id) }
    }
}

/** Folds one recogniser event into the panel's state. */
private fun VoiceUiState.reduce(event: VoiceEvent): VoiceUiState = when (event) {
    is VoiceEvent.Level -> copy(level = VoiceRecognizer.smoothLevel(level, event.value))
    is VoiceEvent.Partial -> copy(partial = event.text)
    // A committed segment joins the transcript and clears the tail, ready for the next one.
    is VoiceEvent.Final -> copy(
        transcript = listOf(transcript, event.text).filter { it.isNotBlank() }.joinToString(" "),
        partial = "",
    )
    is VoiceEvent.Failed -> copy(
        listening = false,
        level = 0f,
        partial = "",
        errorRes = event.reason.toMessageRes(),
    )
}

@StringRes
private fun VoiceError.toMessageRes(): Int = when (this) {
    VoiceError.NoSpeechHeard, VoiceError.SilenceTimeout -> R.string.voice_error_no_speech
    VoiceError.PermissionDenied -> R.string.voice_error_permission
    VoiceError.Busy -> R.string.voice_error_busy
    VoiceError.Network -> R.string.voice_error_network
    VoiceError.LanguageUnavailable -> R.string.voice_error_language
    VoiceError.Unavailable -> R.string.voice_error_unavailable
}

/**
 * A chat's title is its opening question, shortened. Naming a conversation after what was asked is
 * what makes the history list readable without opening anything.
 */
private fun String.toTitle(): String =
    if (length <= TITLE_MAX_LENGTH) this else take(TITLE_MAX_LENGTH).trimEnd() + "…"

private const val TITLE_MAX_LENGTH = 60

@StringRes
private fun Throwable.toMessageRes(): Int = when (this) {
    is TutorApiNotConfiguredException -> R.string.chat_error_no_api
    is AttachmentException -> when (error) {
        is AttachmentError.TooLarge -> R.string.attach_error_too_large
        AttachmentError.Unsupported -> R.string.attach_error_unsupported
        AttachmentError.Unreadable -> R.string.attach_error_unreadable
    }

    else -> R.string.chat_error_generic
}
