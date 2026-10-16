package com.askylix.app.ui.chat

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.askylix.app.R
import com.askylix.app.data.attachment.AttachmentDownloader
import com.askylix.app.data.attachment.AttachmentError
import com.askylix.app.data.attachment.AttachmentException
import com.askylix.app.data.attachment.AttachmentStore
import com.askylix.app.data.auth.AuthRepository
import com.askylix.app.data.prefs.UserPreferencesRepository
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.repository.ChatRepository
import com.askylix.app.data.repository.Question
import com.askylix.app.data.repository.SendResult
import com.askylix.app.data.session.ThreadState
import com.askylix.app.data.session.StoredMessage
import com.askylix.app.data.session.toThread
import com.askylix.app.data.voice.SpeechError
import com.askylix.app.data.voice.Speaker
import com.askylix.app.data.voice.VoiceError
import com.askylix.app.data.voice.VoiceEvent
import com.askylix.app.data.voice.VoiceRecognizer
import com.askylix.app.domain.model.AnswerBlock
import com.askylix.app.domain.model.Attachment
import com.askylix.app.domain.model.ChatMessage
import com.askylix.app.domain.model.ChatTextSize
import com.askylix.app.domain.model.Rating
import com.askylix.app.domain.model.ScanSource
import com.askylix.app.domain.model.SendStatus
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.navigation.Route
import com.askylix.app.ui.AppMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** What the chat screen draws. */
data class ChatUiState(
    val tool: StudyTool = StudyTool.Chatbot,
    /** The conversation's id on the server, or null until the first question is sent and there is a chat to act on. */
    val chatId: String? = null,
    /** The name it has in History — what Rename starts from. */
    val title: String = "",
    val textSize: ChatTextSize = ChatTextSize.Default,
    /** The thread as drawn: day separators in, the typing row not. */
    val storedMessages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    /** The first page of an existing chat is being fetched. */
    val loading: Boolean = false,
    /** Fetching the page before the oldest message held. */
    val loadingOlder: Boolean = false,
    /** There are older messages on the server than the ones shown. */
    val hasOlder: Boolean = false,
    /** Why the chat could not be read, when it could not and there is nothing to show. */
    val loadError: ApiErrorCode? = null,
    /** The chat was deleted, on this or another device: the screen says so and closes. */
    val gone: Boolean = false,
    val signedIn: Boolean = true,
    /** Asked to send while signed out: the "sign in to continue" prompt is showing. */
    val needsSignIn: Boolean = false,
    /** A file chosen but not yet sent, shown as a chip above the composer. */
    val pendingAttachment: Attachment? = null,
    /** True while a picked file is being copied and downscaled. */
    val attaching: Boolean = false,
    /** The message whose file is being downloaded, and how far along it is (0 to 100, or null if unknown). */
    val downloadingMessageId: String? = null,
    val downloadPercent: Int? = null,
    /** Null while the voice panel is closed. */
    val voice: VoiceUiState? = null,
    /** The answer being read aloud, or null when nothing is. */
    val speakingMessageId: String? = null,
    /** Set when a request failed; cleared as soon as it has been shown and dismissed. */
    @param:StringRes val errorRes: Int? = null,
) {
    /**
     * What the list draws: the thread, plus the typing row while a reply is on its way. The indicator
     * belongs to the request rather than the conversation, so it is never part of the thread.
     */
    val messages: List<ChatMessage>
        get() = if (sending) storedMessages + ChatMessage.Typing else storedMessages

    /** A thread with nothing in it yet gets the opening prompt instead of an empty list. */
    val isEmpty: Boolean get() = storedMessages.isEmpty() && !sending && !loading && loadError == null

    /**
     * An attachment on its own is a question — "what is this?" is implied by a photo of a worksheet,
     * so a typed draft is not required once a file is attached.
     */
    val canSend: Boolean
        get() = (draft.isNotBlank() || pendingAttachment != null) && !sending && !attaching

    /** Rename, share and delete act on a saved conversation, and an empty one is not saved yet. */
    val hasChat: Boolean get() = chatId != null

    /**
     * Work that leaving the screen would cut short — what Back asks about before it goes. A file still
     * being added is lost if the screen closes. A question on its way is not: it keeps running and the
     * answer is in the chat when the person returns.
     */
    val isBusy: Boolean get() = attaching

    val newestAnswerBlocks: List<AnswerBlock>
        get() = storedMessages.filterIsInstance<ChatMessage.Assistant>().lastOrNull()?.blocks.orEmpty()
}

/** Things the screen is told once, rather than state it keeps drawing. */
sealed interface ChatEvent {
    /** The free daily questions are used up: show the Pro screen. */
    data object OpenPremium : ChatEvent

    /** A downloaded attachment is ready to be opened. */
    data class OpenFile(val file: File, val mimeType: String) : ChatEvent
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
 * Opening one reads it from the server. A new chat does not exist until its first question is sent:
 * navigating in with no id opens an empty thread, and the server creates and names the chat when the
 * first question arrives, so backing out of a tool opened by mistake leaves no chat behind.
 *
 * A question is asked through the repository, which keeps it running if this screen is left. The text
 * of one that fails goes back into the composer, and sending it again reuses the same key so that a
 * reply lost on the way is not paid for twice.
 */
class ChatViewModel(
    savedStateHandle: SavedStateHandle,
    private val chats: ChatRepository,
    private val auth: AuthRepository,
    private val preferences: UserPreferencesRepository,
    private val attachments: AttachmentStore,
    private val downloader: AttachmentDownloader,
    private val voice: VoiceRecognizer,
    private val speaker: Speaker,
    private val messages: AppMessages,
) : ViewModel() {

    private val tool: StudyTool = savedStateHandle.get<String>(Route.Chat.TOOL_KEY)
        ?.let { name -> StudyTool.entries.firstOrNull { it.name == name } }
        ?: StudyTool.Chatbot

    private var chatId: String? = savedStateHandle.get<String>(Route.Chat.CHAT_ID_KEY)?.takeIf { it.isNotEmpty() }

    // A question placed in the composer by a Home chip. Not sent: the student does that.
    private val _state = MutableStateFlow(
        ChatUiState(tool = tool, draft = savedStateHandle.get<String>(Route.Chat.DRAFT_KEY).orEmpty())
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events: Flow<ChatEvent> = _events.receiveAsFlow()

    // The first question of a new chat is shown before the chat exists, so it lives here until it does.
    private var firstQuestion: ChatMessage.User? = null
    private var threadSending = false
    private var asking = false
    private var thread: ThreadState? = null
    private var announcedGone = false

    // The text and key of a question that failed, so that the same text sent again is the same question.
    private var failedText: String? = null
    private var failedKey: String? = null

    init {
        chatId?.let { id ->
            _state.update { it.copy(chatId = id, loading = true) }
            observe(id)
            load(force = false)
        }
        observeSpeaker()
        observeTextSize()
        observeSignIn()
    }

    /** The size is the person's choice for every chat, so it is read from settings, not kept here. */
    private fun observeTextSize() = viewModelScope.launch {
        preferences.settings.map { it.chatTextSize }.distinctUntilChanged().collect { size ->
            _state.update { it.copy(textSize = size) }
        }
    }

    private fun observeSignIn() = viewModelScope.launch {
        auth.user.map { it != null }.distinctUntilChanged().collect { signedIn ->
            // Signing in from the prompt closes it; the text is still in the composer to be sent.
            _state.update { it.copy(signedIn = signedIn, needsSignIn = it.needsSignIn && !signedIn) }
        }
    }

    /**
     * Mirrors the shared speaker into this screen's state.
     *
     * The speaker outlives the screen, so what it is reading is read from it rather than tracked
     * here — otherwise backing out mid-sentence and returning would show a stopped button over
     * audio that is still playing.
     */
    private fun observeSpeaker() = viewModelScope.launch {
        speaker.state.collect { speaking ->
            _state.update { current ->
                current.copy(
                    speakingMessageId = speaking.messageId,
                    errorRes = speaking.error?.toMessageRes() ?: current.errorRes,
                )
            }
        }
    }

    // ---- The thread ----

    private fun observe(id: String) = viewModelScope.launch {
        chats.thread(id).collect { held ->
            thread = held
            threadSending = held.sending
            publish()
        }
    }

    /** Rebuilds what the screen draws from the thread and the question that has no thread yet. */
    private fun publish() {
        val held = thread
        if (held?.gone == true && !announcedGone) {
            announcedGone = true
            messages.post(R.string.chat_error_deleted)
        }
        val drawn = held?.messages.orEmpty().toThread().let { list ->
            firstQuestion?.let { list + it } ?: list
        }
        _state.update { current ->
            current.copy(
                tool = held?.tool ?: current.tool,
                title = held?.title?.ifBlank { null } ?: current.title,
                storedMessages = drawn,
                sending = threadSending || asking,
                loading = held?.loading == true && held.messages.isEmpty(),
                loadingOlder = held?.loadingOlder == true,
                hasOlder = held?.hasOlder == true,
                loadError = if (held != null && held.messages.isEmpty()) held.error else null,
                gone = held?.gone == true,
            )
        }
    }

    /** Reads the chat from the server. Called on opening and on coming back to the front. */
    fun load(force: Boolean) {
        val id = chatId ?: return
        viewModelScope.launch { chats.loadThread(id, force) }
    }

    /** The screen came back to the front: a chat that changed on another device shows up. */
    fun onResume() = load(force = false)

    /** Scrolled to the top of what is held: fetch the page before it. */
    fun loadOlder() {
        val id = chatId ?: return
        val held = thread ?: return
        if (!held.hasOlder || held.loadingOlder) return
        viewModelScope.launch { chats.loadOlder(id) }
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

    /** The A− and A+ in the chat menu. The size is the same in every chat, and stays on this device. */
    fun setTextSize(size: ChatTextSize) {
        viewModelScope.launch { preferences.setChatTextSize(size) }
    }

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
        // Asked for here rather than after a failure: the local model is what makes dictation work
        // with no connection, and the moment someone is offline is too late to start downloading it.
        voice.warmUpOfflineModel()
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

    // ---- Reading aloud ----

    /**
     * The speaker button under an answer. Pressing the one already playing stops it.
     *
     * The text is rebuilt from the answer's blocks rather than stored, so it always matches what is
     * on screen.
     */
    fun onReadAloud(messageId: String) {
        val blocks = _state.value.storedMessages
            .filterIsInstance<ChatMessage.Assistant>()
            .firstOrNull { it.id == messageId }
            ?.blocks
            ?: return
        speaker.toggle(messageId, Speaker.speakableText(blocks))
    }

    fun onErrorShown() = _state.update { it.copy(errorRes = null) }

    // ---- Asking ----

    /**
     * Sends the draft.
     *
     * Signed out, it asks to sign in instead and keeps the text where it is. The typing row belongs to
     * the request and disappears with it, whether the request succeeded or failed.
     */
    fun send() {
        val current = _state.value
        if (!current.canSend) return
        if (!current.signedIn) {
            _state.update { it.copy(needsSignIn = true) }
            return
        }
        val text = current.draft.trim()
        val attachment = current.pendingAttachment
        // The same text sent again after a failure is the same question: it reuses the key, so a reply
        // that was lost on the way is returned instead of being asked, and counted, a second time.
        val key = failedKey?.takeIf { failedText == text } ?: UUID.randomUUID().toString()
        _state.update { it.copy(draft = "", pendingAttachment = null, errorRes = null) }
        ask(Question(tool = current.tool, chatId = chatId, text = text, attachment = attachment, idempotencyKey = key))
    }

    /** The Regenerate button under an answer: asks the same question again and swaps the answer. */
    fun regenerate(answerId: String) {
        val id = chatId ?: return
        if (_state.value.sending) return
        ask(
            Question(
                tool = _state.value.tool,
                chatId = id,
                text = "",
                idempotencyKey = UUID.randomUUID().toString(),
                regenerateMessageId = answerId,
            )
        )
    }

    /** "Try again" under a question the server saved but could not answer. */
    fun retryUnanswered(questionId: String) {
        val id = chatId ?: return
        val question = _state.value.storedMessages
            .filterIsInstance<ChatMessage.User>()
            .firstOrNull { it.id == questionId && it.status == SendStatus.Unanswered }
            ?: return
        if (_state.value.sending) return
        // The saved question comes out of the thread so it is not shown twice. Until the server offers a
        // way to retry a question it already holds, asking again is a new question.
        chats.discardUnanswered(id, questionId)
        ask(
            Question(
                tool = _state.value.tool,
                chatId = id,
                text = question.text,
                attachment = question.attachment,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        )
    }

    private fun ask(question: Question) {
        asking = true
        if (question.chatId == null) {
            firstQuestion = ChatMessage.User(
                id = ChatRepository.LOCAL_PREFIX + question.idempotencyKey,
                text = question.text,
                attachment = question.attachment,
                status = SendStatus.Sending,
            )
        }
        publish()

        viewModelScope.launch {
            try {
                when (val result = chats.send(question)) {
                    is SendResult.Answered -> adopt(result.chatId)
                    is SendResult.Unanswered -> {
                        adopt(result.chatId)
                        _state.update { it.copy(errorRes = R.string.chat_error_no_answer) }
                    }
                }
                failedText = null
                failedKey = null
            } catch (cancelled: CancellationException) {
                // The screen was left. The repository is still running the request; nothing to undo.
                throw cancelled
            } catch (failure: ApiError) {
                onSendFailed(question, failure)
            } finally {
                asking = false
                firstQuestion = null
                publish()
            }
        }
    }

    /** The server made (or confirmed) the chat: from now on the thread, not this screen, holds it. */
    private fun adopt(id: String) {
        if (chatId == id) return
        chatId = id
        _state.update { it.copy(chatId = id) }
        observe(id)
    }

    private fun onSendFailed(question: Question, failure: ApiError) {
        if (question.regenerateMessageId == null) {
            failedText = question.text
            failedKey = question.idempotencyKey
        }
        // The server turned the file itself away. Putting it back would only fail the same way on the next
        // send, so it is dropped and the question's text stays to be sent without it.
        val fileRefused = failure.refusesFile()
        if (fileRefused) question.attachment?.let { refused -> viewModelScope.launch { attachments.delete(refused) } }
        _state.update { current ->
            current.copy(
                // Back where it was typed, so it can be sent again. Not over something typed since.
                draft = if (question.regenerateMessageId == null && current.draft.isBlank()) question.text else current.draft,
                pendingAttachment = if (fileRefused) current.pendingAttachment else current.pendingAttachment ?: question.attachment,
                errorRes = failure.toSendMessageRes(),
                needsSignIn = failure.code == ApiErrorCode.AccountRequired,
            )
        }
        if (failure.code == ApiErrorCode.QuotaExceeded) _events.trySend(ChatEvent.OpenPremium)
    }

    /** The prompt was dismissed with "Not now". */
    fun onSignInPromptDismissed() = _state.update { it.copy(needsSignIn = false) }

    // ---- Changing the chat ----

    /** Renames the chat. Shown at once, put back with a notice if the server refuses. */
    fun rename(title: String) {
        val id = chatId ?: return
        val clean = title.toTitle().takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            try {
                chats.rename(id, clean)
            } catch (failure: ApiError) {
                messages.post(R.string.chat_error_rename)
            }
        }
    }

    /** Rates an answer; tapping the rating it already has takes it back. */
    fun rate(answerId: String, pressed: Rating) {
        val id = chatId ?: return
        val current = _state.value.storedMessages
            .filterIsInstance<ChatMessage.Assistant>()
            .firstOrNull { it.id == answerId }
            ?.rating
        viewModelScope.launch {
            try {
                chats.rate(id, answerId, if (current == pressed) null else pressed)
            } catch (failure: ApiError) {
                messages.post(R.string.chat_error_rate)
            }
        }
    }

    /**
     * Deletes the chat, then calls [onDeleted] — which is what leaves the screen. The server is asked
     * first: if it cannot be reached the chat is still there, and the person is told, not left looking
     * at a chat that is half gone.
     */
    fun deleteChat(onDeleted: () -> Unit) {
        val id = chatId ?: return
        viewModelScope.launch {
            try {
                chats.delete(id)
                onDeleted()
            } catch (failure: ApiError) {
                messages.post(R.string.chat_error_delete)
            }
        }
    }

    // ---- Attachments ----

    /** Tapping a file: this phone's copy, a download already made, or a fresh download, then open it. */
    fun openAttachment(messageId: String, attachment: Attachment) {
        val id = chatId
        if (_state.value.downloadingMessageId != null) return
        _state.update { it.copy(downloadingMessageId = messageId, downloadPercent = null) }

        viewModelScope.launch {
            val result = downloader.fetch(
                messageId = messageId,
                attachment = attachment,
                refreshLink = { id?.let { chats.refreshAttachmentLink(it, messageId) } },
                onProgress = { percent -> _state.update { it.copy(downloadPercent = percent) } },
            )
            _state.update { it.copy(downloadingMessageId = null, downloadPercent = null) }
            result.fold(
                onSuccess = { file -> _events.trySend(ChatEvent.OpenFile(file, attachment.mimeType)) },
                onFailure = { failure ->
                    _state.update { it.copy(errorRes = (failure as? ApiError)?.toFileMessageRes() ?: R.string.attach_error_download) }
                },
            )
        }
    }

    /**
     * Stops the audio, but does not shut the engine down — it belongs to the application and the
     * next chat will want it already started. Leaving an answer reading after the screen is gone
     * would be a voice with nothing on screen to explain it.
     */
    override fun onCleared() {
        if (_state.value.speakingMessageId != null) speaker.stop()
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
private fun SpeechError.toMessageRes(): Int = when (this) {
    SpeechError.Unavailable -> R.string.speech_error_unavailable
    SpeechError.LanguageMissing -> R.string.speech_error_language
    SpeechError.Failed -> R.string.speech_error_failed
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
internal fun String.toTitle(): String {
    // A question can now run over several lines, and a history row is a single one — cut at the
    // first newline it would read "Solve this:" and lose the rest of what was asked.
    val oneLine = replace(Regex("\\s+"), " ").trim()
    return if (oneLine.length <= TITLE_MAX_LENGTH) oneLine else oneLine.take(TITLE_MAX_LENGTH).trimEnd() + "…"
}

internal const val TITLE_MAX_LENGTH = 60

@StringRes
private fun Throwable.toMessageRes(): Int = when (this) {
    is AttachmentException -> when (error) {
        is AttachmentError.TooLarge -> R.string.attach_error_too_large
        AttachmentError.Unsupported -> R.string.attach_error_unsupported
        AttachmentError.Unreadable -> R.string.attach_error_unreadable
        AttachmentError.Empty -> R.string.attach_error_empty
        AttachmentError.Corrupt -> R.string.attach_error_corrupt
        AttachmentError.PasswordProtected -> R.string.attach_error_password
    }

    else -> R.string.chat_error_generic
}

/** What to tell the student when a question could not be sent or answered. */
@StringRes
internal fun ApiError.toSendMessageRes(): Int = when (code) {
    ApiErrorCode.NoConnection -> R.string.chat_error_offline
    ApiErrorCode.Timeout -> R.string.chat_error_timeout
    ApiErrorCode.QuotaExceeded -> R.string.chat_error_quota
    ApiErrorCode.RateLimited -> R.string.chat_error_rate_limited
    ApiErrorCode.UnknownTool -> R.string.chat_error_tool_off
    ApiErrorCode.AccountRequired -> R.string.chat_error_sign_in
    ApiErrorCode.NotFound -> R.string.chat_error_deleted
    else -> toFileMessageRes() ?: R.string.chat_error_generic
}

/** Whether the server refused the attached file itself, as opposed to the question or the connection. */
internal fun ApiError.refusesFile(): Boolean =
    code == ApiErrorCode.FileTooLarge || code == ApiErrorCode.UnsupportedFileType || code == ApiErrorCode.FileUnreadable

/** The messages for a file the server refused or could not be fetched, or null if the error is not about a file. */
@StringRes
internal fun ApiError.toFileMessageRes(): Int? = when (code) {
    ApiErrorCode.FileTooLarge -> R.string.attach_error_too_large
    ApiErrorCode.UnsupportedFileType -> R.string.attach_error_unsupported
    ApiErrorCode.FileUnreadable -> R.string.attach_error_unreadable
    ApiErrorCode.NoConnection, ApiErrorCode.Timeout -> R.string.chat_error_offline
    else -> null
}
