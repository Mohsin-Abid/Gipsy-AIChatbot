package com.aitutor.chatbot.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    /** "user" or "model" */
    val role: String,
    val text: String,
    val timestampMillis: Long,
    val pending: Boolean = false,
    val liked: Boolean = false,
    val disliked: Boolean = false,
)
