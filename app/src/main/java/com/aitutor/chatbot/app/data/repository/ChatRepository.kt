package com.aitutor.chatbot.app.data.repository

import com.aitutor.chatbot.app.data.api.TutorApiClient
import com.aitutor.chatbot.app.data.api.TutorAttachment
import com.aitutor.chatbot.app.data.attachment.AttachmentStore
import com.aitutor.chatbot.app.data.api.TutorRequest
import com.aitutor.chatbot.app.data.api.TutorTurn
import com.aitutor.chatbot.app.data.local.ChatDao
import com.aitutor.chatbot.app.data.local.ChatEntity
import com.aitutor.chatbot.app.data.local.ChatSummaryRow
import com.aitutor.chatbot.app.data.local.MessageBodies
import com.aitutor.chatbot.app.data.local.MessageDao
import com.aitutor.chatbot.app.data.local.MessageEntity
import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.ChatMessage
import com.aitutor.chatbot.app.domain.model.ScanSource
import com.aitutor.chatbot.app.domain.model.Attachment
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyTool
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.Calendar

/** A conversation's identity, without its messages. */
data class ChatHeader(
    val id: Long,
    val tool: StudyTool,
    val title: String,
    val subject: String,
    val updatedAt: Long,
    val messageCount: Int,
)

/**
 * Conversations.
 *
 * Reads are [Flow]s straight off Room, so a screen showing a thread updates the moment a message is
 * written — there is no in-memory copy to keep in step. Writes go to the database first and only
 * then to the network: a question the student typed is theirs and survives a failed request.
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val api: TutorApiClient,
    private val attachments: AttachmentStore,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun observeHeaders(): Flow<List<ChatHeader>> =
        chatDao.observeSummaries().map { rows -> rows.mapNotNull(ChatSummaryRow::toHeaderOrNull) }

    /**
     * The thread, with a day separator before the first message of each day. Grouping belongs here
     * rather than in the query: SQLite would need a time zone to decide where a day starts.
     */
    fun observeMessages(chatId: Long): Flow<List<ChatMessage>> =
        messageDao.observeForChat(chatId).map { rows -> rows.toThread() }

    suspend fun createChat(tool: StudyTool, title: String, subject: String): Long {
        val timestamp = now()
        return chatDao.insert(
            ChatEntity(
                toolName = tool.name,
                title = title,
                subject = subject,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        )
    }

    suspend fun header(chatId: Long): ChatHeader? =
        chatDao.byId(chatId)?.let { chat ->
            val tool = StudyTool.entries.firstOrNull { it.name == chat.toolName } ?: return null
            ChatHeader(
                id = chat.id,
                tool = tool,
                title = chat.title,
                subject = chat.subject,
                updatedAt = chat.updatedAt,
                messageCount = messageDao.forChat(chat.id).size,
            )
        }

    /**
     * Saves the question, then asks for a reply.
     *
     * The question is committed before the request goes out, so it is still there if the request
     * fails. Nothing is written for the answer unless one actually came back — a failure surfaces
     * as [Result.failure] for the caller to show, and never as a placeholder reply in the thread.
     */
    suspend fun send(
        chatId: Long,
        question: String,
        attachment: Attachment? = null,
        profile: StudentProfile = StudentProfile(),
    ): Result<Unit> {
        val chat = chatDao.byId(chatId) ?: return Result.failure(
            IllegalArgumentException("No chat with id $chatId")
        )
        val tool = StudyTool.entries.firstOrNull { it.name == chat.toolName }
            ?: return Result.failure(IllegalStateException("Unknown tool ${chat.toolName}"))

        val priorTurns = messageDao.forChat(chatId).map { it.toTurn() }

        messageDao.insert(
            MessageEntity(
                chatId = chatId,
                fromUser = true,
                body = question,
                attachmentSource = attachment?.source?.name,
                attachmentName = attachment?.fileName,
                attachmentMime = attachment?.mimeType,
                attachmentSize = attachment?.sizeBytes,
                attachmentPath = attachment?.localPath,
                createdAt = now(),
            )
        )
        chatDao.touch(chatId, now())

        val answer = api.requestAnswer(
            TutorRequest(
                tool = tool,
                subject = chat.subject,
                question = question,
                attachment = attachment?.toTutorAttachment(),
                history = priorTurns,
                profile = profile,
            )
        )

        return answer.mapCatching { reply ->
            messageDao.insert(
                MessageEntity(
                    chatId = chatId,
                    fromUser = false,
                    body = MessageBodies.encode(reply.blocks),
                    createdAt = now(),
                )
            )
            chatDao.touch(chatId, now())
        }
    }

    suspend fun rename(chatId: Long, title: String) = chatDao.rename(chatId, title)

    /**
     * Deletes a chat's messages, and the files they attached.
     *
     * The rows go either way; the copies on disk would not, and an attachment whose message is gone
     * is unreachable — nothing would ever delete it. The files go first, because a failure there
     * leaves rows that still point at them rather than orphans nothing can find.
     */
    suspend fun clearMessages(chatId: Long) {
        deleteAttachmentFiles(chatId)
        messageDao.clearChat(chatId)
        chatDao.touch(chatId, now())
    }

    /** Deletes a chat. Its messages cascade; its attachment files are removed here. */
    suspend fun delete(chatId: Long) {
        deleteAttachmentFiles(chatId)
        chatDao.delete(chatId)
    }

    private suspend fun deleteAttachmentFiles(chatId: Long) {
        messageDao.forChat(chatId).mapNotNull { it.toAttachment() }.forEach { attachments.delete(it) }
    }
}

private fun ChatSummaryRow.toHeaderOrNull(): ChatHeader? {
    // A row naming a tool this build no longer has is skipped rather than guessed at.
    val tool = StudyTool.entries.firstOrNull { it.name == toolName } ?: return null
    return ChatHeader(
        id = id,
        tool = tool,
        title = title,
        subject = subject,
        updatedAt = updatedAt,
        messageCount = messageCount,
    )
}

private fun List<MessageEntity>.toThread(): List<ChatMessage> {
    val thread = mutableListOf<ChatMessage>()
    var lastDay: Long? = null
    forEachIndexed { index, row ->
        val day = row.createdAt.startOfDay()
        if (day != lastDay) {
            thread += ChatMessage.DayMarker(timestamp = row.createdAt)
            lastDay = day
        }
        thread += row.toMessage(isNewest = index == lastIndex)
    }
    return thread
}

/** Midnight local time, which is the boundary a reader means by "a different day". */
private fun Long.startOfDay(): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@startOfDay }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun MessageEntity.toMessage(isNewest: Boolean): ChatMessage =
    if (fromUser) {
        ChatMessage.User(
            id = id,
            text = body,
            attachment = toAttachment(),
        )
    } else {
        ChatMessage.Assistant(
            id = id,
            blocks = MessageBodies.decode(body),
            // The design shows the action row under the latest answer only.
            showActions = isNewest,
        )
    }

/**
 * The attachment a row records, or null. Every field has to be present: a half-written row is a
 * row from a build that stored something else, and is better shown as a plain message than as an
 * attachment pointing at nothing.
 */
private fun MessageEntity.toAttachment(): Attachment? {
    val source = ScanSource.fromNameOrNull(attachmentSource) ?: return null
    return Attachment(
        source = source,
        fileName = attachmentName ?: return null,
        mimeType = attachmentMime ?: return null,
        sizeBytes = attachmentSize ?: return null,
        localPath = attachmentPath ?: return null,
    )
}

/** The upload's view of an attachment: the file on disk, plus what the service needs to name it. */
private fun Attachment.toTutorAttachment(): TutorAttachment = TutorAttachment(
    file = File(localPath),
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
)

private fun MessageEntity.toTurn(): TutorTurn = TutorTurn(
    fromUser = fromUser,
    text = if (fromUser) body else MessageBodies.preview(MessageBodies.decode(body)),
)

/** The blocks of the newest answer in a thread, which is the one the select-text screen opens on. */
fun List<ChatMessage>.newestAnswerBlocks(): List<AnswerBlock> =
    filterIsInstance<ChatMessage.Assistant>().lastOrNull()?.blocks.orEmpty()
