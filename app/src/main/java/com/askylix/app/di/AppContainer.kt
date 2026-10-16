package com.askylix.app.di

import android.content.Context
import com.askylix.app.BuildConfig
import com.askylix.app.data.attachment.AttachmentDownloader
import com.askylix.app.data.attachment.AttachmentStore
import com.askylix.app.data.auth.AuthRepository
import com.askylix.app.data.net.ConnectivityMonitor
import com.askylix.app.data.prefs.UserPreferencesRepository
import com.askylix.app.data.remote.ApiCaller
import com.askylix.app.data.remote.ApiClient
import com.askylix.app.data.remote.addDebugLogging
import com.askylix.app.data.repository.AccountRepository
import com.askylix.app.data.repository.BillingRepository
import com.askylix.app.data.repository.ChatRepository
import com.askylix.app.data.session.SessionStore
import com.askylix.app.data.voice.Speaker
import com.askylix.app.data.voice.VoiceRecognizer
import com.askylix.app.ui.AppMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The app's dependencies, wired by hand.
 *
 * There is one implementation of each, which a DI framework would not make clearer — and this way the
 * whole graph is one readable file, and a test can build its own container with a fake server's address
 * without any setup.
 *
 * Everything is lazy, so nothing is built until a screen needs it rather than on the launch path.
 */
interface AppContainer {
    val preferences: UserPreferencesRepository
    val attachments: AttachmentStore
    val voice: VoiceRecognizer

    /**
     * Shared rather than per-screen: starting a text-to-speech engine takes about a second, and
     * paying that each time a chat opens would be audible as a delay before the first word.
     */
    val speaker: Speaker

    /** Accounts. One instance, because it wraps the one signed-in user Firebase keeps for the app. */
    val auth: AuthRepository

    /** What has been read from the server this session. Memory only. */
    val session: SessionStore

    val account: AccountRepository
    val chats: ChatRepository
    val billing: BillingRepository
    val downloader: AttachmentDownloader

    /** Short notices for failures that happen after a screen has moved on. */
    val messages: AppMessages

    /** Whether the phone has a connection, for the "No connection" note. */
    val connectivity: ConnectivityMonitor

    /** Fires when the server has refused the session even after a fresh token: the person must sign in again. */
    val sessionExpired: SharedFlow<Unit>
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    /** Outlives every screen. A question being answered runs here, so leaving the chat does not cancel it. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    override val preferences: UserPreferencesRepository by lazy { UserPreferencesRepository(appContext) }

    override val attachments: AttachmentStore by lazy { AttachmentStore(appContext) }

    override val voice: VoiceRecognizer by lazy { VoiceRecognizer(appContext) }

    override val speaker: Speaker by lazy { Speaker(appContext) }

    override val auth: AuthRepository by lazy { AuthRepository(appContext) }

    override val session: SessionStore by lazy { SessionStore() }

    override val messages: AppMessages by lazy { AppMessages() }

    override val connectivity: ConnectivityMonitor by lazy { ConnectivityMonitor(appContext) }

    private val client: ApiClient by lazy {
        ApiClient(baseUrl = BuildConfig.API_BASE_URL, tokens = auth, configure = { addDebugLogging() })
    }

    private val caller: ApiCaller by lazy {
        ApiCaller(onUnauthorized = { _sessionExpired.tryEmit(Unit) })
    }

    override val account: AccountRepository by lazy {
        AccountRepository(
            api = client.api,
            caller = caller,
            preferences = preferences,
            session = session,
            signOutOfFirebase = { auth.signOut() },
            clearFiles = { attachments.clear() },
        )
    }

    override val chats: ChatRepository by lazy {
        ChatRepository(api = client.api, caller = caller, session = session, scope = appScope)
    }

    override val billing: BillingRepository by lazy {
        BillingRepository(api = client.api, caller = caller, preferences = preferences, session = session)
    }

    override val downloader: AttachmentDownloader by lazy { AttachmentDownloader(client.files, downloadsDirectory = { attachments.downloadsDirectory }) }
}
