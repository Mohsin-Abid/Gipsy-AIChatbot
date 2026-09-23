package com.aitutor.chatbot.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val modeId: String,
    val title: String,
    val updatedAtMillis: Long,
)
