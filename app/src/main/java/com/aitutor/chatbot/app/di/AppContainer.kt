package com.aitutor.chatbot.app.di

import android.content.Context
import com.aitutor.chatbot.app.data.api.TutorApiClient
import com.aitutor.chatbot.app.data.api.UnconfiguredTutorApiClient
import com.aitutor.chatbot.app.data.attachment.AttachmentStore
import com.aitutor.chatbot.app.data.local.AppDatabase
import com.aitutor.chatbot.app.data.prefs.UserPreferencesRepository
import com.aitutor.chatbot.app.data.repository.ChatRepository

/**
 * The app's dependencies, wired by hand.
 *
 * There are four of them and one implementation of each, which a DI framework would not make
 * clearer — and this way the whole graph is one readable file, and a test can build its own
 * container with an in-memory database and a fake API client without any setup.
 *
 * Everything is lazy, so opening the database is deferred until a screen actually reads from it
 * rather than happening on the launch path.
 */
interface AppContainer {
    val preferences: UserPreferencesRepository
    val chats: ChatRepository
    val attachments: AttachmentStore
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    /**
     * Swap this for the real client once the endpoint exists — it is the only line that changes.
     * Until then every request fails rather than returning invented answers.
     */
    private val api: TutorApiClient by lazy { UnconfiguredTutorApiClient() }

    override val preferences: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    override val attachments: AttachmentStore by lazy { AttachmentStore(appContext) }

    override val chats: ChatRepository by lazy {
        ChatRepository(
            chatDao = database.chatDao(),
            messageDao = database.messageDao(),
            api = api,
            attachments = attachments,
        )
    }
}
