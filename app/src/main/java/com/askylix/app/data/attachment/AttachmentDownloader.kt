package com.askylix.app.data.attachment

import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.FileApi
import com.askylix.app.data.remote.toApiError
import com.askylix.app.domain.model.Attachment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File

/**
 * Fetches an attachment from the link the server gave, on request.
 *
 * Nothing is fetched until the person taps a file. The result is kept in the cache folder under the
 * message's id, so opening the same file again does not download it again, and log-out removes it.
 *
 * The link is signed and good for about ten minutes, so it is used at once and never stored. If it
 * has expired, [refreshLink] is asked for a new one and the download is tried once more.
 */
class AttachmentDownloader(
    private val files: FileApi,
    /** The folder downloads are kept in. A function, so it is only resolved when something is downloaded. */
    private val downloadsDirectory: () -> File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    /**
     * The file for [attachment]: this phone's own copy if it sent it, a download already made, or a
     * fresh download. [onProgress] gets 0 to 100 while bytes arrive, when the size is known.
     */
    suspend fun fetch(
        messageId: String,
        attachment: Attachment,
        refreshLink: suspend () -> String?,
        onProgress: (Int) -> Unit = {},
    ): Result<File> = withContext(io) {
        runCatching {
            attachment.localPath?.let(::File)?.takeIf { it.exists() }?.let { return@runCatching it }

            val target = cacheFileFor(messageId, attachment.fileName)
            if (target.exists() && target.length() > 0) return@runCatching target

            val first = attachment.url ?: refreshLink() ?: throw ApiError(ApiErrorCode.NotFound, 404)
            try {
                download(first, target, onProgress)
            } catch (expired: ApiError) {
                // 401, 403 and 404 on a signed link all mean the signature has lapsed.
                if (expired.status !in EXPIRED_STATUSES) throw expired
                val fresh = refreshLink() ?: throw expired
                download(fresh, target, onProgress)
            }
            target
        }.onFailure { if (it is CancellationException) throw it }
    }

    private suspend fun download(url: String, target: File, onProgress: (Int) -> Unit) {
        val partial = File(target.parentFile, target.name + ".part")
        try {
            val body = try {
                files.download(url)
            } catch (failure: HttpException) {
                throw failure.toApiError()
            } catch (failure: java.io.IOException) {
                throw failure.toApiError()
            }

            body.use { response ->
                val total = response.contentLength()
                if (total > AttachmentStore.MAX_FILE_BYTES) throw ApiError(ApiErrorCode.FileTooLarge, 413)

                var received = 0L
                response.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            received += read
                            if (total > 0) onProgress(((received * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
            }
        } finally {
            partial.delete()
        }
    }

    private fun cacheFileFor(messageId: String, fileName: String): File {
        val folder = File(downloadsDirectory(), messageId.sanitised()).apply { mkdirs() }
        return File(folder, fileName.sanitised())
    }

    /** A name safe to use on disk: whatever the server or the original file was called, it stays inside the folder. */
    private fun String.sanitised(): String = replace(Regex("[^A-Za-z0-9._-]"), "_").take(MAX_NAME).ifEmpty { "file" }

    private companion object {
        val EXPIRED_STATUSES = setOf(401, 403, 404, 410)
        const val BUFFER = 8 * 1024
        const val MAX_NAME = 120
    }
}
