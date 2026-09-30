package com.aitutor.chatbot.app.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.components.BrandRow
import com.aitutor.chatbot.app.ui.components.CircularNextButton
import com.aitutor.chatbot.app.ui.components.HeroBadge
import com.aitutor.chatbot.app.ui.components.OnboardingHero
import com.aitutor.chatbot.app.ui.components.PrimaryCta
import com.aitutor.chatbot.app.ui.components.Reveal
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors
import kotlinx.coroutines.launch

const val ONBOARDING_PAGES = 3

/**
 * Three value pages. Each is the same frame — accent hero on top, copy and controls below — with
 * only the artwork inside the hero changing, so paging feels like one surface rather than three
 * unrelated screens.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES })
    val scope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.background),
    ) { page ->
        OnboardingPage(
            page = page,
            isCurrent = pagerState.currentPage == page,
            onSkip = onFinished,
            onNext = {
                if (page == ONBOARDING_PAGES - 1) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                }
            },
        )
    }
}

@Composable
private fun OnboardingPage(
    page: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    Column(modifier = modifier.fillMaxSize()) {
    // The accent hero runs under the status bar.
    SystemBarIcons(lightStatusBarIcons = true)

        OnboardingHero {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.screenPadding)
                        .padding(top = Dimens.screenPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BrandRow()
                    HeroBadge(
                        text = stringResource(R.string.onboarding_step_badge, page + 1, ONBOARDING_PAGES)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.screenPadding)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Reveal(delayMillis = Motion.Stagger, visible = isCurrent) {
                        when (page) {
                            0 -> AskAnythingArt()
                            1 -> SolveStepsArt()
                            else -> StudyToolsArt()
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = Dimens.screenPadding, end = Dimens.screenPadding)
                .padding(top = 28.dp, bottom = Dimens.spaceXxxl),
        ) {
            StepDots(current = page)
            Reveal(delayMillis = Motion.Stagger * 2, visible = isCurrent, lift = 18.dp) {
                Column {
                    Text(
                        text = stringResource(
                            when (page) {
                                0 -> R.string.onboarding_1_title
                                1 -> R.string.onboarding_2_title
                                else -> R.string.onboarding_3_title
                            }
                        ),
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = Dimens.spaceXl),
                    )
                    Text(
                        text = stringResource(
                            when (page) {
                                0 -> R.string.onboarding_1_body
                                1 -> R.string.onboarding_2_body
                                else -> R.string.onboarding_3_body
                            }
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (page == ONBOARDING_PAGES - 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
                ) {
                    PrimaryCta(text = stringResource(R.string.onboarding_3_cta), onClick = onNext)
                    Text(
                        text = stringResource(R.string.onboarding_3_no_signup),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textTertiary,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.action_skip),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textTertiary,
                        modifier = Modifier
                            .clip(AppShapes.Pill)
                            .clickable(onClick = onSkip)
                            .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceMd),
                    )
                    CircularNextButton(
                        progress = (page + 1f) / ONBOARDING_PAGES,
                        onClick = onNext,
                        contentDescription = stringResource(R.string.cd_next),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val label = stringResource(R.string.cd_onboarding_step, current + 1, ONBOARDING_PAGES)
    Row(
        modifier = modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        repeat(ONBOARDING_PAGES) { index ->
            val active = index == current
            val width by animateDpAsState(
                targetValue = if (active) 26.dp else 8.dp,
                animationSpec = Motion.emphasized(durationMillis = Motion.Medium),
                label = "dotWidth",
            )
            val tint by androidx.compose.animation.animateColorAsState(
                targetValue = if (active) colors.accent else colors.accentRing,
                animationSpec = Motion.medium(),
                label = "dotTint",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(AppShapes.Pill)
                    .background(tint)
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun OnboardingPage1Preview() {
    AITutorTheme { OnboardingPage(page = 0, onSkip = {}, onNext = {}) }
}

@ScreenPreviews
@Composable
private fun OnboardingPage2Preview() {
    AITutorTheme { OnboardingPage(page = 1, onSkip = {}, onNext = {}) }
}

@ScreenPreviews
@Composable
private fun OnboardingPage3Preview() {
    AITutorTheme { OnboardingPage(page = 2, onSkip = {}, onNext = {}) }
}
