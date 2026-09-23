package com.aitutor.chatbot.app.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class EmptyExtractionException : Exception("No readable text found")

/**
 * ML Kit text recognition, on-device. Nothing is uploaded — the photo never leaves the phone, which
 * matters when the "document" is a student's own homework. Scanned PDFs and .docx parsing are a
 * later phase; this covers camera captures and gallery images.
 */
class ImageTextExtractor(private val context: Context) {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    suspend fun extract(uri: Uri): Result<String> = runCatching {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        val text = result.textBlocks
            .joinToString("\n") { block -> block.lines.joinToString("\n") { it.text } }
            .trim()
        if (text.isBlank()) throw EmptyExtractionException()
        text
    }
}
