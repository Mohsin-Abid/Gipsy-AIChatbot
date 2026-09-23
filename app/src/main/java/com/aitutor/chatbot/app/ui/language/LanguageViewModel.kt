package com.aitutor.chatbot.app.ui.language

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.core.ext.applyAppLocale
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.domain.model.AppLanguage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val appContext: Context,
) : ViewModel() {

    val selectedLanguage: StateFlow<AppLanguage?> = preferencesRepository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onLanguageSelected(language: AppLanguage, onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            appContext.applyAppLocale(language.languageTag)
            onDone()
        }
    }
}
