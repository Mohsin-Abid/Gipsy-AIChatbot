package com.aitutor.chatbot.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.ChatTextSize
import com.aitutor.chatbot.app.domain.model.Grade
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.StudyLevel
import com.aitutor.chatbot.app.domain.model.StudyTime
import com.aitutor.chatbot.app.domain.model.Subject
import com.aitutor.chatbot.app.domain.model.ThemeMode
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
    val themeMode: ThemeMode = ThemeMode.Default,
    val chatTextSize: ChatTextSize = ChatTextSize.Default,
    /**
     * Whether the user has Pro. **Read only on the device**: the client may show it but never grants
     * it. The only writer will be purchase verification, once the server confirms a Play token.
     */
    val isPro: Boolean = false,
    /**
     * The account `uid` whose setup is saved here, or null if no account owns it yet. See
     * [com.aitutor.chatbot.app.navigation.afterSignIn] for why the phone alone is not enough to tell.
     */
    val setupOwnerUid: String? = null,
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
        val themeMode = stringPreferencesKey("theme_mode")
        val chatTextSize = stringPreferencesKey("chat_text_size")
        val isPro = booleanPreferencesKey("is_pro")
        val setupOwnerUid = stringPreferencesKey("setup_owner_uid")
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
            themeMode = ThemeMode.fromNameOrDefault(prefs[Keys.themeMode]),
            chatTextSize = ChatTextSize.fromNameOrDefault(prefs[Keys.chatTextSize]),
            isPro = prefs[Keys.isPro] ?: false,
            setupOwnerUid = prefs[Keys.setupOwnerUid],
        )
    }

    suspend fun setLanguage(language: AppLanguage) = edit { it[Keys.languageTag] = language.tag }

    suspend fun setFirstRunComplete() = edit { it[Keys.firstRunComplete] = true }

    /** Records whose setup this is. Null leaves it unowned, for the next account to claim. */
    suspend fun setSetupOwner(uid: String?) = edit { prefs ->
        if (uid == null) prefs.remove(Keys.setupOwnerUid) else prefs[Keys.setupOwnerUid] = uid
    }

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

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.name }

    suspend fun setChatTextSize(size: ChatTextSize) = edit { it[Keys.chatTextSize] = size.name }

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
