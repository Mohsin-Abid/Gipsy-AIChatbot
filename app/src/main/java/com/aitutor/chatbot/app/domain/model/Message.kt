package com.aitutor.chatbot.app.domain.model

enum class MessageRole { User, Model }

data class Message(
    val id: String,
    val chatId: String,
    val role: MessageRole,
    val text: String,
    val timestampMillis: Long,
    val pending: Boolean = false,
    val liked: Boolean = false,
    val disliked: Boolean = false,
)

enum class ReportReason(val label: String) {
    Inaccurate("Inaccurate or wrong"),
    Inappropriate("Inappropriate content"),
    NotHelpful("Not helpful"),
    Other("Other"),
}
