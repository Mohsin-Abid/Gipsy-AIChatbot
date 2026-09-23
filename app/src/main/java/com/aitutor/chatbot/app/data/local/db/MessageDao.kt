package com.aitutor.chatbot.app.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMillis ASC")
    fun observeForChat(chatId: String): Flow<List<MessageEntity>>

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET liked = :liked, disliked = :disliked WHERE id = :messageId")
    suspend fun setFeedback(messageId: String, liked: Boolean, disliked: Boolean)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearForChat(chatId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun lastForChat(chatId: String): MessageEntity?

    /** Dashboard stat: how many questions this student has asked, all time. */
    @Query("SELECT COUNT(*) FROM messages WHERE role = 'user'")
    fun observeQuestionCount(): Flow<Int>

    /** Raw timestamps for the dashboard's 7-day activity chart; bucketed into days by the ViewModel. */
    @Query("SELECT timestampMillis FROM messages WHERE role = 'user' AND timestampMillis >= :sinceMillis")
    fun observeQuestionTimestampsSince(sinceMillis: Long): Flow<List<Long>>
}
