package com.aitutor.chatbot.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.HeroGradientEnd
import com.aitutor.chatbot.app.ui.theme.HeroGradientStart
import com.aitutor.chatbot.app.ui.theme.spacing

/**
 * The study composer. Unlike a plain chat input it leads with the two things students actually reach
 * for — scanning a question and attaching material — and keeps them one tap away rather than buried
 * behind a generic "+".
 */
@Composable
fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onScanClick: () -> Unit,
    onMicClick: () -> Unit,
    placeholder: String,
    attachmentLabel: String?,
    extracting: Boolean,
    onClearAttachment: () -> Unit,
    starterPrompts: List<String>,
    onStarterPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val canSend = value.isNotBlank() && !extracting

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = starterPrompts.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.padding(bottom = spacing.sm),
            ) {
                items(starterPrompts) { prompt ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        onClick = { onStarterPromptClick(prompt) },
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = attachmentLabel != null || extracting, enter = fadeIn(), exit = fadeOut()) {
            AttachmentChip(
                label = attachmentLabel.orEmpty(),
                extracting = extracting,
                onClear = onClearAttachment,
                modifier = Modifier.padding(start = spacing.lg, end = spacing.lg, bottom = spacing.sm)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
            shape = AppShapes.CardLarge,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(modifier = Modifier.padding(spacing.sm)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .padding(horizontal = spacing.sm, vertical = spacing.sm),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    ComposerAction(
                        icon = Icons.Outlined.DocumentScanner,
                        contentDescription = "Scan homework",
                        onClick = onScanClick,
                        highlighted = true,
                    )
                    ComposerAction(
                        icon = Icons.Outlined.AddCircleOutline,
                        contentDescription = "Attach a file",
                        onClick = onAttachClick,
                    )
                    ComposerAction(
                        icon = Icons.Filled.Mic,
                        contentDescription = "Voice input",
                        onClick = onMicClick,
                    )
                    Box(modifier = Modifier.weight(1f))
                    SendButton(enabled = canSend, onClick = onSend)
                }
            }
        }
    }
}

@Composable
private fun ComposerAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(targetValue = if (enabled) 1f else 0.9f, label = "sendScale")
    Box(
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (enabled) {
                    Brush.linearGradient(listOf(HeroGradientStart, HeroGradientEnd))
                } else {
                    SolidColor(MaterialTheme.colorScheme.outline)
                }
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AttachmentChip(
    label: String,
    extracting: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (extracting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = if (extracting) "Reading your page…" else label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!extracting) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove attachment",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(role = Role.Button, onClick = onClear)
                )
            }
        }
    }
}
