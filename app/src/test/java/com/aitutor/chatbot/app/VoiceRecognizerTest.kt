package com.aitutor.chatbot.app

import android.speech.SpeechRecognizer
import com.aitutor.chatbot.app.data.voice.VoiceError
import com.aitutor.chatbot.app.data.voice.VoiceRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The parts of dictation that are arithmetic and mapping rather than platform calls. */
class VoiceRecognizerTest {

    @Test
    fun `rms maps silence to zero and a shout to one`() {
        assertEquals(0f, VoiceRecognizer.normaliseRms(-2f), 0.001f)
        assertEquals(1f, VoiceRecognizer.normaliseRms(10f), 0.001f)
        assertEquals(0.5f, VoiceRecognizer.normaliseRms(4f), 0.001f)
    }

    @Test
    fun `rms outside the documented range is clamped, not trusted`() {
        // No OEM treats the -2..10 range as a contract, so readings beyond it must not escape 0..1.
        assertEquals(0f, VoiceRecognizer.normaliseRms(-120f), 0.001f)
        assertEquals(1f, VoiceRecognizer.normaliseRms(120f), 0.001f)
    }

    @Test
    fun `the level rises faster than it falls`() {
        // A syllable should register at once; the bars should settle rather than flicker between
        // words. Both are the same call, so the asymmetry is what this pins down.
        val rise = VoiceRecognizer.smoothLevel(current = 0f, target = 1f)
        val fall = VoiceRecognizer.smoothLevel(current = 1f, target = 0f)
        assertTrue("rose $rise", rise > 0.5f)
        assertTrue("fell to $fall", fall > 0.5f)
    }

    @Test
    fun `smoothing converges on the target and stays in range`() {
        var level = 0f
        repeat(40) { level = VoiceRecognizer.smoothLevel(level, 1f) }
        assertEquals(1f, level, 0.01f)

        repeat(200) { level = VoiceRecognizer.smoothLevel(level, 0f) }
        assertEquals(0f, level, 0.01f)
    }

    @Test
    fun `error codes keep the distinctions that change what a student should do`() {
        assertEquals(VoiceError.NoSpeechHeard, VoiceError.fromCode(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(VoiceError.SilenceTimeout, VoiceError.fromCode(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
        assertEquals(
            VoiceError.PermissionDenied,
            VoiceError.fromCode(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS),
        )
        assertEquals(VoiceError.Busy, VoiceError.fromCode(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(VoiceError.Network, VoiceError.fromCode(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(
            VoiceError.LanguageUnavailable,
            VoiceError.fromCode(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE),
        )
        assertEquals(
            VoiceError.LanguageUnavailable,
            VoiceError.fromCode(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED),
        )
    }

    @Test
    fun `a stub recogniser's failures land on unavailable`() {
        // ERROR_CLIENT is what an OEM build that advertises a recogniser and has none tends to
        // return, and an unknown code from some future ROM means the same thing to a student.
        assertEquals(VoiceError.Unavailable, VoiceError.fromCode(SpeechRecognizer.ERROR_CLIENT))
        assertEquals(VoiceError.Unavailable, VoiceError.fromCode(9999))
    }

    @Test
    fun `a language failure blames the engine, not the speech`() {
        // The Pixel case: an on-device recogniser that reports itself available, has never
        // downloaded its English pack, and refuses the first request. Another engine on the same
        // phone answers fine — so this must be distinguishable from "you said nothing".
        assertTrue(VoiceError.isEngineFailure(VoiceError.LanguageUnavailable))
        assertTrue(VoiceError.isEngineFailure(VoiceError.Unavailable))

        assertFalse(VoiceError.isEngineFailure(VoiceError.NoSpeechHeard))
        assertFalse(VoiceError.isEngineFailure(VoiceError.PermissionDenied))
        assertFalse(VoiceError.isEngineFailure(VoiceError.Network))
        assertFalse(VoiceError.isEngineFailure(VoiceError.Busy))
    }

    @Test
    fun `an engine failure is never also a plain retry`() {
        // Retrying the same engine that just said it cannot do this would spin forever; the two
        // responses have to stay mutually exclusive.
        VoiceError.entries.forEach { error ->
            assertFalse(
                "$error claims both",
                VoiceError.isRetryable(error) && VoiceError.isEngineFailure(error),
            )
        }
    }

    @Test
    fun `only the transient failures are worth retrying on their own`() {
        assertTrue(VoiceError.isRetryable(VoiceError.NoSpeechHeard))
        assertTrue(VoiceError.isRetryable(VoiceError.SilenceTimeout))
        assertTrue(VoiceError.isRetryable(VoiceError.Busy))

        // These need something to change first — retrying in a loop would just spin.
        assertFalse(VoiceError.isRetryable(VoiceError.PermissionDenied))
        assertFalse(VoiceError.isRetryable(VoiceError.Unavailable))
        assertFalse(VoiceError.isRetryable(VoiceError.Network))
    }
}
