package com.aitutor.chatbot.app.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.ModeId
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.UserPlan
import com.aitutor.chatbot.app.domain.model.modes
import com.aitutor.chatbot.app.domain.model.recommendationsFor
import com.aitutor.chatbot.app.ui.components.SkeletonRow
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    onModeClick: (Mode) -> Unit,
    onLockedModeClick: (Mode) -> Unit,
    onChatClick: (ChatSummary) -> Unit,
    onSeeAllChats: () -> Unit,
    onUpgradeClick: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsLifecycleAwareState()
    var editingProfile by remember { mutableStateOf<StudentProfile?>(null) }

    HomeScreen(
        state = state,
        onOpenSettings = onOpenSettings,
        onModeClick = onModeClick,
        onLockedModeClick = onLockedModeClick,
        onChatClick = onChatClick,
        onSeeAllChats = onSeeAllChats,
        onUpgradeClick = onUpgradeClick,
        onEditProfile = { editingProfile = it },
    )

    editingProfile?.let { profile ->
        ProfileEditSheet(
            profile = profile,
            onSave = { updated ->
                viewModel.onProfileUpdated(updated)
                editingProfile = null
            },
            onDismiss = { editingProfile = null },
        )
    }
}

@Composable
fun HomeScreen(
    state: UiState<DashboardData>,
    onOpenSettings: () -> Unit,
    onModeClick: (Mode) -> Unit,
    onLockedModeClick: (Mode) -> Unit,
    onChatClick: (ChatSummary) -> Unit,
    onSeeAllChats: () -> Unit,
    onUpgradeClick: () -> Unit,
    onEditProfile: (StudentProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading, is UiState.Error -> DashboardLoading(modifier)
        is UiState.Success -> Dashboard(
            data = state.data,
            onOpenSettings = onOpenSettings,
            onModeClick = onModeClick,
            onLockedModeClick = onLockedModeClick,
            onChatClick = onChatClick,
            onSeeAllChats = onSeeAllChats,
            onUpgradeClick = onUpgradeClick,
            onEditProfile = onEditProfile,
            modifier = modifier,
        )
    }
}

@Composable
private fun Dashboard(
    data: DashboardData,
    onOpenSettings: () -> Unit,
    onModeClick: (Mode) -> Unit,
    onLockedModeClick: (Mode) -> Unit,
    onChatClick: (ChatSummary) -> Unit,
    onSeeAllChats: () -> Unit,
    onUpgradeClick: () -> Unit,
    onEditProfile: (StudentProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val darkTheme = isSystemInDarkTheme()
    val chatMode = data.modes.first { it.id == ModeId.AiChatbot }
    val sidePadding = Modifier.padding(horizontal = spacing.lg)

    fun openMode(mode: Mode) {
        if (data.plan.canOpen(mode)) onModeClick(mode) else onLockedModeClick(mode)
    }

    val questionsToday = if (data.plan.isPremium) {
        data.weeklyActivity.lastOrNull()?.count ?: 0
    } else {
        data.plan.questionsUsedToday
    }

    val quickActions = listOf(
        QuickAction("Scan homework", Icons.Outlined.CameraAlt) {
            openMode(data.modes.first { it.id == ModeId.HomeworkSolver })
        },
        QuickAction("Ask by voice", Icons.Outlined.Mic) { openMode(chatMode) },
        QuickAction("Summarize notes", Icons.Outlined.Description) {
            openMode(data.modes.first { it.id == ModeId.NotesSummarizer })
        },
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "header") {
            DashboardHeader(
                greeting = data.greeting,
                profileSummary = data.profile.summary,
                streakDays = data.streakDays,
                darkTheme = darkTheme,
                onOpenSettings = onOpenSettings,
                onEditProfile = { onEditProfile(data.profile) },
                onAskClick = { onModeClick(chatMode) },
            )
        }

        item(key = "today") {
            TodayCard(
                questionsToday = questionsToday,
                plan = data.plan,
                streakDays = data.streakDays,
                weeklyActivity = data.weeklyActivity,
                modifier = sidePadding.padding(top = spacing.xs),
            )
        }

        item(key = "quick-actions") {
            QuickActionsRow(actions = quickActions, modifier = sidePadding)
        }

        if (data.recentChats.isNotEmpty()) {
            item(key = "continue-header") {
                SectionHeader(
                    title = "Continue studying",
                    actionLabel = "See all",
                    onActionClick = onSeeAllChats,
                    modifier = sidePadding.padding(top = spacing.sm),
                )
            }
            item(key = "continue-rail") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    items(data.recentChats, key = { it.id }) { chat ->
                        ContinueChatCard(
                            chat = chat,
                            mode = modes.firstOrNull { it.id == chat.modeId },
                            onClick = { onChatClick(chat) },
                        )
                    }
                }
            }
        } else {
            item(key = "start-studying") {
                StartStudyingCard(onClick = { onModeClick(chatMode) }, modifier = sidePadding)
            }
        }

        item(key = "plan") {
            if (data.plan.isPremium) {
                PremiumActiveCard(modifier = sidePadding.padding(top = spacing.sm))
            } else {
                UpgradeCard(onClick = onUpgradeClick, modifier = sidePadding.padding(top = spacing.sm))
            }
        }

        item(key = "week-header") {
            SectionHeader(title = "Your week", modifier = sidePadding.padding(top = spacing.sm))
        }
        item(key = "week") {
            ProgressCard(
                weeklyActivity = data.weeklyActivity,
                streakDays = data.streakDays,
                questionsAsked = data.questionsAsked,
                chatsThisWeek = data.chatsThisWeek,
                modifier = sidePadding,
            )
        }

        item(key = "milestones") {
            MilestonesCard(
                questionsAsked = data.questionsAsked,
                streakDays = data.streakDays,
                modifier = sidePadding,
            )
        }

        item(key = "recommended-header") {
            SectionHeader(title = "Recommended for you", modifier = sidePadding.padding(top = spacing.sm))
        }
        items(data.recommendations, key = { "rec-${it.mode.id.name}" }) { recommendation ->
            RecommendationCard(
                recommendation = recommendation,
                locked = !data.plan.canOpen(recommendation.mode),
                onClick = { openMode(recommendation.mode) },
                modifier = sidePadding,
            )
        }

        item(key = "tip") {
            StudyTipCard(modifier = sidePadding.padding(top = spacing.sm))
        }

        item(key = "tools-header") {
            SectionHeader(title = "All study tools", modifier = sidePadding.padding(top = spacing.sm))
        }
        items(data.modes.chunked(2), key = { row -> "tools-${row.first().id.name}" }) { row ->
            Row(
                modifier = sidePadding.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                row.forEach { mode ->
                    ModeTile(
                        mode = mode,
                        locked = !data.plan.canOpen(mode),
                        onClick = { openMode(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DashboardLoading(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    // No gradient header in this state, so the skeletons have to clear the status bar themselves.
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = spacing.lg, vertical = spacing.lg)
    ) {
        repeat(5) { SkeletonRow() }
    }
}

@ThemePreviews
@Composable
private fun DashboardPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(
                state = UiState.Success(
                    DashboardData(
                        greeting = "Good evening",
                        profile = StudentProfile(),
                        plan = UserPlan(isPremium = false, questionsUsedToday = 3),
                        modes = modes,
                        recommendations = recommendationsFor(StudentProfile()),
                        recentChats = emptyList(),
                        weeklyActivity = listOf(2, 0, 5, 3, 0, 1, 4).mapIndexed { index, count ->
                            DayActivity(label = "MTWTFSS"[index].toString(), count = count, isToday = index == 6)
                        },
                        streakDays = 4,
                        questionsAsked = 37,
                        chatsThisWeek = 6,
                    )
                ),
                onOpenSettings = {},
                onModeClick = {},
                onLockedModeClick = {},
                onChatClick = {},
                onSeeAllChats = {},
                onUpgradeClick = {},
                onEditProfile = {},
            )
        }
    }
}
