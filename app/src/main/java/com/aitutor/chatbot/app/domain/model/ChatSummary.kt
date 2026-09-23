package com.aitutor.chatbot.app.domain.model

/**
 * A row in Home's Recent Chats list. Backed by an empty repository flow in
 * this phase — Firestore + Room wiring lands in Phase 2.
 */
data class ChatSummary(
    val id: String,
    val modeId: ModeId,
    val title: String,
    val timestampMillis: Long,
)
