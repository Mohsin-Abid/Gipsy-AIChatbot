package com.aitutor.chatbot.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One conversation. [toolName] is a [com.aitutor.chatbot.app.domain.model.StudyTool] name rather
 * than its ordinal, so reordering the enum can never silently re-label saved chats.
 */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val title: String,
    val subject: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * One message.
 *
 * [body] holds the text for a question and the serialized answer blocks for a reply — see
 * [MessageBodies]. The attachment is stored as its source and word count only: the photo or
 * document itself never enters the database, because only the extracted text was ever used.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chatId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val fromUser: Boolean,
    val body: String,
    val attachmentSource: String? = null,
    val attachmentWordCount: Int? = null,
    val createdAt: Long,
)

/** A chat plus what the history rows need about it, without loading its messages. */
data class ChatSummaryRow(
    val id: Long,
    val toolName: String,
    val title: String,
    val subject: String,
    val updatedAt: Long,
    val messageCount: Int,
)
