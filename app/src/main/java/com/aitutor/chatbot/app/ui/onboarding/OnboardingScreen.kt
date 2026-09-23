package com.aitutor.chatbot.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.cardStyle
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.domain.model.GradeLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.Subject
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.HeroGradientEnd
import com.aitutor.chatbot.app.ui.theme.HeroGradientStart
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Three value pages (per spec) followed by a short, skippable personalization flow. The questions
 * aren't a form for its own sake — the answers become the student-profile block in every Gemini
 * system instruction, and the final page reflects them back so the app feels already tuned.
 */
private enum class OnboardingStep { Companion, Instant, Smarter, Grade, Subjects, Goal, Ready }

private val valueSteps = setOf(OnboardingStep.Companion, OnboardingStep.Instant, OnboardingStep.Smarter)

@Composable
fun OnboardingRoute(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val profile by viewModel.profile.collectAsLifecycleAwareState()

    OnboardingScreen(
        profile = profile,
        onGradeSelected = viewModel::onGradeSelected,
        onSubjectToggled = viewModel::onSubjectToggled,
        onGoalSelected = viewModel::onGoalSelected,
        onFinished = { viewModel.onOnboardingFinished(onDone) },
    )
}

@Composable
fun OnboardingScreen(
    profile: StudentProfile,
    onGradeSelected: (GradeLevel) -> Unit,
    onSubjectToggled: (Subject) -> Unit,
    onGoalSelected: (StudyGoal) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val steps = OnboardingStep.entries
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val step = steps[pagerState.currentPage]

    fun goTo(page: Int) = scope.launch { pagerState.animateScrollToPage(page) }

    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
        OnboardingTopBar(
            progress = (pagerState.currentPage + 1f) / steps.size,
            showBack = pagerState.currentPage > 0,
            showSkip = step in valueSteps,
            onBack = { goTo(pagerState.currentPage - 1) },
            onSkip = onFinished,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            userScrollEnabled = true,
        ) { page ->
            when (steps[page]) {
                OnboardingStep.Companion -> CompanionPage()
                OnboardingStep.Instant -> InstantPage()
                OnboardingStep.Smarter -> SmarterPage()
                OnboardingStep.Grade -> GradePage(selected = profile.grade, onSelect = onGradeSelected)
                OnboardingStep.Subjects -> SubjectsPage(selected = profile.subjects, onToggle = onSubjectToggled)
                OnboardingStep.Goal -> GoalPage(selected = profile.goal, onSelect = onGoalSelected)
                OnboardingStep.Ready -> ReadyPage(profile = profile)
            }
        }

        if (step in valueSteps) {
            PageDots(
                count = valueSteps.size,
                selectedIndex = pagerState.currentPage,
                modifier = Modifier.padding(bottom = spacing.md)
            )
        }

        OnboardingCta(
            step = step,
            profile = profile,
            onNext = { goTo(pagerState.currentPage + 1) },
            onFinish = onFinished,
            modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.lg)
        )
    }
}

@Composable
private fun OnboardingTopBar(
    progress: Float,
    showBack: Boolean,
    showSkip: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "onboardingProgress")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showSkip) {
                TextButton(onClick = onSkip) {
                    Text("Skip", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
        Box(
            modifier = Modifier
                .padding(horizontal = spacing.xl)
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, selectedIndex: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(count) { index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(if (selected) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

@Composable
private fun OnboardingCta(
    step: OnboardingStep,
    profile: StudentProfile,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val answered = when (step) {
        OnboardingStep.Grade -> profile.grade != null
        OnboardingStep.Subjects -> profile.subjects.isNotEmpty()
        OnboardingStep.Goal -> profile.goal != null
        else -> true
    }
    val label = when (step) {
        OnboardingStep.Ready -> "Start learning"
        OnboardingStep.Smarter -> "Personalize my tutor"
        else -> if (answered) "Continue" else "Choose one to continue"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = { if (step == OnboardingStep.Ready) onFinish() else onNext() },
            enabled = answered,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        AnimatedVisibility(
            visible = step in setOf(OnboardingStep.Grade, OnboardingStep.Subjects, OnboardingStep.Goal),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Skip this step", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ---- Value pages ----

@Composable
private fun PageScaffold(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(spacing.xl))
        content()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xl)
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xl)
        )
    }
}

@Composable
private fun CompanionPage() {
    PageScaffold(
        title = "Your AI study companion",
        body = "A patient tutor in your pocket — ready whenever you're stuck, at any hour.",
    ) {
        val transition = rememberInfiniteTransition(label = "heroPulse")
        val scale by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "heroScale"
        )
        Box(
            modifier = Modifier
                .size(148.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/** Page two shows the product rather than describing it — a real step-by-step answer preview. */
@Composable
private fun InstantPage() {
    val spacing = MaterialTheme.spacing
    PageScaffold(
        title = "Homework, math and essays — solved",
        body = "Every answer is worked through step by step, so you can follow the reasoning.",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            ) {
                Text(
                    text = "Solve 3x + 7 = 22",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(spacing.md)) {
                    listOf(
                        "1. Subtract 7 from both sides → 3x = 15",
                        "2. Divide both sides by 3 → x = 5",
                        "3. Check: 3(5) + 7 = 22 ✓",
                    ).forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmarterPage() {
    val spacing = MaterialTheme.spacing
    PageScaffold(
        title = "Learn smarter, not faster",
        body = "Explanations that build understanding — not answers to copy and forget.",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            FeatureLine(Icons.Outlined.CameraAlt, "Snap a photo", "Solve from a picture of your homework")
            FeatureLine(Icons.Outlined.RecordVoiceOver, "Ask out loud", "Voice questions and spoken answers")
            FeatureLine(Icons.Outlined.Translate, "Your language", "English, Urdu, Roman Urdu, Arabic or Hindi")
        }
    }
}

@Composable
private fun FeatureLine(icon: ImageVector, title: String, detail: String) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().cardStyle().padding(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- Personalization pages ----

@Composable
private fun QuestionPage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = spacing.xl)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.xs, bottom = spacing.xl)
        )
        content()
        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Composable
private fun GradePage(selected: GradeLevel?, onSelect: (GradeLevel) -> Unit) {
    val spacing = MaterialTheme.spacing
    QuestionPage(
        title = "What level are you studying at?",
        subtitle = "Your tutor will pitch every explanation at this level.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            GradeLevel.entries.forEach { grade ->
                SelectableRow(
                    title = grade.label,
                    detail = grade.caption,
                    selected = grade == selected,
                    onClick = { onSelect(grade) },
                )
            }
        }
    }
}

@Composable
private fun SubjectsPage(selected: Set<Subject>, onToggle: (Subject) -> Unit) {
    val spacing = MaterialTheme.spacing
    QuestionPage(
        title = "Which subjects do you need help with?",
        subtitle = "Pick as many as you like — we'll surface the right tools first.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Subject.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { subject ->
                        SubjectChip(
                            subject = subject,
                            selected = subject in selected,
                            onClick = { onToggle(subject) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GoalPage(selected: StudyGoal?, onSelect: (StudyGoal) -> Unit) {
    val spacing = MaterialTheme.spacing
    QuestionPage(
        title = "What are you working towards?",
        subtitle = "This shapes how your tutor paces you.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            StudyGoal.entries.forEach { goal ->
                SelectableRow(
                    title = goal.label,
                    detail = goal.detail,
                    selected = goal == selected,
                    onClick = { onSelect(goal) },
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectChip(
    subject: Subject,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier,
        shape = AppShapes.Card,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(
                imageVector = subject.icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = subject.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** The payoff: what they told us, reflected straight back before they ever open the app. */
@Composable
private fun ReadyPage(profile: StudentProfile) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(spacing.xxl))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
        Text(
            text = "Your tutor is ready",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xl)
        )
        Text(
            text = "Tuned to how you learn — you can change any of this later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs)
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.xl).cardStyle().padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            SummaryLine(label = "Level", value = profile.grade?.label ?: "Any level")
            SummaryLine(
                label = "Subjects",
                value = profile.subjects.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.label } ?: "All subjects",
            )
            SummaryLine(label = "Goal", value = profile.goal?.label ?: "General study help")
        }
        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@ThemePreviews
@Composable
private fun OnboardingScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OnboardingScreen(
                profile = StudentProfile(grade = GradeLevel.HighSchool, subjects = setOf(Subject.Math)),
                onGradeSelected = {},
                onSubjectToggled = {},
                onGoalSelected = {},
                onFinished = {},
            )
        }
    }
}
