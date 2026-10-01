package com.aitutor.chatbot.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.data.prefs.UserSettings
import com.aitutor.chatbot.app.di.AppViewModelFactory
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.ui.chat.ChatRoute
import com.aitutor.chatbot.app.ui.chat.SelectTextScreen
import com.aitutor.chatbot.app.ui.language.LanguageScreen
import com.aitutor.chatbot.app.ui.main.MainShell
import com.aitutor.chatbot.app.ui.onboarding.OnboardingScreen
import com.aitutor.chatbot.app.ui.premium.PremiumScreen
import com.aitutor.chatbot.app.ui.premium.PremiumUiState
import com.aitutor.chatbot.app.ui.settings.SettingsViewModel
import com.aitutor.chatbot.app.ui.setup.SetupDoneScreen
import com.aitutor.chatbot.app.ui.setup.SetupGoalsScreen
import com.aitutor.chatbot.app.ui.setup.SetupLevelScreen
import com.aitutor.chatbot.app.ui.setup.SetupNameScreen
import com.aitutor.chatbot.app.ui.setup.SetupSubjectsScreen
import com.aitutor.chatbot.app.ui.splash.SplashScreen
import com.aitutor.chatbot.app.ui.theme.SampleContent
import kotlinx.serialization.Serializable

/** Type-safe destinations. */
sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object Language : Route
    @Serializable data object Onboarding : Route
    @Serializable data object SetupName : Route
    @Serializable data object SetupLevel : Route
    @Serializable data object SetupSubjects : Route
    @Serializable data object SetupGoals : Route
    @Serializable data object SetupDone : Route
    @Serializable data object Main : Route

    /** The same picker as [Language], reached from Profile rather than from first-run setup. */
    @Serializable data object LanguageSettings : Route

    /**
     * A conversation. The tool travels by name so the route stays a plain string, and [chatId] is
     * `0` for a chat that does not exist yet — it is written the first time a question is sent.
     */
    @Serializable
    data class Chat(val toolName: String, val chatId: Long = 0L) : Route {
        companion object {
            const val TOOL_KEY = "toolName"
            const val CHAT_ID_KEY = "chatId"
        }
    }

    /** Text selection over the newest answer of the chat below it. */
    @Serializable data object SelectText : Route

    /** The Pro paywall, reached from Profile's upgrade card or its subscription row. */
    @Serializable data object Premium : Route
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory.settings)
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    // Setup's answers are held here and committed once, at the end. Abandoning setup halfway leaves
    // nothing stored, so a returning user never meets a half-filled profile.
    var draftProfile by rememberSaveable(stateSaver = StudentProfileSaver) {
        mutableStateOf(StudentProfile())
    }

    NavHost(navController = navController, startDestination = Route.Splash, modifier = modifier) {
        composable<Route.Splash> {
            SplashScreen(
                // `settings` is null until the stored values arrive. Holding the splash until then
                // is what stops a returning user flashing past onboarding on the way to Home.
                ready = settings != null,
                onFinished = {
                    val resolved = settings ?: return@SplashScreen
                    navController.navigate(
                        if (resolved.firstRunComplete) Route.Main else Route.Language
                    ) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable<Route.Language> {
            LanguageScreen(
                selected = settings.orDefault().language,
                onSelect = settingsViewModel::setLanguage,
                onContinue = { navController.navigate(Route.Onboarding) },
            )
        }
        composable<Route.Onboarding> {
            OnboardingScreen(onFinished = { navController.navigate(Route.SetupName) })
        }
        composable<Route.SetupName> {
            SetupNameScreen(
                name = draftProfile.name,
                onNameChange = { draftProfile = draftProfile.copy(name = it) },
                onBack = { navController.popBackStack() },
                onSkip = { navController.navigate(Route.SetupLevel) },
                onContinue = { navController.navigate(Route.SetupLevel) },
            )
        }
        composable<Route.SetupLevel> {
            SetupLevelScreen(
                level = draftProfile.level,
                grade = draftProfile.grade,
                onLevelChange = { draftProfile = draftProfile.copy(level = it) },
                onGradeChange = { draftProfile = draftProfile.copy(grade = it) },
                onBack = { navController.popBackStack() },
                onSkip = { navController.navigate(Route.SetupSubjects) },
                onContinue = { navController.navigate(Route.SetupSubjects) },
            )
        }
        composable<Route.SetupSubjects> {
            SetupSubjectsScreen(
                selected = draftProfile.subjects,
                onToggle = { subject ->
                    draftProfile = draftProfile.copy(
                        subjects = if (subject in draftProfile.subjects) {
                            draftProfile.subjects - subject
                        } else {
                            draftProfile.subjects + subject
                        }
                    )
                },
                onBack = { navController.popBackStack() },
                onSkip = { navController.navigate(Route.SetupGoals) },
                onContinue = { navController.navigate(Route.SetupGoals) },
            )
        }
        composable<Route.SetupGoals> {
            SetupGoalsScreen(
                goals = draftProfile.goals,
                dailyTime = draftProfile.dailyTime,
                reminderEnabled = draftProfile.reminderEnabled,
                onToggleGoal = { goal ->
                    val goals = draftProfile.goals
                    draftProfile = draftProfile.copy(
                        goals = when {
                            goal in goals -> goals - goal
                            goals.size < StudentProfile.MAX_GOALS -> goals + goal
                            else -> goals
                        }
                    )
                },
                onTimeChange = { draftProfile = draftProfile.copy(dailyTime = it) },
                onReminderChange = { draftProfile = draftProfile.copy(reminderEnabled = it) },
                onBack = { navController.popBackStack() },
                onSkip = { navController.navigate(Route.SetupDone) },
                onContinue = { navController.navigate(Route.SetupDone) },
            )
        }
        composable<Route.SetupDone> {
            SetupDoneScreen(
                profile = draftProfile,
                onStart = {
                    settingsViewModel.completeSetup(draftProfile)
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Language) { inclusive = true }
                    }
                },
            )
        }
        composable<Route.Main> {
            val resolved = settings.orDefault()
            MainShell(
                profile = resolved.profile,
                language = resolved.language,
                remindersEnabled = resolved.remindersEnabled,
                themeMode = resolved.themeMode,
                voiceInputEnabled = resolved.voiceInputEnabled,
                onRemindersChange = settingsViewModel::setRemindersEnabled,
                onThemeModeChange = settingsViewModel::setThemeMode,
                onVoiceInputChange = settingsViewModel::setVoiceInputEnabled,
                onToolClick = { tool -> navController.navigate(Route.Chat(tool.name)) },
                onOpenChat = { entry ->
                    navController.navigate(Route.Chat(entry.tool.name, entry.chatId))
                },
                onOpenLanguage = { navController.navigate(Route.LanguageSettings) },
                onEditProfile = { navController.navigate(Route.SetupLevel) },
                onSeeProPlans = { navController.navigate(Route.Premium) },
            )
        }
        composable<Route.Chat> { entry ->
            val route = entry.toRoute<Route.Chat>()
            ChatRoute(
                onBack = { navController.popBackStack() },
                onSelectText = { navController.navigate(Route.SelectText) },
                // A distinct key per conversation, so moving between two chats cannot reuse one
                // ViewModel and briefly show the wrong thread.
                viewModelKey = "${route.toolName}:${route.chatId}",
            )
        }
        composable<Route.SelectText> {
            // Reads the design's answer rather than the live one: selection needs a measured text
            // layout to map handles onto, which is the piece still to be built.
            SelectTextScreen(
                tool = StudyTool.AiTutor,
                subject = stringResource(R.string.chat_subject_default),
                timestamp = stringResource(R.string.chat_sample_timestamp),
                blocks = SampleContent.answerBlocks(),
                onClose = { navController.popBackStack() },
                onAskAboutSelection = { navController.popBackStack() },
            )
        }
        composable<Route.Premium> {
            // No prices yet: Play Billing has to quote them, so the screen opens with its
            // unavailable state and the purchase button stays disabled until that lands.
            var state by rememberSaveable(stateSaver = PremiumUiStateSaver) {
                mutableStateOf(PremiumUiState())
            }
            PremiumScreen(
                state = state,
                onClose = { navController.popBackStack() },
                onSelectPlan = { plan -> state = state.copy(selected = plan) },
                onPurchase = {},
                onRestore = {},
                onOpenTerms = {},
                onOpenPrivacy = {},
            )
        }
        composable<Route.LanguageSettings> {
            LanguageScreen(
                selected = settings.orDefault().language,
                onSelect = settingsViewModel::setLanguage,
                onContinue = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Settings to draw with while the stored ones are still being read. Only screens past the splash
 * reach this, and by then the real values have arrived — the splash is what waits for them.
 */
private fun UserSettings?.orDefault(): UserSettings = this ?: UserSettings()
