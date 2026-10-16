package com.askylix.app

import com.askylix.app.data.voice.Speaker
import com.askylix.app.data.voice.chunkForSpeech
import com.askylix.app.domain.model.AnswerBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parts of reading aloud that are pure text, and therefore the parts a unit test can reach —
 * the engine itself needs a device.
 */
class SpeakerTest {

    @Test
    fun `a bullet's term is closed off before its text`() {
        // Without the stop the two run together: "Subtract 5 both sides" instead of
        // "Subtract 5. Both sides." The pause is what makes it read as a step rather than a phrase.
        val spoken = Speaker.speakableText(
            listOf(AnswerBlock.Bullet(term = "Subtract 5", text = "Both sides, giving 2x = 12"))
        )
        assertEquals("Subtract 5. Both sides, giving 2x = 12.", spoken)
    }

    @Test
    fun `punctuation already there is not doubled`() {
        val spoken = Speaker.speakableText(listOf(AnswerBlock.Paragraph("Is the sky blue?")))
        assertEquals("Is the sky blue?", spoken)
    }

    @Test
    fun `blocks are joined into one pass of speech`() {
        val spoken = Speaker.speakableText(
            listOf(
                AnswerBlock.Paragraph("Start by isolating the variable"),
                AnswerBlock.Bullet(term = "Divide by 2", text = "x = 6"),
            )
        )
        assertEquals("Start by isolating the variable. Divide by 2. x = 6.", spoken)
    }

    @Test
    fun `short text is left as a single utterance`() {
        assertEquals(listOf("Hello there."), "Hello there.".chunkForSpeech(limit = 100))
    }

    @Test
    fun `long text splits on sentence ends`() {
        val text = "One sentence here. Another sentence here. A third sentence here."
        val chunks = text.chunkForSpeech(limit = 30)

        assertTrue("should have split", chunks.size > 1)
        // Every chunk fits, or the engine would silently truncate it.
        chunks.forEach { assertTrue("'$it' is ${it.length} chars", it.length <= 30) }
        // Nothing is lost on the way through.
        assertEquals(text.replace(" ", ""), chunks.joinToString("").replace(" ", ""))
        // And the breaks land after full stops rather than mid-sentence.
        assertTrue(chunks.first().endsWith("."))
    }

    @Test
    fun `text with no sentence ends still splits rather than overflowing`() {
        // A wall of words with no punctuation is the case that would otherwise exceed the limit.
        val text = List(40) { "word" }.joinToString(" ")
        val chunks = text.chunkForSpeech(limit = 50)

        chunks.forEach { assertTrue("'$it' is ${it.length} chars", it.length <= 50) }
        assertEquals(text.replace(" ", ""), chunks.joinToString("").replace(" ", ""))
    }
}
