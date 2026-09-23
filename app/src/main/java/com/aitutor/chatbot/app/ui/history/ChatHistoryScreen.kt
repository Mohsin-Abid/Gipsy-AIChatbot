package com.aitutor.chatbot.app.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.cardStyle
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.core.ext.truncate
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.ModeCategory
import com.aitutor.chatbot.app.domain.model.modes
import com.aitutor.chatbot.app.ui.components.GradientHeader
import com.aitutor.chatbot.app.ui.components.SkeletonRow
import com.aitutor.chatbot.app.ui.components.pressScale
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.HeroGradientEnd
import com.aitutor.chatbot.app.ui.theme.HeroGradientStart
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun ChatHistoryRoute(
    onChatClick: (ChatSummary) -> Unit,
    viewModel: ChatHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsLifecycleAwareState()
    val query by viewModel.searchQuery.collectAsLifecycleAwareState()
    val category by viewModel.selectedCategory.collectAsLifecycleAwareState()

    ChatHistoryScreen(
        state = state,
        query = query,
        selectedCategory = category,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onChatClick = onChatClick,
        onDeleteChat = viewModel::onDeleteChat,
    )
}

@Composable
fun ChatHistoryScreen(
    state: UiState<ChatHistoryData>,
    query: String,
    selectedCategory: ModeCategory?,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (ModeCategory?) -> Unit,
    onChatClick: (ChatSummary) -> Unit,
    onDeleteChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val darkTheme = isSystemInDarkTheme()
    val sidePadding = Modifier.padding(horizontal = spacing.lg)
    val data = (state as? UiState.Success)?.data

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "header") {
            GradientHeader(
                title = "Chat History",
                subtitle = data?.let { historySubtitle(it) } ?: "Your conversations",
                darkTheme = darkTheme,
                content = {
                    HeaderSearchField(query = query, onQueryChange = onQueryChange)
                },
            )
        }

        if (data != null && data.availableCategories.size > 1) {
            item(key = "filters") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    item {
                        CategoryChip(
                            label = "All",
                            selected = selectedCategory == null,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = { onCategorySelected(null) },
                        )
                    }
                    items(data.availableCategories) { category ->
                        CategoryChip(
                            label = category.label,
                            selected = selectedCategory == category,
                            tint = category.tint,
                            onClick = { onCategorySelected(category) },
                        )
                    }
                }
            }
        }

        when {
            state is UiState.Loading -> item(key = "loading") {
                Column(modifier = sidePadding) { repeat(5) { SkeletonRow() } }
            }

            data != null && data.chats.isEmpty() -> item(key = "empty") {
                HistoryEmptyState(
                    searching = query.isNotBlank() || selectedCategory != null,
                    modifier = sidePadding.padding(top = spacing.xxl),
                )
            }

            data != null -> {
                val grouped = data.chats.groupBy { dateBucket(it.timestampMillis) }
                DateBucket.entries.forEach { bucket ->
                    val bucketChats = grouped[bucket].orEmpty()
                    if (bucketChats.isNotEmpty()) {
                        item(key = "header-${bucket.name}") {
                            BucketHeader(
                                label = bucket.label,
                                count = bucketChats.size,
                                modifier = sidePadding.padding(top = spacing.sm),
                            )
                        }
                        items(bucketChats, key = { it.id }) { chat ->
                            HistoryChatCard(
                                chat = chat,
                                mode = modes.firstOrNull { it.id == chat.modeId },
                                onClick = { onChatClick(chat) },
                                onDelete = { onDeleteChat(chat.id) },
                                modifier = sidePadding,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun historySubtitle(data: ChatHistoryData): String {
    if (data.totalChats == 0) return "No conversations yet"
    val chats = if (data.totalChats == 1) "1 conversation" else "${data.totalChats} conversations"
    return "$chats · ${data.chatsThisWeek} this week"
}

/** White pill on the gradient, matching the dashboard's mini composer so the tabs feel related. */
@Composable
private fun HeaderSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search your chats",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(role = Role.Button) { onQueryChange("") }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, if (selected) tint else MaterialTheme.colorScheme.outline),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
        )
    }
}

@Composable
private fun BucketHeader(label: String, count: Int, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.sm, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun HistoryChatCard(
    chat: ChatSummary,
    mode: Mode?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val tint = mode?.category?.tint ?: MaterialTheme.colorScheme.primary

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
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.08f)))),
            contentAlignment = Alignment.Center
        ) {
            if (mode != null) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.title.truncate(70),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.padding(top = spacing.xxs)
            ) {
                Text(
                    text = mode?.title.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatRelativeTime(chat.timestampMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete chat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HistoryEmptyState(searching: Boolean, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = if (searching) "No matches" else "No chats yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.lg)
        )
        Text(
            text = if (searching) {
                "Try a different search or filter."
            } else {
                "Start a chat from Home and it'll be saved here for you to pick up later."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs, start = spacing.xl, end = spacing.xl)
        )
    }
}

private enum class DateBucket(val label: String) {
    Today("Today"), Yesterday("Yesterday"), ThisWeek("This week"), Earlier("Earlier")
}

private fun dateBucket(timestampMillis: Long): DateBucket {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val daysAgo = today.toEpochDay() - day.toEpochDay()
    return when {
        daysAgo <= 0L -> DateBucket.Today
        daysAgo == 1L -> DateBucket.Yesterday
        daysAgo <= 7L -> DateBucket.ThisWeek
        else -> DateBucket.Earlier
    }
}

private fun formatRelativeTime(timestampMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val daysAgo = today.toEpochDay() - day.toEpochDay()
    val pattern = when {
        daysAgo <= 0L -> "h:mm a"
        daysAgo <= 7L -> "EEE"
        else -> "d MMM"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMillis))
}

@ThemePreviews
@Composable
private fun ChatHistoryScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ChatHistoryScreen(
                state = UiState.Success(
                    ChatHistoryData(
                        chats = emptyList(),
                        totalChats = 0,
                        chatsThisWeek = 0,
                        availableCategories = emptyList(),
                    )
                ),
                query = "",
                selectedCategory = null,
                onQueryChange = {},
                onCategorySelected = {},
                onChatClick = {},
                onDeleteChat = {},
            )
        }
    }
}
