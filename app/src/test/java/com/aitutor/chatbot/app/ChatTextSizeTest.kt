package com.aitutor.chatbot.app

import com.aitutor.chatbot.app.domain.model.ChatTextSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The A− / A+ pair: which way each step goes, and where it stops. */
class ChatTextSizeTest {

    @Test
    fun `the normal size is exactly one`() {
        assertEquals(1f, ChatTextSize.Default.scale, 0f)
    }

    @Test
    fun `sizes grow in order`() {
        val scales = ChatTextSize.entries.map { it.scale }
        assertEquals(scales.sorted(), scales)
    }

    @Test
    fun `larger and smaller step one at a time`() {
        assertEquals(ChatTextSize.Large, ChatTextSize.Default.larger)
        assertEquals(ChatTextSize.Small, ChatTextSize.Default.smaller)
    }

    @Test
    fun `there is nothing smaller than the smallest or larger than the largest`() {
        assertNull(ChatTextSize.Small.smaller)
        assertNull(ChatTextSize.Largest.larger)
    }

    @Test
    fun `stepping all the way up reaches the largest and no further`() {
        var size = ChatTextSize.Small
        while (true) size = size.larger ?: break
        assertEquals(ChatTextSize.Largest, size)
    }

    @Test
    fun `an unreadable stored value falls back to the normal size`() {
        assertEquals(ChatTextSize.Default, ChatTextSize.fromNameOrDefault(null))
        assertEquals(ChatTextSize.Default, ChatTextSize.fromNameOrDefault("Gigantic"))
    }

    @Test
    fun `every size reads back from its own name`() {
        assertTrue(ChatTextSize.entries.all { ChatTextSize.fromNameOrDefault(it.name) == it })
    }
}
