package com.aitutor.chatbot.app.di

import com.aitutor.chatbot.app.data.ai.NotConfiguredAiClient
import com.aitutor.chatbot.app.data.ai.TutorAiClient
import com.aitutor.chatbot.app.data.chat.ChatRepository
import com.aitutor.chatbot.app.data.firebase.ReportRepository
import com.aitutor.chatbot.app.data.firebase.UserProfileRepository
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.data.ocr.ImageTextExtractor
import com.aitutor.chatbot.app.data.tts.TtsManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { UserPreferencesRepository(androidContext()) }
    // Swap this binding for the custom API client once it exists; nothing else has to change.
    single<TutorAiClient> { NotConfiguredAiClient() }
    single { ChatRepository(get(), get(), get(), get()) }
    single { ReportRepository() }
    single { UserProfileRepository(get()) }
    single { TtsManager(androidContext()) }
    single { ImageTextExtractor(androidContext()) }
}
