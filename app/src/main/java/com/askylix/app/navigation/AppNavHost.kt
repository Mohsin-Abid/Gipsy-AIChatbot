package com.askylix.app.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.askylix.app.AITutorApplication
import com.askylix.app.BuildConfig
import com.askylix.app.R
import com.askylix.app.data.prefs.UserSettings
import com.askylix.app.di.AppViewModelFactory
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.ui.chat.ChatRoute
import com.askylix.app.ui.chat.SelectTextScreen
import com.askylix.app.ui.components.rememberLeaveGuard
import com.askylix.app.ui.gate.AppGate
import com.askylix.app.ui.gate.AppGateScreen
import com.askylix.app.ui.gate.gateFor
import com.askylix.app.ui.language.LanguageScreen
import com.askylix.app.ui.main.MainShell
import com.askylix.app.ui.onboarding.OnboardingScreen
import com.askylix.app.ui.premium.PremiumScreen
import com.askylix.app.ui.premium.PremiumUiState
import com.askylix.app.ui.settings.SettingsViewModel
import com.askylix.app.ui.signin.SignInGate
import com.askylix.app.ui.signin.SignInReason
import com.askylix.app.ui.signin.SignInResult
import com.askylix.app.ui.signin.SignInScreen
import com.askylix.app.ui.signin.AuthViewModel
import com.askylix.app.ui.signin.findActivity
import com.askylix.app.ui.setup.SetupDoneScreen
import com.askylix.app.ui.setup.SetupGoalsScreen
import com.askylix.app.ui.setup.SetupLevelScreen
import com.askylix.app.ui.setup.SetupNameScreen
import com.askylix.app.ui.setup.SetupSubjectsScreen
import com.askylix.app.ui.splash.SplashScreen
import com.askylix.app.ui.theme.SampleContent
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
     * A conversation. The tool travels by name so the route stays a plain string, and [chatId] is the
     * server's id for the chat, or empty for one that does not exist yet — the server creates it when
     * the first question is sent.
     * [start] and [draft] are how Home's ask bar and chips open a new chat; see [ChatLaunch].
     */
    @Serializable
    data class Chat(
        val toolName: String,
        val chatId: String = "",
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
    val context = LocalContext.current
    val container = (context.applicationContext as AITutorApplication).container
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory.settings)
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val authViewModel: AuthViewModel = viewModel(factory = AppViewModelFactory.auth)
    val signedInUser by authViewModel.user.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Why an account is wanted right now, while the prompt for it is open. Shared by every screen that
    // needs one, so each only says why.
    var signInReason by remember { mutableStateOf<SignInReason?>(null) }
    val requestSignIn: (SignInReason) -> Unit = { signInReason = it }

    // The server's settings, asked for each time the app comes to the front: they are tiny, and they
    // are how an update or a maintenance window reaches a phone that is already open.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { scope.launch { container.account.loadConfig() } }
    val config by container.session.config.collectAsStateWithLifecycle()
    val gate = gateFor(config, BuildConfig.VERSION_NAME)
    val online by container.connectivity.online.collectAsStateWithLifecycle(initialValue = true)

    // Notices for failures that happen after a screen has moved on: a rename the server refused, say.
    LaunchedEffect(Unit) {
        container.messages.events.collect { message ->
            Toast.makeText(context, message.text, if (message.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }

    // The server refused the session even with a fresh token — the account was deleted, or signed out
    // everywhere. Everything about it is cleared and the person is taken to sign in.
    LaunchedEffect(Unit) {
        container.sessionExpired.collect {
            if (authViewModel.user.value == null) return@collect
            container.account.signOut()
            container.messages.post(R.string.session_expired)
            navController.navigate(Route.SignIn) { popUpTo(navController.graph.id) { inclusive = true } }
        }
    }

    // Setup's answers are held here and committed once, at the end. Abandoning setup halfway leaves
    // nothing stored, so a returning user never meets a half-filled profile.
    var draftProfile by rememberSaveable(stateSaver = StudentProfileSaver) {
        mutableStateOf(StudentProfile())
    }

    Box(modifier = modifier) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash,
        // Behind the gate the screens are out of reach, so a screen reader must not offer them either.
        modifier = if (gate == AppGate.Open) Modifier else Modifier.clearAndSetSemantics { },
    ) {
        composable<Route.Splash> {
            SplashScreen(
                // `settings` is null until the stored values arrive. Holding the splash until then
                // is what stops a returning user flashing past onboarding on the way to Home.
                ready = settings != null,
                onFinished = {
                    val resolved = settings ?: return@SplashScreen
                    val user = authViewModel.user.value
                    val start = startOnLaunch(resolved.firstRunComplete, resolved.skippedSignIn, signedIn = user != null)

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
            val signInScope = rememberCoroutineScope()

            // Back on this screen leaves the app. While a sign-in is still running that is asked
            // about first; the request is cancelled with the screen, so confirming just finishes.
            rememberLeaveGuard(
                busy = authState.inProgress,
                bodyRes = R.string.leave_signin_body,
                onLeave = { activity?.finish() },
            )

            // Signing in has already found out what the account needs (see AuthViewModel.signIn): an
            // account that has been set up goes straight to Home with its own profile; a new one goes to
            // setup with its Google first name suggested, which can be changed or skipped. SignIn is
            // popped either way, so Back never returns a signed-in person to it.
            SignInScreen(
                onContinueWithGoogle = {
                    activity?.let { host ->
                        signInScope.launch {
                            when (authViewModel.signIn(host, alwaysUseSavedAnswers = false)) {
                                SignInResult.Home -> navController.navigate(Route.Main) {
                                    popUpTo(Route.SignIn) { inclusive = true }
                                }
                                SignInResult.Setup -> {
                                    // Fresh, so nothing from an earlier person's setup is offered to this one.
                                    draftProfile = StudentProfile(
                                        name = authViewModel.user.value?.suggestedFirstName().orEmpty(),
                                    )
                                    navController.navigate(Route.SetupName) {
                                        popUpTo(Route.SignIn) { inclusive = true }
                                    }
                                }
                                SignInResult.NotSignedIn -> Unit
                            }
                        }
                    }
                },
                // Skipping is a browse mode with no account. It is remembered, so the next launch opens
                // the app instead of asking again.
                onSkip = {
                    val next = afterSkip(settings.orDefault().firstRunComplete)
                    settingsViewModel.setSkipped(true)
                    if (next == Start.Setup) draftProfile = StudentProfile()
                    navController.navigate(next.toRoute()) { popUpTo(Route.SignIn) { inclusive = true } }
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
            var finishing by remember { mutableStateOf(false) }
            val doneScope = rememberCoroutineScope()
            SetupDoneScreen(
                profile = draftProfile,
                finishing = finishing,
                onStart = {
                    finishing = true
                    doneScope.launch {
                        // Signed in, the answers go to the account first; if it cannot be reached the
                        // person stays here, is told, and can press the button again.
                        if (settingsViewModel.completeSetup(draftProfile)) {
                            navController.navigate(Route.Main) { popUpTo(Route.Language) { inclusive = true } }
                        } else {
                            finishing = false
                        }
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
                signedIn = signedInUser != null,
                onRequestSignIn = requestSignIn,
                online = online,
                onDeleteAccount = {
                    scope.launch {
                        authViewModel.deleteAccount().fold(
                            onSuccess = { deleted ->
                                // Deleting the account does not cancel a Play subscription, and charges go on.
                                if (deleted.activeSubscriptionRemains) container.messages.post(R.string.profile_delete_subscription, long = true)
                                navController.navigate(Route.SignIn) { popUpTo(Route.Main) { inclusive = true } }
                            },
                            onFailure = { container.messages.post(R.string.profile_delete_failed) },
                        )
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
                onOpenPremium = { navController.navigate(Route.Premium) },
                authViewModel = authViewModel,
                online = online,
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
                // A subscription belongs to an account, so someone who skipped sign-in is asked to sign
                // in first, and comes back to this screen with their plan still chosen. Starting the Play
                // purchase itself is the part still to be built; until it is, nothing happens after sign-in.
                onPurchase = { if (signedInUser == null) requestSignIn(SignInReason.Subscribe) },
                onRestore = { if (signedInUser == null) requestSignIn(SignInReason.Subscribe) },
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

    // The gate for a forced update or maintenance covers everything underneath it.
    AppGateScreen(gate = gate, onRetry = { scope.launch { container.account.loadConfig() } })

    SignInGate(reason = signInReason, authViewModel = authViewModel, onDismiss = { signInReason = null })
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
