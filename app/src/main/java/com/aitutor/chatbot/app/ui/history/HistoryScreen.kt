package com.aitutor.chatbot.app.ui.history

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.HistoryEntry
import com.aitutor.chatbot.app.domain.model.HistoryFilter
import com.aitutor.chatbot.app.domain.model.HistoryGroup
import com.aitutor.chatbot.app.ui.components.Reveal
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.main.FilterChipRow
import com.aitutor.chatbot.app.ui.main.GlyphTile
import com.aitutor.chatbot.app.ui.main.HeroIconButton
import com.aitutor.chatbot.app.ui.main.HeroStatStrip
import com.aitutor.chatbot.app.ui.main.RowChevron
import com.aitutor.chatbot.app.ui.main.SectionOverline
import com.aitutor.chatbot.app.ui.main.TabBottomInset
import com.aitutor.chatbot.app.ui.main.TabHero
import com.aitutor.chatbot.app.ui.main.TabHeroTitle
import com.aitutor.chatbot.app.ui.main.TabSearchBar
import com.aitutor.chatbot.app.ui.main.TabSidePadding
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.SectionOverlineStyle
import com.aitutor.chatbot.app.ui.theme.SampleContent
import com.aitutor.chatbot.app.ui.theme.TallScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

private val RowShape = RoundedCornerShape(20.dp)
private val ShadowTint = Color(0xFF080C28)

/**
 * Past sessions, newest first, grouped by how recent they are. The week's totals sit on the hero so
 * the numbers are the first thing read, then the filter narrows what is listed beneath.
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onFilterChange: (HistoryFilter) -> Unit,
    onEntryClick: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

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

        TabHero(bottomPadding = 64.dp) {
            TabHeroTitle(
                title = stringResource(R.string.history_title),
                subtitle = stringResource(R.string.history_subtitle),
                action = {
                    HeroIconButton(
                        icon = AppIcons.Filter,
                        contentDescription = stringResource(R.string.cd_filter_history),
                        onClick = {},
                    )
                },
            )
            WeekRow(modifier = Modifier.padding(top = 22.dp))
            HeroStatStrip(
                stats = listOf(
                    stringResource(R.string.history_stat_questions_value) to
                        stringResource(R.string.history_stat_questions_label),
                    stringResource(R.string.history_stat_solved_value) to
                        stringResource(R.string.history_stat_solved_label),
                    stringResource(R.string.history_stat_time_value) to
                        stringResource(R.string.history_stat_time_label),
                ),
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Reveal(
            delayMillis = Motion.Stagger,
            modifier = Modifier
                .offset(y = (-28).dp)
                .padding(horizontal = TabSidePadding),
        ) {
            TabSearchBar(hint = stringResource(R.string.history_search_hint), onClick = {})
        }

        Column(
            modifier = Modifier
                .offset(y = (-28).dp)
                .padding(
                    start = TabSidePadding,
                    end = TabSidePadding,
                    top = 28.dp,
                    bottom = TabBottomInset,
                ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            FilterChipRow(
                labels = HistoryFilter.entries.map { stringResource(it.labelRes) },
                selectedIndex = state.filter.ordinal,
                onSelect = { onFilterChange(HistoryFilter.entries[it]) },
            )

            HistoryGroup.entries.forEach { group ->
                val entries = state.inGroup(group)
                // A group with nothing in it under the current filter disappears entirely rather
                // than leaving a stranded heading.
                AnimatedVisibility(
                    visible = entries.isNotEmpty(),
                    enter = fadeIn(Motion.medium()),
                    exit = fadeOut(Motion.fast()),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionOverline(text = stringResource(group.labelRes))
                        entries.forEach { entry ->
                            HistoryRow(entry = entry, onClick = { onEntryClick(entry) })
                        }
                    }
                }
            }

            // Two different kinds of nothing: no history at all reads as an invitation, while
            // nothing under the current filter is just a narrower view of what is there.
            if (state.isEmpty) {
                EmptyHistory()
            } else if (!state.loading && state.visible.isEmpty()) {
                Text(
                    text = stringResource(R.string.history_empty_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceXxl),
                )
            }
        }
    }
}

/** The week the totals cover. Overline on the left, dates on the right. */
@Composable
private fun WeekRow(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.history_this_week).uppercase(),
            style = SectionOverlineStyle,
            color = colors.onAccentMuted,
        )
        Text(
            text = stringResource(R.string.history_week_range),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
            color = colors.onAccentMuted,
        )
    }
}

/**
 * One past session. The glyph tile is filled rather than tinted here — on this screen the tool is
 * what distinguishes one row from the next, so it carries the accent.
 */
@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RowShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(RowShape)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, RowShape)
            .clickable(onClick = onClick)
            .padding(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        GlyphTile(icon = entry.tool.icon, filled = true)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                ToolChip(label = stringResource(entry.tool.labelRes))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
                ) {
                    Icon(
                        imageVector = AppIcons.Clock,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = relativeTime(entry.updatedAt),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = colors.textTertiary,
                        maxLines = 1,
                    )
                }
            }
        }
        RowChevron()
    }
}

@Composable
private fun ToolChip(label: String) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .height(22.dp)
            .clip(AppShapes.Pill)
            .background(colors.accentTint)
            .padding(horizontal = Dimens.spaceSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = colors.accentText,
            maxLines = 1,
        )
    }
}

/**
 * "12m ago", "Yesterday" and so on, from the platform rather than hand-rolled: it is already
 * translated for every locale the app can run in, and it handles the awkward boundaries.
 */
@Composable
private fun relativeTime(timestamp: Long): String = DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
).toString()

/** Shown before the first conversation exists. */
@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        GlyphTile(icon = AppIcons.HistoryClock, size = 64.dp, cornerRadius = 22.dp)
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@TallScreenPreviews
@Composable
private fun HistoryScreenPreview() {
    AITutorTheme {
        HistoryScreen(
            state = HistoryUiState(entries = SampleContent.history(), loading = false),
            onFilterChange = {},
            onEntryClick = {},
        )
    }
}

@TallScreenPreviews
@Composable
private fun HistoryEmptyPreview() {
    AITutorTheme {
        HistoryScreen(
            state = HistoryUiState(entries = emptyList(), loading = false),
            onFilterChange = {},
            onEntryClick = {},
        )
    }
}
