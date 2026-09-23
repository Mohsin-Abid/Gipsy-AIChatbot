package com.aitutor.chatbot.app.core.ext

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * A content:// destination for a homework scan, inside the app's own cache so captures are cleaned
 * up with it and never land in the user's photo gallery.
 */
fun Context.createScanCaptureUri(): Uri {
    val scansDir = File(cacheDir, "scans").apply { mkdirs() }
    val file = File(scansDir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}
