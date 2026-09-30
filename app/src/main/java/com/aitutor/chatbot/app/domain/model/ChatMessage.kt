package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import com.aitutor.chatbot.app.R

/** Where an attachment's text came from. Each maps to one tile in the scan sheet. */
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
 * Text pulled off a photo or document and attached to a question. It carries the extracted text's
 * length, not the file — extraction happens on the device, and only the text is ever sent.
 */
data class ScannedText(
    val source: ScanSource,
    val wordCount: Int,
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
        val attachment: ScannedText? = null,
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
