package com.aitutor.chatbot.app.data.voice

import android.speech.SpeechRecognizer

/**
 * Why dictation stopped.
 *
 * The platform returns a flat list of integers, but they are not interchangeable to someone holding
 * the phone: one means *say it again*, one means *grant the permission*, one means *this device
 * cannot do this at all*. Collapsing them into a single "something went wrong" would hide the only
 * difference that matters.
 */
enum class VoiceError {
    /** Heard audio, matched nothing. The ordinary "say that again" case. */
    NoSpeechHeard,

    /** Nothing was said before the recogniser gave up waiting. */
    SilenceTimeout,

    /** `RECORD_AUDIO` is not granted. */
    PermissionDenied,

    /** Something else is already using the recogniser. Usually transient. */
    Busy,

    /** The recogniser needs a network it does not have. */
    Network,

    /** The recogniser cannot work with the language it was asked for. */
    LanguageUnavailable,

    /**
     * No usable recognition service on this device.
     *
     * The case that matters for China-market ROMs: speech recognition is provided by an installed
     * app (usually Google's), and a build shipped without Google Play Services may have none. Some
     * OEM builds report one and then fail on the first request, which lands here too.
     */
    Unavailable;

    companion object {
        /** Maps a [SpeechRecognizer] error code. Unknown codes read as [Unavailable]. */
        fun fromCode(code: Int): VoiceError = when (code) {
            SpeechRecognizer.ERROR_NO_MATCH -> NoSpeechHeard
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SilenceTimeout
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> PermissionDenied
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Busy
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            -> Network
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            -> LanguageUnavailable
            // ERROR_CLIENT and ERROR_AUDIO both show up on ROMs whose recogniser is a stub, as does
            // anything this build does not recognise. All of them mean the same thing to a student.
            else -> Unavailable
        }

        /** True where retrying immediately is reasonable — the rest need something to change first. */
        fun isRetryable(error: VoiceError): Boolean =
            error == NoSpeechHeard || error == SilenceTimeout || error == Busy

        /**
         * True where the *engine* is at fault rather than the speech, so another engine on the same
         * device may well succeed.
         *
         * The case this exists for: a Pixel reports an on-device recogniser, but its English pack
         * has never been downloaded, so the first request comes back [LanguageUnavailable] — while
         * the ordinary system recogniser beside it would have answered immediately.
         */
        fun isEngineFailure(error: VoiceError): Boolean =
            error == LanguageUnavailable || error == Unavailable
    }
}
