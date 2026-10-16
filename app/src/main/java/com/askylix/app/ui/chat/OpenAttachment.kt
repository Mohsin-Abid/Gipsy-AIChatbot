package com.askylix.app.ui.chat

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a downloaded file to the phone's own viewer. A photo, a PDF and a Word document each open in
 * whatever app the phone has for them, which is how a file is opened on any phone and needs no viewer
 * of this app's own. Returns false when no app can open it.
 */
internal fun Context.openAttachmentFile(file: File, mimeType: String): Boolean {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType.ifBlank { "*/*" })
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return try {
        startActivity(view)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
