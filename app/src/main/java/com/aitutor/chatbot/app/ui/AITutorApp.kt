package com.aitutor.chatbot.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aitutor.chatbot.app.model.ToolCategory
import com.aitutor.chatbot.app.model.studyTools
import com.aitutor.chatbot.app.navigation.AppDestination
import com.aitutor.chatbot.app.ui.screens.ChatHistoryScreen
import com.aitutor.chatbot.app.ui.screens.HomeScreen
import com.aitutor.chatbot.app.ui.screens.SettingsScreen
import com.aitutor.chatbot.app.ui.screens.ToolsScreen
import com.aitutor.chatbot.app.ui.state.ChatHistoryUiState
import com.aitutor.chatbot.app.ui.state.HomeUiState
import com.aitutor.chatbot.app.ui.state.SampleData
import com.aitutor.chatbot.app.ui.state.SettingsPreferences
import com.aitutor.chatbot.app.ui.state.SettingsUiState
import com.aitutor.chatbot.app.ui.state.ToolsUiState
import com.aitutor.chatbot.app.ui.state.tabLabel
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.ThemePreviews

/**
 * Owns the app's theme state (so Settings' Dark Mode switch actually works)
 * and, in place of a real ViewModel layer, the small bits of UI state each
 * screen needs — the history tab, the tools search query, the two
 * preference toggles. Screens themselves stay pure functions of UiState.
 */
@Composable
fun AITutorApp() {
    val systemDark = isSystemInDarkTheme()
    var darkMode by rememberSaveable { mutableStateOf(systemDark) }

    AITutorTheme(darkTheme = darkMode) {
        AITutorAppContent(
            darkMode = darkMode,
            onDarkModeChange = { darkMode = it }
        )
    }
}

@Composable
private fun AITutorAppContent(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var selectedTab by rememberSaveable { mutableStateOf<ToolCategory?>(null) }
    var toolsQuery by rememberSaveable { mutableStateOf("") }
    var notifications by rememberSaveable { mutableStateOf(true) }

    val homeState = remember {
        HomeUiState.Success(
            greeting = SampleData.Greeting,
            quickTools = SampleData.quickTools,
            recentHistory = SampleData.recentHistory
        )
    }

    val chatHistoryState = remember(selectedTab) {
        val filtered = if (selectedTab == null) {
            SampleData.history
        } else {
            SampleData.history.filter { it.category == selectedTab }
        }
        if (filtered.isEmpty()) {
            ChatHistoryUiState.Empty(
                title = "No ${selectedTab.tabLabel()} chats yet",
                message = "Try a different tab, or ask a question with that tool."
            )
        } else {
            ChatHistoryUiState.Success(filtered)
        }
    }

    val toolsState = remember { ToolsUiState.Success(studyTools) }

    val settingsState = remember(darkMode, notifications) {
        SettingsUiState.Success(
            profile = SampleData.profile,
            preferences = SettingsPreferences(
                darkMode = darkMode,
                notifications = notifications,
                language = SampleData.preferences.language
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                AppDestination.entries.forEach { destination ->
                    val selected = destination == selectedDestination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.filledIcon else destination.outlinedIcon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "tabCrossfade"
        ) { destination ->
            when (destination) {
                AppDestination.Home -> HomeScreen(
                    state = homeState,
                    onOpenSettings = { selectedDestination = AppDestination.Settings },
                    onToolClick = { selectedDestination = AppDestination.History },
                    onHistoryItemClick = { selectedDestination = AppDestination.History },
                    onSeeAllHistory = { selectedDestination = AppDestination.History },
                    onSendMessage = { selectedDestination = AppDestination.History },
                    onRetry = {}
                )

                AppDestination.History -> ChatHistoryScreen(
                    state = chatHistoryState,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onEntryClick = {},
                    onSearchClick = {},
                    onRetry = {}
                )

                AppDestination.Tools -> ToolsScreen(
                    state = toolsState,
                    query = toolsQuery,
                    onQueryChange = { toolsQuery = it },
                    onToolClick = { selectedDestination = AppDestination.History }
                )

                AppDestination.Settings -> SettingsScreen(
                    state = settingsState,
                    onProfileClick = {},
                    onGradeSubjectsClick = {},
                    onParentalControlsClick = {},
                    onDarkModeChange = onDarkModeChange,
                    onNotificationsChange = { notifications = it },
                    onLanguageClick = {},
                    onHelpClick = {},
                    onAboutClick = {},
                    onSignOut = {}
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AITutorAppPreview() {
    AITutorTheme {
        AITutorAppContent(darkMode = false, onDarkModeChange = {})
    }
}
