package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import com.aitutor.chatbot.app.R

/** Where an attached file came from. Each maps to one tile in the scan sheet. */
enum class ScanSource(
    @param:StringRes val labelRes: Int,
    @param:StringRes val detailRes: Int,
) {
    Camera(R.string.scan_camera, R.string.scan_camera_detail),
    Gallery(R.string.scan_gallery, R.string.scan_gallery_detail),
    Pdf(R.string.scan_pdf, R.string.scan_pdf_detail),
    Word(R.string.scan_word, R.string.scan_word_detail);

    companion object {
        /** Reads a stored name back, tolerating a value written by an older build. */
        fun fromNameOrNull(name: String?): ScanSource? = entries.firstOrNull { it.name == name }
    }
}

/**
 * A file attached to a question — a photo of a page, or a document.
 *
 * The app does not read the file: it is uploaded as it is and the tutor service makes sense of it.
 * [localPath] is a copy kept in the app's own storage rather than the picked URI, because a URI
 * borrowed from the photo picker or a file manager stops being readable once that grant lapses,
 * and a thread is expected to still show what was sent weeks later.
 */
data class Attachment(
    val source: ScanSource,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPath: String,
)

/**
 * One line of an answer. The tutor's replies are structured rather than one blob of prose: a
 * [Bullet] leads with a bolded [term], which is what makes an explanation skimmable and what lets
 * the same answer be re-rendered at reading size on the select-text screen.
 */
sealed interface AnswerBlock {
    data class Paragraph(val text: String) : AnswerBlock
    data class Bullet(val term: String, val text: String) : AnswerBlock
}

/**
 * An item in a conversation as the screen draws it.
 *
 * [DayMarker] and [Typing] are not messages and are never stored — the ViewModel inserts them while
 * turning stored rows into a thread.
 */
sealed interface ChatMessage {
    /**
     * A day separator, centred in the thread. It carries the day's timestamp rather than a label,
     * so "Today" and "Yesterday" are formatted where the locale is known.
     */
    data class DayMarker(val timestamp: Long) : ChatMessage

    data class User(
        val id: Long,
        val text: String,
        val attachment: Attachment? = null,
    ) : ChatMessage

    data class Assistant(
        val id: Long,
        val blocks: List<AnswerBlock>,
        /** Only the newest answer carries the action row. */
        val showActions: Boolean = false,
    ) : ChatMessage

    /** The three-dot indicator while a reply is being generated. */
    data object Typing : ChatMessage
}
