package com.askylix.app.ui.main

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.askylix.app.R
import com.askylix.app.ui.icons.AppIcons

/** The four destinations in the bottom bar, in the order the design lays them out. */
enum class MainTab(
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    /** Home is the only tab whose glyph fills when active, matching the design. */
    val selectedIcon: ImageVector = icon,
) {
    Home(R.string.nav_home, AppIcons.HomeOutline, AppIcons.HomeFilled),
    Tools(R.string.nav_tools, AppIcons.Grid),
    History(R.string.nav_history, AppIcons.HistoryClock),
    Profile(R.string.nav_profile, AppIcons.Person),
}
