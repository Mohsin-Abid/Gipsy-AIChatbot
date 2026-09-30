package com.aitutor.chatbot.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity): Long

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun byId(id: Long): ChatEntity?

    /**
     * Newest first, with each chat's message count worked out in the same statement — the history
     * list needs the count for every row, and fetching the messages to count them would load the
     * whole database to draw one screen.
     */
    @Query(
        """
        SELECT c.id, c.toolName, c.title, c.subject, c.updatedAt,
               (SELECT COUNT(*) FROM messages m WHERE m.chatId = c.id) AS messageCount
        FROM chats c
        ORDER BY c.updatedAt DESC
        """
    )
    fun observeSummaries(): Flow<List<ChatSummaryRow>>

    @Query("UPDATE chats SET updatedAt = :at WHERE id = :id")
    suspend fun touch(id: Long, at: Long)

    @Query("UPDATE chats SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    /** Oldest first. Ties break on id, so two messages written in the same millisecond keep order. */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC, id ASC")
    fun observeForChat(chatId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC, id ASC")
    suspend fun forChat(chatId: Long): List<MessageEntity>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearChat(chatId: Long)
}
