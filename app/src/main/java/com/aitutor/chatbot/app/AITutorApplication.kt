package com.aitutor.chatbot.app

import android.app.Application
import com.aitutor.chatbot.app.di.AppContainer
import com.aitutor.chatbot.app.di.DefaultAppContainer

class AITutorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
