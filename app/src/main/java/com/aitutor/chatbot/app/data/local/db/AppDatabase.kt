package com.aitutor.chatbot.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatEntity::class, MessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
}
