package com.aitutor.chatbot.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.cardStyle
import com.aitutor.chatbot.app.core.ext.elevatedCardStyle
import com.aitutor.chatbot.app.domain.model.UserPlan
import com.aitutor.chatbot.app.ui.components.InitialAvatar
import com.aitutor.chatbot.app.ui.components.pressScale
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.HeroGradientEnd
import com.aitutor.chatbot.app.ui.theme.HeroGradientEndDark
import com.aitutor.chatbot.app.ui.theme.HeroGradientStart
import com.aitutor.chatbot.app.ui.theme.HeroGradientStartDark
import com.aitutor.chatbot.app.ui.theme.PremiumGold
import com.aitutor.chatbot.app.ui.theme.PremiumGoldBright
import com.aitutor.chatbot.app.ui.theme.TabularFigures
import com.aitutor.chatbot.app.ui.theme.spacing
import java.time.LocalDate

private val HeaderShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

/**
 * The gradient masthead: identity, greeting, learning profile and the way into a conversation, all
 * in one surface. Folding the "ask anything" affordance in here means the dashboard opens with a
 * single obvious action instead of competing hero cards.
 */
@Composable
fun DashboardHeader(
    greeting: String,
    profileSummary: String,
    streakDays: Int,
    darkTheme: Boolean,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onAskClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val brush = Brush.linearGradient(
        colors = if (darkTheme) {
            listOf(HeroGradientStartDark, HeroGradientEndDark)
        } else {
            listOf(HeroGradientStart, HeroGradientEnd)
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeaderShape)
            .background(brush)
            // Applied AFTER the background so the gradient itself fills the status bar area while
            // the content below is pushed clear of it by the device's actual inset.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = spacing.lg)
            .padding(top = spacing.sm, bottom = spacing.xl)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "AI Tutor",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = spacing.sm)
                )
                Spacer(modifier = Modifier.weight(1f))
                GlassStreakPill(days = streakDays)
                Box(
                    modifier = Modifier
                        .padding(start = spacing.sm)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(role = Role.Button) { /* Phase 3+: notifications */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsNone,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(modifier = Modifier.padding(start = spacing.sm)) {
                    InitialAvatar(
                        initial = "S",
                        contentDescription = "Open settings",
                        size = 34.dp,
                        onClick = onOpenSettings,
                    )
                }
            }

            Text(
                text = greeting,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = spacing.xl)
            )
            Text(
                text = "What are we learning today?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.padding(top = spacing.xxs)
            )

            GlassProfileChip(
                summary = profileSummary,
                onClick = onEditProfile,
                modifier = Modifier.padding(top = spacing.md)
            )

            MiniComposer(onClick = onAskClick, modifier = Modifier.padding(top = spacing.xl))
        }
    }
}

/** Looks like the chat composer on purpose — the product's core surface, previewed. */
@Composable
private fun MiniComposer(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .elevatedCardStyle(shape = CircleShape, elevation = 8.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(start = spacing.lg, end = spacing.xs, top = spacing.xs, bottom = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Ask me anything…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = spacing.sm)
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassStreakPill(days: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = if (days > 0) PremiumGoldBright else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$days",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun GlassProfileChip(summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        Icon(
            imageVector = Icons.Outlined.School,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Edit learning profile",
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * Today at a glance: an animated goal ring, the streak, and the week as dots. For free users the
 * ring doubles as the quota meter, so there's one place to look rather than two.
 */
@Composable
fun TodayCard(
    questionsToday: Int,
    plan: UserPlan,
    streakDays: Int,
    weeklyActivity: List<DayActivity>,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val goal = if (plan.isPremium) DAILY_GOAL else UserPlan.FREE_DAILY_QUESTIONS
    val progress = (questionsToday.toFloat() / goal).coerceIn(0f, 1f)

    Row(
        modifier = modifier.fillMaxWidth().cardStyle().padding(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalRing(progress = progress, value = questionsToday, goal = goal)

        Column(modifier = Modifier.padding(start = spacing.lg).weight(1f)) {
            Text(
                text = if (plan.isPremium) {
                    "$questionsToday of $goal today"
                } else {
                    "${plan.freeQuestionsRemaining} free questions left"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    streakDays >= 2 -> "$streakDays days in a row — keep it going"
                    questionsToday > 0 -> "Nice start. One more keeps the streak alive."
                    else -> "Ask one question to start your streak"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.xxs)
            )
            WeekDots(weeklyActivity = weeklyActivity, modifier = Modifier.padding(top = spacing.md))
        }
    }
}

@Composable
private fun GoalRing(progress: Float, value: Int, goal: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "goalRing"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ringBrush = Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))

    Box(modifier = modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = ringBrush,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TabularFigures),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "of $goal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekDots(weeklyActivity: List<DayActivity>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        weeklyActivity.forEach { day ->
            val active = day.count > 0
            Box(
                modifier = Modifier
                    .size(if (day.isToday) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            active -> MaterialTheme.colorScheme.primary
                            day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}

private data class Milestone(val label: String, val current: Int, val target: Int, val icon: ImageVector)

/**
 * Progress worth coming back for. Derived entirely from figures already on screen — no extra
 * tracking — so it stays honest about what the student has actually done.
 */
@Composable
fun MilestonesCard(questionsAsked: Int, streakDays: Int, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val milestones = remember(questionsAsked, streakDays) {
        listOf(
            Milestone("First question", questionsAsked, 1, Icons.Filled.AutoAwesome),
            Milestone("10 questions", questionsAsked, 10, Icons.Filled.EmojiEvents),
            Milestone("3-day streak", streakDays, 3, Icons.Filled.LocalFireDepartment),
            Milestone("50 questions", questionsAsked, 50, Icons.Filled.EmojiEvents),
            Milestone("7-day streak", streakDays, 7, Icons.Filled.LocalFireDepartment),
        )
    }
    val unlocked = milestones.count { it.current >= it.target }

    Column(modifier = modifier.fillMaxWidth().cardStyle().padding(vertical = spacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Milestones",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = spacing.sm).weight(1f)
            )
            Text(
                text = "$unlocked of ${milestones.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.padding(top = spacing.lg)
        ) {
            items(milestones) { milestone -> MilestoneBadge(milestone) }
        }
    }
}

@Composable
private fun MilestoneBadge(milestone: Milestone, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val achieved = milestone.current >= milestone.target

    Column(
        modifier = modifier.width(76.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (achieved) {
                        Brush.linearGradient(listOf(PremiumGoldBright, PremiumGold))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (achieved) Icons.Filled.Check else milestone.icon,
                contentDescription = null,
                tint = if (achieved) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = milestone.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = spacing.sm)
        )
        if (!achieved) {
            Text(
                text = "${milestone.current.coerceAtMost(milestone.target)}/${milestone.target}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val studyTips = listOf(
    "Explain a topic out loud as if teaching it — gaps in your understanding show up fast.",
    "Study in 25-minute blocks with 5-minute breaks. Short and focused beats long and foggy.",
    "Do practice questions before re-reading notes. Retrieval beats review, every time.",
    "Write formulas from memory once a day. Recall builds much faster than recognition.",
    "Mix subjects in one session — switching topics improves long-term retention.",
    "Sleep after studying. Memory gets consolidated overnight, not at your desk.",
    "When stuck, ask for a hint rather than the answer. The struggle is where learning happens.",
    "Summarize each chapter in three sentences. If you can't, you haven't finished it yet.",
)

/** Something to read even on a day with no activity, so the dashboard is never a dead page. */
@Composable
fun StudyTipCard(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val tip = remember { studyTips[LocalDate.now().dayOfYear % studyTips.size] }

    Row(
        modifier = modifier.fillMaxWidth().cardStyle().padding(spacing.lg),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PremiumGold.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = "Study tip of the day",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = tip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.xxs)
            )
        }
    }
}

/** The Continue rail's stand-in before any chats exist — an invitation, not a blank space. */
@Composable
fun StartStudyingCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .cardStyle()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Ask your first question",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Your chats will show up here so you can pick up where you left off.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

private const val DAILY_GOAL = 5
