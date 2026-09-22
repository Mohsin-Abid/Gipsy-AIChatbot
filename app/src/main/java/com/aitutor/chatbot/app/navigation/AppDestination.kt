package com.aitutor.chatbot.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    History("History", Icons.Outlined.History, Icons.Filled.History),
    Tools("Tools", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    Settings("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}
