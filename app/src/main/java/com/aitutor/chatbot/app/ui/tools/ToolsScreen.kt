package com.aitutor.chatbot.app.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.domain.model.ToolCategory
import com.aitutor.chatbot.app.ui.components.Reveal
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.main.CardCornerRing
import com.aitutor.chatbot.app.ui.main.FilterChipRow
import com.aitutor.chatbot.app.ui.main.GlyphTile
import com.aitutor.chatbot.app.ui.main.ListPanel
import com.aitutor.chatbot.app.ui.main.NewBadge
import com.aitutor.chatbot.app.ui.main.RowChevron
import com.aitutor.chatbot.app.ui.main.RowLabels
import com.aitutor.chatbot.app.ui.main.SectionTitleWithCount
import com.aitutor.chatbot.app.ui.main.TabBottomInset
import com.aitutor.chatbot.app.ui.main.TabHero
import com.aitutor.chatbot.app.ui.main.TabHeroTitle
import com.aitutor.chatbot.app.ui.main.TabSearchBar
import com.aitutor.chatbot.app.ui.main.TabSidePadding
import com.aitutor.chatbot.app.ui.main.dividedRows
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.OnHero
import com.aitutor.chatbot.app.ui.theme.OverlineStyle
import com.aitutor.chatbot.app.ui.theme.TallScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

private val CardShape = RoundedCornerShape(24.dp)
private val RowIconInset = 78.dp
private val ShadowTint = Color(0xFF080C28)

/** The chips on the hero: "All", then one per group. A null entry is the "All" chip. */
private val FilterCategories: List<ToolCategory?> = listOf(null) + ToolCategory.entries

/**
 * The full tool catalogue: one recommended card overlapping the hero, the two popular tools as
 * large cards, then the rest grouped into panels.
 */
@Composable
fun ToolsScreen(
    onToolClick: (StudyTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeCategory = FilterCategories[filterIndex]
    val visibleCategories = ToolCategory.entries.filter { activeCategory == null || it == activeCategory }

    Column(
        modifier = modifier
            .fillMaxSize()
            // Background first, then the inset: the page colour fills the strip behind the
            // navigation bar, while the scrolling content stops above it rather than sliding under.
            .background(colors.pageTint)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
    // The accent hero runs under the status bar.
    SystemBarIcons(lightStatusBarIcons = true)

        TabHero {
            TabHeroTitle(
                title = stringResource(R.string.tools_title),
                subtitle = stringResource(R.string.tools_subtitle, StudyTool.entries.size),
            )
            TabSearchBar(
                hint = stringResource(R.string.tools_search_hint),
                onClick = {},
                modifier = Modifier.padding(top = Dimens.spaceLg + Dimens.spaceXs),
            )
            FilterChipRow(
                labels = FilterCategories.map { category ->
                    stringResource(category?.chipLabelRes ?: R.string.filter_all)
                },
                selectedIndex = filterIndex,
                onSelect = { filterIndex = it },
                onHero = true,
                modifier = Modifier.padding(top = Dimens.spaceLg + 2.dp),
            )
        }

        Reveal(
            delayMillis = Motion.Stagger,
            modifier = Modifier
                .offset(y = (-48).dp)
                .padding(horizontal = TabSidePadding),
        ) {
            RecommendedCard(
                tool = StudyTool.recommendedTool,
                onClick = { onToolClick(StudyTool.recommendedTool) },
            )
        }

        Column(
            modifier = Modifier
                .offset(y = (-48).dp)
                .padding(
                    start = TabSidePadding,
                    end = TabSidePadding,
                    top = 28.dp,
                    bottom = TabBottomInset,
                ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            visibleCategories.forEach { category ->
                val tools = StudyTool.inCategory(category)
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
                    SectionTitleWithCount(
                        title = stringResource(category.labelRes),
                        count = tools.size,
                    )
                    if (category == ToolCategory.Popular) {
                        PopularCards(tools = tools, onToolClick = onToolClick)
                    } else {
                        ListPanel {
                            dividedRows(items = tools, inset = RowIconInset) { tool ->
                                ToolRow(tool = tool, onClick = { onToolClick(tool) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The single promoted tool. Sits over the hero's lower edge, like Home's "continue" card. */
@Composable
private fun RecommendedCard(
    tool: StudyTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, CardShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(CardShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.cardBorder, CardShape)
            .clickable(onClick = onClick)
            .padding(Dimens.spaceLg + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        GlyphTile(icon = tool.icon, size = 56.dp, cornerRadius = 18.dp, filled = true)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Spark,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = stringResource(R.string.tools_recommended).uppercase(),
                    style = OverlineStyle,
                    color = colors.accentText,
                )
            }
            Text(
                text = stringResource(tool.labelRes),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(tool.taglineRes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
            )
        }
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(AppShapes.Pill)
                .background(colors.accent)
                .clickable(onClick = onClick)
                .padding(horizontal = Dimens.spaceLg + 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.action_start),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onAccent,
            )
        }
    }
}

@Composable
private fun PopularCards(
    tools: List<StudyTool>,
    onToolClick: (StudyTool) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
        tools.forEachIndexed { index, tool ->
            PopularCard(
                tool = tool,
                // The second card is the deepened accent, so a pair reads as one object with a fold.
                deep = index % 2 == 1,
                onClick = { onToolClick(tool) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PopularCard(
    tool: StudyTool,
    deep: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .height(156.dp)
            .shadow(14.dp, CardShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(CardShape)
            .background(if (deep) colors.accentDeep else colors.accent)
            .clickable(onClick = onClick),
    ) {
        CardCornerRing()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spaceLg + 2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OnHero.Fill)
                        .border(1.dp, OnHero.StrokeSoft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(AppShapes.Pill)
                        .background(OnHero.Fill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowUpRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(tool.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(tool.taglineRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.onAccentMuted,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun ToolRow(tool: StudyTool, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(onClick = onClick)
            .padding(start = Dimens.spaceLg + 2.dp, end = Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        GlyphTile(icon = tool.icon)
        RowLabels(
            title = stringResource(tool.labelRes),
            subtitle = stringResource(tool.taglineRes),
            modifier = Modifier.weight(1f),
            badge = if (tool.isNew) {
                { NewBadge(text = stringResource(R.string.badge_new)) }
            } else {
                null
            },
        )
        RowChevron()
    }
}

@TallScreenPreviews
@Composable
private fun ToolsScreenPreview() {
    AITutorTheme { ToolsScreen(onToolClick = {}) }
}
