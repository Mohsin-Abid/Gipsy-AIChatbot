package com.aitutor.chatbot.app.data.chat

import com.aitutor.chatbot.app.core.ext.cleanWhitespace
import com.aitutor.chatbot.app.core.ext.truncate
import com.aitutor.chatbot.app.data.ai.TutorAiClient
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.data.local.db.ChatDao
import com.aitutor.chatbot.app.data.local.db.ChatEntity
import com.aitutor.chatbot.app.data.local.db.MessageDao
import com.aitutor.chatbot.app.data.local.db.MessageEntity
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.MessageRole
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.ModeId
import com.aitutor.chatbot.app.domain.model.modes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

enum class ExpandDirection { Shorter, Longer }

/**
 * Chat storage, local only. Room (`chatDao`/`messageDao`) is the sole source of truth — nothing
 * about a student's conversations leaves the device.
 *
 * This deliberately has no remote mirror. An earlier version dual-wrote every chat and message to
 * `users/{uid}/chats/...` in Firestore and listened for remote changes; that was removed because
 * study conversations are private and there is no cross-device requirement. The consequence worth
 * knowing: uninstalling the app, or clearing its data, loses the history for good.
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val aiClient: TutorAiClient,
    private val preferencesRepository: UserPreferencesRepository,
) {

    fun recentChats(limit: Int = 8): Flow<List<ChatSummary>> =
        chatDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    fun allChats(): Flow<List<ChatSummary>> =
        chatDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun questionCount(): Flow<Int> = messageDao.observeQuestionCount()

    fun questionTimestampsSince(sinceMillis: Long): Flow<List<Long>> =
        messageDao.observeQuestionTimestampsSince(sinceMillis)

    fun messages(chatId: String): Flow<List<Message>> =
        messageDao.observeForChat(chatId).map { list -> list.map { it.toDomain() } }

    fun chatTitle(chatId: String): Flow<String?> =
        chatDao.observeChat(chatId).map { it?.title }

    suspend fun createChat(modeId: ModeId): String {
        val chatId = UUID.randomUUID().toString()
        chatDao.upsert(
            ChatEntity(
                id = chatId,
                modeId = modeId.name,
                title = "New chat",
                updatedAtMillis = System.currentTimeMillis(),
            )
        )
        return chatId
    }

    /** Appends the student's message, calls the tutor model with the full history, then appends the reply. */
    suspend fun sendMessage(chatId: String, mode: Mode, text: String): Result<Unit> = runCatching {
        val cleaned = text.cleanWhitespace()
        require(cleaned.isNotBlank()) { "Message is blank" }

        appendMessage(chatId, MessageRole.User, cleaned)
        renameIfFirstMessage(chatId, cleaned)
        requestAndAppendReply(chatId, mode)
    }

    /** Drops the previous AI reply (if any) and re-sends the last user prompt for a new response. */
    suspend fun regenerate(chatId: String, mode: Mode): Result<Unit> = runCatching {
        val history = messageDao.observeForChat(chatId).first().map { it.toDomain() }
        val lastMessage = history.lastOrNull()
        if (lastMessage != null && lastMessage.role == MessageRole.Model) {
            messageDao.deleteById(lastMessage.id)
        }
        requestAndAppendReply(chatId, mode)
    }

    /** Re-prompts the tutor model to condense/expand [targetMessage], appended as a new message. */
    suspend fun expand(
        chatId: String,
        mode: Mode,
        targetMessage: Message,
        direction: ExpandDirection
    ): Result<Unit> =
        runCatching {
            val instruction = when (direction) {
                ExpandDirection.Shorter -> "Make the following answer significantly shorter while keeping it correct:\n\n${targetMessage.text}"
                ExpandDirection.Longer -> "Expand the following answer with more detail and an example, while keeping it correct:\n\n${targetMessage.text}"
            }
            appendMessage(chatId, MessageRole.User, instruction)
            requestAndAppendReply(chatId, mode)
        }

    suspend fun setFeedback(chatId: String, messageId: String, liked: Boolean, disliked: Boolean) {
        messageDao.setFeedback(messageId, liked, disliked)
    }

    suspend fun renameChat(chatId: String, title: String) {
        val cleanTitle = title.cleanWhitespace().takeIf { it.isNotBlank() } ?: return
        chatDao.rename(chatId, cleanTitle)
    }

    suspend fun clearChat(chatId: String) {
        messageDao.clearForChat(chatId)
    }

    suspend fun deleteChat(chatId: String) {
        clearChat(chatId)
        chatDao.deleteById(chatId)
    }

    /**
     * On failure, nothing is appended — the student's own message stays visible and the ViewModel
     * shows an inline retry affordance instead of a permanent "sorry" bubble in the chat history.
     */
    private suspend fun requestAndAppendReply(chatId: String, mode: Mode) {
        val history = messageDao.observeForChat(chatId).first().map { it.toDomain() }
        val explanationLevel = preferencesRepository.explanationLevel.first()
        val language = preferencesRepository.language.first() ?: AppLanguage.English
        val profile = preferencesRepository.studentProfile.first()

        val replyText =
            aiClient.reply(mode, explanationLevel, language, profile, history).getOrThrow()
        appendMessage(chatId, MessageRole.Model, replyText)
    }

    private suspend fun appendMessage(chatId: String, role: MessageRole, text: String) {
        val now = System.currentTimeMillis()

        messageDao.upsert(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = if (role == MessageRole.User) "user" else "model",
                text = text,
                timestampMillis = now,
            )
        )
        chatDao.upsert(
            (chatDao.observeChat(chatId).first() ?: ChatEntity(
                id = chatId,
                modeId = modes.first().id.name,
                title = "New chat",
                updatedAtMillis = now
            )).copy(updatedAtMillis = now)
        )
    }

    private suspend fun renameIfFirstMessage(chatId: String, firstUserText: String) {
        val chat = chatDao.observeChat(chatId).first() ?: return
        if (chat.title == "New chat") {
            renameChat(chatId, firstUserText.truncate(48))
        }
    }
}
