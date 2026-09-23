package com.aitutor.chatbot.app.ui.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.cardStyle
import com.aitutor.chatbot.app.core.ext.toast
import com.aitutor.chatbot.app.domain.model.premiumBenefits
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.PremiumGold
import com.aitutor.chatbot.app.ui.theme.PremiumGoldBright
import com.aitutor.chatbot.app.ui.theme.PremiumInkEnd
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
    Monthly("premium_monthly", "Monthly", "$9.99", "billed every month"),
    Yearly("premium_yearly", "Yearly", "$59.99", "$5.00 / month, billed yearly", badge = "Best value"),
}

private data class ComparisonRow(val label: String, val free: String?, val premium: String)

private val comparisonRows = listOf(
    ComparisonRow("Questions per day", "10", "Unlimited"),
    ComparisonRow("Study tools", "5 of 10", "All 10"),
    ComparisonRow("Photo & PDF solving", null, "Included"),
    ComparisonRow("Voice answers", null, "Included"),
    ComparisonRow("Response speed", "Standard", "Priority"),
)

@Composable
fun PremiumScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedPlan by remember { mutableStateOf(PremiumPlan.Yearly) }
    var purchasing by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 132.dp),
        ) {
            item { PremiumHeader(onBack = onBack) }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    premiumBenefits.forEach { benefit ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = PremiumGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = benefit.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = benefit.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                ComparisonTable(modifier = Modifier.padding(horizontal = spacing.lg))
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    PremiumPlan.entries.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            selected = plan == selectedPlan,
                            onClick = { selectedPlan = plan },
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Cancel anytime in Google Play. Subscriptions renew automatically until cancelled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xl)
                )
            }
        }

        SubscribeBar(
            plan = selectedPlan,
            purchasing = purchasing,
            onSubscribe = {
                purchasing = true
                // Phase 5 replaces this with the Play Billing flow; the purchase is then verified
                // by a Cloud Function which is what actually flips users/{uid}.isPremium.
                context.toast("Billing isn't connected yet — coming in the next update")
                purchasing = false
            },
            onRestore = { context.toast("Nothing to restore yet") },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PremiumHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(PremiumInkStart, PremiumInkEnd)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = spacing.xxl)
    ) {
        Column {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.lg)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(PremiumGoldBright.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = PremiumGoldBright,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "AI Tutor Premium",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    modifier = Modifier.padding(top = spacing.lg)
                )
                Text(
                    text = "Every tool, no daily limits, and answers that keep up with you.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = spacing.xs)
                )
            }
        }
    }
}

@Composable
private fun ComparisonTable(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val hairline = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth().cardStyle().padding(spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "",
                modifier = Modifier.weight(1.4f),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = "Free",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Premium",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = PremiumGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        comparisonRows.forEach { row ->
            HorizontalDivider(color = hairline, modifier = Modifier.padding(vertical = spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1.4f)
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (row.free == null) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Not included",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = row.free,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Text(
                    text = row.premium,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (plan.badge != null) {
                        Surface(shape = CircleShape, color = PremiumGold) {
                            Text(
                                text = plan.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = plan.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = plan.price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SubscribeBar(
    plan: PremiumPlan,
    purchasing: Boolean,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onSubscribe,
                enabled = !purchasing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            ) {
                if (purchasing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "Start Premium — ${plan.price}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            TextButton(onClick = onRestore) {
                Text("Restore purchases", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
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
