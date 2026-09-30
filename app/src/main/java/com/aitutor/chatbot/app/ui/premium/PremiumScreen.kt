package com.aitutor.chatbot.app.ui.premium

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.PlanOffer
import com.aitutor.chatbot.app.domain.model.ProBenefit
import com.aitutor.chatbot.app.domain.model.ProPlan
import com.aitutor.chatbot.app.ui.components.Reveal
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppInsets
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.OnHero
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

private val SidePadding = 20.dp
private val HeroShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val PlanShape = RoundedCornerShape(22.dp)
private val CrownShape = RoundedCornerShape(24.dp)
private val ShadowTint = Color(0xFF080C28)

/** What the paywall draws. */
data class PremiumUiState(
    val offers: List<PlanOffer> = ProPlan.entries.map(::PlanOffer),
    val selected: ProPlan = ProPlan.entries.first { it.bestValue },
    val purchasing: Boolean = false,
) {
    val selectedOffer: PlanOffer? get() = offers.firstOrNull { it.plan == selected }

    /**
     * Buying is only possible once Play has quoted a price. There is no fallback: starting a
     * purchase flow for a product the store has not confirmed is how you sell something at a price
     * you never showed.
     */
    val canPurchase: Boolean get() = selectedOffer?.isAvailable == true && !purchasing
}

/**
 * The Pro paywall.
 *
 * Nothing here grants anything. Choosing a plan and pressing the button hands off to Play Billing;
 * entitlement is only ever read back after a purchase token has been verified server-side, never
 * minted on the device — a client that can promote itself to Pro is a client that will be asked to.
 */
@Composable
fun PremiumScreen(
    state: PremiumUiState,
    onClose: () -> Unit,
    onSelectPlan: (ProPlan) -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
    // The accent hero runs under the status bar.
    SystemBarIcons(lightStatusBarIcons = true)

        // The design fits on one screen at 390×844. Scrolling the middle rather than the whole page
        // keeps it that way there and stops the plans being unreachable on a shorter device.
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Stops above the navigation bar; the page colour behind it comes from the Box.
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            PremiumHero(onClose = onClose, onRestore = onRestore)

            Reveal(
                delayMillis = Motion.Stagger,
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .padding(horizontal = SidePadding),
            ) {
                BenefitsCard()
            }

            Column(
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .padding(
                        start = SidePadding,
                        end = SidePadding,
                        top = 22.dp,
                        // Clears the fixed purchase bar. The navigation bar is already excluded
                        // from this scrolling viewport, so adding it here would count it twice.
                        bottom = 180.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg + 2.dp),
            ) {
                Text(
                    text = stringResource(R.string.pro_choose_plan),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                    state.offers.forEach { offer ->
                        PlanCard(
                            offer = offer,
                            selected = offer.plan == state.selected,
                            onClick = { onSelectPlan(offer.plan) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        PurchaseBar(
            state = state,
            onPurchase = onPurchase,
            onRestore = onRestore,
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ---- Hero ----

@Composable
private fun PremiumHero(onClose: () -> Unit, onRestore: () -> Unit) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(colors.hero),
    ) {
        // Unlike the tab heroes, this glow is centred and lights the crown from behind.
        Canvas(modifier = Modifier.matchParentSize()) {
            val glowCenter = Offset(size.width / 2f, 40.dp.toPx() + 180.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = glowCenter,
                    radius = 180.dp.toPx(),
                ),
                radius = 180.dp.toPx(),
                center = glowCenter,
            )
            val hairline = Stroke(width = 1.dp.toPx())
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = 190.dp.toPx(),
                center = Offset(40.dp.toPx(), size.height + 30.dp.toPx()),
                style = hairline,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = 160.dp.toPx(),
                center = Offset(size.width + 30.dp.toPx(), (-10).dp.toPx()),
                style = hairline,
            )
        }

        Column(
            // The accent panel reaches the top of the display; the close row starts below the clock.
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = SidePadding,
                    end = SidePadding,
                    top = 20.dp,
                    bottom = 60.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppShapes.Pill)
                        .background(OnHero.FillSoft)
                        .border(1.dp, OnHero.StrokeSoft, AppShapes.Pill)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.pro_restore),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    modifier = Modifier
                        .clip(AppShapes.Pill)
                        .clickable(onClick = onRestore)
                        .padding(horizontal = Dimens.spaceMd, vertical = Dimens.spaceSm),
                )
            }

            Reveal(lift = 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CrownMark(modifier = Modifier.padding(top = Dimens.spaceXs))
                    Text(
                        text = stringResource(R.string.pro_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        text = stringResource(R.string.pro_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                        ),
                        color = colors.onAccentMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Dimens.spaceXs),
                    )
                }
            }
        }
    }
}

/** The 76dp white tile, ringed by a soft halo rather than a hard border. */
@Composable
private fun CrownMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier.size(92.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                cornerRadius = CornerRadius(32.dp.toPx()),
            )
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(20.dp, CrownShape, ambientColor = ShadowTint, spotColor = ShadowTint)
                .clip(CrownShape)
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Crown,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

// ---- Benefits ----

@Composable
private fun BenefitsCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, CardShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(CardShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.cardBorder, CardShape)
            .padding(horizontal = Dimens.spaceLg + 2.dp, vertical = Dimens.spaceXxs),
    ) {
        ProBenefit.entries.forEachIndexed { index, benefit ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 54.dp)
                        .height(1.dp)
                        .background(colors.cardBorder)
                )
            }
            BenefitRow(benefit = benefit)
        }
    }
}

@Composable
private fun BenefitRow(benefit: ProBenefit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = benefit.icon,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = stringResource(benefit.titleRes),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(benefit.detailRes),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.textTertiary,
            )
        }
        Icon(
            imageVector = AppIcons.Check,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ---- Plans ----

@Composable
private fun PlanCard(
    offer: PlanOffer,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val background by animateColorAsState(
        targetValue = if (selected) colors.accentTint else colors.surface,
        animationSpec = Motion.medium(),
        label = "planBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.fieldBorder,
        animationSpec = Motion.medium(),
        label = "planBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.5.dp,
        animationSpec = Motion.medium(),
        label = "planBorderWidth",
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clip(PlanShape)
                .background(background)
                .border(borderWidth, borderColor, PlanShape)
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 16.dp, top = Dimens.spaceXl, bottom = Dimens.spaceLg),
        ) {
            Text(
                text = stringResource(offer.plan.labelRes),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.15).sp,
                ),
                color = colors.textPrimary,
            )
            Row(
                modifier = Modifier.padding(top = Dimens.spaceSm),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
            ) {
                Text(
                    // A dash until Play quotes a price, rather than a number this app invented.
                    text = offer.formattedPrice ?: stringResource(R.string.pro_price_unknown),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 25.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.75).sp,
                    ),
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(offer.plan.periodRes),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.textTertiary,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                // The yearly plan shows what it works out to per month, which is the comparison
                // that sells it; without a price from Play it falls back to its own billing line
                // rather than borrowing the monthly plan's.
                text = offer.formattedPerMonth?.let {
                    stringResource(R.string.pro_plan_per_month_equivalent, it)
                } ?: stringResource(offer.plan.footnoteRes),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (selected) colors.accentText else colors.textTertiary,
            )
        }

        PlanCheck(
            selected = selected,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
        )

        if (offer.plan.bestValue) {
            BestValueBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp)
            )
        }
    }
}

@Composable
private fun PlanCheck(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier.size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        // The ring stays put and the filled disc grows into it, so selection reads as one movement
        // rather than two shapes swapping.
        if (!selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(AppShapes.Pill)
                    .border(2.dp, colors.radioOff, AppShapes.Pill)
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(Motion.emphasized(durationMillis = Motion.Fast), initialScale = 0.5f),
            exit = scaleOut(Motion.fast(), targetScale = 0.5f),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(AppShapes.Pill)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun BestValueBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .height(24.dp)
            .shadow(6.dp, AppShapes.Pill, ambientColor = colors.accent, spotColor = colors.accent)
            .clip(AppShapes.Pill)
            .background(colors.accent)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.pro_best_value),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.84.sp,
            ),
            color = colors.onAccent,
            maxLines = 1,
        )
    }
}

// ---- Purchase bar ----

@Composable
private fun PurchaseBar(
    state: PremiumUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                // Fades in rather than cutting a hard edge, so the plans scroll away underneath it.
                Brush.verticalGradient(
                    0f to colors.pageTint.copy(alpha = 0f),
                    0.3f to colors.pageTint,
                )
            )
            .navigationBarsPadding()
            .padding(start = SidePadding, end = SidePadding, top = 26.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val ctaBackground by animateColorAsState(
            targetValue = if (state.canPurchase) colors.accent else colors.accent.copy(alpha = 0.4f),
            animationSpec = Motion.medium(),
            label = "ctaBackground",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ctaHeight)
                .shadow(14.dp, AppShapes.Button, ambientColor = colors.accent, spotColor = colors.accent)
                .clip(AppShapes.Button)
                .background(ctaBackground)
                .clickable(enabled = state.canPurchase, onClick = onPurchase),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(
                    R.string.pro_continue_with,
                    stringResource(state.selected.labelRes),
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.16).sp,
                ),
                color = colors.onAccent,
            )
            Icon(
                imageVector = AppIcons.ArrowRight,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(18.dp),
            )
        }

        Text(
            // Says plainly that this renews and where to stop it — Play requires it, and a reader
            // deserves it before they tap, not after.
            text = stringResource(state.selected.renewalRes),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            FinePrintLink(text = stringResource(R.string.pro_terms), onClick = onOpenTerms)
            FinePrintSeparator()
            FinePrintLink(text = stringResource(R.string.pro_privacy), onClick = onOpenPrivacy)
            FinePrintSeparator()
            FinePrintLink(text = stringResource(R.string.pro_restore_purchase), onClick = onRestore)
        }
    }
}

@Composable
private fun FinePrintLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.appColors.accentText,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceXxs, vertical = Dimens.spaceXxs),
    )
}

@Composable
private fun FinePrintSeparator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.appColors.textTertiary,
    )
}

/** Prices as Play would return them, so the previews render the design rather than dashes. */
private val previewOffers = listOf(
    PlanOffer(ProPlan.Yearly, formattedPrice = "$39.99", formattedPerMonth = "$3.33"),
    PlanOffer(ProPlan.Monthly, formattedPrice = "$6.99"),
)

@ScreenPreviews
@Composable
private fun PremiumScreenPreview() {
    AITutorTheme {
        PremiumScreen(
            state = PremiumUiState(offers = previewOffers),
            onClose = {},
            onSelectPlan = {},
            onPurchase = {},
            onRestore = {},
            onOpenTerms = {},
            onOpenPrivacy = {},
        )
    }
}

@ScreenPreviews
@Composable
private fun PremiumMonthlySelectedPreview() {
    AITutorTheme {
        PremiumScreen(
            state = PremiumUiState(offers = previewOffers, selected = ProPlan.Monthly),
            onClose = {},
            onSelectPlan = {},
            onPurchase = {},
            onRestore = {},
            onOpenTerms = {},
            onOpenPrivacy = {},
        )
    }
}

/** How the screen looks before Play has answered — which is how it looks today. */
@ScreenPreviews
@Composable
private fun PremiumNoPricesPreview() {
    AITutorTheme {
        PremiumScreen(
            state = PremiumUiState(),
            onClose = {},
            onSelectPlan = {},
            onPurchase = {},
            onRestore = {},
            onOpenTerms = {},
            onOpenPrivacy = {},
        )
    }
}
