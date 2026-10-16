package com.askylix.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.ChatTextSize
import com.askylix.app.domain.model.Grade
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyGoal
import com.askylix.app.domain.model.StudyLevel
import com.askylix.app.domain.model.StudyTime
import com.askylix.app.domain.model.Subject
import com.askylix.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    /** "Skip for now" was chosen: the app is in browse mode, with no account. */
    val skippedSignIn: Boolean = false,
)

/**
 * Settings, on DataStore.
 *
 * Every enum is stored by **name**, never by ordinal: a saved profile has to survive someone adding
 * a subject to the middle of the list, and an ordinal would quietly re-point at its neighbour.
 * Anything unreadable falls back to the default rather than failing the whole read, so one bad
 * value cannot lock a user out of their own settings.
 */
class UserPreferencesRepository(private val context: Context) : AccountPreferences {

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
        val skippedSignIn = booleanPreferencesKey("skipped_sign_in")
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
            skippedSignIn = prefs[Keys.skippedSignIn] ?: false,
        )
    }

    suspend fun setLanguage(language: AppLanguage) = edit { it[Keys.languageTag] = language.tag }

    suspend fun setFirstRunComplete() = edit { it[Keys.firstRunComplete] = true }

    suspend fun setSkippedSignIn(skipped: Boolean) = edit { it[Keys.skippedSignIn] = skipped }

    override suspend fun isFirstRunComplete(): Boolean = settings.first().firstRunComplete

    override suspend fun setIsPro(pro: Boolean) = edit { it[Keys.isPro] = pro }

    /**
     * Puts the signed-in account's profile on this phone.
     *
     * The profile, the language and the plan are the account's, so they follow it. Reminders belong to
     * the device: they are only copied the first time an account is used on a phone ([seedReminders]),
     * so a later refresh never switches a reminder someone has changed here.
     */
    override suspend fun applyAccount(
        profile: StudentProfile,
        language: AppLanguage?,
        isPro: Boolean,
        seedReminders: Boolean,
    ) = edit { prefs ->
        writeProfile(prefs, profile, includeReminders = seedReminders)
        language?.let { prefs[Keys.languageTag] = it.tag }
        prefs[Keys.isPro] = isPro
        prefs[Keys.firstRunComplete] = true
        prefs[Keys.skippedSignIn] = false
    }

    /**
     * Log-out, or a deleted account: the profile and the plan leave the phone. What belongs to the
     * device stays — language, reminders, theme, voice input and text size — and so does the fact that
     * this phone has been through first run, so the next launch asks for sign-in rather than starting over.
     */
    override suspend fun clearAccountData() = edit { prefs ->
        listOf(
            Keys.name,
            Keys.level,
            Keys.grade,
            Keys.subjects,
            Keys.goals,
            Keys.dailyTime
        ).forEach(prefs::remove)
        prefs[Keys.isPro] = false
        prefs[Keys.skippedSignIn] = false
    }

    suspend fun setProfile(profile: StudentProfile) = edit { prefs ->
        writeProfile(prefs, profile, includeReminders = true)
    }

    private fun writeProfile(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
        profile: StudentProfile,
        includeReminders: Boolean,
    ) {
        prefs[Keys.name] = profile.name
        profile.level?.let { prefs[Keys.level] = it.name } ?: prefs.remove(Keys.level)
        profile.grade?.let { prefs[Keys.grade] = it.name } ?: prefs.remove(Keys.grade)
        prefs[Keys.subjects] = profile.subjects.joinToString(SEPARATOR) { it.name }
        prefs[Keys.goals] = profile.goals.joinToString(SEPARATOR) { it.name }
        prefs[Keys.dailyTime] = profile.dailyTime.name
        if (includeReminders) prefs[Keys.reminders] = profile.reminderEnabled
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
