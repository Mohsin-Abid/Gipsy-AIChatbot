package com.aitutor.chatbot.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.ExplanationLevel
import com.aitutor.chatbot.app.domain.model.FORCE_PREMIUM_UNLOCK
import com.aitutor.chatbot.app.domain.model.GradeLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.Subject
import com.aitutor.chatbot.app.domain.model.ThemeMode
import com.aitutor.chatbot.app.ui.theme.FontScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * DataStore-backed app preferences: onboarding progress, language, theme override, font scale,
 * default explanation level, notifications. Single source of truth for Settings and the app root.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val EXPLANATION_LEVEL = stringPreferencesKey("explanation_level")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PREMIUM_CACHED = booleanPreferencesKey("premium_cached")
        val USAGE_DAY = stringPreferencesKey("usage_day")
        val USAGE_COUNT = intPreferencesKey("usage_count")
        val PROFILE_GRADE = stringPreferencesKey("profile_grade")
        val PROFILE_SUBJECTS = stringSetPreferencesKey("profile_subjects")
        val PROFILE_GOAL = stringPreferencesKey("profile_goal")
    }

    private val today: String get() = LocalDate.now().toString()

    val onboardingComplete: Flow<Boolean> =
        context.userPreferencesDataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val languageTag: Flow<String?> =
        context.userPreferencesDataStore.data.map { it[Keys.LANGUAGE_TAG] }

    val language: Flow<AppLanguage?> =
        languageTag.map { tag -> tag?.let(AppLanguage::fromTag) }

    val themeMode: Flow<ThemeMode> =
        context.userPreferencesDataStore.data.map { ThemeMode.fromName(it[Keys.THEME_MODE]) }

    val fontScale: Flow<Float> =
        context.userPreferencesDataStore.data.map { it[Keys.FONT_SCALE] ?: FontScale.DEFAULT }

    val explanationLevel: Flow<ExplanationLevel> =
        context.userPreferencesDataStore.data.map { ExplanationLevel.fromName(it[Keys.EXPLANATION_LEVEL]) }

    val notificationsEnabled: Flow<Boolean> =
        context.userPreferencesDataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    /**
     * Last known premium entitlement, cached so the UI is correct offline and on cold start.
     * Every premium consumer reads through here — the dashboard, Settings and the chat gates — so
     * [FORCE_PREMIUM_UNLOCK] is applied at this single point while premium is being tested.
     */
    val premiumCached: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        FORCE_PREMIUM_UNLOCK || (prefs[Keys.PREMIUM_CACHED] ?: false)
    }

    /** Free-tier questions asked today; resets automatically when the calendar day rolls over. */
    val questionsUsedToday: Flow<Int> = context.userPreferencesDataStore.data.map { prefs ->
        if (prefs[Keys.USAGE_DAY] == today) prefs[Keys.USAGE_COUNT] ?: 0 else 0
    }

    /** Collected in onboarding; feeds both the dashboard chip and every chat's system instruction. */
    val studentProfile: Flow<StudentProfile> = context.userPreferencesDataStore.data.map { prefs ->
        StudentProfile(
            grade = GradeLevel.fromName(prefs[Keys.PROFILE_GRADE]),
            subjects = Subject.fromNames(prefs[Keys.PROFILE_SUBJECTS].orEmpty()),
            goal = StudyGoal.fromName(prefs[Keys.PROFILE_GOAL]),
        )
    }

    suspend fun setStudentProfile(profile: StudentProfile) {
        context.userPreferencesDataStore.edit { prefs ->
            profile.grade?.let { prefs[Keys.PROFILE_GRADE] = it.name } ?: prefs.remove(Keys.PROFILE_GRADE)
            prefs[Keys.PROFILE_SUBJECTS] = profile.subjects.map { it.name }.toSet()
            profile.goal?.let { prefs[Keys.PROFILE_GOAL] = it.name } ?: prefs.remove(Keys.PROFILE_GOAL)
        }
    }

    suspend fun setPremiumCached(isPremium: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.PREMIUM_CACHED] = isPremium }
    }

    suspend fun incrementQuestionsUsedToday() {
        context.userPreferencesDataStore.edit { prefs ->
            val sameDay = prefs[Keys.USAGE_DAY] == today
            prefs[Keys.USAGE_DAY] = today
            prefs[Keys.USAGE_COUNT] = if (sameDay) (prefs[Keys.USAGE_COUNT] ?: 0) + 1 else 1
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.userPreferencesDataStore.edit { it[Keys.LANGUAGE_TAG] = language.languageTag }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(FontScale.MIN, FontScale.MAX)
        context.userPreferencesDataStore.edit { it[Keys.FONT_SCALE] = clamped }
    }

    suspend fun setExplanationLevel(level: ExplanationLevel) {
        context.userPreferencesDataStore.edit { it[Keys.EXPLANATION_LEVEL] = level.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    /** Local reset on Log Out / Delete Account — the Firestore users/{uid} tree itself is Phase 5 (Cloud Function). */
    suspend fun clearAll() {
        context.userPreferencesDataStore.edit { it.clear() }
    }
}
