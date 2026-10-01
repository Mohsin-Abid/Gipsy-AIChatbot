package com.aitutor.chatbot.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * The on-device store. Conversations live here and nowhere else: the manifest's backup rules
 * exclude this file and its journals from both cloud backup and device-to-device transfer, so
 * nothing written here leaves the device it was written on.
 */
@Database(
    entities = [ChatEntity::class, MessageEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao

    companion object {
        /** Named in the backup rules too — the two have to agree, so change them together. */
        const val NAME = "aitutor.db"

        /**
         * Attachments stopped being "some extracted words" and became a file on disk, so the word
         * count gave way to the file's name, type, size and path.
         *
         * Written out rather than left to a destructive fallback: dropping the table would take
         * every saved conversation with it, which is the one thing this database exists to keep.
         * The new columns are nullable, so existing rows need no backfill — a message sent before
         * this version simply had no attachment.
         */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE messages DROP COLUMN attachmentWordCount")
                connection.execSQL("ALTER TABLE messages ADD COLUMN attachmentName TEXT")
                connection.execSQL("ALTER TABLE messages ADD COLUMN attachmentMime TEXT")
                connection.execSQL("ALTER TABLE messages ADD COLUMN attachmentSize INTEGER")
                connection.execSQL("ALTER TABLE messages ADD COLUMN attachmentPath TEXT")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
