package com.aitutor.chatbot.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aitutor.chatbot.app.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The 1 → 2 migration, which trades an attachment's word count for its file details.
 *
 * Worth a real test rather than trust: this database holds every conversation a student has had,
 * and the alternative to a correct migration is Room dropping the table on upgrade.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_keepsExistingMessagesAndSwapsTheAttachmentColumns() {
        helper.createDatabase(databaseName, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO chats (id, toolName, title, subject, createdAt, updatedAt)
                VALUES (1, 'Chatbot', 'Why is the sky blue?', 'Physics', 100, 200)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO messages (id, chatId, fromUser, body, attachmentSource, attachmentWordCount, createdAt)
                VALUES (1, 1, 1, 'Why is the sky blue?', 'Camera', 48, 150)
                """.trimIndent()
            )
        }

        // The migration is handed over explicitly: without it the helper reports that no path
        // from 1 to 2 exists, which is exactly the failure a missing migration causes in the app.
        val db = helper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            AppDatabase.MIGRATION_1_2,
        )

        db.query("SELECT * FROM messages").use { cursor ->
            assertTrue("the message survived the migration", cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals(
                "Why is the sky blue?",
                cursor.getString(cursor.getColumnIndexOrThrow("body")),
            )
            // The source is kept; the word count is gone and the file columns have taken its place.
            assertEquals("Camera", cursor.getString(cursor.getColumnIndexOrThrow("attachmentSource")))
            assertFalse(cursor.isNull(cursor.getColumnIndexOrThrow("attachmentSource")))
            listOf("attachmentName", "attachmentMime", "attachmentSize", "attachmentPath").forEach {
                assertTrue("$it was added", cursor.getColumnIndex(it) >= 0)
                assertTrue("$it is null for a pre-existing row", cursor.isNull(cursor.getColumnIndex(it)))
            }
            assertEquals("attachmentWordCount is gone", -1, cursor.getColumnIndex("attachmentWordCount"))
        }

        db.query("SELECT COUNT(*) FROM chats").use { cursor ->
            cursor.moveToFirst()
            assertEquals("the conversation survived", 1, cursor.getInt(0))
        }
    }
}
