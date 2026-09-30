package com.aitutor.chatbot.app.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.di.AppViewModelFactory
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.HistoryEntry
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.ui.history.HistoryScreen
import com.aitutor.chatbot.app.ui.history.HistoryViewModel
import com.aitutor.chatbot.app.ui.home.HomeScreen
import com.aitutor.chatbot.app.ui.home.HomeViewModel
import com.aitutor.chatbot.app.ui.profile.ProfileScreen
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.appColors
import com.aitutor.chatbot.app.ui.tools.ToolsScreen

/**
 * Holds the four primary tabs and the floating nav above them. The bar overlays the content rather
 * than taking layout space, which is why each tab pads its own bottom clear of it by
 * [TabBottomInset].
 *
 * Each tab owns its own ViewModel rather than being handed one state object: Home and History read
 * the same conversations but at different depths, and keeping them separate means switching tabs
 * doesn't re-query what the other one was showing.
 */
@Composable
fun MainShell(
    profile: StudentProfile,
    language: AppLanguage,
    remindersEnabled: Boolean,
    darkModeEnabled: Boolean,
    voiceInputEnabled: Boolean,
    onRemindersChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onVoiceInputChange: (Boolean) -> Unit,
    onToolClick: (StudyTool) -> Unit,
    onOpenChat: (HistoryEntry) -> Unit,
    onOpenLanguage: () -> Unit,
    onEditProfile: () -> Unit,
    onSeeProPlans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    val colors = MaterialTheme.appColors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(Motion.medium()).togetherWith(fadeOut(Motion.fast())) },
            label = "tabContent",
        ) { current ->
            when (current) {
                MainTab.Home -> HomeTab(
                    userName = profile.name,
                    onToolClick = onToolClick,
                    onOpenChat = onOpenChat,
                    onSeeAllTools = { tab = MainTab.Tools },
                    onViewHistory = { tab = MainTab.History },
                    onOpenProfile = { tab = MainTab.Profile },
                )
                MainTab.Tools -> ToolsScreen(onToolClick = onToolClick)
                MainTab.History -> HistoryTab(onEntryClick = onOpenChat)
                MainTab.Profile -> ProfileScreen(
                    profile = profile,
                    language = language,
                    remindersEnabled = remindersEnabled,
                    darkModeEnabled = darkModeEnabled,
                    voiceInputEnabled = voiceInputEnabled,
                    onRemindersChange = onRemindersChange,
                    onDarkModeChange = onDarkModeChange,
                    onVoiceInputChange = onVoiceInputChange,
                    onEditProfile = onEditProfile,
                    onOpenLanguage = onOpenLanguage,
                    onSeeProPlans = onSeeProPlans,
                )
            }
        }

        BottomNavBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun HomeTab(
    userName: String,
    onToolClick: (StudyTool) -> Unit,
    onOpenChat: (HistoryEntry) -> Unit,
    onSeeAllTools: () -> Unit,
    onViewHistory: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory.home)
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreen(
        userName = userName.ifBlank { stringResource(R.string.profile_default_name) },
        activity = state.activity,
        onToolClick = onToolClick,
        onOpenChat = onOpenChat,
        onSeeAllTools = onSeeAllTools,
        onViewHistory = onViewHistory,
        onOpenProfile = onOpenProfile,
    )
}

@Composable
private fun HistoryTab(onEntryClick: (HistoryEntry) -> Unit) {
    val viewModel: HistoryViewModel = viewModel(factory = AppViewModelFactory.history)
    val state by viewModel.state.collectAsStateWithLifecycle()

    HistoryScreen(
        state = state,
        onFilterChange = viewModel::setFilter,
        onEntryClick = onEntryClick,
    )
}
