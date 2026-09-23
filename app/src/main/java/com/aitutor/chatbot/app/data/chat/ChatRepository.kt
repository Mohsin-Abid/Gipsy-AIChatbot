package com.aitutor.chatbot.app.data.chat

import com.aitutor.chatbot.app.data.ai.GeminiService
import com.aitutor.chatbot.app.data.firebase.FirebaseAuthGateway
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.data.local.db.ChatDao
import com.aitutor.chatbot.app.data.local.db.ChatEntity
import com.aitutor.chatbot.app.data.local.db.MessageDao
import com.aitutor.chatbot.app.data.local.db.MessageEntity
import com.aitutor.chatbot.app.core.ext.cleanWhitespace
import com.aitutor.chatbot.app.core.ext.truncate
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.MessageRole
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.ModeId
import com.aitutor.chatbot.app.domain.model.modes
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class ExpandDirection { Shorter, Longer }

/**
 * Offline-first chat storage: Room (`chatDao`/`messageDao`) is the single source of truth the UI
 * reads from. Writes go to Room first (so the UI updates instantly) and then to Firestore under
 * `users/{uid}/chats/{chatId}/messages`; [startSync] mirrors remote changes back into Room via
 * snapshot listeners, so other devices/writes reconcile automatically.
 */
class ChatRepository(
    private val firestore: FirebaseFirestore,
    private val authGateway: FirebaseAuthGateway,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val geminiService: GeminiService,
    private val preferencesRepository: UserPreferencesRepository,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var allChatsSyncStarted = false
    private val syncedChatIds = mutableSetOf<String>()

    private fun chatsCollection(uid: String) =
        firestore.collection("users").document(uid).collection("chats")

    private fun messagesCollection(uid: String, chatId: String) =
        chatsCollection(uid).document(chatId).collection("messages")

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

    /**
     * Starts mirroring this device's chats + a specific chat's messages from Firestore into Room.
     * Idempotent per chat id / for the all-chats listener — safe to call every time a screen opens
     * without piling up duplicate snapshot listeners for the life of the process.
     */
    fun startSync(chatId: String? = null) {
        repoScope.launch {
            val uid = authGateway.uid.first() ?: return@launch

            if (!allChatsSyncStarted) {
                allChatsSyncStarted = true
                callbackFlow {
                    val registration = chatsCollection(uid)
                        .orderBy("updatedAtMillis", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, _ -> trySend(snapshot?.documents.orEmpty()) }
                    awaitClose { registration.remove() }
                }.onEach { docs -> docs.forEach { doc -> doc.toChatEntity()?.let { chatDao.upsert(it) } } }
                    .launchIn(repoScope)
            }

            if (chatId != null && syncedChatIds.add(chatId)) {
                callbackFlow {
                    val registration = messagesCollection(uid, chatId)
                        .orderBy("timestampMillis", Query.Direction.ASCENDING)
                        .addSnapshotListener { snapshot, _ -> trySend(snapshot?.documents.orEmpty()) }
                    awaitClose { registration.remove() }
                }.onEach { docs -> docs.forEach { doc -> doc.toMessageEntity(chatId)?.let { messageDao.upsert(it) } } }
                    .launchIn(repoScope)
            }
        }
    }

    suspend fun createChat(modeId: ModeId): String {
        val uid = authGateway.uid.first()
        val chatId = uid?.let { chatsCollection(it).document().id } ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        chatDao.upsert(
            ChatEntity(
                id = chatId,
                modeId = modeId.name,
                title = "New chat",
                updatedAtMillis = now,
            )
        )
        if (uid != null) {
            runCatching {
                chatsCollection(uid).document(chatId)
                    .set(mapOf("modeId" to modeId.name, "title" to "New chat", "updatedAtMillis" to now))
                    .await()
            }
        }
        return chatId
    }

    /** Appends the student's message, calls Gemini with the full history, then appends the reply. */
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
            deleteMessage(chatId, lastMessage.id)
        }
        requestAndAppendReply(chatId, mode)
    }

    /** Re-prompts Gemini to condense/expand [targetMessage], appended as a new message. */
    suspend fun expand(chatId: String, mode: Mode, targetMessage: Message, direction: ExpandDirection): Result<Unit> =
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
        val uid = authGateway.uid.first() ?: return
        runCatching {
            messagesCollection(uid, chatId).document(messageId)
                .update(mapOf("liked" to liked, "disliked" to disliked)).await()
        }
    }

    suspend fun renameChat(chatId: String, title: String) {
        val cleanTitle = title.cleanWhitespace().takeIf { it.isNotBlank() } ?: return
        chatDao.rename(chatId, cleanTitle)
        val uid = authGateway.uid.first() ?: return
        runCatching { chatsCollection(uid).document(chatId).update("title", cleanTitle).await() }
    }

    suspend fun clearChat(chatId: String) {
        messageDao.clearForChat(chatId)
        val uid = authGateway.uid.first() ?: return
        runCatching {
            messagesCollection(uid, chatId).get().await().documents.forEach { it.reference.delete().await() }
        }
    }

    suspend fun deleteChat(chatId: String) {
        clearChat(chatId)
        chatDao.deleteById(chatId)
        val uid = authGateway.uid.first() ?: return
        runCatching { chatsCollection(uid).document(chatId).delete().await() }
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

        val replyText = geminiService.reply(mode, explanationLevel, language, profile, history).getOrThrow()
        appendMessage(chatId, MessageRole.Model, replyText)
    }

    private suspend fun deleteMessage(chatId: String, messageId: String) {
        messageDao.deleteById(messageId)
        val uid = authGateway.uid.first() ?: return
        runCatching { messagesCollection(uid, chatId).document(messageId).delete().await() }
    }

    private suspend fun appendMessage(chatId: String, role: MessageRole, text: String) {
        val uid = authGateway.uid.first()
        val messageId = uid?.let { messagesCollection(it, chatId).document().id } ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val message = Message(id = messageId, chatId = chatId, role = role, text = text, timestampMillis = now)

        messageDao.upsert(
            MessageEntity(
                id = messageId,
                chatId = chatId,
                role = if (role == MessageRole.User) "user" else "model",
                text = text,
                timestampMillis = now,
            )
        )
        chatDao.upsert(
            (chatDao.observeChat(chatId).first() ?: ChatEntity(
                id = chatId, modeId = modes.first().id.name, title = "New chat", updatedAtMillis = now
            )).copy(updatedAtMillis = now)
        )

        if (uid != null) {
            runCatching {
                messagesCollection(uid, chatId).document(messageId).set(message.toFirestoreMap()).await()
                chatsCollection(uid).document(chatId).update("updatedAtMillis", now).await()
            }
        }
    }

    private suspend fun renameIfFirstMessage(chatId: String, firstUserText: String) {
        val chat = chatDao.observeChat(chatId).first() ?: return
        if (chat.title == "New chat") {
            renameChat(chatId, firstUserText.truncate(48))
        }
    }
}
