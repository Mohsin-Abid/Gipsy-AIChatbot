package com.aitutor.chatbot.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The on-device store. Conversations live here and nowhere else: the manifest's backup rules
 * exclude this file and its journals from both cloud backup and device-to-device transfer, so
 * nothing written here leaves the device it was written on.
 */
@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        /** Named in the backup rules too — the two have to agree, so change them together. */
        const val NAME = "aitutor.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
