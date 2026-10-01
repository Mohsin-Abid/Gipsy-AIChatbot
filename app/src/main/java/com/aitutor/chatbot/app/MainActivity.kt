package com.aitutor.chatbot.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitutor.chatbot.app.di.AppViewModelFactory
import com.aitutor.chatbot.app.domain.model.ThemeMode
import com.aitutor.chatbot.app.navigation.AppNavHost
import com.aitutor.chatbot.app.ui.settings.SettingsViewModel
import com.aitutor.chatbot.app.ui.theme.AITutorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // With three-button navigation the system paints a pale scrim behind the buttons unless
        // asked not to. Every screen already keeps its content clear of that strip, so the scrim
        // adds nothing but a mismatched band across the bottom — on the accent splash and heroes it
        // reads as a rendering fault. Turning it off lets each screen's own background run through.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            // The stored choice has to be read out here, above the theme, because the theme is what
            // it decides. The same ViewModel instance serves the nav host below — both resolve
            // against this Activity's store — so the setting has exactly one owner.
            val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory.settings)
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            // Follow the device until the stored choice arrives: guessing a palette and correcting
            // it a frame later would flash the whole app.
            val mode = settings?.themeMode ?: ThemeMode.Default

            AITutorTheme(darkTheme = mode.isDark(systemInDark = isSystemInDarkTheme())) {
                AppNavHost()
            }
        }
    }
}
