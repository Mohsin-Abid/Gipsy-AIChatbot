package com.aitutor.chatbot.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.Grade
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.StudyLevel
import com.aitutor.chatbot.app.domain.model.StudyTime
import com.aitutor.chatbot.app.domain.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** What the app remembers between launches, other than the conversations themselves. */
data class UserSettings(
    val firstRunComplete: Boolean = false,
    val language: AppLanguage = AppLanguage.English,
    val profile: StudentProfile = StudentProfile(),
    val remindersEnabled: Boolean = true,
    val voiceInputEnabled: Boolean = true,
    /** `null` follows the system; the Profile switch sets it either way once touched. */
    val darkMode: Boolean? = null,
)

/**
 * Settings, on DataStore.
 *
 * Every enum is stored by **name**, never by ordinal: a saved profile has to survive someone adding
 * a subject to the middle of the list, and an ordinal would quietly re-point at its neighbour.
 * Anything unreadable falls back to the default rather than failing the whole read, so one bad
 * value cannot lock a user out of their own settings.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val firstRunComplete = booleanPreferencesKey("first_run_complete")
        val languageTag = stringPreferencesKey("language_tag")
        val name = stringPreferencesKey("profile_name")
        val level = stringPreferencesKey("profile_level")
        val grade = stringPreferencesKey("profile_grade")
        val subjects = stringPreferencesKey("profile_subjects")
        val goals = stringPreferencesKey("profile_goals")
        val dailyTime = stringPreferencesKey("profile_daily_time")
        val reminders = booleanPreferencesKey("reminders_enabled")
        val voiceInput = booleanPreferencesKey("voice_input_enabled")
        val darkMode = intPreferencesKey("dark_mode")
    }

    /** Dark mode is three-state, and DataStore has no nullable boolean. */
    private object DarkModeValue {
        const val System = 0
        const val Light = 1
        const val Dark = 2
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            firstRunComplete = prefs[Keys.firstRunComplete] ?: false,
            language = AppLanguage.entries.firstOrNull { it.tag == prefs[Keys.languageTag] }
                ?: AppLanguage.English,
            profile = StudentProfile(
                name = prefs[Keys.name] ?: "",
                level = StudyLevel.entries.firstOrNull { it.name == prefs[Keys.level] },
                grade = Grade.entries.firstOrNull { it.name == prefs[Keys.grade] },
                subjects = prefs[Keys.subjects].toEnumSet { name ->
                    Subject.entries.firstOrNull { it.name == name }
                },
                goals = prefs[Keys.goals].toEnumSet { name ->
                    StudyGoal.entries.firstOrNull { it.name == name }
                },
                dailyTime = StudyTime.entries.firstOrNull { it.name == prefs[Keys.dailyTime] }
                    ?: StudyTime.Hour1,
                reminderEnabled = prefs[Keys.reminders] ?: true,
            ),
            remindersEnabled = prefs[Keys.reminders] ?: true,
            voiceInputEnabled = prefs[Keys.voiceInput] ?: true,
            darkMode = when (prefs[Keys.darkMode]) {
                DarkModeValue.Light -> false
                DarkModeValue.Dark -> true
                else -> null
            },
        )
    }

    suspend fun setLanguage(language: AppLanguage) = edit { it[Keys.languageTag] = language.tag }

    suspend fun setFirstRunComplete() = edit { it[Keys.firstRunComplete] = true }

    suspend fun setProfile(profile: StudentProfile) = edit { prefs ->
        prefs[Keys.name] = profile.name
        profile.level?.let { prefs[Keys.level] = it.name } ?: prefs.remove(Keys.level)
        profile.grade?.let { prefs[Keys.grade] = it.name } ?: prefs.remove(Keys.grade)
        prefs[Keys.subjects] = profile.subjects.joinToString(SEPARATOR) { it.name }
        prefs[Keys.goals] = profile.goals.joinToString(SEPARATOR) { it.name }
        prefs[Keys.dailyTime] = profile.dailyTime.name
        prefs[Keys.reminders] = profile.reminderEnabled
    }

    suspend fun setRemindersEnabled(enabled: Boolean) = edit { it[Keys.reminders] = enabled }

    suspend fun setVoiceInputEnabled(enabled: Boolean) = edit { it[Keys.voiceInput] = enabled }

    suspend fun setDarkMode(enabled: Boolean) = edit {
        it[Keys.darkMode] = if (enabled) DarkModeValue.Dark else DarkModeValue.Light
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        const val SEPARATOR = ","
    }
}

/** Reads a stored name list back, dropping any name this build no longer recognises. */
private fun <T> String?.toEnumSet(parse: (String) -> T?): Set<T> =
    this?.split(",").orEmpty().mapNotNull { name -> name.takeIf { it.isNotBlank() }?.let(parse) }
        .toSet()
