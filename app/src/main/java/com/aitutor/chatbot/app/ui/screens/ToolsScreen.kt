package com.aitutor.chatbot.app.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.model.StudyTool
import com.aitutor.chatbot.app.model.ToolCategory
import com.aitutor.chatbot.app.model.studyTools
import com.aitutor.chatbot.app.ui.components.EmptyState
import com.aitutor.chatbot.app.ui.components.SectionLabel
import com.aitutor.chatbot.app.ui.components.SkeletonRow
import com.aitutor.chatbot.app.ui.components.ToolCard
import com.aitutor.chatbot.app.ui.components.ToolGridCard
import com.aitutor.chatbot.app.ui.state.ToolsUiState
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.FontScalePreviews
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing

@Composable
fun ToolsScreen(
    state: ToolsUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onToolClick: (StudyTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg)
                .padding(top = spacing.xl, bottom = spacing.lg)
        ) {
            Text(
                text = "Study Tools",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Everything you need, in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.xxs)
            )
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(top = spacing.lg)
            )
        }

        when (state) {
            ToolsUiState.Loading -> Column(Modifier.padding(horizontal = spacing.lg)) {
                repeat(5) { SkeletonRow() }
            }
            is ToolsUiState.Success -> {
                val filtered = remember(query, state.tools) {
                    if (query.isBlank()) {
                        state.tools
                    } else {
                        state.tools.filter {
                            it.title.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true)
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        EmptyState(
                            title = "No tools match “$query”",
                            message = "Try “essay”, “math”, or “grammar.”",
                            icon = Icons.Outlined.SearchOff
                        )
                    }
                } else {
                    ToolsList(tools = filtered, onToolClick = onToolClick)
                }
            }
        }
    }
}

@Composable
private fun ToolsList(
    tools: List<StudyTool>,
    onToolClick: (StudyTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val featured = tools.filter { it.isFeatured }
    val grouped = tools.filterNot { it.isFeatured }.groupBy { it.category }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        if (featured.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionLabel(text = "Featured", modifier = Modifier.padding(bottom = spacing.xxs))
            }
            items(featured, span = { GridItemSpan(maxLineSpan) }, key = { it.id }) { tool ->
                ToolCard(
                    title = tool.title,
                    description = tool.description,
                    icon = tool.icon,
                    emphasized = true,
                    onClick = { onToolClick(tool) }
                )
            }
        }
        ToolCategory.entries.forEach { category ->
            val categoryTools = grouped[category].orEmpty()
            if (categoryTools.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "header_${category.name}") {
                    SectionLabel(
                        text = category.label,
                        modifier = Modifier.padding(top = spacing.md, bottom = spacing.xxs)
                    )
                }
                items(categoryTools, key = { it.id }) { tool ->
                    ToolGridCard(
                        title = tool.title,
                        description = tool.description,
                        icon = tool.icon,
                        onClick = { onToolClick(tool) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            if (query.isEmpty()) {
                Text(
                    text = "Search tools",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// ---- Previews ----

private val sampleToolsState = ToolsUiState.Success(studyTools)

@ThemePreviews
@Composable
private fun ToolsScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ToolsScreen(state = sampleToolsState, query = "", onQueryChange = {}, onToolClick = {})
        }
    }
}

@FontScalePreviews
@Composable
private fun ToolsScreenFontScalePreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ToolsScreen(state = sampleToolsState, query = "", onQueryChange = {}, onToolClick = {})
        }
    }
}

@ThemePreviews
@Composable
private fun ToolsScreenLoadingPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ToolsScreen(state = ToolsUiState.Loading, query = "", onQueryChange = {}, onToolClick = {})
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Empty — search", showBackground = true, backgroundColor = 0xFFFAF6EE)
@Composable
private fun ToolsScreenEmptySearchPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ToolsScreen(state = sampleToolsState, query = "chemistry", onQueryChange = {}, onToolClick = {})
        }
    }
}
