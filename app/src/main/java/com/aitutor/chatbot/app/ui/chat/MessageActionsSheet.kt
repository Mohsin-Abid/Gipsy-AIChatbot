package com.aitutor.chatbot.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.spacing

data class MessageActionCallbacks(
    val onCopy: () -> Unit,
    val onShare: () -> Unit,
    val onToggleSpeak: () -> Unit,
    val onSelectText: () -> Unit,
    val onRegenerate: () -> Unit,
    val onMakeShorter: () -> Unit,
    val onMakeLonger: () -> Unit,
    val onReport: () -> Unit,
    val onLike: () -> Unit,
    val onDislike: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionsSheet(
    isSpeaking: Boolean,
    liked: Boolean,
    disliked: Boolean,
    callbacks: MessageActionCallbacks,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val spacing = MaterialTheme.spacing
        val hairline = MaterialTheme.colorScheme.outlineVariant

        ActionRow(icon = Icons.Outlined.ContentCopy, label = "Copy", onClick = { callbacks.onCopy(); onDismiss() })
        ActionRow(icon = Icons.Outlined.Share, label = "Share", onClick = { callbacks.onShare(); onDismiss() })
        ActionRow(
            icon = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
            label = if (isSpeaking) "Stop" else "Text to Speech",
            onClick = { callbacks.onToggleSpeak(); onDismiss() }
        )
        ActionRow(icon = Icons.Outlined.TextFields, label = "Select Text", onClick = { callbacks.onSelectText(); onDismiss() })

        HorizontalDivider(color = hairline, modifier = Modifier.padding(vertical = spacing.xs))

        ActionRow(icon = Icons.Outlined.Refresh, label = "Regenerate", onClick = { callbacks.onRegenerate(); onDismiss() })
        ActionRow(icon = Icons.AutoMirrored.Filled.ShortText, label = "Make Shorter", onClick = { callbacks.onMakeShorter(); onDismiss() })
        ActionRow(icon = Icons.AutoMirrored.Outlined.Notes, label = "Make Longer", onClick = { callbacks.onMakeLonger(); onDismiss() })

        HorizontalDivider(color = hairline, modifier = Modifier.padding(vertical = spacing.xs))

        ActionRow(icon = Icons.Outlined.Flag, label = "Report", onClick = { callbacks.onReport(); onDismiss() })
        FeedbackRow(
            liked = liked,
            disliked = disliked,
            onLike = { callbacks.onLike(); onDismiss() },
            onDislike = { callbacks.onDislike(); onDismiss() },
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FeedbackRow(
    liked: Boolean,
    disliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            "Helpful?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        FeedbackIcon(
            selected = liked,
            selectedIcon = Icons.Filled.ThumbUp,
            unselectedIcon = Icons.Outlined.ThumbUp,
            contentDescription = "Like",
            onClick = onLike,
        )
        FeedbackIcon(
            selected = disliked,
            selectedIcon = Icons.Filled.ThumbDown,
            unselectedIcon = Icons.Outlined.ThumbDown,
            contentDescription = "Dislike",
            onClick = onDislike,
        )
    }
}

@Composable
private fun FeedbackIcon(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = if (selected) selectedIcon else unselectedIcon,
        contentDescription = contentDescription,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(40.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp)
    )
}
