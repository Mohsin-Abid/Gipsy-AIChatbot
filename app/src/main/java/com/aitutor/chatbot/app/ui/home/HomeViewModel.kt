package com.aitutor.chatbot.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.data.prefs.UserPreferencesRepository
import com.aitutor.chatbot.app.data.repository.ChatRepository
import com.aitutor.chatbot.app.domain.model.HistoryEntry
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.ui.history.toEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
) {
    val recent: HistoryEntry? get() = activity.firstOrNull()
}

class HomeViewModel(
    chats: ChatRepository,
    preferences: UserPreferencesRepository,
    now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(chats.observeHeaders(), preferences.settings) { headers, settings ->
            HomeUiState(
                profile = settings.profile,
                activity = headers.take(ACTIVITY_LIMIT).map { it.toEntry(now()) },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    private companion object {
        /** Home is a summary, not the history screen — it lists a handful and links to the rest. */
        const val ACTIVITY_LIMIT = 3
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
