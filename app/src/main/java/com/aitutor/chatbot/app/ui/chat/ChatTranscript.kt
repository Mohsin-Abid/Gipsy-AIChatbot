package com.aitutor.chatbot.app.ui.chat

import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.ChatMessage

/**
 * A conversation as plain text, for the share sheet.
 *
 * Plain text because that is what every app that can receive a share understands. The labels come in
 * as parameters so the words are the reader's language and live in `strings.xml` — this only decides
 * the shape: who spoke, then what they said, one blank line between turns.
 */
object ChatTranscript {

    fun format(
        title: String,
        messages: List<ChatMessage>,
        youLabel: String,
        tutorLabel: String,
        attachedLabel: (fileName: String) -> String,
    ): String {
        val turns = messages.mapNotNull { message ->
            when (message) {
                is ChatMessage.User -> userTurn(message, youLabel, attachedLabel)
                is ChatMessage.Assistant -> "$tutorLabel:\n${answerText(message.blocks)}"
                // Neither is something anyone said: a day separator and the "typing" dots.
                is ChatMessage.DayMarker, ChatMessage.Typing -> null
            }
        }
        return (listOf(title.trim()).filter { it.isNotEmpty() } + turns).joinToString("\n\n")
    }

    private fun userTurn(
        message: ChatMessage.User,
        youLabel: String,
        attachedLabel: (String) -> String,
    ): String {
        val lines = buildList {
            if (message.text.isNotBlank()) add(message.text.trim())
            // The file itself cannot travel in text, but saying one was attached keeps the question
            // from reading as if it were about nothing.
            message.attachment?.let { add(attachedLabel(it.fileName)) }
        }
        return "$youLabel:\n${lines.joinToString("\n")}"
    }

    private fun answerText(blocks: List<AnswerBlock>): String = blocks.joinToString("\n") { block ->
        when (block) {
            is AnswerBlock.Paragraph -> block.text
            is AnswerBlock.Bullet -> "• ${block.term} ${block.text}"
        }
    }
}
