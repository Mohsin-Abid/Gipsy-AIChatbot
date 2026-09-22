package com.aitutor.chatbot.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.Locale

/**
 * The one recurring section marker used across every screen — "CONTINUE",
 * "TODAY", "ACCOUNT", "FEATURED" — instead of a different treatment per
 * screen. Quiet by design: small, wide letter-spacing, muted color.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
