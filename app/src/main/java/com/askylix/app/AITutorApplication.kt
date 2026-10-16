package com.askylix.app

import android.app.Application
import com.askylix.app.di.AppContainer
import com.askylix.app.di.DefaultAppContainer

/** The app's dependencies, from any context. For the few places that have a context but no ViewModel. */
fun android.content.Context.appContainer(): AppContainer =
    (applicationContext as AITutorApplication).container

class AITutorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
