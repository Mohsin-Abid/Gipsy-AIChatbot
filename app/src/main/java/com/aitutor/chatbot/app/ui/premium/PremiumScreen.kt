package com.aitutor.chatbot.app.ui.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.aitutor.chatbot.app.core.ext.elevatedCardStyle
import com.aitutor.chatbot.app.core.ext.toast
import com.aitutor.chatbot.app.domain.model.premiumBenefits
import com.aitutor.chatbot.app.ui.components.heroBrush
import com.aitutor.chatbot.app.ui.components.heroSheen
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.PremiumGoldBright
import com.aitutor.chatbot.app.ui.theme.PremiumInkStart
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing

/**
 * Plan options. Prices here are placeholders for layout only — once Play Billing is wired up
 * (Phase 5) these come from `ProductDetails`, which is the only source allowed to state a price.
 */
private enum class PremiumPlan(
    val productId: String,
    val title: String,
    val price: String,
    val caption: String,
    val badge: String? = null,
) {
    Monthly("premium_monthly", "MONTHLY", "$9.99", "billed monthly"),
    Yearly("premium_yearly", "YEARLY", "$59.99", "$5.00 / month", badge = "SAVE 50%"),
}

/**
 * The upgrade screen. Everything sits on one page with no scrolling — a paywall that makes people
 * hunt for the price reads as evasive, and the whole offer fitting in a single glance is itself a
 * signal of confidence.
 *
 * It's painted in the app's own hero gradient rather than the separate ink-and-gold treatment it
 * used to have. That earlier version was legible but read as a different product; the one place
 * the app asks for money is the last place it should look unfamiliar. Gold survives as an accent
 * on the benefit ticks and the savings badge, which is enough to say "this is the paid tier".
 */
@Composable
fun PremiumScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf(PremiumPlan.Yearly) }
    var purchasing by remember { mutableStateOf(false) }

    LightStatusBarIcons()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(heroBrush())
            .heroSheen(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = spacing.xl),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            BrandBlock()

            Spacer(modifier = Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                // Four lines, not five: the fifth ("Priority responses") moved into the subtitle so
                // the whole offer still lands above the fold on a short phone.
                premiumBenefits.take(4).forEach { benefit ->
                    BenefitRow(title = benefit.title, detail = benefit.detail)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                PremiumPlan.entries.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = plan == selectedPlan,
                        onClick = { selectedPlan = plan },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SubscribeBlock(
                plan = selectedPlan,
                purchasing = purchasing,
                onSubscribe = {
                    purchasing = true
                    // Phase 5 replaces this with the Play Billing flow; the purchase is then
                    // verified by a Cloud Function, which is what actually flips
                    // users/{uid}.isPremium — the client is never trusted to set it.
                    context.toast("Billing isn't connected yet — coming in the next update")
                    purchasing = false
                },
                onRestore = { context.toast("Nothing to restore yet") },
                modifier = Modifier.padding(top = spacing.lg, bottom = spacing.sm),
            )
        }
    }
}

@Composable
private fun BrandBlock(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = PremiumGoldBright,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = "PREMIUM",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.2.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = PremiumGoldBright,
            modifier = Modifier.padding(top = spacing.lg),
        )
        Text(
            text = "Study without limits",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.padding(top = spacing.xs),
        )
        Text(
            text = "Every tool, no daily cap, and priority answers even at peak times.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = spacing.sm),
        )
    }
}

@Composable
private fun BenefitRow(title: String, detail: String) {
    val spacing = MaterialTheme.spacing
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PremiumGoldBright.copy(alpha = 0.18f))
                .border(1.dp, PremiumGoldBright.copy(alpha = 0.32f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PremiumGoldBright,
                modifier = Modifier.size(13.dp),
            )
        }
        Column(modifier = Modifier.padding(start = spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val fill by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (selected) 0.22f else 0.10f),
        animationSpec = tween(220),
        label = "planFill",
    )
    val borderColor by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (selected) 0.90f else 0.18f),
        animationSpec = tween(220),
        label = "planBorder",
    )

    Column(
        modifier = modifier
            .clip(AppShapes.CardLarge)
            .background(fill)
            .border(if (selected) 2.dp else 1.dp, borderColor, AppShapes.CardLarge)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.md, horizontal = spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Both cards reserve the badge's height so the two prices stay on the same baseline.
        Box(modifier = Modifier.height(18.dp), contentAlignment = Alignment.Center) {
            if (plan.badge != null) {
                Text(
                    text = plan.badge,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = PremiumInkStart,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PremiumGoldBright)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = plan.title,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = spacing.sm),
        )
        Text(
            text = plan.price,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = plan.caption,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.68f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A white button on the gradient, rather than another gradient on a gradient. It's the highest
 * contrast available here, so the one thing the screen wants you to do is also the brightest.
 */
@Composable
private fun SubscribeBlock(
    plan: PremiumPlan,
    purchasing: Boolean,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .elevatedCardStyle(CircleShape, 14.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(enabled = !purchasing, onClick = onSubscribe),
            contentAlignment = Alignment.Center,
        ) {
            if (purchasing) {
                CircularProgressIndicator(
                    color = PremiumInkStart,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                AnimatedContent(
                    targetState = plan,
                    transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(140))) },
                    label = "subscribeLabel",
                ) { target ->
                    Text(
                        text = "Start Premium — ${target.price}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumInkStart,
                    )
                }
            }
        }
        Text(
            text = "Restore purchases",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.82f),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onRestore)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
        )
        Text(
            text = "Cancel anytime in Google Play. Renews automatically until cancelled.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The gradient runs behind the status bar, so its icons have to be light while this screen is up.
 * Restored on dispose, so whatever it was pushed on top of gets its own icons back.
 */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && previous != null) {
                controller.isAppearanceLightStatusBars = previous
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@ThemePreviews
@Composable
private fun PremiumScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PremiumScreen(onBack = {})
        }
    }
}
