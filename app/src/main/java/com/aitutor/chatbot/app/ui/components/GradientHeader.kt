package com.aitutor.chatbot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.HeroGradientEnd
import com.aitutor.chatbot.app.ui.theme.HeroGradientEndDark
import com.aitutor.chatbot.app.ui.theme.HeroGradientStart
import com.aitutor.chatbot.app.ui.theme.HeroGradientStartDark
import com.aitutor.chatbot.app.ui.theme.spacing

private val HeaderShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

/**
 * The app's masthead treatment, shared by every top-level tab so the three screens read as one
 * product. [trailing] holds actions beside the title; [content] is the slot under it, used for a
 * search field or a status row.
 */
@Composable
fun GradientHeader(
    title: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    val brush = Brush.linearGradient(
        colors = if (darkTheme) {
            listOf(HeroGradientStartDark, HeroGradientEndDark)
        } else {
            listOf(HeroGradientStart, HeroGradientEnd)
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeaderShape)
            .background(brush)
            // Applied AFTER the background so the gradient itself fills the status bar area while
            // the content below is pushed clear of it by the device's actual inset.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = spacing.lg)
            .padding(top = spacing.md, bottom = spacing.xl)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = spacing.xxs)
                        )
                    }
                }
                trailing?.invoke(this)
            }

            if (content != null) {
                Box(modifier = Modifier.padding(top = spacing.lg)) { content() }
            }
        }
    }
}

/** A translucent pill for actions sitting on the gradient — the header's only button treatment. */
@Composable
fun GlassIconSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color.White.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
