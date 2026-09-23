package com.aitutor.chatbot.app.data.chat

import com.aitutor.chatbot.app.data.local.db.ChatEntity
import com.aitutor.chatbot.app.data.local.db.MessageEntity
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.MessageRole
import com.aitutor.chatbot.app.domain.model.ModeId

internal fun ChatEntity.toDomain() = ChatSummary(
    id = id,
    modeId = runCatching { ModeId.valueOf(modeId) }.getOrDefault(ModeId.AiChatbot),
    title = title,
    timestampMillis = updatedAtMillis,
)

internal fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    role = if (role == "user") MessageRole.User else MessageRole.Model,
    text = text,
    timestampMillis = timestampMillis,
    pending = pending,
    liked = liked,
    disliked = disliked,
)
