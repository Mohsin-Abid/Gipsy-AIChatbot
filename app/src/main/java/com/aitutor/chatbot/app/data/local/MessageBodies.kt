package com.aitutor.chatbot.app.data.local

import com.aitutor.chatbot.app.domain.model.AnswerBlock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * How an answer is written into [MessageEntity.body].
 *
 * A question is stored as its text. An answer is structured — paragraphs and bullets — so it is
 * stored as JSON rather than flattened to a string: flattening would mean re-parsing prose on every
 * read, and any convention chosen for that would break the first time an answer contained it.
 */
object MessageBodies {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class BlockDto(
        @SerialName("t") val type: String,
        @SerialName("x") val text: String,
        @SerialName("m") val term: String? = null,
    )

    private const val TYPE_PARAGRAPH = "p"
    private const val TYPE_BULLET = "b"

    fun encode(blocks: List<AnswerBlock>): String = json.encodeToString(
        blocks.map { block ->
            when (block) {
                is AnswerBlock.Paragraph -> BlockDto(TYPE_PARAGRAPH, block.text)
                is AnswerBlock.Bullet -> BlockDto(TYPE_BULLET, block.text, block.term)
            }
        }
    )

    /**
     * Reads blocks back. A row that cannot be parsed — written by a build that stored something
     * else — is shown as the one paragraph it literally is, rather than dropped or crashed on.
     */
    fun decode(body: String): List<AnswerBlock> = runCatching {
        json.decodeFromString<List<BlockDto>>(body).map { dto ->
            if (dto.type == TYPE_BULLET && dto.term != null) {
                AnswerBlock.Bullet(term = dto.term, text = dto.text)
            } else {
                AnswerBlock.Paragraph(dto.text)
            }
        }
    }.getOrElse { listOf(AnswerBlock.Paragraph(body)) }

    /** The one-line preview a history row shows, built from the same blocks. */
    fun preview(blocks: List<AnswerBlock>): String = blocks.joinToString(" ") { block ->
        when (block) {
            is AnswerBlock.Paragraph -> block.text
            is AnswerBlock.Bullet -> "${block.term} ${block.text}"
        }
    }
}
