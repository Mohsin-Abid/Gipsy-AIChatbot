package com.aitutor.chatbot.app

import com.aitutor.chatbot.app.data.local.MessageBodies
import com.aitutor.chatbot.app.domain.model.AnswerBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageBodiesTest {

    private val blocks = listOf(
        AnswerBlock.Paragraph("Both are cell divisions:"),
        AnswerBlock.Bullet(term = "Mitosis", text = "makes 2 identical cells."),
        AnswerBlock.Paragraph("Want a quiz?"),
    )

    @Test
    fun `blocks survive a round trip`() {
        assertEquals(blocks, MessageBodies.decode(MessageBodies.encode(blocks)))
    }

    @Test
    fun `text that looks like json is not mistaken for blocks`() {
        val awkward = listOf(AnswerBlock.Paragraph("""[{"t":"p","x":"nested"}]"""))
        assertEquals(awkward, MessageBodies.decode(MessageBodies.encode(awkward)))
    }

    @Test
    fun `an unparseable body is shown rather than dropped`() {
        // A row written by some other build still has to render as something.
        assertEquals(
            listOf(AnswerBlock.Paragraph("not json at all")),
            MessageBodies.decode("not json at all"),
        )
    }

    @Test
    fun `preview flattens a bullet's term and text into one line`() {
        assertEquals(
            "Both are cell divisions: Mitosis makes 2 identical cells. Want a quiz?",
            MessageBodies.preview(blocks),
        )
    }
}
