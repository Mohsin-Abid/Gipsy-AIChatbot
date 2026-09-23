package com.aitutor.chatbot.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.data.firebase.FirebaseAuthGateway
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.ExplanationLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.ThemeMode
import com.aitutor.chatbot.app.ui.theme.FontScale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val authGateway: FirebaseAuthGateway,
) : ViewModel() {

    private val displayPreferences = combine(
        preferencesRepository.language,
        preferencesRepository.themeMode,
        preferencesRepository.explanationLevel,
    ) { language, themeMode, explanationLevel ->
        Triple(language ?: AppLanguage.English, themeMode, explanationLevel)
    }

    val uiState: StateFlow<UiState<SettingsData>> = combine(
        displayPreferences,
        preferencesRepository.fontScale,
        preferencesRepository.notificationsEnabled,
        preferencesRepository.premiumCached,
        preferencesRepository.studentProfile,
    ) { (language, themeMode, explanationLevel), fontScale, notifications, isPremium, profile ->
        SettingsData(
            language = language,
            themeMode = themeMode,
            explanationLevel = explanationLevel,
            fontScale = fontScale,
            notificationsEnabled = notifications,
            isPremium = isPremium,
            profile = profile,
        )
    }.map<SettingsData, UiState<SettingsData>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onProfileUpdated(profile: StudentProfile) {
        viewModelScope.launch { preferencesRepository.setStudentProfile(profile) }
    }

    private val _accountCleared = MutableSharedFlow<Unit>()
    val accountCleared: SharedFlow<Unit> = _accountCleared

    fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun onExplanationLevelSelected(level: ExplanationLevel) {
        viewModelScope.launch { preferencesRepository.setExplanationLevel(level) }
    }

    fun onNotificationsChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun onFontScaleIncrease() = adjustFontScale(FontScale.STEP)
    fun onFontScaleDecrease() = adjustFontScale(-FontScale.STEP)
    fun onFontScaleReset() {
        viewModelScope.launch { preferencesRepository.setFontScale(FontScale.DEFAULT) }
    }

    private fun adjustFontScale(delta: Float) {
        viewModelScope.launch {
            val current = uiState.value.let { (it as? UiState.Success)?.data?.fontScale } ?: FontScale.DEFAULT
            preferencesRepository.setFontScale(current + delta)
        }
    }

    /** Resets to a fresh anonymous session — local prefs stay (this is "log out" in an app with no accounts). */
    fun onLogOutConfirmed() {
        viewModelScope.launch {
            authGateway.signOut()
            authGateway.ensureSignedIn()
            _accountCleared.emit(Unit)
        }
    }

    /** Deletes the Firebase anonymous user and all local preferences, then starts a brand new device identity. */
    fun onDeleteAccountConfirmed() {
        viewModelScope.launch {
            preferencesRepository.clearAll()
            authGateway.deleteAccount()
            authGateway.ensureSignedIn()
            _accountCleared.emit(Unit)
        }
    }
}
