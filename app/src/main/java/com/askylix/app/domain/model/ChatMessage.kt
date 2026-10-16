package com.askylix.app.domain.model

import androidx.annotation.StringRes
import com.askylix.app.R

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
 *
 * It comes from one of two places. Before it is sent, it is a copy in the app's own storage
 * ([localPath]) rather than the picked URI, because a URI borrowed from the photo picker or a file
 * manager stops being readable once that grant lapses. Once a chat has been read back from the
 * server, it is a [url] instead: a signed link that stops working after about ten minutes, so it is
 * used straight away and never stored.
 */
data class Attachment(
    val source: ScanSource,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localPath: String? = null,
    val url: String? = null,
) {
    companion object {
        /**
         * What a file is, from its type alone. The server does not say whether a photo came from the
         * camera or the gallery, so a photo is called a gallery photo.
         */
        fun sourceFor(mimeType: String): ScanSource = when {
            mimeType == "application/pdf" -> ScanSource.Pdf
            mimeType.contains("wordprocessingml") || mimeType.contains("msword") -> ScanSource.Word
            else -> ScanSource.Gallery
        }
    }
}

/**
 * One line of an answer. The tutor's replies are structured rather than one blob of prose: a
 * [Bullet] leads with a bolded [term], which is what makes an explanation skimmable and what lets
 * the same answer be re-rendered at reading size on the select-text screen.
 */
sealed interface AnswerBlock {
    data class Paragraph(val text: String) : AnswerBlock
    data class Bullet(val term: String, val text: String) : AnswerBlock
}

/** Where a question stands. */
enum class SendStatus {
    /** The server has it. */
    Sent,

    /** On its way. Shown at once, so the thread reacts to a tap before the network does. */
    Sending,

    /** The server saved it but could not produce an answer (HTTP 207). It can be asked again. */
    Unanswered,
}

/** How the student rated an answer. */
enum class Rating { Helpful, NotHelpful }

/**
 * An item in a conversation as the screen draws it.
 *
 * [DayMarker] and [Typing] are not messages and are never stored — the thread builder inserts the
 * markers, and the screen adds the typing row while a reply is on its way. Message ids are the
 * server's (`msg_…`), or `local:…` for a question that has not been answered yet.
 */
sealed interface ChatMessage {
    /**
     * A day separator, centred in the thread. It carries the day's timestamp rather than a label,
     * so "Today" and "Yesterday" are formatted where the locale is known.
     */
    data class DayMarker(val timestamp: Long) : ChatMessage

    data class User(
        val id: String,
        val text: String,
        val attachment: Attachment? = null,
        val status: SendStatus = SendStatus.Sent,
    ) : ChatMessage

    data class Assistant(
        val id: String,
        val blocks: List<AnswerBlock>,
        /** Only the newest answer carries the action row. */
        val showActions: Boolean = false,
        val rating: Rating? = null,
    ) : ChatMessage

    /** The three-dot indicator while a reply is being generated. */
    data object Typing : ChatMessage
}
