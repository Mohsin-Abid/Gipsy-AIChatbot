package com.askylix.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.appColors

/**
 * The large option row used for study level and goals: icon tile, title, detail, and a check on
 * the right. Selecting it tints the whole row and flips the tile to solid accent.
 */
@Composable
fun OptionCard(
    icon: ImageVector,
    title: String,
    detail: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val background = if (selected) colors.accentTint else colors.surface
    val border = if (selected) colors.accent else colors.fieldBorder
    val tileFill = if (selected) colors.accent else colors.accentTint
    val tileIcon = if (selected) colors.onAccent else colors.accentText
    val alpha = if (enabled || selected) 1f else 0.45f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(OptionCardShape)
            .background(background)
            .border(1.5.dp, border, OptionCardShape)
            .clickable(enabled = enabled || selected, onClick = onClick)
            .padding(start = Dimens.spaceLg, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(OptionTileShape)
                .background(tileFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tileIcon, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp),
                    color = colors.textTertiary,
                    maxLines = 1,
                )
            }
        }
        CheckIndicator(selected = selected)
    }
}

@Composable
internal fun CheckIndicator(selected: Boolean) {
    val colors = MaterialTheme.appColors
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        if (!selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(AppShapes.Pill)
                    .border(2.dp, colors.radioOff, AppShapes.Pill)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(AppShapes.Pill)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** The 44dp pill used for grades, subjects and study times. */
@Composable
fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.appColors
    val background = if (selected) colors.accent else colors.surface
    val border = if (selected) colors.accent else colors.fieldBorder
    val content = if (selected) colors.onAccent else colors.textPrimary
    val iconTint = if (selected) colors.onAccent else colors.accentText

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(AppShapes.Pill)
            .background(background)
            .border(1.dp, border, AppShapes.Pill)
            .clickable(onClick = onClick)
            .padding(start = if (icon != null) Dimens.spaceMd else 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
            maxLines = 1,
        )
    }
}
