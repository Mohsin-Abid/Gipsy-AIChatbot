package com.aitutor.chatbot.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.data.prefs.UserPreferencesRepository
import com.aitutor.chatbot.app.data.prefs.UserSettings
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.Grade
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.StudyLevel
import com.aitutor.chatbot.app.domain.model.StudyTime
import com.aitutor.chatbot.app.domain.model.Subject
import com.aitutor.chatbot.app.domain.model.ThemeMode
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
 * Setup is the exception: it holds its answers in memory and commits once, via [completeSetup], so
 * abandoning it halfway leaves nothing behind.
 */
class SettingsViewModel(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    val settings: StateFlow<UserSettings?> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        // Null means "not read yet", which is what the splash screen waits on. A default here
        // would send a first-time user straight past onboarding for a frame.
        initialValue = null,
    )

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        preferences.setLanguage(language)
    }

    /** Writes the profile gathered during setup and marks first run done, in that order. */
    fun completeSetup(profile: StudentProfile) = viewModelScope.launch {
        preferences.setProfile(profile)
        preferences.setFirstRunComplete()
    }

    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setRemindersEnabled(enabled)
    }

    fun setVoiceInputEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setVoiceInputEnabled(enabled)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        preferences.setThemeMode(mode)
    }

    // ---- Profile edits made outside setup ----

    fun setLevel(level: StudyLevel) = updateProfile { it.copy(level = level) }

    fun setGrade(grade: Grade) = updateProfile { it.copy(grade = grade) }

    fun toggleSubject(subject: Subject) = updateProfile { profile ->
        profile.copy(
            subjects = if (subject in profile.subjects) {
                profile.subjects - subject
            } else {
                profile.subjects + subject
            }
        )
    }

    fun toggleGoal(goal: StudyGoal) = updateProfile { profile ->
        profile.copy(
            goals = when {
                goal in profile.goals -> profile.goals - goal
                profile.goals.size < StudentProfile.MAX_GOALS -> profile.goals + goal
                // At the cap, extra picks are ignored rather than silently replacing a choice.
                else -> profile.goals
            }
        )
    }

    fun setDailyTime(time: StudyTime) = updateProfile { it.copy(dailyTime = time) }

    private fun updateProfile(transform: (StudentProfile) -> StudentProfile) {
        val current = settings.value?.profile ?: return
        viewModelScope.launch { preferences.setProfile(transform(current)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
