package com.aitutor.chatbot.app.ui.setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.components.PrimaryCta
import com.aitutor.chatbot.app.ui.components.Reveal
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.AppInsets
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

const val SETUP_STEPS = 4

private val SidePadding = 20.dp

/**
 * The frame all four setup steps share: back arrow, a four-segment progress bar, skip, then the
 * step's own copy, and a floating action bar at the bottom.
 *
 * The bar sits over the content behind a gradient fade rather than a divider, so a long list looks
 * like it continues underneath instead of being cut off.
 */
@Composable
fun SetupScaffold(
    step: Int,
    title: String,
    subtitle: String,
    ctaText: String,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    ctaEnabled: Boolean = true,
    bottomHint: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
    // A plain surface sits under the status bar, so the icons follow the theme.
    SystemBarIcons(lightStatusBarIcons = colors.isDark)

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            SetupTopBar(step = step, onBack = onBack, onSkip = onSkip)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Reveal(modifier = Modifier.padding(start = SidePadding, end = SidePadding, top = Dimens.spaceLg)) {
                    Column {
                        Text(
                            text = stringResource(R.string.setup_step_label, step, SETUP_STEPS).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.84.sp,
                            ),
                            color = colors.accentText,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.displaySmall,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = Dimens.spaceSm),
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = Dimens.spaceSm),
                        )
                    }
                }

                Reveal(
                    delayMillis = Motion.Stagger,
                    modifier = Modifier.padding(start = SidePadding, end = SidePadding, top = 20.dp),
                ) {
                    Column(content = content)
                }

                // Clears the floating action bar so nothing hides under it.
                // Keeps the last field clear of the floating action bar and the nav bar below it.
                Box(modifier = Modifier.height(140.dp + AppInsets.bottom))
            }
        }

        SetupActionBar(
            ctaText = ctaText,
            ctaEnabled = ctaEnabled,
            onContinue = onContinue,
            bottomHint = bottomHint,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SetupTopBar(step: Int, onBack: () -> Unit, onSkip: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 12.dp, end = SidePadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(AppShapes.Pill)
                .background(colors.accentTint)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.ChevronLeft,
                contentDescription = stringResource(R.string.cd_back),
                tint = colors.accentText,
                modifier = Modifier.size(20.dp),
            )
        }

        StepProgress(step = step, modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.action_skip),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.textTertiary,
            modifier = Modifier
                .clip(AppShapes.Pill)
                .clickable(onClick = onSkip)
                .padding(vertical = 10.dp, horizontal = Dimens.spaceXxs),
        )
    }
}

/** Four equal segments that fill in as the flow advances, each easing rather than snapping. */
@Composable
private fun StepProgress(step: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val label = stringResource(R.string.setup_step_label, step, SETUP_STEPS)
    Row(
        modifier = modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        repeat(SETUP_STEPS) { index ->
            val tint by animateColorAsState(
                targetValue = if (index < step) colors.accent else colors.accentRing,
                animationSpec = Motion.medium(),
                label = "segmentTint",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(AppShapes.Pill)
                    .background(tint)
            )
        }
    }
}

@Composable
private fun SetupActionBar(
    ctaText: String,
    ctaEnabled: Boolean,
    onContinue: () -> Unit,
    bottomHint: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to colors.pageTint.copy(alpha = 0f),
                    0.40f to colors.pageTint,
                )
            )
            .navigationBarsPadding()
            .padding(start = SidePadding, end = SidePadding, top = 40.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        bottomHint?.invoke()
        PrimaryCta(text = ctaText, onClick = onContinue, enabled = ctaEnabled)
    }
}

/** A labelled field heading, with the design's optional-suffix treatment. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, optional: Boolean = false) {
    val colors = MaterialTheme.appColors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )
        if (optional) {
            Text(
                text = " " + stringResource(R.string.setup_optional),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                color = colors.textTertiary,
            )
        }
    }
}

internal val OptionCardShape = RoundedCornerShape(20.dp)
internal val OptionTileShape = RoundedCornerShape(14.dp)
internal val SetupSidePadding = SidePadding
