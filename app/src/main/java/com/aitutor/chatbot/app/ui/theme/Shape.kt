package com.aitutor.chatbot.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Three radii only. [Row] is anything list-like (settings/history/tools
 * rows). [Card] is the one bordered card treatment (suggestion chips, the
 * featured tools row). [Pill] is fully rounded — the chat input bar and its
 * send button, matching the one unmistakably "chat app" shape in the UI.
 */
object AppShapes {
    val Row = RoundedCornerShape(10.dp)
    val Card = RoundedCornerShape(16.dp)
    val Pill = CircleShape
    val PillLarge = RoundedCornerShape(28.dp)
}

val Shapes = Shapes(
    extraSmall = AppShapes.Row,
    small = AppShapes.Row,
    medium = AppShapes.Card,
    large = AppShapes.Card,
    extraLarge = AppShapes.PillLarge,
)
