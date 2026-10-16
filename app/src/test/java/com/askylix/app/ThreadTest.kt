package com.askylix.app

import com.askylix.app.data.remote.AttachmentDto
import com.askylix.app.data.remote.BlockDto
import com.askylix.app.data.remote.MessageDto
import com.askylix.app.data.session.StoredMessage
import com.askylix.app.data.session.toStoredOrNull
import com.askylix.app.data.session.toThread
import com.askylix.app.domain.model.AnswerBlock
import com.askylix.app.domain.model.ChatMessage
import com.askylix.app.domain.model.Rating
import com.askylix.app.domain.model.ScanSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** How messages from the server become the thread a screen draws. */
class ThreadTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val noon = 1_760_000_000_000L // a fixed instant, so the day boundaries do not depend on when this runs

    private fun user(id: String, at: Long) = StoredMessage(ChatMessage.User(id, "q $id"), at)
    private fun answer(id: String, at: Long) = StoredMessage(ChatMessage.Assistant(id, listOf(AnswerBlock.Paragraph("a $id"))), at)

    @Test
    fun `a separator opens the thread and again whenever the day changes`() {
        val thread = listOf(user("1", noon), answer("2", noon), user("3", noon + 2 * day)).toThread()

        val kinds = thread.map { it::class.simpleName }
        assertEquals(listOf("DayMarker", "User", "Assistant", "DayMarker", "User"), kinds)
    }

    @Test
    fun `an empty thread has nothing in it`() {
        assertTrue(emptyList<StoredMessage>().toThread().isEmpty())
    }

    @Test
    fun `only the newest answer carries the action row`() {
        val thread = listOf(
            user("1", noon), answer("2", noon), user("3", noon), answer("4", noon),
        ).toThread().filterIsInstance<ChatMessage.Assistant>()

        assertFalse(thread[0].showActions)
        assertTrue(thread[1].showActions)
    }

    @Test
    fun `a question and its answer from the same second keep the server's order`() {
        val thread = listOf(user("1", noon), answer("2", noon)).toThread()

        assertEquals("1", (thread[1] as ChatMessage.User).id)
        assertEquals("2", (thread[2] as ChatMessage.Assistant).id)
    }

    @Test
    fun `a user message from the server`() {
        val stored = MessageDto(
            messageId = "msg_1", role = "USER", createdAt = "2026-10-15T09:47:45Z", text = "Solve 2x + 5 = 17",
            attachment = AttachmentDto("tiny.jpg", "image/jpeg", 160, url = "https://files/1?sig=x"),
        ).toStoredOrNull()!!

        val message = stored.message as ChatMessage.User
        assertEquals("msg_1", message.id)
        assertEquals("Solve 2x + 5 = 17", message.text)
        assertEquals("tiny.jpg", message.attachment?.fileName)
        assertEquals("https://files/1?sig=x", message.attachment?.url)
        assertNull(message.attachment?.localPath)
        assertEquals(java.time.Instant.parse("2026-10-15T09:47:45Z").toEpochMilli(), stored.createdAt)
    }

    @Test
    fun `an assistant message from the server keeps its blocks and its rating`() {
        val message = MessageDto(
            messageId = "msg_2", role = "ASSISTANT", rating = "HELPFUL",
            blocks = listOf(BlockDto("PARAGRAPH", null, "Hello"), BlockDto("BULLET", "Step", "Do it")),
        ).toStoredOrNull()!!.message as ChatMessage.Assistant

        assertEquals(Rating.Helpful, message.rating)
        assertEquals(AnswerBlock.Paragraph("Hello"), message.blocks[0])
        assertEquals(AnswerBlock.Bullet("Step", "Do it"), message.blocks[1])
    }

    @Test
    fun `a bullet with no term is read as a paragraph rather than dropped`() {
        val message = MessageDto("m", "ASSISTANT", blocks = listOf(BlockDto("BULLET", null, "Loose text")))
            .toStoredOrNull()!!.message as ChatMessage.Assistant

        assertEquals(AnswerBlock.Paragraph("Loose text"), message.blocks.single())
    }

    @Test
    fun `a role this build does not know is skipped`() {
        assertNull(MessageDto("m", "SYSTEM", text = "x").toStoredOrNull())
    }

    @Test
    fun `a file's kind comes from its type`() {
        fun kind(mime: String) = MessageDto("m", "USER", attachment = AttachmentDto("f", mime, 1))
            .toStoredOrNull()!!.let { (it.message as ChatMessage.User).attachment?.source }

        assertEquals(ScanSource.Pdf, kind("application/pdf"))
        assertEquals(ScanSource.Word, kind("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(ScanSource.Gallery, kind("image/jpeg"))
    }
}
