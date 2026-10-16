package com.askylix.app.ui.history

import android.text.format.DateUtils
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.askylix.app.R
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.domain.model.HistoryEntry
import com.askylix.app.domain.model.HistoryFilter
import com.askylix.app.domain.model.HistoryGroup
import com.askylix.app.ui.components.PrimaryCta
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.main.FilterChipRow
import com.askylix.app.ui.main.GlyphTile
import com.askylix.app.ui.main.RowChevron
import com.askylix.app.ui.main.SectionOverline
import com.askylix.app.ui.main.TabBottomInset
import com.askylix.app.ui.main.TabHero
import com.askylix.app.ui.main.TabHeroTitle
import com.askylix.app.ui.main.UpgradeDiamondButton
import com.askylix.app.ui.main.TabSearchField
import com.askylix.app.ui.main.TabSidePadding
import com.askylix.app.ui.theme.AITutorTheme
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.SampleContent
import com.askylix.app.ui.theme.TallScreenPreviews
import com.askylix.app.ui.theme.SystemBarIcons
import com.askylix.app.ui.theme.appColors

private val RowShape = RoundedCornerShape(20.dp)
private val ShadowTint = Color(0xFF080C28)

/**
 * Past sessions, newest first, grouped by how recent they are. The hero is just the title; the
 * search and the filter narrow what is listed beneath it.
 *
 * Everything shown comes from the server. Signed out there is nothing to show, so the screen says so
 * and offers to sign in; while the first page is on its way a static placeholder stands in for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onFilterChange: (HistoryFilter) -> Unit,
    onEntryClick: (HistoryEntry) -> Unit,
    onSeeProPlans: () -> Unit,
    modifier: Modifier = Modifier,
    showUpgrade: Boolean = false,
    onQueryChange: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onSignIn: () -> Unit = {},
) {
    val colors = MaterialTheme.appColors

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
        // A line of text rather than the spinning default: this app does not animate.
        indicator = {
            if (state.refreshing) RefreshingNote(modifier = Modifier.align(Alignment.TopCenter))
        },
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // The page colour fills the strip behind the navigation bar, while the scrolling content
            // stops above it rather than sliding under.
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
    // The accent hero runs under the status bar.
    SystemBarIcons(lightStatusBarIcons = true)

        // Short now that it holds only a title. The search bar below overlaps the hero's bottom edge by
        // 28dp, so this padding is that overlap plus the breathing room under the subtitle.
        TabHero(bottomPadding = 60.dp) {
            TabHeroTitle(
                title = stringResource(R.string.history_title),
                subtitle = stringResource(R.string.history_subtitle),
                action = if (showUpgrade) {
                    { UpgradeDiamondButton(onClick = onSeeProPlans) }
                } else {
                    null
                },
            )
        }

        if (state.signedIn) {
            Box(
                modifier = Modifier
                    .offset(y = (-28).dp)
                    .padding(horizontal = TabSidePadding),
            ) {
                TabSearchField(
                    query = state.query,
                    onQueryChange = onQueryChange,
                    hint = stringResource(R.string.history_search_hint),
                    clearDescription = stringResource(R.string.cd_clear_search),
                )
            }
        }

        Column(
            modifier = Modifier
                .offset(y = (-28).dp)
                .padding(
                    start = TabSidePadding,
                    end = TabSidePadding,
                    top = if (state.signedIn) 28.dp else 56.dp,
                    bottom = TabBottomInset,
                ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            if (!state.signedIn) {
                SignedOutHistory(onSignIn = onSignIn)
                return@Column
            }

            FilterChipRow(
                labels = HistoryFilter.entries.map { stringResource(it.labelRes) },
                selectedIndex = state.filter.ordinal,
                onSelect = { onFilterChange(HistoryFilter.entries[it]) },
            )

            HistoryGroup.entries.forEach { group ->
                val entries = state.inGroup(group)
                // A group with nothing in it under the current filter is left out entirely rather
                // than leaving a stranded heading.
                if (entries.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionOverline(text = stringResource(group.labelRes))
                        entries.forEach { entry ->
                            HistoryRow(entry = entry, onClick = { onEntryClick(entry) })
                        }
                    }
                }
            }

            when {
                state.loading -> HistoryPlaceholder()
                state.error != null && state.visible.isEmpty() -> HistoryLoadError(
                    offline = state.error == ApiErrorCode.NoConnection || state.error == ApiErrorCode.Timeout,
                    onRetry = onRefresh,
                )
                // Two different kinds of nothing: no history at all reads as an invitation, while
                // nothing under the current filter or search is just a narrower view of what is there.
                state.isEmpty -> EmptyHistory()
                state.visible.isEmpty() -> Text(
                    text = stringResource(R.string.history_empty_filter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceXxl),
                )
            }

            if (state.hasMore) {
                TextAction(label = stringResource(R.string.history_show_more), onClick = onLoadMore, busy = state.loadingMore)
            }
        }
    }
    }
}

/** "Refreshing…", drawn at the top while the list is being fetched again. */
@Composable
private fun RefreshingNote(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Text(
        text = stringResource(R.string.refreshing),
        style = MaterialTheme.typography.labelLarge,
        color = colors.onAccent,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = Dimens.spaceSm)
            .clip(AppShapes.Pill)
            .background(colors.accentDeep)
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
    )
}

/** A centred text button: "Show more" at the foot of the list, and "Try again" under an error. [busy] shows a plain line instead. */
@Composable
private fun TextAction(label: String, onClick: () -> Unit, busy: Boolean = false) {
    val colors = MaterialTheme.appColors
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = if (busy) stringResource(R.string.loading) else label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (busy) colors.textTertiary else colors.accentText,
            modifier = Modifier
                .clip(AppShapes.Pill)
                .clickable(enabled = !busy, role = Role.Button, onClick = onClick)
                .padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceMd),
        )
    }
}

/** Stand-in rows while the first page is fetched: static blocks the shape of the real ones, then a line saying so. */
@Composable
private fun HistoryPlaceholder() {
    val colors = MaterialTheme.appColors
    var stillLoading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(3_000)
        stillLoading = true
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RowShape)
                    .background(colors.accentTint),
            )
        }
        Text(
            text = stringResource(if (stillLoading) R.string.loading_still else R.string.loading),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HistoryLoadError(offline: Boolean, onRetry: () -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Text(
            text = stringResource(if (offline) R.string.history_load_offline else R.string.history_load_error),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        TextAction(label = stringResource(R.string.chat_try_again), onClick = onRetry)
    }
}

/** What History says without an account: there is nothing to list, and one button to change that. */
@Composable
private fun SignedOutHistory(onSignIn: () -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        GlyphTile(icon = AppIcons.HistoryClock, size = 64.dp, cornerRadius = 22.dp)
        Text(
            text = stringResource(R.string.history_signed_out_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.history_signed_out_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.spaceMd))
        PrimaryCta(text = stringResource(R.string.history_signed_out_action), onClick = onSignIn, trailingArrow = false)
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
            onSeeProPlans = {},
            showUpgrade = true,
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
            onSeeProPlans = {},
            showUpgrade = true,
            onEntryClick = {},
        )
    }
}
