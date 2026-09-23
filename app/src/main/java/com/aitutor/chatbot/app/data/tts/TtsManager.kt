package com.aitutor.chatbot.app.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin wrapper over Android's [TextToSpeech] using the device's default voice. Per-language/
 * per-voice selection (Settings > Voice) is a later phase — this plays whichever voice the engine
 * currently defaults to for the spoken text's language.
 */
class TtsManager(context: Context) {

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId

    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(context.applicationContext) { }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _speakingMessageId.value = utteranceId
                }
                override fun onDone(utteranceId: String?) {
                    if (_speakingMessageId.value == utteranceId) _speakingMessageId.value = null
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (_speakingMessageId.value == utteranceId) _speakingMessageId.value = null
                }
            })
        }
    }

    fun speak(messageId: String, text: String) {
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun stop() {
        engine?.stop()
        _speakingMessageId.value = null
    }

    fun shutdown() {
        engine?.shutdown()
    }
}
