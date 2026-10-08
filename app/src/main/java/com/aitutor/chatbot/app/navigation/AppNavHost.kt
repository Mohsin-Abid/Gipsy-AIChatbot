package com.aitutor.chatbot.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.aitutor.chatbot.app.ui.components.rememberLeaveGuard
import com.aitutor.chatbot.app.ui.language.LanguageScreen
import com.aitutor.chatbot.app.ui.main.MainShell
import com.aitutor.chatbot.app.ui.onboarding.OnboardingScreen
import com.aitutor.chatbot.app.ui.premium.PremiumScreen
import com.aitutor.chatbot.app.ui.premium.PremiumUiState
import com.aitutor.chatbot.app.ui.settings.SettingsViewModel
import com.aitutor.chatbot.app.ui.signin.SignInScreen
import com.aitutor.chatbot.app.ui.signin.AuthViewModel
import com.aitutor.chatbot.app.ui.signin.findActivity
import com.aitutor.chatbot.app.ui.setup.SetupDoneScreen
import com.aitutor.chatbot.app.ui.setup.SetupGoalsScreen
import com.aitutor.chatbot.app.ui.setup.SetupLevelScreen
import com.aitutor.chatbot.app.ui.setup.SetupNameScreen
import com.aitutor.chatbot.app.ui.setup.SetupSubjectsScreen
import com.aitutor.chatbot.app.ui.splash.SplashScreen
import com.aitutor.chatbot.app.ui.theme.SampleContent
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch

/** Type-safe destinations. */
sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object Language : Route
    @Serializable data object Onboarding : Route

    /** Google sign-in. The design puts it after onboarding and before setup. */
    @Serializable data object SignIn : Route
    @Serializable data object SetupName : Route
    @Serializable data object SetupLevel : Route
    @Serializable data object SetupSubjects : Route
    @Serializable data object SetupGoals : Route
    @Serializable data object SetupDone : Route
    @Serializable data object Main : Route

    /** The same picker as [Language], reached from Profile rather than from first-run setup. */
    @Serializable data object LanguageSettings : Route

    /**
     * One setup answer, changed from Profile. Each opens the very screen setup uses for that answer,
     * alone: pre-filled from what is saved, with no progress and no Skip, and saving only the fields
     * it shows. They are not steps of the setup flow and never lead into it.
     */
    @Serializable data object EditName : Route
    @Serializable data object EditLevel : Route
    @Serializable data object EditSubjects : Route
    @Serializable data object EditGoals : Route

    /**
     * A conversation. The tool travels by name so the route stays a plain string, and [chatId] is
     * `0` for a chat that does not exist yet — it is written the first time a question is sent.
     * [start] and [draft] are how Home's ask bar and chips open a new chat; see [ChatLaunch].
     */
    @Serializable
    data class Chat(
        val toolName: String,
        val chatId: Long = 0L,
        val start: ChatStart = ChatStart.None,
        val draft: String = "",
    ) : Route {
        companion object {
            const val TOOL_KEY = "toolName"
            const val CHAT_ID_KEY = "chatId"
            const val DRAFT_KEY = "draft"
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
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelFactory.auth)

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
                    val user = authViewModel.user.value
                    val start = startOnLaunch(resolved.firstRunComplete, resolved.setupOwnerUid, user?.uid)

                    if (shouldClaimSetup(resolved.firstRunComplete, resolved.setupOwnerUid, user?.uid)) {
                        settingsViewModel.claimSetup(user!!.uid)
                    }
                    // Setup picked up again after an interruption: offer the account's name, as a
                    // fresh sign-in would, unless something has already been typed.
                    if (start == Start.Setup && draftProfile.name.isBlank()) {
                        draftProfile = draftProfile.copy(name = user?.suggestedFirstName().orEmpty())
                    }
                    navController.navigate(start.toRoute()) {
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
            OnboardingScreen(onFinished = { navController.navigate(Route.SignIn) })
        }
        composable<Route.SignIn> {
            val authState by authViewModel.state.collectAsStateWithLifecycle()
            val activity = LocalContext.current.findActivity()
            val scope = rememberCoroutineScope()

            // Back on this screen leaves the app. While a sign-in is still running that is asked
            // about first; the request is cancelled with the screen, so confirming just finishes.
            rememberLeaveGuard(
                busy = authState.inProgress,
                bodyRes = R.string.leave_signin_body,
                onLeave = { activity?.finish() },
            )

            // Where to go depends on whether setup is already done *for this account* (see
            // afterSignIn). Someone who has set up before goes straight to Home. A new account goes
            // to setup with an empty draft and its Google first name suggested, which they can change
            // or skip; someone who skipped sign-in gets the same, with the name left empty. SignIn is
            // popped either way, so Back never returns a signed-in person to it.
            val continueIntoApp = {
                val saved = settings.orDefault()
                val user = authViewModel.user.value
                val next = afterSignIn(saved.firstRunComplete, saved.setupOwnerUid, user?.uid)

                if (next == Start.Setup) {
                    // Fresh, so nothing from an earlier person's setup is offered to this one.
                    draftProfile = StudentProfile(name = user?.suggestedFirstName().orEmpty())
                } else if (shouldClaimSetup(saved.firstRunComplete, saved.setupOwnerUid, user?.uid)) {
                    settingsViewModel.claimSetup(user!!.uid)
                }
                navController.navigate(next.toRoute()) {
                    popUpTo(Route.SignIn) { inclusive = true }
                }
            }

            SignInScreen(
                onContinueWithGoogle = {
                    activity?.let { host ->
                        scope.launch { if (authViewModel.signInWithGoogle(host)) continueIntoApp() }
                    }
                },
                onSkip = {
                    scope.launch {
                        authViewModel.continueWithoutAccount()
                        continueIntoApp()
                    }
                },
                onOpenTerms = {},
                onOpenPrivacy = {},
                inProgress = authState.inProgress,
                errorRes = authState.errorRes,
            )
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
                onToggle = { subject -> draftProfile = draftProfile.toggled(subject) },
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
                onToggleGoal = { goal -> draftProfile = draftProfile.toggled(goal) },
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
                    settingsViewModel.completeSetup(draftProfile, ownerUid = authViewModel.user.value?.uid)
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
                onLaunchChat = { launch ->
                    navController.navigate(
                        Route.Chat(launch.tool.name, start = launch.start, draft = launch.draft)
                    )
                },
                onOpenChat = { entry ->
                    navController.navigate(Route.Chat(entry.tool.name, entry.chatId))
                },
                onOpenLanguage = { navController.navigate(Route.LanguageSettings) },
                onEditName = { navController.navigate(Route.EditName) },
                onEditLevel = { navController.navigate(Route.EditLevel) },
                onEditSubjects = { navController.navigate(Route.EditSubjects) },
                onEditGoals = { navController.navigate(Route.EditGoals) },
                onSeeProPlans = { navController.navigate(Route.Premium) },
                // A subscriber has nothing to upgrade to, so the diamond is for everyone else.
                onLogOut = {
                    authViewModel.signOut()
                    navController.navigate(Route.SignIn) {
                        popUpTo(Route.Main) { inclusive = true }
                    }
                },
                showUpgrade = !resolved.isPro,
            )
        }
        composable<Route.EditName> {
            var draft by rememberEditableProfile(settings.orDefault().profile)
            SetupNameScreen(
                name = draft.name,
                onNameChange = { draft = draft.copy(name = it) },
                onBack = { navController.popBackStack() },
                onContinue = {
                    val name = draft.name.trim()
                    settingsViewModel.updateProfile { it.copy(name = name) }
                    navController.popBackStack()
                },
                editing = true,
            )
        }
        composable<Route.EditLevel> {
            var draft by rememberEditableProfile(settings.orDefault().profile)
            SetupLevelScreen(
                level = draft.level,
                grade = draft.grade,
                onLevelChange = { draft = draft.copy(level = it) },
                onGradeChange = { draft = draft.copy(grade = it) },
                onBack = { navController.popBackStack() },
                onContinue = {
                    val edited = draft
                    settingsViewModel.updateProfile { it.copy(level = edited.level, grade = edited.grade) }
                    navController.popBackStack()
                },
                editing = true,
            )
        }
        composable<Route.EditSubjects> {
            var draft by rememberEditableProfile(settings.orDefault().profile)
            SetupSubjectsScreen(
                selected = draft.subjects,
                onToggle = { subject -> draft = draft.toggled(subject) },
                onBack = { navController.popBackStack() },
                onContinue = {
                    val subjects = draft.subjects
                    settingsViewModel.updateProfile { it.copy(subjects = subjects) }
                    navController.popBackStack()
                },
                editing = true,
            )
        }
        composable<Route.EditGoals> {
            var draft by rememberEditableProfile(settings.orDefault().profile)
            SetupGoalsScreen(
                goals = draft.goals,
                dailyTime = draft.dailyTime,
                reminderEnabled = draft.reminderEnabled,
                onToggleGoal = { goal -> draft = draft.toggled(goal) },
                onTimeChange = { draft = draft.copy(dailyTime = it) },
                onReminderChange = { draft = draft.copy(reminderEnabled = it) },
                onBack = { navController.popBackStack() },
                onContinue = {
                    val edited = draft
                    settingsViewModel.updateProfile {
                        it.copy(
                            goals = edited.goals,
                            dailyTime = edited.dailyTime,
                            reminderEnabled = edited.reminderEnabled,
                        )
                    }
                    navController.popBackStack()
                },
                editing = true,
            )
        }
        composable<Route.Chat> { entry ->
            val route = entry.toRoute<Route.Chat>()
            ChatRoute(
                onBack = { navController.popBackStack() },
                // Replaces this chat rather than stacking on it, so Back from the new one returns
                // to where this one was opened from.
                onNewChat = { tool ->
                    navController.navigate(Route.Chat(toolName = tool.name)) {
                        popUpTo<Route.Chat> { inclusive = true }
                    }
                },
                onSelectText = { navController.navigate(Route.SelectText) },
                start = route.start,
                voiceInputEnabled = settings.orDefault().voiceInputEnabled,
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

private fun Start.toRoute(): Route = when (this) {
    Start.Language -> Route.Language
    Start.SignIn -> Route.SignIn
    Start.Setup -> Route.SetupName
    Start.Home -> Route.Main
}

/**
 * The profile an edit screen works on: what is saved at the moment it opens, changed only on that
 * screen and only written back when Save is pressed, so Back discards it. Held in saveable state so a
 * rotation does not throw away a half-made change.
 */
@Composable
private fun rememberEditableProfile(saved: StudentProfile) =
    rememberSaveable(stateSaver = StudentProfileSaver) { mutableStateOf(saved) }

/**
 * Settings to draw with while the stored ones are still being read. Only screens past the splash
 * reach this, and by then the real values have arrived — the splash is what waits for them.
 */
private fun UserSettings?.orDefault(): UserSettings = this ?: UserSettings()
