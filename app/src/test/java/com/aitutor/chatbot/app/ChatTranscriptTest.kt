package com.aitutor.chatbot.app

import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.Attachment
import com.aitutor.chatbot.app.domain.model.ChatMessage
import com.aitutor.chatbot.app.domain.model.ScanSource
import com.aitutor.chatbot.app.ui.chat.ChatTranscript
import com.aitutor.chatbot.app.ui.chat.TITLE_MAX_LENGTH
import com.aitutor.chatbot.app.ui.chat.toTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** What is handed to the share sheet, and the rules a renamed chat's title follows. */
class ChatTranscriptTest {

    private fun format(vararg messages: ChatMessage, title: String = "Derivatives") =
        ChatTranscript.format(
            title = title,
            messages = messages.toList(),
            youLabel = "You",
            tutorLabel = "AI Tutor",
            attachedLabel = { "Attached: $it" },
        )

    @Test
    fun `a question and its answer read as a conversation`() {
        val text = format(
            ChatMessage.User(id = 1, text = "What is a derivative?"),
            ChatMessage.Assistant(id = 2, blocks = listOf(AnswerBlock.Paragraph("A rate of change."))),
        )
        assertEquals(
            "Derivatives\n\nYou:\nWhat is a derivative?\n\nAI Tutor:\nA rate of change.",
            text,
        )
    }

    @Test
    fun `bullets keep their term and are marked`() {
        val text = format(
            ChatMessage.Assistant(
                id = 1,
                blocks = listOf(
                    AnswerBlock.Paragraph("Two ideas:"),
                    AnswerBlock.Bullet(term = "Limit", text = "what it approaches."),
                ),
            ),
        )
        assertEquals("Derivatives\n\nAI Tutor:\nTwo ideas:\n• Limit what it approaches.", text)
    }

    @Test
    fun `day separators and the typing dots are not part of the conversation`() {
        val text = format(
            ChatMessage.DayMarker(timestamp = 0L),
            ChatMessage.User(id = 1, text = "Hi"),
            ChatMessage.Typing,
        )
        assertEquals("Derivatives\n\nYou:\nHi", text)
    }

    @Test
    fun `an attached file is named after the question`() {
        val file = Attachment(
            source = ScanSource.Camera,
            fileName = "worksheet.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 10,
            localPath = "/x",
        )
        assertEquals(
            "Derivatives\n\nYou:\nSolve this\nAttached: worksheet.jpg",
            format(ChatMessage.User(id = 1, text = "Solve this", attachment = file)),
        )
    }

    @Test
    fun `a file sent with no question still reads as something the student said`() {
        val file = Attachment(ScanSource.Camera, "worksheet.jpg", "image/jpeg", 10, "/x")
        assertEquals(
            "Derivatives\n\nYou:\nAttached: worksheet.jpg",
            format(ChatMessage.User(id = 1, text = "", attachment = file)),
        )
    }

    @Test
    fun `a blank title is left out rather than leaving a gap`() {
        assertEquals("You:\nHi", format(ChatMessage.User(id = 1, text = "Hi"), title = "  "))
    }

    // ---- Titles (Rename uses the same rules as the first question does) ----

    @Test
    fun `a title is one line with the spaces tidied`() {
        assertEquals("Solve this problem", "  Solve\n this   problem ".toTitle())
    }

    @Test
    fun `a blank title comes out empty, which Rename refuses`() {
        assertEquals("", "   \n ".toTitle())
    }

    @Test
    fun `a title over the limit is cut and marked`() {
        val title = "x".repeat(TITLE_MAX_LENGTH + 20).toTitle()
        assertEquals(TITLE_MAX_LENGTH + 1, title.length)
        assertEquals('…', title.last())
    }

    @Test
    fun `a title at the limit is left whole`() {
        assertFalse("x".repeat(TITLE_MAX_LENGTH).toTitle().endsWith("…"))
    }
}
