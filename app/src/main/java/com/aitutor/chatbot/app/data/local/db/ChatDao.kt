package com.aitutor.chatbot.app.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    @Upsert
    suspend fun upsert(chat: ChatEntity)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun rename(chatId: String, title: String)

    @Delete
    suspend fun delete(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteById(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun clearAll()
}
