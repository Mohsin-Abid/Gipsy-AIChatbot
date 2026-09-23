package com.aitutor.chatbot.app.di

import androidx.room.Room
import com.aitutor.chatbot.app.data.local.db.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "ai_tutor.db").build()
    }
    single { get<AppDatabase>().chatDao() }
    single { get<AppDatabase>().messageDao() }
}
