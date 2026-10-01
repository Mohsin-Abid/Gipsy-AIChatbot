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
 * [MessageBodies]. An attachment is recorded by name, type, size and the path of the copy kept in
 * app storage; the file's bytes stay on disk rather than being inlined into a row.
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
    val attachmentName: String? = null,
    val attachmentMime: String? = null,
    val attachmentSize: Long? = null,
    val attachmentPath: String? = null,
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
