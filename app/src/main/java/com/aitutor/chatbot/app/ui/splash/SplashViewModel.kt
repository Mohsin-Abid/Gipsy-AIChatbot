package com.aitutor.chatbot.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.data.firebase.FirebaseAuthGateway
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashDestination {
    data object Onboarding : SplashDestination
    data object LanguageSelect : SplashDestination
    data object Main : SplashDestination
}

/**
 * Blocks navigation past Splash until anonymous auth resolves and the onboarding-complete flag
 * is read from DataStore, then decides where the app lands.
 */
class SplashViewModel(
    private val authGateway: FirebaseAuthGateway,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val signInResult = authGateway.ensureSignedIn()
            val onboardingDone = preferencesRepository.onboardingComplete.first()
            val languageChosen = preferencesRepository.languageTag.first() != null

            _destination.value = when {
                signInResult.isFailure -> SplashDestination.Onboarding
                !onboardingDone -> SplashDestination.Onboarding
                !languageChosen -> SplashDestination.LanguageSelect
                else -> SplashDestination.Main
            }
        }
    }
}
