package com.askylix.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.askylix.app.R
import com.askylix.app.data.auth.AuthRepository
import com.askylix.app.data.prefs.UserPreferencesRepository
import com.askylix.app.data.prefs.UserSettings
import com.askylix.app.data.repository.AccountRepository
import com.askylix.app.data.repository.profilePatch
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.ThemeMode
import com.askylix.app.ui.AppMessages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the app remembers about its user: the language, the study profile built during setup,
 * and the switches on Profile.
 *
 * It is one ViewModel rather than one per screen because these screens edit the same record — setup
 * writes the profile, Profile reads it back and edits parts of it, and the first-run gate depends on
 * whether setup finished. Splitting them would mean three ViewModels racing on one DataStore file.
 *
 * Someone signed in has their profile and language on the server as well, so a change to either is
 * made on the phone at once and sent to the account; if the server refuses it, the phone goes back
 * to what it was and says so. Reminders, theme, voice input and text size belong to the device and
 * are never sent.
 *
 * Setup holds its answers in memory and commits once, via [completeSetup], so abandoning it halfway
 * leaves nothing behind.
 */
class SettingsViewModel(
    private val preferences: UserPreferencesRepository,
    private val account: AccountRepository,
    private val auth: AuthRepository,
    private val messages: AppMessages,
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        // Null means "not read yet", which is what the splash screen waits on. A default here
        // would send a first-time user straight past onboarding for a frame.
        initialValue = null,
    )

    private val signedIn: Boolean get() = auth.currentUser != null

    fun setLanguage(language: AppLanguage) {
        val previous = settings.value?.language
        viewModelScope.launch {
            preferences.setLanguage(language)
            if (!signedIn || previous == language) return@launch
            account.updateLanguage(language).onFailure {
                previous?.let { preferences.setLanguage(it) }
                messages.post(R.string.settings_error_language)
            }
        }
    }

    /**
     * Finishes setup. Signed in, the answers go to the account first and nothing is kept on the phone
     * unless it accepts them. Skipped, they are saved on the phone, to be sent if the person signs in
     * later. Returns false, after saying why, if it could not be done.
     */
    suspend fun completeSetup(profile: StudentProfile): Boolean {
        if (!signedIn) {
            preferences.setProfile(profile)
            preferences.setFirstRunComplete()
            return true
        }
        val language = settings.value?.language ?: AppLanguage.English
        return account.finishSetup(profile, language).fold(
            onSuccess = { true },
            onFailure = {
                messages.post(R.string.setup_error_save)
                false
            },
        )
    }

    /** "Skip for now": the app runs as a browse mode, with no account. */
    fun setSkipped(skipped: Boolean) = viewModelScope.launch { preferences.setSkippedSignIn(skipped) }

    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setRemindersEnabled(enabled)
    }

    fun setVoiceInputEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setVoiceInputEnabled(enabled)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        preferences.setThemeMode(mode)
    }

    /**
     * Applies [transform] to the *stored* profile and writes the result.
     *
     * This is how the edit screens on Profile save. Each one changes only the fields it owns, and
     * changing them against what is stored right now — rather than against a copy taken when the
     * screen opened — means saving the name cannot undo a reminder toggled in the meantime.
     */
    fun updateProfile(transform: (StudentProfile) -> StudentProfile) {
        val current = settings.value?.profile ?: return
        val updated = transform(current)
        viewModelScope.launch {
            preferences.setProfile(updated)
            val changes = profilePatch(current, updated)
            if (!signedIn || changes == null) return@launch
            account.updateProfile(changes).onFailure {
                preferences.setProfile(current)
                messages.post(R.string.settings_error_profile)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
