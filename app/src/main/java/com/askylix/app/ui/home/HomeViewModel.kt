package com.askylix.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.askylix.app.data.auth.AuthRepository
import com.askylix.app.data.prefs.UserPreferencesRepository
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.QuotaDto
import com.askylix.app.data.remote.ToolFlagDto
import com.askylix.app.data.remote.toStudyTool
import com.askylix.app.data.repository.AccountRepository
import com.askylix.app.data.repository.ChatRepository
import com.askylix.app.data.session.SessionStore
import com.askylix.app.domain.model.HistoryEntry
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.ui.history.toEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the server says about a tool that the app cannot know on its own. */
data class ToolFlag(val enabled: Boolean = true, val isNew: Boolean = false)

/**
 * What the dashboard draws.
 *
 * [recent] is the conversation the "continue" card resumes, and it is simply the first of
 * [activity] — Home shows the same list twice at different weights, so deriving one from the other
 * keeps them from ever disagreeing.
 */
data class HomeUiState(
    val profile: StudentProfile = StudentProfile(),
    val activity: List<HistoryEntry> = emptyList(),
    /** The daily question allowance, for the pill under the greeting. Null until it is known. */
    val quota: QuotaDto? = null,
    val flags: Map<StudyTool, ToolFlag> = emptyMap(),
    val signedIn: Boolean = false,
    /** The first reading of the dashboard is on its way and nothing is held yet. */
    val loading: Boolean = false,
    /** The dashboard is being read again with rows already shown: pull to refresh, or coming back to the tab. */
    val refreshing: Boolean = false,
    /** Why the dashboard could not be read, when it could not. What is shown is then from an earlier reading, or nothing. */
    val error: ApiErrorCode? = null,
) {
    val recent: HistoryEntry? get() = activity.firstOrNull()
}

/** What the server says about [tool], or what the app ships with while it has not said anything yet. */
fun Map<StudyTool, ToolFlag>.flagFor(tool: StudyTool): ToolFlag = this[tool] ?: ToolFlag(isNew = tool.isNew)

class HomeViewModel(
    private val chats: ChatRepository,
    private val account: AccountRepository,
    session: SessionStore,
    preferences: UserPreferencesRepository,
    private val auth: AuthRepository,
    now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        preferences.settings,
        session.dashboard,
        session.quota,
        session.toolFlags,
        auth.user,
    ) { settings, dashboard, quota, flags, user ->
        HomeUiState(
            profile = settings.profile,
            activity = dashboard.recent.take(ACTIVITY_LIMIT).map { it.toEntry(now()) },
            quota = quota,
            flags = flags.toToolFlags(),
            signedIn = user != null,
            loading = user != null && dashboard.loadedAt == 0L && dashboard.error == null,
            refreshing = user != null && dashboard.loading && dashboard.loadedAt != 0L,
            error = dashboard.error.takeIf { user != null },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    init {
        // Firebase restores the signed-in person a moment after the app starts, so a look at launch can
        // find nobody and then, a moment later, somebody. Asking when somebody appears is what makes the
        // first screen fill in; coming back to the front (onResume) is the other time it asks.
        viewModelScope.launch {
            auth.user.map { it != null }.distinctUntilChanged().collect { signedIn ->
                if (signedIn) load(force = false)
            }
        }
    }

    /** Home came back to the front. Signed out there is nothing to ask the server. */
    fun onResume() {
        if (auth.currentUser != null) load(force = false)
    }

    private fun load(force: Boolean) {
        viewModelScope.launch { account.refreshMe(force) }
        viewModelScope.launch { chats.loadDashboard(force) }
    }

    /** Pull to refresh: always asks again. */
    fun refresh() {
        if (auth.currentUser != null) load(force = true)
    }

    private companion object {
        /** Home is a summary, not the history screen — it lists a handful and links to the rest. */
        const val ACTIVITY_LIMIT = 3
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun Map<String, ToolFlagDto>.toToolFlags(): Map<StudyTool, ToolFlag> =
    entries.mapNotNull { (name, flag) -> name.toStudyTool()?.let { it to ToolFlag(flag.enabled, flag.isNew) } }.toMap()
