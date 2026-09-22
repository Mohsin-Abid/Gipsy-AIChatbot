package com.aitutor.chatbot.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.model.ChatEntry
import com.aitutor.chatbot.app.model.ToolCategory
import com.aitutor.chatbot.app.ui.components.EmptyState
import com.aitutor.chatbot.app.ui.components.ErrorState
import com.aitutor.chatbot.app.ui.components.SectionLabel
import com.aitutor.chatbot.app.ui.components.SkeletonRow
import com.aitutor.chatbot.app.ui.state.ChatHistoryTabs
import com.aitutor.chatbot.app.ui.state.ChatHistoryUiState
import com.aitutor.chatbot.app.ui.state.SampleData
import com.aitutor.chatbot.app.ui.state.tabLabel
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.FontScalePreviews
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing

@Composable
fun ChatHistoryScreen(
    state: ChatHistoryUiState,
    selectedTab: ToolCategory?,
    onTabSelected: (ToolCategory?) -> Unit,
    onEntryClick: (ChatEntry) -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .padding(top = spacing.md, bottom = spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chat History",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            val interactionSource = remember { MutableInteractionSource() }
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search chat history",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onSearchClick
                    )
            )
        }

        val selectedIndex = ChatHistoryTabs.indexOf(selectedTab).coerceAtLeast(0)
        SecondaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = spacing.lg,
        ) {
            ChatHistoryTabs.forEach { tab ->
                val isSelected = tab == selectedTab
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.tabLabel(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        AnimatedContent(
            targetState = state,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            contentKey = { it::class },
            label = "historyState"
        ) { target ->
            when (target) {
                ChatHistoryUiState.Loading -> HistoryLoading()
                is ChatHistoryUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    ErrorState(message = target.message, onRetry = onRetry)
                }
                is ChatHistoryUiState.Empty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    EmptyState(
                        title = target.title,
                        message = target.message,
                        icon = if (selectedTab == null) Icons.Outlined.History else Icons.Outlined.SearchOff
                    )
                }
                is ChatHistoryUiState.Success -> HistoryList(entries = target.entries, onEntryClick = onEntryClick)
            }
        }
    }
}

@Composable
private fun HistoryList(
    entries: List<ChatEntry>,
    onEntryClick: (ChatEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val grouped = remember(entries) { entries.groupBy { it.dateGroup } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md)
    ) {
        grouped.forEach { (dateGroup, groupEntries) ->
            item(key = "header_$dateGroup") {
                SectionLabel(text = dateGroup, modifier = Modifier.padding(top = spacing.md, bottom = spacing.xs))
            }
            items(groupEntries, key = { it.id }) { entry ->
                ChatHistoryItem(entry = entry, onClick = { onEntryClick(entry) })
                if (entry != groupEntries.last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun ChatHistoryItem(
    entry: ChatEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = spacing.md)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = entry.time,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = spacing.sm)
            )
        }
        Text(
            text = entry.toolLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = spacing.xxs)
        )
    }
}

@Composable
private fun HistoryLoading(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxSize().padding(horizontal = spacing.lg)) {
        repeat(5) { SkeletonRow() }
    }
}

// ---- Previews ----

@ThemePreviews
@Composable
private fun ChatHistoryScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatHistoryScreen(
                state = ChatHistoryUiState.Success(SampleData.history),
                selectedTab = null,
                onTabSelected = {},
                onEntryClick = {},
                onSearchClick = {},
                onRetry = {}
            )
        }
    }
}

@FontScalePreviews
@Composable
private fun ChatHistoryScreenFontScalePreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatHistoryScreen(
                state = ChatHistoryUiState.Success(SampleData.history.take(3) + SampleData.longChatEntry),
                selectedTab = null,
                onTabSelected = {},
                onEntryClick = {},
                onSearchClick = {},
                onRetry = {}
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ChatHistoryScreenLoadingPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatHistoryScreen(
                state = ChatHistoryUiState.Loading,
                selectedTab = null,
                onTabSelected = {},
                onEntryClick = {},
                onSearchClick = {},
                onRetry = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Empty — no history yet", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ChatHistoryScreenEmptyPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatHistoryScreen(
                state = ChatHistoryUiState.Empty(
                    title = "Nothing here yet",
                    message = "Ask a question with any tool and it'll show up in your history."
                ),
                selectedTab = null,
                onTabSelected = {},
                onEntryClick = {},
                onSearchClick = {},
                onRetry = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Empty — filtered tab", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ChatHistoryScreenEmptyFilteredPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatHistoryScreen(
                state = ChatHistoryUiState.Empty(
                    title = "No Problem Solving chats yet",
                    message = "Try a different tab, or ask a homework or math question."
                ),
                selectedTab = ToolCategory.ProblemSolving,
                onTabSelected = {},
                onEntryClick = {},
                onSearchClick = {},
                onRetry = {}
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Error", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ChatHistoryScreenErrorPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatHistoryScreen(
                state = ChatHistoryUiState.Error("Couldn't load your history."),
                selectedTab = null,
                onTabSelected = {},
                onEntryClick = {},
                onSearchClick = {},
                onRetry = {}
            )
        }
    }
}
