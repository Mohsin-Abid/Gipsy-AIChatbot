package com.askylix.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.askylix.app.data.attachment.AttachmentError
import com.askylix.app.data.attachment.AttachmentException
import com.askylix.app.data.attachment.AttachmentStore
import com.askylix.app.domain.model.Attachment
import com.askylix.app.domain.model.ScanSource
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every kind of file a student might attach, put through the real store on a real phone: real content
 * URIs, the real PDF reader, the real image decoder. Each bad file must be refused with the right
 * reason **and leave nothing behind** — a refused file never becomes something that could be sent.
 */
@RunWith(AndroidJUnit4::class)
class AttachmentChecksInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
    private val store = AttachmentStore(context)

    private val keptFiles: List<File>
        get() = File(context.cacheDir, AttachmentStore.DIRECTORY).listFiles().orEmpty().filter { it.isFile }

    @Before
    fun startClean() {
        runBlocking { store.clear() }
    }

    /** The fixture as a content URI, the way a picker hands one over. */
    private fun uriFor(name: String): Uri {
        val directory = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(directory, name)
        testAssets.open("attachments/$name").use { input -> file.outputStream().use(input::copyTo) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun attach(name: String, source: ScanSource = ScanSource.Pdf): Result<Attachment> =
        runBlocking { store.store(uriFor(name), source) }

    private fun assertRefused(name: String, reason: AttachmentError) {
        val result = attach(name)
        val failure = result.exceptionOrNull()
        assertTrue("$name should have been refused but was accepted", failure != null)
        assertEquals("$name was refused for the wrong reason", reason, (failure as AttachmentException).error)
        assertTrue("$name was refused but a copy was left behind: $keptFiles", keptFiles.isEmpty())
    }

    private fun assertAccepted(name: String): Attachment {
        val result = attach(name)
        assertTrue("$name should have been accepted but: ${result.exceptionOrNull()}", result.isSuccess)
        return result.getOrThrow().also { assertTrue(File(it.localPath!!).length() > 0) }
    }

    // ---- PDF ----

    @Test fun validPdfIsAccepted() { assertAccepted("valid.pdf") }

    @Test fun pdfLockedWithOldEncryptionIsPasswordProtected() = assertRefused("password-rc4.pdf", AttachmentError.PasswordProtected)

    @Test fun pdfLockedWithModernEncryptionIsPasswordProtected() = assertRefused("password-aes256.pdf", AttachmentError.PasswordProtected)

    @Test fun pdfThatOpensWithoutAPasswordIsAccepted() {
        // Only restrictions (no copying, no printing) are set; anyone can open it, so it is not "password protected".
        assertAccepted("owner-only.pdf")
    }

    @Test fun truncatedPdfIsCorrupt() = assertRefused("truncated.pdf", AttachmentError.Corrupt)

    @Test fun noisePdfIsCorrupt() = assertRefused("garbage.pdf", AttachmentError.Corrupt)

    @Test fun noisePdfWithAnEndingIsStillCorruptBecauseTheReaderCannotOpenIt() = assertRefused("garbage-with-eof.pdf", AttachmentError.Corrupt)

    @Test fun webPageNamedPdfIsCorrupt() = assertRefused("html-named.pdf", AttachmentError.Corrupt)

    @Test fun emptyPdfIsEmpty() = assertRefused("empty.pdf", AttachmentError.Empty)

    // ---- Word ----

    @Test fun validWordDocumentIsAccepted() { assertAccepted("valid.docx") }

    @Test fun wordDocumentLockedWithAPasswordIsPasswordProtected() = assertRefused("password.docx", AttachmentError.PasswordProtected)

    @Test fun truncatedWordDocumentIsCorrupt() = assertRefused("truncated.docx", AttachmentError.Corrupt)

    @Test fun wordDocumentWithDamagedDataIsCorrupt() = assertRefused("corrupt-data.docx", AttachmentError.Corrupt)

    @Test fun zipThatIsNotWordIsCorrupt() = assertRefused("not-word.docx", AttachmentError.Corrupt)

    @Test fun oldStyleDocumentRenamedDocxIsUnsupported() = assertRefused("legacy-ole.docx", AttachmentError.Unsupported)

    @Test fun emptyWordDocumentIsEmpty() = assertRefused("empty.docx", AttachmentError.Empty)

    // ---- Photos ----

    @Test fun validPhotoIsAcceptedAndShrunk() {
        val stored = assertAccepted("valid.jpg")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(stored.localPath, bounds)
        assertEquals("image/jpeg", stored.mimeType)
        assertTrue("long edge was ${maxOf(bounds.outWidth, bounds.outHeight)}", maxOf(bounds.outWidth, bounds.outHeight) <= AttachmentStore.MAX_IMAGE_EDGE)
    }

    @Test fun validPngIsAcceptedAndBecomesJpeg() {
        assertEquals("image/jpeg", assertAccepted("valid.png").mimeType)
    }

    @Test fun truncatedJpegIsCorrupt() = assertRefused("truncated.jpg", AttachmentError.Corrupt)

    @Test fun truncatedPngIsCorrupt() = assertRefused("truncated.png", AttachmentError.Corrupt)

    @Test fun noiseWithAJpegNameIsCorrupt() = assertRefused("garbage.jpg", AttachmentError.Corrupt)
}
