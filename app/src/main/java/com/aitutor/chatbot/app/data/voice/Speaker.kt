package com.aitutor.chatbot.app.data.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.aitutor.chatbot.app.domain.model.AnswerBlock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Why reading aloud stopped, or never started. */
enum class SpeechError {
    /** No usable text-to-speech engine on this device. */
    Unavailable,

    /**
     * The engine is there but has no English voice data.
     *
     * Recoverable, and worth saying so rather than reporting a flat failure: the data is a download
     * away in the system's own text-to-speech settings.
     */
    LanguageMissing,

    /** The engine accepted the text and then failed partway through. */
    Failed,
}

/** What the read-aloud button draws. */
data class SpeakingState(
    /** The message being read, or null when nothing is. */
    val messageId: Long? = null,
    val error: SpeechError? = null,
) {
    fun isSpeaking(id: Long): Boolean = messageId == id
}

/**
 * Reads an answer out loud.
 *
 * **This works with no connection**, which is the reason the voice is chosen rather than left to
 * the engine's default. Google's engine ships two kinds: local voices, and higher-quality ones
 * synthesised on Google's servers. The default is often a network voice, which is why read-aloud is
 * typically the first thing to stop working on a plane. [preferOfflineVoice] picks the best voice
 * that reports `isNetworkConnectionRequired == false`, so the common case needs nothing but the
 * phone.
 *
 * Owned by the application rather than by a screen: starting an engine takes about a second, and
 * paying that on every chat the student opens would be audible as a delay before the first word.
 */
class Speaker(context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(SpeakingState())
    val state: StateFlow<SpeakingState> = _state.asStateFlow()

    @Volatile private var engine: TextToSpeech? = null
    @Volatile private var initStatus: Int? = null
    @Volatile private var ready = false

    /** What to say once the engine finishes starting, if someone pressed play before it did. */
    @Volatile private var pending: Request? = null

    private data class Request(val messageId: Long, val text: String)

    /** The read-aloud button: pressing the message that is already playing stops it. */
    fun toggle(messageId: Long, text: String) {
        if (_state.value.isSpeaking(messageId)) stop() else speak(messageId, text)
    }

    /**
     * Reads [text], replacing anything already being read.
     *
     * Starting a second answer stops the first rather than queueing behind it — two answers read
     * back to back is never what the button press meant.
     */
    fun speak(messageId: Long, text: String) {
        if (text.isBlank()) return
        _state.value = SpeakingState(messageId = messageId)

        val current = engine
        if (current != null && ready) {
            enqueue(current, messageId, text)
            return
        }
        pending = Request(messageId, text)
        if (current == null) start()
    }

    fun stop() {
        pending = null
        runCatching { engine?.stop() }
        _state.value = SpeakingState()
    }

    /** Releases the engine. For the application's own teardown, not a screen's. */
    fun shutdown() {
        pending = null
        ready = false
        runCatching {
            engine?.stop()
            engine?.shutdown()
        }
        engine = null
        _state.value = SpeakingState()
    }

    // ---- Engine lifecycle ----

    private fun start() {
        runCatching {
            engine = TextToSpeech(appContext) { status ->
                initStatus = status
                configure()
            }
        }.onFailure {
            Log.w(TAG, "Could not create a TextToSpeech engine", it)
            fail(SpeechError.Unavailable)
            return
        }
        // The callback can land before or after the constructor returns, and only one of the two
        // orderings leaves `engine` assigned. Calling from both and making this idempotent covers
        // it without depending on which the platform picks.
        configure()
    }

    @Synchronized
    private fun configure() {
        if (ready) return
        val tts = engine ?: return
        val status = initStatus ?: return

        if (status != TextToSpeech.SUCCESS) {
            fail(SpeechError.Unavailable)
            return
        }
        val language = runCatching { tts.setLanguage(LANGUAGE) }.getOrDefault(TextToSpeech.ERROR)
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            fail(SpeechError.LanguageMissing)
            return
        }

        preferOfflineVoice(tts)
        tts.setOnUtteranceProgressListener(progress)
        ready = true

        pending?.let { request ->
            pending = null
            enqueue(tts, request.messageId, request.text)
        }
    }

    /**
     * Picks the best voice that needs no network.
     *
     * Falls through silently when there is none: the engine's own default still works online, and a
     * device with only network voices is better served by speaking than by refusing. `voices` is
     * known to throw on some OEM engines rather than return empty, hence the guard.
     */
    private fun preferOfflineVoice(tts: TextToSpeech) {
        val offline = runCatching {
            tts.voices.orEmpty()
                .filter { it.locale?.language == LANGUAGE.language }
                .filterNot { it.isNetworkConnectionRequired }
                .maxByOrNull { it.quality }
        }.getOrNull()

        if (offline == null) {
            Log.i(TAG, "No offline voice for ${LANGUAGE.language}; using the engine default")
            return
        }
        runCatching { tts.voice = offline }
            .onFailure { Log.w(TAG, "Could not select the offline voice", it) }
    }

    // ---- Speaking ----

    private fun enqueue(tts: TextToSpeech, messageId: Long, text: String) {
        // FLUSH on the first chunk drops whatever was playing; the rest queue behind it.
        val chunks = text.chunkForSpeech(maxLength())
        chunks.forEachIndexed { index, chunk ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val last = index == chunks.lastIndex
            val result = runCatching {
                tts.speak(chunk, mode, null, utteranceId(messageId, index, last))
            }.getOrDefault(TextToSpeech.ERROR)
            if (result == TextToSpeech.ERROR) {
                fail(SpeechError.Failed)
                return
            }
        }
    }

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            // Only the final chunk ends the read. The ones before it are the same answer continuing.
            if (utteranceId?.endsWith(LAST_SUFFIX) != true) return
            if (_state.value.messageId == messageIdOf(utteranceId)) _state.value = SpeakingState()
        }

        @Deprecated("Kept for engines that have not moved to the two-argument overload")
        override fun onError(utteranceId: String?) = fail(SpeechError.Failed)

        override fun onError(utteranceId: String?, errorCode: Int) {
            Log.w(TAG, "TextToSpeech error $errorCode on $utteranceId")
            fail(SpeechError.Failed)
        }
    }

    private fun fail(error: SpeechError) {
        pending = null
        _state.value = SpeakingState(error = error)
    }

    companion object {
        private const val TAG = "Speaker"
        private const val LAST_SUFFIX = ":last"

        /** Answers are read in English, matching the one language dictation accepts. */
        val LANGUAGE: Locale = Locale.US

        /** A conservative floor for engines whose reported maximum is implausible. */
        private const val MIN_CHUNK = 200

        private fun maxLength(): Int =
            runCatching { TextToSpeech.getMaxSpeechInputLength() }
                .getOrDefault(MIN_CHUNK)
                .coerceAtLeast(MIN_CHUNK)

        private fun utteranceId(messageId: Long, index: Int, last: Boolean): String =
            "$messageId:$index" + if (last) LAST_SUFFIX else ""

        private fun messageIdOf(utteranceId: String): Long? =
            utteranceId.substringBefore(':').toLongOrNull()

        /**
         * The answer as one piece of speech.
         *
         * Each block is made to end in punctuation before they are joined, because a bullet's term
         * runs straight into its text otherwise — "Subtract 5 both sides" instead of
         * "Subtract 5. Both sides." The pause is the difference between a list and a run-on.
         */
        fun speakableText(blocks: List<AnswerBlock>): String = blocks.joinToString(" ") { block ->
            when (block) {
                is AnswerBlock.Paragraph -> block.text.endingInAStop()
                is AnswerBlock.Bullet -> "${block.term.endingInAStop()} ${block.text.endingInAStop()}"
            }
        }.trim()

        private fun String.endingInAStop(): String {
            val trimmed = trim()
            return if (trimmed.isEmpty() || trimmed.last() in STOPS) trimmed else "$trimmed."
        }

        private const val STOPS = ".!?,:;"

    }
}


    /**
     * Splits text the engine would otherwise reject, on sentence ends where possible.
     *
     * `speak` silently drops anything past its maximum, so a long answer would stop mid-word
     * with no error to report.
     */
    internal fun String.chunkForSpeech(limit: Int): List<String> {
        if (length <= limit) return listOf(this)
        val chunks = mutableListOf<String>()
        var rest = this
        while (rest.length > limit) {
            val window = rest.take(limit)
            // Break at the last sentence end, then the last space, then mid-word as a last resort.
            val cut = window.lastIndexOfAny(charArrayOf('.', '!', '?'))
                .takeIf { it > limit / 2 }
                ?.plus(1)
                ?: window.lastIndexOf(' ').takeIf { it > limit / 2 }
                ?: limit
            chunks += rest.take(cut).trim()
            rest = rest.drop(cut)
        }
        if (rest.isNotBlank()) chunks += rest.trim()
        return chunks.filter { it.isNotBlank() }
    }
