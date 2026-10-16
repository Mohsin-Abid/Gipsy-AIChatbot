package com.askylix.app.data.attachment

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Looks inside a file that is about to be attached, so that one the tutor service could not read is
 * turned away on the phone, with a reason, instead of after an upload.
 *
 * The three things worth catching are a file with nothing in it, a file that is damaged (cut short by
 * an interrupted download, or not the kind of file its name says), and a document locked with a
 * password, which nothing downstream can open. Photos are not handled here: they are checked by being
 * decoded, in [AttachmentStore].
 *
 * Works on the copy that is about to be uploaded, not on the original, so what is checked is exactly
 * what would be sent.
 */
internal object FileChecks {

    /** What is wrong with [file], or null when nothing is. [mimeType] is the type it will be sent as. */
    fun problemWith(file: File, mimeType: String): AttachmentError? {
        if (file.length() == 0L) return AttachmentError.Empty
        return when (mimeType) {
            AttachmentStore.MIME_PDF -> pdfProblem(file)
            AttachmentStore.MIME_DOCX -> docxProblem(file)
            else -> null
        }
    }

    // ---- PDF ----

    private fun pdfProblem(file: File): AttachmentError? =
        pdfStructureProblem(file) ?: PdfProbe.problemWith(file)

    /**
     * The cheap checks, which need no PDF reader: a PDF starts with `%PDF-` and ends with `%%EOF`.
     * A download that stopped half way keeps its start but never gets its end, which is the most common
     * way a PDF is damaged, and a web page saved with a .pdf name has neither.
     */
    internal fun pdfStructureProblem(file: File): AttachmentError? {
        if (!file.head(PDF_HEADER_WINDOW).containsAscii("%PDF-")) return AttachmentError.Corrupt
        if (!file.tail(PDF_TRAILER_WINDOW).containsAscii("%%EOF")) return AttachmentError.Corrupt
        return null
    }

    // ---- Word ----

    /**
     * A .docx is a zip holding `[Content_Types].xml` and `word/document.xml`. Checked by opening the zip
     * and reading both through to the end, which verifies their checksums.
     *
     * A document locked with a password is not a zip at all: Word wraps it in an older container
     * (starting with [OLE_SIGNATURE]) that holds the encrypted zip in a stream called `EncryptedPackage`.
     * An old-style .doc is the same container with nothing locked, and is not accepted either.
     */
    internal fun docxProblem(file: File): AttachmentError? {
        if (file.head(OLE_SIGNATURE.size).startsWith(OLE_SIGNATURE)) {
            return if (file.contains(ENCRYPTED_PACKAGE_UTF16)) AttachmentError.PasswordProtected else AttachmentError.Unsupported
        }
        return try {
            ZipFile(file).use { zip ->
                for (name in REQUIRED_DOCX_ENTRIES) {
                    val entry = zip.getEntry(name) ?: return AttachmentError.Corrupt
                    if (!zip.readsCleanly(entry)) return AttachmentError.Corrupt
                }
                null
            }
        } catch (_: IOException) {
            // Cut short (no directory at the end), or not a zip. ZipException is one of these.
            AttachmentError.Corrupt
        }
    }

    /** Reads [entry] to its end and compares its checksum with the one the zip recorded. */
    private fun ZipFile.readsCleanly(entry: ZipEntry): Boolean {
        if (entry.size > MAX_ENTRY_BYTES) return false
        val checksum = CRC32()
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0L
        getInputStream(entry).use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                checksum.update(buffer, 0, read)
                total += read
                // A tiny file that inflates to gigabytes is not a document.
                if (total > MAX_ENTRY_BYTES) return false
            }
        }
        return entry.crc == -1L || checksum.value == entry.crc
    }

    // ---- Reading bytes ----

    private fun File.head(count: Int): ByteArray = RandomAccessFile(this, "r").use { file ->
        ByteArray(minOf(count.toLong(), file.length()).toInt()).also { file.readFully(it) }
    }

    private fun File.tail(count: Int): ByteArray = RandomAccessFile(this, "r").use { file ->
        val size = minOf(count.toLong(), file.length()).toInt()
        file.seek(file.length() - size)
        ByteArray(size).also { file.readFully(it) }
    }

    /** Whether [pattern] appears anywhere in the file, read in pieces so a large file is never held whole. */
    private fun File.contains(pattern: ByteArray): Boolean = inputStream().buffered().use { input ->
        val buffer = ByteArray(BUFFER_BYTES + pattern.size)
        var carried = 0
        var found = false
        while (!found) {
            val read = input.read(buffer, carried, BUFFER_BYTES)
            if (read < 0) break
            val filled = carried + read
            found = buffer.find(pattern, filled) >= 0
            // Keep the end, in case the pattern straddles two reads.
            carried = minOf(pattern.size - 1, filled)
            System.arraycopy(buffer, filled - carried, buffer, 0, carried)
        }
        found
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.containsAscii(text: String): Boolean = find(text.toByteArray(Charsets.US_ASCII), size) >= 0

    /** Where [pattern] first appears within the first [limit] bytes, or -1. */
    private fun ByteArray.find(pattern: ByteArray, limit: Int): Int {
        outer@ for (start in 0..limit - pattern.size) {
            for (i in pattern.indices) if (this[start + i] != pattern[i]) continue@outer
            return start
        }
        return -1
    }

    // `%PDF-` may follow a little junk, and `%%EOF` may be followed by some: readers allow a kilobyte or two.
    private const val PDF_HEADER_WINDOW = 1024
    private const val PDF_TRAILER_WINDOW = 2048

    private val OLE_SIGNATURE = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte())
    private val ENCRYPTED_PACKAGE_UTF16 = "EncryptedPackage".toByteArray(Charsets.UTF_16LE)
    private val REQUIRED_DOCX_ENTRIES = listOf("[Content_Types].xml", "word/document.xml")

    private const val MAX_ENTRY_BYTES = 200L * 1024 * 1024
    private const val BUFFER_BYTES = 64 * 1024
}

/**
 * Opens a PDF the way the phone's own PDF viewer would, which is the one reliable way to learn two
 * things from a file: that it needs a password, and that its pages can actually be read.
 */
internal object PdfProbe {

    fun problemWith(file: File): AttachmentError? = try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                // A document with no pages has nothing to read, and the first page is where a damaged one gives way.
                if (renderer.pageCount < 1) {
                    AttachmentError.Corrupt
                } else {
                    renderer.openPage(0).close()
                    null
                }
            }
        }
    } catch (_: SecurityException) {
        // What PdfRenderer throws for a file that asks for a password, or uses protection it cannot open.
        AttachmentError.PasswordProtected
    } catch (_: Exception) {
        AttachmentError.Corrupt
    }
}
