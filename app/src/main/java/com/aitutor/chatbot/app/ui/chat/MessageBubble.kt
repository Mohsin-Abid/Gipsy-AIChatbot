package com.aitutor.chatbot.app.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.MessageRole
import com.aitutor.chatbot.app.ui.theme.spacing
import com.mikepenz.markdown.m3.Markdown

private val UserBubbleShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
private val AiBubbleShape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)

@Composable
fun MessageBubble(
    message: Message,
    selectable: Boolean,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (message.role) {
        MessageRole.User -> UserBubble(text = message.text, modifier = modifier)
        MessageRole.Model -> AiBubble(
            text = message.text,
            selectable = selectable,
            onOpenActions = onOpenActions,
            modifier = modifier,
        )
    }
}

@Composable
private fun UserBubble(text: String, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = UserBubbleShape,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
            )
        }
    }
}

@Composable
private fun AiBubble(
    text: String,
    selectable: Boolean,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        AiAvatar()
        Column(modifier = Modifier.padding(start = spacing.sm).widthIn(max = 300.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = AiBubbleShape,
            ) {
                Column {
                    val content = @Composable {
                        Markdown(
                            content = text,
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm)
                        )
                    }
                    if (selectable) {
                        SelectionContainer { content() }
                    } else {
                        content()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onOpenActions, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Message actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        AiAvatar()
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = AiBubbleShape,
            modifier = Modifier.padding(start = spacing.sm)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                repeat(3) { index -> TypingDot(delayMillis = index * 150) }
            }
        }
    }
}

@Composable
private fun TypingDot(delayMillis: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typingDot")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typingDotAlpha"
    )
    Box(
        modifier = modifier
            .size(6.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), CircleShape)
    )
}
