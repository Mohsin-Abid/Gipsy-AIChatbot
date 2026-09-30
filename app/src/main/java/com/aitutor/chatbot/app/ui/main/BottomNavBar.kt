package com.aitutor.chatbot.app.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.appColors

private val BarShape = RoundedCornerShape(34.dp)
private val TabShape = RoundedCornerShape(26.dp)

/**
 * The floating navigation bar. Only the selected tab shows its label, expanding into a white pill
 * — so the bar stays compact while still naming where you are.
 */
@Composable
fun BottomNavBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 22.dp,
                shape = BarShape,
                ambientColor = if (colors.isDark) Color.Black else colors.accentDeep,
                spotColor = if (colors.isDark) Color.Black else colors.accentDeep,
            )
            .clip(BarShape)
            .background(if (colors.isDark) colors.surfaceRaised else colors.accentDeep)
            .then(
                if (colors.isDark) Modifier.border(1.dp, colors.cardBorder, BarShape) else Modifier
            )
            .height(68.dp)
            .padding(Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MainTab.entries.forEach { tab ->
            NavTab(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun NavTab(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    val content by animateColorAsState(
        targetValue = if (selected) colors.accent else Color.White.copy(alpha = 0.66f),
        animationSpec = Motion.medium(),
        label = "navContent",
    )
    val background by animateColorAsState(
        targetValue = if (selected) Color.White else Color.Transparent,
        animationSpec = Motion.medium(),
        label = "navBackground",
    )
    val label = stringResource(tab.labelRes)
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .height(52.dp)
            .clip(TabShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = if (selected) 16.dp else 0.dp)
            .then(if (selected) Modifier else Modifier.width(64.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = tab.selectedIcon,
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(Motion.medium()) + expandHorizontally(Motion.medium()),
                exit = fadeOut(Motion.fast()) + shrinkHorizontally(Motion.fast()),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = content,
                    modifier = Modifier.padding(start = Dimens.spaceSm, end = Dimens.spaceXxs),
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = label,
                    tint = content,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = content,
                    maxLines = 1,
                )
            }
        }
    }
}
