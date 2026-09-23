package com.aitutor.chatbot.app.ui.main

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.ui.history.ChatHistoryRoute
import com.aitutor.chatbot.app.ui.home.HomeRoute
import com.aitutor.chatbot.app.ui.premium.PaywallSheet
import com.aitutor.chatbot.app.ui.settings.SettingsRoute

enum class MainTab(val label: String, val outlinedIcon: ImageVector, val filledIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    History("History", Icons.Outlined.History, Icons.Filled.History),
    Settings("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/**
 * The app's persistent 3-tab shell: Home (tools grid), Chat History, Settings. Each tab keeps its
 * own ViewModel/state across tab switches (Koin scopes them to this composable's lifetime, same as
 * the destinations pushed on top of it — Chat Detail, Premium, Language — which hide this bottom bar
 * entirely since they're separate NavHost destinations).
 */
@Composable
fun MainScreen(
    onModeClick: (Mode) -> Unit,
    onChatClick: (ChatSummary) -> Unit,
    onLanguageClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onAccountCleared: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var lockedMode by remember { mutableStateOf<Mode?>(null) }

    LightStatusBarIcons()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The tabs draw their own gradient header up behind the status bar, so the shell must not
        // reserve that space. The bottom bar still pads itself for the navigation bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                MainTab.entries.forEach { tab ->
                    val selected = tab == selectedTab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                MainTab.Home -> HomeRoute(
                    onOpenSettings = { selectedTab = MainTab.Settings },
                    onModeClick = onModeClick,
                    onLockedModeClick = { lockedMode = it },
                    onChatClick = onChatClick,
                    onSeeAllChats = { selectedTab = MainTab.History },
                    onUpgradeClick = onPremiumClick,
                )
                MainTab.History -> ChatHistoryRoute(onChatClick = onChatClick)
                MainTab.Settings -> SettingsRoute(
                    onLanguageClick = onLanguageClick,
                    onPremiumClick = onPremiumClick,
                    onAccountCleared = onAccountCleared,
                )
            }
        }
    }

    lockedMode?.let { mode ->
        PaywallSheet(
            title = "${mode.title} is a Premium tool",
            subtitle = mode.description,
            onSeePlans = { lockedMode = null; onPremiumClick() },
            onDismiss = { lockedMode = null },
        )
    }
}

/**
 * The tabs put a dark gradient behind the status bar, so its icons have to be light while they're
 * on screen. Restored on dispose, so the light-backgrounded destinations pushed on top of this
 * shell — Chat Detail, Language — get their dark icons back.
 */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            if (controller != null && previous != null) {
                controller.isAppearanceLightStatusBars = previous
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
