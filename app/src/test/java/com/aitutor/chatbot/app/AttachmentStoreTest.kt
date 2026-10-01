package com.aitutor.chatbot.app

import com.aitutor.chatbot.app.data.attachment.AttachmentStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The parts of the store that are pure decisions, not file I/O. */
class AttachmentStoreTest {

    private val pdf = "application/pdf"
    private val docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    @Test
    fun `images and the two document types are accepted`() {
        assertTrue(AttachmentStore.isSupported("image/jpeg"))
        assertTrue(AttachmentStore.isSupported("image/png"))
        assertTrue(AttachmentStore.isSupported("image/heic"))
        assertTrue(AttachmentStore.isSupported(pdf))
        assertTrue(AttachmentStore.isSupported(docx))
    }

    @Test
    fun `legacy doc is not docx and is rejected`() {
        // The old binary Word format shares a name but nothing else.
        assertFalse(AttachmentStore.isSupported("application/msword"))
        assertFalse(AttachmentStore.isSupported("text/plain"))
        assertFalse(AttachmentStore.isSupported("application/octet-stream"))
    }

    @Test
    fun `extensions follow the type, not the picked file's name`() {
        assertEquals("pdf", AttachmentStore.extensionFor(pdf))
        assertEquals("docx", AttachmentStore.extensionFor(docx))
        assertEquals("jpg", AttachmentStore.extensionFor("image/jpeg"))
    }

    @Test
    fun `every image becomes jpeg, because every image is re-encoded`() {
        // The service is told what is actually on the wire. A PNG labelled PNG but holding JPEG
        // bytes is a decode failure on the other end.
        assertEquals("image/jpeg", AttachmentStore.storedMimeFor("image/png"))
        assertEquals("image/jpeg", AttachmentStore.storedMimeFor("image/heic"))
        assertEquals("image/jpeg", AttachmentStore.storedMimeFor("image/jpeg"))
    }

    @Test
    fun `documents are copied untouched, so their type is unchanged`() {
        assertEquals(pdf, AttachmentStore.storedMimeFor(pdf))
        assertEquals(docx, AttachmentStore.storedMimeFor(docx))
    }

    @Test
    fun `the shown name keeps its base and gains the real extension`() {
        assertEquals("notes.jpg", AttachmentStore.renameToMatch("notes.png", "image/jpeg"))
        assertEquals("worksheet.pdf", AttachmentStore.renameToMatch("worksheet.pdf", pdf))
        // A picked file with no extension at all still ends up named for what it is.
        assertEquals("scan.jpg", AttachmentStore.renameToMatch("scan", "image/jpeg"))
    }

    @Test
    fun `sample size halves until one more halving would undershoot`() {
        val max = 2000
        assertEquals(1, AttachmentStore.sampleSizeFor(longestEdge = 1500, maxEdge = max))
        assertEquals(1, AttachmentStore.sampleSizeFor(longestEdge = 3000, maxEdge = max))
        assertEquals(2, AttachmentStore.sampleSizeFor(longestEdge = 4000, maxEdge = max))
        assertEquals(4, AttachmentStore.sampleSizeFor(longestEdge = 8000, maxEdge = max))
    }

    @Test
    fun `sampling never takes the image below the target`() {
        // Whatever the source, dividing by the chosen sample must leave at least maxEdge to work
        // with — otherwise the final scale would have to enlarge, which loses detail for nothing.
        val max = 2000
        listOf(2001, 2600, 4000, 4096, 6000, 12_000).forEach { edge ->
            val sampled = edge / AttachmentStore.sampleSizeFor(edge, max)
            assertTrue("$edge sampled to $sampled", sampled >= max)
        }
    }
}
