package com.askylix.app

import com.askylix.app.data.attachment.AttachmentError
import com.askylix.app.data.attachment.AttachmentStore
import com.askylix.app.data.attachment.FileChecks
import java.io.File
import java.io.RandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * What is looked for inside a document before it is allowed to be sent, against real files: a valid
 * one, one locked with a password, one cut short, one with damaged data, and the look-alikes.
 *
 * The PDF reader itself is part of Android and runs on a phone; the on-device tests exercise it. What
 * runs here is everything that needs no phone.
 */
class FileChecksTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun fixture(name: String): File {
        val target = folder.newFile(name)
        checkNotNull(javaClass.getResourceAsStream("/attachments/$name")) { "missing fixture $name" }
            .use { input -> target.outputStream().use(input::copyTo) }
        return target
    }

    // ---- Word documents ----

    @Test
    fun `a real Word document passes`() {
        assertNull(FileChecks.docxProblem(fixture("valid.docx")))
    }

    @Test
    fun `a Word document locked with a password is recognised as that, not as damaged`() {
        assertEquals(AttachmentError.PasswordProtected, FileChecks.docxProblem(fixture("password.docx")))
    }

    @Test
    fun `a Word document cut short is damaged`() {
        assertEquals(AttachmentError.Corrupt, FileChecks.docxProblem(fixture("truncated.docx")))
    }

    @Test
    fun `a Word document whose contents were damaged is caught by its checksum`() {
        assertEquals(AttachmentError.Corrupt, FileChecks.docxProblem(fixture("corrupt-data.docx")))
    }

    @Test
    fun `a zip that is not a Word document is turned away`() {
        assertEquals(AttachmentError.Corrupt, FileChecks.docxProblem(fixture("not-word.docx")))
    }

    @Test
    fun `an old-style document renamed docx is unsupported, not a password problem`() {
        assertEquals(AttachmentError.Unsupported, FileChecks.docxProblem(fixture("legacy-ole.docx")))
    }

    @Test
    fun `plain text with a docx name is damaged`() {
        val file = folder.newFile("notes.docx").apply { writeText("just some words, not a document") }
        assertEquals(AttachmentError.Corrupt, FileChecks.docxProblem(file))
    }

    @Test
    fun `the lock marker is found even when it straddles two reads of a large file`() {
        // The scan reads 64 KB at a time; this puts the marker across the first boundary.
        val marker = "EncryptedPackage".toByteArray(Charsets.UTF_16LE)
        val file = folder.newFile("big.docx")
        RandomAccessFile(file, "rw").use {
            it.write(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()))
            it.setLength(300_000)
            it.seek(64 * 1024L - 9)
            it.write(marker)
        }
        assertEquals(AttachmentError.PasswordProtected, FileChecks.docxProblem(file))
    }

    // ---- PDF structure (the part that needs no PDF reader) ----

    @Test
    fun `a real PDF has the start and the end every PDF has`() {
        assertNull(FileChecks.pdfStructureProblem(fixture("valid.pdf")))
    }

    @Test
    fun `a PDF cut short loses its end and is damaged`() {
        assertEquals(AttachmentError.Corrupt, FileChecks.pdfStructureProblem(fixture("truncated.pdf")))
    }

    @Test
    fun `a web page saved with a pdf name is damaged`() {
        assertEquals(AttachmentError.Corrupt, FileChecks.pdfStructureProblem(fixture("html-named.pdf")))
    }

    @Test
    fun `a PDF header followed by noise and no ending is damaged`() {
        assertEquals(AttachmentError.Corrupt, FileChecks.pdfStructureProblem(fixture("garbage.pdf")))
    }

    @Test
    fun `the structure check alone is not enough - noise can end in an ending too`() {
        // Why the PDF is also opened with the real reader; see the on-device tests.
        assertNull(FileChecks.pdfStructureProblem(fixture("garbage-with-eof.pdf")))
    }

    // ---- Anything empty ----

    @Test
    fun `a file with nothing in it is empty, whatever it claims to be`() {
        assertEquals(AttachmentError.Empty, FileChecks.problemWith(fixture("empty.pdf"), AttachmentStore.MIME_PDF))
        assertEquals(AttachmentError.Empty, FileChecks.problemWith(fixture("empty.docx"), AttachmentStore.MIME_DOCX))
    }

    @Test
    fun `a good Word document passes the full check`() {
        assertNull(FileChecks.problemWith(fixture("valid.docx"), AttachmentStore.MIME_DOCX))
    }

    @Test
    fun `a type with no document check is left to the caller`() {
        // Photos are checked by being decoded, not here.
        val file = folder.newFile("photo.jpg").apply { writeText("x") }
        assertNull(FileChecks.problemWith(file, "image/jpeg"))
    }
}
