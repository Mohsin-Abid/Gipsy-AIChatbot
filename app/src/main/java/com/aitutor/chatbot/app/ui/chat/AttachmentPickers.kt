package com.aitutor.chatbot.app.ui.chat

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.aitutor.chatbot.app.data.attachment.AttachmentStore
import com.aitutor.chatbot.app.domain.model.ScanSource
import java.io.File

/**
 * The three ways a file gets into a chat, and the one thing they have in common: **none of them
 * needs a runtime permission.**
 *
 * The photo picker and the document picker hand back a URI for exactly what was chosen, and the
 * camera is the system camera app writing to a URI this app supplies. Asking for `CAMERA`,
 * `READ_MEDIA_IMAGES` or storage access would buy nothing and cost a permission dialog.
 */
@Immutable
class AttachmentPickers(
    val pick: (ScanSource) -> Unit,
)

/**
 * Wires the scan sheet's four tiles to their launchers.
 *
 * [onPicked] receives the chosen URI and which tile produced it; [onNoCameraApp] fires when the
 * device has no camera app at all, which is rare but silent otherwise.
 */
@Composable
fun rememberAttachmentPickers(
    onPicked: (Uri, ScanSource) -> Unit,
    onNoCameraApp: () -> Unit,
): AttachmentPickers {
    val context = LocalContext.current

    // Where the camera app writes. Held across recompositions so the result callback, which runs
    // after this composable has been through a configuration change, still knows the target.
    val captureTarget = remember { mutableCaptureTarget() }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val uri = captureTarget.uri
        if (saved && uri != null) onPicked(uri, ScanSource.Camera)
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onPicked(it, ScanSource.Gallery) } }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { onPicked(it, ScanSource.Pdf) } }

    return remember(takePicture, pickImage, openDocument) {
        AttachmentPickers { source ->
            when (source) {
                ScanSource.Camera -> {
                    val uri = context.newCaptureUri()
                    captureTarget.uri = uri
                    try {
                        takePicture.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        onNoCameraApp()
                    }
                }

                ScanSource.Gallery -> pickImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )

                // One picker for both document tiles, filtered to what the service accepts. The
                // tiles differ only in which type the student is looking for.
                ScanSource.Pdf, ScanSource.Word ->
                    openDocument.launch(AttachmentStore.DOCUMENT_MIME_TYPES)
            }
        }
    }
}

/** A mutable holder, because the capture URI is chosen on launch and read back on result. */
private class CaptureTarget { var uri: Uri? = null }

private fun mutableCaptureTarget() = CaptureTarget()

/**
 * A fresh file in the cache for the camera to fill, exposed through the app's `FileProvider`.
 *
 * The capture lands in the cache rather than in `files/attachments/`, because the kept copy is
 * written afterwards by [AttachmentStore] — downscaled, and only once the shot was actually taken.
 * A cancelled capture leaves an empty cache file the system is free to clear.
 */
private fun Context.newCaptureUri(): Uri {
    val directory = File(cacheDir, CAPTURE_DIRECTORY).apply { mkdirs() }
    val file = File(directory, "capture-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

private const val CAPTURE_DIRECTORY = "captures"
