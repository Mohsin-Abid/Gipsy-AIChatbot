package com.aitutor.chatbot.app.data.chat

import com.aitutor.chatbot.app.data.local.db.ChatEntity
import com.aitutor.chatbot.app.data.local.db.MessageEntity
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.MessageRole
import com.aitutor.chatbot.app.domain.model.ModeId
import com.google.firebase.firestore.DocumentSnapshot

internal fun DocumentSnapshot.toChatEntity(): ChatEntity? {
    val modeId = getString("modeId") ?: return null
    return ChatEntity(
        id = id,
        modeId = modeId,
        title = getString("title") ?: "New chat",
        updatedAtMillis = getLong("updatedAtMillis") ?: 0L,
    )
}

internal fun DocumentSnapshot.toMessageEntity(chatId: String): MessageEntity? {
    val role = getString("role") ?: return null
    val text = getString("text") ?: return null
    return MessageEntity(
        id = id,
        chatId = chatId,
        role = role,
        text = text,
        timestampMillis = getLong("timestampMillis") ?: 0L,
        pending = false,
        liked = getBoolean("liked") ?: false,
        disliked = getBoolean("disliked") ?: false,
    )
}

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

internal fun Message.toFirestoreMap(): Map<String, Any?> = mapOf(
    "role" to if (role == MessageRole.User) "user" else "model",
    "text" to text,
    "timestampMillis" to timestampMillis,
    "liked" to liked,
    "disliked" to disliked,
)
