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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.model.ChatEntry
import com.aitutor.chatbot.app.model.StudyTool
import com.aitutor.chatbot.app.ui.components.ChalkRow
import com.aitutor.chatbot.app.ui.components.ChatInputBar
import com.aitutor.chatbot.app.ui.components.ErrorState
import com.aitutor.chatbot.app.ui.components.InitialAvatar
import com.aitutor.chatbot.app.ui.components.SectionLabel
import com.aitutor.chatbot.app.ui.components.SkeletonBlock
import com.aitutor.chatbot.app.ui.components.SkeletonRow
import com.aitutor.chatbot.app.ui.components.ToolIcon
import com.aitutor.chatbot.app.ui.components.pressScale
import com.aitutor.chatbot.app.ui.state.HomeUiState
import com.aitutor.chatbot.app.ui.state.SampleData
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.FontScalePreviews
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onToolClick: (StudyTool) -> Unit,
    onHistoryItemClick: (ChatEntry) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val spacing = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        HomeTopBar(onOpenSettings = onOpenSettings)

        ChatInputBar(
            value = draft,
            onValueChange = { draft = it },
            onSend = {
                if (draft.isNotBlank()) {
                    onSendMessage(draft)
                    draft = ""
                }
            },
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
        )

        AnimatedContent(
            targetState = state,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            contentKey = { it::class },
            label = "homeState"
        ) { target ->
            when (target) {
                HomeUiState.Loading -> HomeLoading()
                is HomeUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    ErrorState(message = target.message, onRetry = onRetry)
                }
                is HomeUiState.Success -> HomeBody(
                    state = target,
                    onToolClick = onToolClick,
                    onHistoryItemClick = onHistoryItemClick,
                    onSeeAllHistory = onSeeAllHistory
                )
            }
        }
    }
}

@Composable
private fun HomeTopBar(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg)
            .padding(top = spacing.md, bottom = spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = SampleData.Greeting,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        InitialAvatar(
            initial = SampleData.profile.initial,
            contentDescription = "Open settings",
            size = 32.dp,
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun HomeBody(
    state: HomeUiState.Success,
    onToolClick: (StudyTool) -> Unit,
    onHistoryItemClick: (ChatEntry) -> Unit,
    onSeeAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val hairline = MaterialTheme.colorScheme.outlineVariant

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = spacing.lg)
    ) {
        item {
            SectionLabel(
                text = "Quick tools",
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(spacing.lg)
            ) {
                items(state.quickTools, key = { it.id }) { tool ->
                    QuickToolShortcut(tool = tool, onClick = { onToolClick(tool) })
                }
            }
        }

        item {
            HorizontalDivider(
                color = hairline,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xl)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel(text = "Recent")
                if (state.recentHistory.isNotEmpty()) {
                    TextButton(onClick = onSeeAllHistory) {
                        Text("See all", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (state.recentHistory.isEmpty()) {
            item {
                Text(
                    text = "Nothing here yet — try one of the tools above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm)
                )
            }
        } else {
            items(state.recentHistory, key = { it.id }) { entry ->
                ChalkRow(
                    title = entry.title,
                    description = entry.toolLabel,
                    trailingText = entry.time,
                    onClick = { onHistoryItemClick(entry) },
                    showChevron = false,
                    modifier = Modifier.padding(horizontal = spacing.lg)
                )
                if (entry != state.recentHistory.last()) {
                    HorizontalDivider(color = hairline, modifier = Modifier.padding(horizontal = spacing.lg))
                }
            }
        }
    }
}

@Composable
private fun QuickToolShortcut(
    tool: StudyTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(72.dp)
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        ToolIcon(icon = tool.icon, size = 52.dp, iconSize = 24.dp)
        Text(
            text = tool.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxSize().padding(horizontal = spacing.lg, vertical = spacing.lg)) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth(0.3f))
        Row(
            modifier = Modifier.padding(top = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            repeat(4) { SkeletonBlock(modifier = Modifier.width(52.dp), height = 52.dp) }
        }
        SkeletonBlock(modifier = Modifier.padding(top = spacing.xl).fillMaxWidth(0.25f))
        repeat(3) { SkeletonRow() }
    }
}

// ---- Previews ----

private val sampleSuccess = HomeUiState.Success(
    greeting = SampleData.Greeting,
    quickTools = SampleData.quickTools,
    recentHistory = SampleData.recentHistory
)

@ThemePreviews
@Composable
private fun HomeScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(sampleSuccess, {}, {}, {}, {}, {}, {})
        }
    }
}

@FontScalePreviews
@Composable
private fun HomeScreenFontScalePreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(sampleSuccess, {}, {}, {}, {}, {}, {})
        }
    }
}

@ThemePreviews
@Composable
private fun HomeScreenLoadingPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(HomeUiState.Loading, {}, {}, {}, {}, {}, {})
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Error", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun HomeScreenErrorPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(HomeUiState.Error("Couldn't load your home screen."), {}, {}, {}, {}, {}, {})
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Partial data — no history yet", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun HomeScreenNoHistoryPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(sampleSuccess.copy(recentHistory = emptyList()), {}, {}, {}, {}, {}, {})
        }
    }
}
