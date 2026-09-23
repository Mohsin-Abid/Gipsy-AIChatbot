package com.aitutor.chatbot.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.cardStyle
import com.aitutor.chatbot.app.core.ext.elevatedCardStyle
import com.aitutor.chatbot.app.core.ext.truncate
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.ModeRecommendation
import com.aitutor.chatbot.app.ui.components.pressScale
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.HeroGradientEnd
import com.aitutor.chatbot.app.ui.theme.HeroGradientStart
import com.aitutor.chatbot.app.ui.theme.PremiumGold
import com.aitutor.chatbot.app.ui.theme.PremiumGoldBright
import com.aitutor.chatbot.app.ui.theme.PremiumGoldSoft
import com.aitutor.chatbot.app.ui.theme.PremiumInkEnd
import com.aitutor.chatbot.app.ui.theme.PremiumInkStart
import com.aitutor.chatbot.app.ui.theme.TabularFigures
import com.aitutor.chatbot.app.ui.theme.spacing

/** The one "this is paid" marker in the app. Small, gold, never used for anything else. */
@Composable
fun ProBadge(modifier: Modifier = Modifier, dark: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (dark) PremiumGold.copy(alpha = 0.22f) else PremiumGoldSoft,
    ) {
        Text(
            text = "PRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (dark) PremiumGoldBright else PremiumGold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * The week as a chart plus the three headline numbers. Consolidated deliberately — three separate
 * cards read like a junk drawer, one reads like a report.
 */
@Composable
fun ProgressCard(
    weeklyActivity: List<DayActivity>,
    streakDays: Int,
    questionsAsked: Int,
    chatsThisWeek: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val peak = (weeklyActivity.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val barBrush = Brush.verticalGradient(listOf(HeroGradientStart, HeroGradientEnd))

    Column(modifier = modifier.fillMaxWidth().cardStyle().padding(spacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            weeklyActivity.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (day.count > 0) "${day.count}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = spacing.xxs)
                            .fillMaxWidth()
                            .height(64.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        val heightFraction = day.count.toFloat() / peak
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((64 * heightFraction).dp.coerceAtLeast(5.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (day.count > 0) {
                                        Modifier.background(barBrush)
                                    } else {
                                        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                    }
                                )
                        )
                    }
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = spacing.xs)
                    )
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = spacing.lg)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            InlineStat(value = streakDays, label = "Day streak", modifier = Modifier.weight(1f))
            InlineStat(value = questionsAsked, label = "Questions", modifier = Modifier.weight(1f))
            InlineStat(value = chatsThisWeek, label = "This week", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InlineStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            // Typography tokens carry the user's in-app font scale; only the figure style is
            // overridden here so changing digits don't shift the column's width.
            text = "$value",
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = TabularFigures),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Fast paths into the three things students actually arrive wanting to do. */
@Composable
fun QuickActionsRow(actions: List<QuickAction>, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .cardStyle(AppShapes.Card)
                    .clickable(role = Role.Button, onClick = action.onClick)
                    .padding(vertical = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Shown to free users only — the single upgrade surface on the dashboard. */
@Composable
fun UpgradeCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .elevatedCardStyle()
            .clip(AppShapes.CardLarge)
            .background(Brush.linearGradient(listOf(PremiumInkStart, PremiumInkEnd)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(spacing.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = PremiumGoldBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Go Premium",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Text(
                    text = "Unlimited questions, all 10 study tools, photo solving and voice answers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = spacing.xs, end = spacing.md)
                )
            }
            Surface(shape = CircleShape, color = PremiumGoldBright) {
                Text(
                    text = "Upgrade",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = PremiumInkStart,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
                )
            }
        }
    }
}

/** Replaces [UpgradeCard] once the Cloud Function has flipped the entitlement on. */
@Composable
fun PremiumActiveCard(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .cardStyle()
            .padding(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = PremiumGold,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Premium active",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Every tool unlocked, no daily limit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProBadge()
    }
}

/** A resumable conversation, sized for a horizontal rail rather than a full-width list row. */
@Composable
fun ContinueChatCard(
    chat: ChatSummary,
    mode: Mode?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val tint = mode?.category?.tint ?: MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .width(210.dp)
            .pressScale(interactionSource)
            .cardStyle()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            if (mode != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(tint.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = mode?.title.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = chat.title.truncate(60),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = spacing.sm)
        )
    }
}

/** A recommendation carries its reason — "For your Physics problems" — so it never feels random. */
@Composable
fun RecommendationCard(
    recommendation: ModeRecommendation,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val tint = recommendation.mode.category.tint

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
                .size(42.dp)
                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = recommendation.mode.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = recommendation.mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (locked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Premium",
                        tint = PremiumGold,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

/**
 * A study tool on the grid. Premium tools stay fully visible for free users — browsing what you
 * can't have yet is the point — but carry a PRO badge and open the paywall instead of a chat.
 */
@Composable
fun ModeTile(
    mode: Mode,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val tint = mode.category.tint

    Column(
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
            .padding(spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.08f))),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            // Only flag what the student can't reach yet — once a tool is unlocked, a "PRO" tag is
            // just noise on something they already own.
            if (locked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Premium",
                    tint = PremiumGold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            text = mode.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = spacing.md)
        )
        Text(
            text = mode.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = spacing.xxs)
        )
    }
}

/** Section heading with an optional trailing action, used above each dashboard rail. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(role = Role.Button, onClick = onActionClick)
            )
        }
    }
}
