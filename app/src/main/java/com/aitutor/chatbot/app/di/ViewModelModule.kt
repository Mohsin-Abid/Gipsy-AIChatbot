package com.aitutor.chatbot.app.di

import com.aitutor.chatbot.app.ui.chat.ChatDetailViewModel
import com.aitutor.chatbot.app.ui.history.ChatHistoryViewModel
import com.aitutor.chatbot.app.ui.home.HomeViewModel
import com.aitutor.chatbot.app.ui.language.LanguageViewModel
import com.aitutor.chatbot.app.ui.onboarding.OnboardingViewModel
import com.aitutor.chatbot.app.ui.settings.SettingsViewModel
import com.aitutor.chatbot.app.ui.splash.SplashViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SplashViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { LanguageViewModel(get(), androidContext()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { ChatHistoryViewModel(get()) }
    viewModel { params -> ChatDetailViewModel(params.get(), get(), get(), get(), get(), get()) }
}
