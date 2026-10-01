package com.aitutor.chatbot.app.data.attachment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.aitutor.chatbot.app.domain.model.Attachment
import com.aitutor.chatbot.app.domain.model.ScanSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max

/** Why a file could not be attached. Each case is shown differently, so each is named. */
sealed interface AttachmentError {
    data class TooLarge(val limitBytes: Long) : AttachmentError
    data object Unsupported : AttachmentError
    data object Unreadable : AttachmentError
}

class AttachmentException(
    val error: AttachmentError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)

/**
 * Keeps a copy of every file the student attaches.
 *
 * The picked URI itself is never held onto. A photo-picker or file-manager URI is a temporary grant;
 * once it lapses the thread would show an attachment it can no longer open. Copying into the app's
 * own storage means a conversation still makes sense weeks later, and it gives the upload something
 * stable to read when a send is retried.
 *
 * Nothing here inspects the file's contents — the tutor service does that. The store's whole job is
 * to get a reasonable copy onto disk and say how big it is.
 */
class AttachmentStore(
    context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext
    private val directory: File get() = File(appContext.filesDir, DIRECTORY).apply { mkdirs() }

    /**
     * Copies [uri] into app storage, downscaling it first if it is an image.
     *
     * Returns a [Result] rather than throwing: a file too large or a document of the wrong type is
     * an ordinary thing a student does, and the chat screen has to say so rather than crash.
     */
    suspend fun store(uri: Uri, source: ScanSource): Result<Attachment> = withContext(io) {
        runCatching {
            val meta = readMetadata(uri)
            if (!isSupported(meta.mimeType)) throw AttachmentException(AttachmentError.Unsupported)

            // Checked before copying: there is no point writing 60 MB to disk to then reject it.
            // An image escapes the check because downscaling is about to shrink it anyway.
            val isImage = meta.mimeType.startsWith(IMAGE_PREFIX)
            if (!isImage && meta.sizeBytes > MAX_FILE_BYTES) {
                throw AttachmentException(AttachmentError.TooLarge(MAX_FILE_BYTES))
            }

            // Downscaling re-encodes as JPEG, so the type the service is told must change with it.
            // A PNG that arrives labelled PNG but is actually JPEG is a decoding failure waiting to
            // happen on the other end.
            val storedMime = storedMimeFor(meta.mimeType)
            val target = File(directory, "${UUID.randomUUID()}.${extensionFor(storedMime)}")
            if (isImage) copyDownscaled(uri, target) else copyBytes(uri, target)

            if (target.length() > MAX_FILE_BYTES) {
                target.delete()
                throw AttachmentException(AttachmentError.TooLarge(MAX_FILE_BYTES))
            }

            Attachment(
                source = source,
                fileName = renameToMatch(meta.fileName, storedMime),
                mimeType = storedMime,
                sizeBytes = target.length(),
                localPath = target.absolutePath,
            )
        }.recoverCatching { cause ->
            // Anything the content resolver throws reads the same to a student: it didn't open.
            // The original is kept as the cause so a failure is still diagnosable in a log.
            throw if (cause is AttachmentException) {
                cause
            } else {
                AttachmentException(AttachmentError.Unreadable, cause)
            }
        }
    }

    /** Removes a stored copy — when a pending attachment is dismissed, or its chat is cleared. */
    suspend fun delete(attachment: Attachment) = withContext(io) {
        File(attachment.localPath).delete()
    }

    private data class Metadata(val fileName: String, val mimeType: String, val sizeBytes: Long)

    private fun readMetadata(uri: Uri): Metadata {
        val resolver = appContext.contentResolver
        val mime = resolver.getType(uri) ?: MIME_OCTET_STREAM
        var name: String? = null
        var size = -1L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { name = cursor.getString(it) }
                cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { size = cursor.getLong(it) }
            }
        }
        return Metadata(
            fileName = name ?: uri.lastPathSegment ?: "attachment",
            mimeType = mime,
            sizeBytes = size,
        )
    }

    /** Opens [uri], or fails with the one error a student can act on. */
    private fun openStream(uri: Uri): InputStream =
        appContext.contentResolver.openInputStream(uri)
            ?: throw AttachmentException(AttachmentError.Unreadable)

    private fun copyBytes(uri: Uri, target: File) {
        openStream(uri).use { input -> target.outputStream().use(input::copyTo) }
    }

    /**
     * Writes an image at no more than [MAX_IMAGE_EDGE] on its long side.
     *
     * A modern phone camera produces 4–8 MB per shot, which is slow to upload and larger than most
     * services accept. Text on a page is still comfortably legible at this size, so the decode is
     * sampled down first — `inSampleSize` keeps the full image out of memory, which matters because
     * decoding a 50-megapixel photo at full size is a reliable way to run out of it.
     */
    private fun copyDownscaled(uri: Uri, target: File) {
        // A bounds-only pass reports the size without allocating the pixels. It returns null by
        // design — the dimensions come back on `bounds`, so the stream is what gets null-checked
        // here, not the decode's result.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

        val longestEdge = max(bounds.outWidth, bounds.outHeight)
        if (longestEdge <= 0) throw AttachmentException(AttachmentError.Unreadable)

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(longestEdge, MAX_IMAGE_EDGE)
        }
        // This decode really can fail — a truncated or unsupported image returns null.
        val decoded = openStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw AttachmentException(AttachmentError.Unreadable)

        val scaled = scaleToFit(decoded, MAX_IMAGE_EDGE)
        try {
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        } finally {
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
    }

    private fun scaleToFit(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxEdge) return source
        val ratio = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    companion object {
        /** Where the copies live. Named here and excluded from backup in the manifest's XML rules. */
        const val DIRECTORY = "attachments"

        /** Beyond this a send is a bad idea on mobile data, whatever the service would accept. */
        const val MAX_FILE_BYTES = 20L * 1024 * 1024

        /** The long edge an uploaded photo is reduced to. */
        const val MAX_IMAGE_EDGE = 2000

        private const val JPEG_QUALITY = 85
        private const val IMAGE_PREFIX = "image/"
        private const val MIME_JPEG = "image/jpeg"
        private const val MIME_PDF = "application/pdf"
        private const val MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        private const val MIME_OCTET_STREAM = "application/octet-stream"

        /** The types the tiles offer. `.doc` is a different, older format and is not one of them. */
        val DOCUMENT_MIME_TYPES = arrayOf(MIME_PDF, MIME_DOCX)

        fun isSupported(mimeType: String): Boolean =
            mimeType.startsWith(IMAGE_PREFIX) || mimeType in DOCUMENT_MIME_TYPES

        /**
         * The type of what actually ends up on disk. Every image is re-encoded as JPEG on the way
         * in, so that is what it becomes, whatever it arrived as.
         */
        fun storedMimeFor(pickedMimeType: String): String =
            if (pickedMimeType.startsWith(IMAGE_PREFIX)) MIME_JPEG else pickedMimeType

        /** Keeps the name the student recognises, with the extension the file actually has. */
        fun renameToMatch(fileName: String, mimeType: String): String {
            val base = fileName.substringBeforeLast('.', fileName)
            return "$base.${extensionFor(mimeType)}"
        }

        fun extensionFor(mimeType: String): String = when {
            mimeType == MIME_PDF -> "pdf"
            mimeType == MIME_DOCX -> "docx"
            mimeType.startsWith(IMAGE_PREFIX) -> "jpg"
            else -> "bin"
        }

        /**
         * The largest power of two that keeps [longestEdge] at or above [maxEdge] once halved —
         * what `BitmapFactory` expects, and deliberately not past the target, so the final scale
         * never has to enlarge.
         */
        fun sampleSizeFor(longestEdge: Int, maxEdge: Int): Int {
            var sample = 1
            while (longestEdge / (sample * 2) >= maxEdge) sample *= 2
            return sample
        }
    }
}
