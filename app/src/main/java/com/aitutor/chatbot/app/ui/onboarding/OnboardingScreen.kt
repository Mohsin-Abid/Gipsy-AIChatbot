package com.aitutor.chatbot.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.aitutor.chatbot.app.core.ext.cardStyle
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.core.ext.elevatedCardStyle
import com.aitutor.chatbot.app.domain.model.GradeLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.Subject
import com.aitutor.chatbot.app.ui.components.heroBrush
import com.aitutor.chatbot.app.ui.components.heroSheen
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.PremiumGold
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Three value pages followed by a short, skippable personalization flow. The questions aren't a form
 * for its own sake — the answers become the student-profile block in every system instruction,
 * and the final page reflects them back so the app feels already tuned.
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        OnboardingTopBar(
            stepIndex = pagerState.currentPage,
            stepCount = steps.size,
            showBack = pagerState.currentPage > 0,
            showSkip = step in valueSteps,
            onBack = { goTo(pagerState.currentPage - 1) },
            onSkip = onFinished,
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
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

        OnboardingCta(
            step = step,
            profile = profile,
            onNext = { goTo(pagerState.currentPage + 1) },
            onFinish = onFinished,
            modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.lg)
        )
    }
}

// ---- Chrome ----

/**
 * Back / progress / skip on one line. The progress is seven segments rather than a bar or dots —
 * it reads as "a short, finite flow" at a glance, which is what stops people bailing on page one.
 */
@Composable
private fun OnboardingTopBar(
    stepIndex: Int,
    stepCount: Int,
    showBack: Boolean,
    showSkip: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        SegmentedProgress(
            stepIndex = stepIndex,
            stepCount = stepCount,
            modifier = Modifier.weight(1f).padding(horizontal = spacing.sm),
        )

        if (showSkip) {
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun SegmentedProgress(stepIndex: Int, stepCount: Int, modifier: Modifier = Modifier) {
    val brush = heroBrush()
    val idle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(stepCount) { index ->
            val done = index <= stepIndex
            val weight by animateFloatAsState(
                targetValue = if (index == stepIndex) 1.8f else 1f,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label = "segmentWeight",
            )
            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (done) brush else SolidColor(idle))
            )
        }
    }
}

/** Gradient pill CTA. A flat accent button under a gradient hero looks like two different apps. */
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
    val brush = heroBrush()
    val disabledFill = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(if (answered) Modifier.elevatedCardStyle(CircleShape, 12.dp) else Modifier)
                .clip(CircleShape)
                .background(if (answered) brush else disabledFill)
                .then(if (answered) Modifier.heroSheen(rings = false) else Modifier)
                .clickable(enabled = answered) {
                    if (step == OnboardingStep.Ready) onFinish() else onNext()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (answered) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            visible = step in setOf(OnboardingStep.Grade, OnboardingStep.Subjects, OnboardingStep.Goal),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Skip this step",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---- Shared page furniture ----

/** Fades and lifts its content in on first composition. Staggering these is what makes a page land. */
@Composable
private fun Reveal(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedRevealEffect(progress, delayMillis)
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 24.dp.toPx()
        }
    ) {
        content()
    }
}

@Composable
private fun LaunchedRevealEffect(progress: Animatable<Float, *>, delayMillis: Int) {
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
}

/** Small caps kicker above every headline — the cheapest signal that a layout was art-directed. */
@Composable
private fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/** The gradient art panel every value page leads with — the premium anchor of the flow. */
@Composable
private fun HeroArtPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(252.dp)
            .elevatedCardStyle(shape = AppShapes.CardLarge, elevation = 16.dp)
            .clip(AppShapes.CardLarge)
            .background(heroBrush())
            .heroSheen(),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A translucent tile on the gradient — the "frosted glass" element the hero panels are built from. */
@Composable
private fun GlassTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(AppShapes.Card)
            .background(Color.White.copy(alpha = 0.13f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), AppShapes.Card)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ValuePage(
    eyebrow: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl),
    ) {
        Spacer(modifier = Modifier.height(spacing.lg))
        Reveal(modifier = Modifier.fillMaxWidth()) { art() }
        Reveal(delayMillis = 110, modifier = Modifier.fillMaxWidth()) {
            Column {
                Eyebrow(text = eyebrow, modifier = Modifier.padding(top = spacing.xxl))
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = spacing.sm),
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.md, bottom = spacing.xl),
                )
            }
        }
    }
}

// ---- Value pages ----

@Composable
private fun CompanionPage() {
    ValuePage(
        eyebrow = "MEET YOUR TUTOR",
        title = "A tutor who never runs out of patience",
        body = "Ask the same question five different ways at two in the morning. You will get five clear answers, and no sighing.",
        art = {
            HeroArtPanel {
                val transition = rememberInfiniteTransition(label = "heroBreath")
                val breath by transition.animateFloat(
                    initialValue = 0.97f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "heroBreathScale",
                )
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer { scaleX = breath; scaleY = breath }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp),
                    )
                }
                FloatingLabel("Algebra", Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 30.dp))
                FloatingLabel("Essays", Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 56.dp))
                FloatingLabel("Physics", Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 34.dp))
            }
        }
    )
}

@Composable
private fun FloatingLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.95f),
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Page two shows the product rather than describing it — a real step-by-step answer preview. */
@Composable
private fun InstantPage() {
    val spacing = MaterialTheme.spacing
    ValuePage(
        eyebrow = "HOW IT ANSWERS",
        title = "Worked through, never just handed over",
        body = "Homework, equations and essays come back as steps you can follow — and check.",
        art = {
            HeroArtPanel {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(spacing.lg),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp),
                    ) {
                        Text(
                            text = "Solve 3x + 7 = 22",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(spacing.md)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(heroBrush())
                                )
                                Text(
                                    text = "AI TUTOR",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = spacing.sm),
                                )
                            }
                            Spacer(modifier = Modifier.height(spacing.sm))
                            SolutionStep(1, "Subtract 7 from both sides  →  3x = 15")
                            SolutionStep(2, "Divide both sides by 3  →  x = 5")
                            SolutionStep(3, "Check: 3(5) + 7 = 22")
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SolutionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun SmarterPage() {
    val spacing = MaterialTheme.spacing
    ValuePage(
        eyebrow = "WHAT YOU GET",
        title = "Built for how you actually study",
        body = "Photograph a page, ask out loud, or read it back in your own language.",
        art = {
            HeroArtPanel {
                Column(
                    modifier = Modifier.fillMaxSize().padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        GlassTile(Icons.Outlined.CameraAlt, "Snap a photo", Modifier.weight(1f).fillMaxHeight())
                        GlassTile(Icons.Outlined.RecordVoiceOver, "Ask out loud", Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        GlassTile(Icons.Outlined.Translate, "5 languages", Modifier.weight(1f).fillMaxHeight())
                        GlassTile(Icons.Outlined.Bolt, "Step by step", Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    )
}

// ---- Personalization pages ----

@Composable
private fun QuestionPage(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl)
    ) {
        Reveal(modifier = Modifier.fillMaxWidth()) {
            Column {
                Eyebrow(text = eyebrow, modifier = Modifier.padding(top = spacing.xl))
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = spacing.sm),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xl),
                )
            }
        }
        Reveal(delayMillis = 110, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Composable
private fun GradePage(selected: GradeLevel?, onSelect: (GradeLevel) -> Unit) {
    val spacing = MaterialTheme.spacing
    QuestionPage(
        eyebrow = "ABOUT YOU",
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
        eyebrow = "YOUR SUBJECTS",
        title = "Where do you need the most help?",
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
        eyebrow = "YOUR GOAL",
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

/**
 * Single-select row. Selection is carried by a tinted border and a filled radio, not by flooding the
 * card with `primaryContainer` — the filled treatment reads heavy on light and muddy on dark.
 */
@Composable
private fun SelectableRow(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(220),
        label = "rowBorder",
    )
    val fill by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(220),
        label = "rowFill",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        color = fill,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioDot(selected = selected)
            Column(modifier = Modifier.weight(1f).padding(start = spacing.md)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val ring by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(220),
        label = "radioRing",
    )
    val innerSize by animateFloatAsState(
        targetValue = if (selected) 11f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "radioInner",
    )
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(if (selected) 2.dp else 1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(innerSize.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
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
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(220),
        label = "chipBorder",
    )
    val fill by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(220),
        label = "chipFill",
    )

    Surface(
        modifier = modifier,
        shape = AppShapes.Card,
        color = fill,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.16f else 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = subject.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = subject.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
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
        Reveal {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .elevatedCardStyle(CircleShape, 16.dp)
                    .clip(CircleShape)
                    .background(heroBrush())
                    .heroSheen(rings = false),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(46.dp),
                )
            }
        }
        Reveal(delayMillis = 110, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Eyebrow(text = "ALL SET", modifier = Modifier.padding(top = spacing.xl))
                Text(
                    text = "Your tutor is ready",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = spacing.sm),
                )
                Text(
                    text = "Tuned to how you learn. You can change any of this later in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
        }
        Reveal(delayMillis = 220, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl)
                    .cardStyle()
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                SummaryLine(label = "LEVEL", value = profile.grade?.label ?: "Any level")
                SummaryLine(
                    label = "SUBJECTS",
                    value = profile.subjects.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { it.label } ?: "All subjects",
                )
                SummaryLine(label = "GOAL", value = profile.goal?.label ?: "General study help")
            }
        }
        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    val spacing = MaterialTheme.spacing
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(PremiumGold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = spacing.md).width(80.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
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
