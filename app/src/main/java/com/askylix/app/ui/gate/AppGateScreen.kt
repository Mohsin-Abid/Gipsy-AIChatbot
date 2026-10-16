package com.askylix.app.ui.gate

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.askylix.app.R
import com.askylix.app.ui.components.PrimaryCta
import com.askylix.app.ui.signin.findActivity
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.appColors

/**
 * The wall shown over the whole app when the server says to stop: an update is required, or it is
 * being worked on. It covers everything, so nothing underneath can be tapped.
 */
@Composable
fun AppGateScreen(gate: AppGate, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    if (gate == AppGate.Open) return
    val context = LocalContext.current

    // Back has nothing to go back to: the screens underneath are out of reach, so it leaves the app.
    BackHandler { context.findActivity()?.finish() }

    val covering = modifier.swallowTouches()
    when (gate) {
        AppGate.Open -> Unit
        AppGate.UpdateRequired -> ForceUpdateScreen(onUpdate = { context.openStorePage() }, modifier = covering)
        is AppGate.Maintenance -> MaintenanceScreen(message = gate.message, onRetry = onRetry, modifier = covering)
    }
}

/**
 * Takes every touch that lands on this view, so none reaches the screen drawn underneath. Without it
 * a tap on the gate's empty areas falls through and operates whatever is behind.
 */
private fun Modifier.swallowTouches(): Modifier = pointerInput(Unit) { detectTapGestures { } }

/** "We'll be back soon", with the server's own words when it sent some. */
@Composable
private fun MaintenanceScreen(message: String?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint)
            .systemBarsPadding()
            .padding(horizontal = Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.gate_maintenance_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.gate_maintenance_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        PrimaryCta(text = stringResource(R.string.gate_retry), onClick = onRetry, trailingArrow = false)
    }
}

/** The app's page in Google Play, or the web page if the Play app is not there. */
private fun android.content.Context.openStorePage() {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
    try {
        startActivity(market)
    } catch (_: android.content.ActivityNotFoundException) {
        runCatching { startActivity(web) }
    }
}
