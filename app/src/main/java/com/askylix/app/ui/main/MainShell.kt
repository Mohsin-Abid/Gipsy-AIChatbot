package com.askylix.app.ui.main

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.askylix.app.R
import com.askylix.app.di.AppViewModelFactory
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.HistoryEntry
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.domain.model.ThemeMode
import com.askylix.app.navigation.ChatLaunch
import com.askylix.app.ui.components.OfflineNote
import com.askylix.app.ui.history.HistoryScreen
import com.askylix.app.ui.history.HistoryViewModel
import com.askylix.app.ui.home.HomeScreen
import com.askylix.app.ui.home.HomeViewModel
import com.askylix.app.ui.profile.ProfileScreen
import com.askylix.app.ui.theme.appColors
import com.askylix.app.ui.signin.SignInReason
import com.askylix.app.ui.tools.ToolsScreen

/**
 * Holds the four primary tabs and the floating nav above them. The bar overlays the content rather
 * than taking layout space, which is why each tab pads its own bottom clear of it by
 * [TabBottomInset].
 *
 * Each tab owns its own ViewModel rather than being handed one state object: Home and History read
 * the same conversations but at different depths, and keeping them separate means switching tabs
 * doesn't re-query what the other one was showing.
 */
/** The "No connection" note sits this far above the bottom of the screen: clear of the tab bar. */
private val OfflineNoteBottomPadding = 96.dp

@Composable
fun MainShell(
    profile: StudentProfile,
    language: AppLanguage,
    remindersEnabled: Boolean,
    themeMode: ThemeMode,
    voiceInputEnabled: Boolean,
    onRemindersChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onVoiceInputChange: (Boolean) -> Unit,
    onToolClick: (StudyTool) -> Unit,
    onLaunchChat: (ChatLaunch) -> Unit,
    onOpenChat: (HistoryEntry) -> Unit,
    onOpenLanguage: () -> Unit,
    onEditName: () -> Unit,
    onEditLevel: () -> Unit,
    onEditSubjects: () -> Unit,
    onEditGoals: () -> Unit,
    onSeeProPlans: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
    showUpgrade: Boolean = false,
    /** False in the browse mode that "Skip for now" leads to. Several tabs change what they offer. */
    signedIn: Boolean = true,
    onRequestSignIn: (SignInReason) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    online: Boolean = true,
) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Home) }
    val colors = MaterialTheme.appColors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
        when (tab) {
                MainTab.Home -> HomeTab(
                    userName = profile.name,
                    onToolClick = onToolClick,
                    onLaunchChat = onLaunchChat,
                    onOpenChat = onOpenChat,
                    onSeeAllTools = { tab = MainTab.Tools },
                    onViewHistory = { tab = MainTab.History },
                    onOpenProfile = { tab = MainTab.Profile },
                    showUpgrade = showUpgrade,
                    onSeeProPlans = onSeeProPlans,
                )
                MainTab.Tools -> ToolsTab(
                    onToolClick = onToolClick,
                    showUpgrade = showUpgrade,
                    onSeeProPlans = onSeeProPlans,
                )
                MainTab.History -> HistoryTab(
                    onEntryClick = onOpenChat,
                    onSignIn = { onRequestSignIn(SignInReason.ViewHistory) },
                    showUpgrade = showUpgrade,
                    onSeeProPlans = onSeeProPlans,
                )
                MainTab.Profile -> ProfileScreen(
                    // The same ViewModel Home uses, so the allowance shown here is the one Home just showed.
                    quota = viewModel<HomeViewModel>(factory = AppViewModelFactory.home).state
                        .collectAsStateWithLifecycle().value.quota,
                    profile = profile,
                    language = language,
                    remindersEnabled = remindersEnabled,
                    themeMode = themeMode,
                    voiceInputEnabled = voiceInputEnabled,
                    onRemindersChange = onRemindersChange,
                    onThemeModeChange = onThemeModeChange,
                    onVoiceInputChange = onVoiceInputChange,
                    onEditName = onEditName,
                    onEditLevel = onEditLevel,
                    onEditSubjects = onEditSubjects,
                    onEditGoals = onEditGoals,
                    onOpenLanguage = onOpenLanguage,
                    onSeeProPlans = onSeeProPlans,
                    onLogOut = onLogOut,
                    signedIn = signedIn,
                    onSignIn = { onRequestSignIn(SignInReason.AskQuestion) },
                    onDeleteAccount = onDeleteAccount,
                    showUpgrade = showUpgrade,
                )
        }

        // Above the tab bar, where nothing else of the screen's is drawn.
        OfflineNote(
            online = online,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = OfflineNoteBottomPadding),
        )

        BottomNavBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun HomeTab(
    userName: String,
    onToolClick: (StudyTool) -> Unit,
    onLaunchChat: (ChatLaunch) -> Unit,
    onOpenChat: (HistoryEntry) -> Unit,
    onSeeAllTools: () -> Unit,
    onViewHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onSeeProPlans: () -> Unit,
    showUpgrade: Boolean,
) {
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory.home)
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Back on Home after a chat or another app: a conversation just made shows in "Continue learning".
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    HomeScreen(
        userName = userName.ifBlank { stringResource(R.string.profile_default_name) },
        activity = state.activity,
        quota = state.quota,
        signedIn = state.signedIn,
        loading = state.loading,
        refreshing = state.refreshing,
        loadError = state.error,
        flags = state.flags,
        onRefresh = viewModel::refresh,
        onToolClick = onToolClick,
        onLaunchChat = onLaunchChat,
        onOpenChat = onOpenChat,
        onSeeAllTools = onSeeAllTools,
        onViewHistory = onViewHistory,
        onOpenProfile = onOpenProfile,
        onSeeProPlans = onSeeProPlans,
        showUpgrade = showUpgrade,
    )
}

/** The tool list. Which tools are on is the server's to say, and Home already holds it, so it shares Home's ViewModel. */
@Composable
private fun ToolsTab(
    onToolClick: (StudyTool) -> Unit,
    onSeeProPlans: () -> Unit,
    showUpgrade: Boolean,
) {
    val viewModel: HomeViewModel = viewModel(factory = AppViewModelFactory.home)
    val state by viewModel.state.collectAsStateWithLifecycle()

    ToolsScreen(
        onToolClick = onToolClick,
        showUpgrade = showUpgrade,
        onSeeProPlans = onSeeProPlans,
        flags = state.flags,
    )
}

@Composable
private fun HistoryTab(
    onEntryClick: (HistoryEntry) -> Unit,
    onSignIn: () -> Unit,
    onSeeProPlans: () -> Unit,
    showUpgrade: Boolean,
) {
    val viewModel: HistoryViewModel = viewModel(factory = AppViewModelFactory.history)
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Back on this tab after another screen or another app: ask again, unless it was asked a moment ago.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }

    HistoryScreen(
        state = state,
        onFilterChange = viewModel::setFilter,
        onQueryChange = viewModel::setQuery,
        onEntryClick = onEntryClick,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        onSignIn = onSignIn,
        onSeeProPlans = onSeeProPlans,
        showUpgrade = showUpgrade,
    )
}
