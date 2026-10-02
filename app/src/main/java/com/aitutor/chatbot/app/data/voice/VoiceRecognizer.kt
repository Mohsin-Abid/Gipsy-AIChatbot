package com.aitutor.chatbot.app.data.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.MainThread
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** What the panel needs to know while someone is speaking. */
sealed interface VoiceEvent {
    /** Microphone level, already normalised to 0..1. Drives the waveform. */
    data class Level(val value: Float) : VoiceEvent

    /** Text the recogniser is still revising — the grey tail in the panel. */
    data class Partial(val text: String) : VoiceEvent

    /** Text the recogniser has committed to. */
    data class Final(val text: String) : VoiceEvent

    data class Failed(val reason: VoiceError) : VoiceEvent
}

/**
 * Speech to text, using whatever recognition service the device provides.
 *
 * Nothing is recorded to a file and nothing is sent to this app's own API — the recogniser is handed
 * the microphone and hands back text. Note that the *system* recogniser is Google's on most phones
 * and may do its work on Google's servers; only [Engine.OnDevice] is local by construction, and it
 * is not available everywhere. That is the platform's behaviour, not a choice this class makes.
 *
 * The platform exposes this as an eight-callback listener that must be touched only from the main
 * thread. That is wrapped here into a [Flow] of the four things a screen actually cares about.
 */
class VoiceRecognizer(context: Context) {

    private val appContext = context.applicationContext

    /** Which of the device's recognisers is being used. Most phones offer both. */
    private enum class Engine { System, OnDevice }

    /**
     * Whether this device can do it at all.
     *
     * Requires the `android.speech.RecognitionService` `<queries>` entry to see past package
     * visibility on API 30+.
     */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext) || onDeviceAvailable()

    /**
     * Listens until the flow is cancelled.
     *
     * Two behaviours matter here beyond bridging the callbacks:
     *
     * **Continuous dictation.** [SpeechRecognizer] stops at the first pause, which is wrong for
     * dictating more than a sentence, so a committed result restarts it and the caller accumulates
     * the pieces.
     *
     * **Falling back between engines.** A device can advertise a recogniser that then refuses the
     * request — most commonly a Pixel or Samsung whose on-device English pack was never downloaded.
     * The first such failure switches to the other engine and tries once more, rather than telling
     * a student their phone cannot understand English while a working recogniser sits beside it.
     */
    @MainThread
    fun listen(): Flow<VoiceEvent> = callbackFlow {
        var finished = false
        var recognizer: SpeechRecognizer? = null
        var engine = preferredEngine()
        var switched = false

        if (engine == null) {
            trySend(VoiceEvent.Failed(VoiceError.Unavailable))
            close()
            return@callbackFlow
        }

        lateinit var listener: RecognitionListener

        fun startWith(target: Engine) {
            recognizer?.let { old -> runCatching { old.destroy() } }
            val created = create(target)
            recognizer = created
            if (created == null) {
                trySend(VoiceEvent.Failed(VoiceError.Unavailable))
                close()
                return
            }
            created.setRecognitionListener(listener)
            created.startListening(recognizerIntent())
        }

        listener = object : RecognitionListener {
            override fun onRmsChanged(rmsdB: Float) {
                trySend(VoiceEvent.Level(normaliseRms(rmsdB)))
            }

            override fun onPartialResults(partialResults: Bundle?) {
                firstResult(partialResults)?.let { trySend(VoiceEvent.Partial(it)) }
            }

            override fun onResults(results: Bundle?) {
                firstResult(results)?.let { trySend(VoiceEvent.Final(it)) }
                // A pause ended the segment, not the dictation. Pick the microphone back up.
                if (!finished) recognizer?.startListening(recognizerIntent())
            }

            override fun onError(error: Int) {
                // The raw code and the engine are logged deliberately: on a device that advertises
                // a recogniser and then refuses, this line is the only evidence of which did what.
                Log.w(TAG, "SpeechRecognizer error $error on $engine")
                val reason = VoiceError.fromCode(error)
                val other = engine?.let(::otherEngine)
                when {
                    finished -> Unit

                    // A silent gap or an empty result is not worth surfacing — keep listening.
                    VoiceError.isRetryable(reason) -> recognizer?.startListening(recognizerIntent())

                    // This engine cannot serve the request; the other one on this device may.
                    !switched && other != null && VoiceError.isEngineFailure(reason) -> {
                        if (engine == Engine.OnDevice) requestOnDeviceModel(recognizer)
                        switched = true
                        engine = other
                        startWith(other)
                    }

                    else -> {
                        trySend(VoiceEvent.Failed(reason))
                        close()
                    }
                }
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

        startWith(engine)

        awaitClose {
            finished = true
            runCatching {
                recognizer?.stopListening()
                recognizer?.destroy()
            }
        }
    }

    /**
     * The engine to try first.
     *
     * The ordinary system recogniser leads, because it is the one every Android dictation uses and
     * the one that works out of the box on a Pixel or a Samsung. The on-device engine is preferable
     * in principle — it needs no network and keeps the audio local by construction — but it only
     * works once its language pack has been downloaded, and reporting itself available says nothing
     * about that. It is therefore the fallback, not the default.
     */
    private fun preferredEngine(): Engine? = when {
        SpeechRecognizer.isRecognitionAvailable(appContext) -> Engine.System
        onDeviceAvailable() -> Engine.OnDevice
        else -> null
    }

    private fun otherEngine(engine: Engine): Engine? = when (engine) {
        Engine.System -> Engine.OnDevice.takeIf { onDeviceAvailable() }
        Engine.OnDevice -> Engine.System.takeIf { SpeechRecognizer.isRecognitionAvailable(appContext) }
    }

    private fun create(engine: Engine): SpeechRecognizer? = runCatching {
        when (engine) {
            Engine.OnDevice -> SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            Engine.System -> SpeechRecognizer.createSpeechRecognizer(appContext)
        }
    }.getOrNull()

    private fun onDeviceAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext) }.getOrDefault(false)

    /**
     * Asks the platform to fetch the missing on-device language pack.
     *
     * Fire and forget, and deliberately so: this dictation has already fallen back to the system
     * recogniser and will not wait. The point is that the *next* one may find the local model in
     * place, and work without a network.
     */
    private fun requestOnDeviceModel(recognizer: SpeechRecognizer?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || recognizer == null) return
        runCatching { recognizer.triggerModelDownload(recognizerIntent()) }
            .onFailure { Log.w(TAG, "Could not request the on-device model", it) }
    }

    private fun recognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }

    companion object {
        private const val TAG = "VoiceRecognizer"

        /** Dictation is English regardless of the interface language. */
        const val LANGUAGE = "en-US"

        /**
         * `onRmsChanged` reports roughly -2 dB (silence) to 10 dB (loud), though no OEM treats that
         * as a contract, so the result is clamped rather than trusted.
         */
        fun normaliseRms(rmsdB: Float): Float =
            ((rmsdB - RMS_FLOOR) / (RMS_CEILING - RMS_FLOOR)).coerceIn(0f, 1f)

        /**
         * Smooths the level towards a new reading.
         *
         * A raw RMS stream jitters hard enough that bars driven straight from it look broken rather
         * than alive. Rising fast and falling slow matches how a level meter is expected to behave:
         * a syllable registers immediately, and the bars settle instead of flickering between words.
         */
        fun smoothLevel(current: Float, target: Float): Float {
            val weight = if (target > current) RISE_WEIGHT else FALL_WEIGHT
            return (current + (target - current) * weight).coerceIn(0f, 1f)
        }

        private const val RMS_FLOOR = -2f
        private const val RMS_CEILING = 10f
        private const val RISE_WEIGHT = 0.6f
        private const val FALL_WEIGHT = 0.15f
    }
}
