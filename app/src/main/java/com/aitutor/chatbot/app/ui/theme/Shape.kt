package com.aitutor.chatbot.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** The design's corner radii, named by what they belong to rather than by size. */
object AppShapes {
    /** The accent panel at the top of onboarding — square top, generous bottom. */
    val HeroPanel = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
    val LogoLarge = RoundedCornerShape(34.dp)
    val LogoSmall = RoundedCornerShape(11.dp)
    val CardLarge = RoundedCornerShape(22.dp)
    val Card = RoundedCornerShape(20.dp)
    val CardSmall = RoundedCornerShape(16.dp)
    val Tool = RoundedCornerShape(26.dp)
    val Button = RoundedCornerShape(28.dp)
    val Pill = RoundedCornerShape(percent = 50)

    /** Chat bubbles: the flattened corner points back at whoever is speaking. */
    val BubbleUser = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp)
    val BubbleAi = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
}

val MaterialShapes = Shapes(
    extraSmall = AppShapes.CardSmall,
    small = AppShapes.CardSmall,
    medium = AppShapes.Card,
    large = AppShapes.CardLarge,
    extraLarge = AppShapes.Button,
)
