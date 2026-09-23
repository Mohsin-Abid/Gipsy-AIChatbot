package com.aitutor.chatbot.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.domain.model.ThemeMode
import com.aitutor.chatbot.app.navigation.AppNavHost
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.FontScale
import com.aitutor.chatbot.app.ui.theme.Typography
import com.aitutor.chatbot.app.ui.theme.scaled
import org.koin.compose.koinInject

/**
 * App root: neutralizes the device's accessibility font scale, applies the user's in-app font
 * scale + theme override, and hosts navigation. This is the only place either of those is decided.
 */
@Composable
fun AiTutorRoot() {
    val preferencesRepository = koinInject<UserPreferencesRepository>()
    val themeMode by preferencesRepository.themeMode.collectAsLifecycleAwareState(initial = ThemeMode.System)
    val fontScale by preferencesRepository.fontScale.collectAsLifecycleAwareState(initial = FontScale.DEFAULT)

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = 1f)) {
        AITutorTheme(darkTheme = darkTheme, typography = Typography.scaled(fontScale)) {
            // The window background comes from the XML theme, which can't follow the in-app theme
            // override — so every destination sits on a themed Surface instead. Without this, any
            // screen that doesn't paint its own background shows white in dark mode.
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AppNavHost()
            }
        }
    }
}
