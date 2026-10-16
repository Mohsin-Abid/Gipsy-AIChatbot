package com.askylix.app.ui.gate

import com.askylix.app.data.remote.ConfigDto

/** Whether the app may be used right now, as the server's configuration says. */
sealed interface AppGate {
    data object Open : AppGate

    /** This installed version is older than the server will talk to. Nothing works until it is updated. */
    data object UpdateRequired : AppGate

    /** The server is being worked on. [message] is its own words, or null for the app's default. */
    data class Maintenance(val message: String?) : AppGate
}

/**
 * The gate for a configuration. No configuration yet — the app has just opened, or it could not be
 * reached — leaves it open: a phone with a bad connection should still show its screens, not a wall.
 */
fun gateFor(config: ConfigDto?, installedVersion: String): AppGate = when {
    config == null -> AppGate.Open
    config.maintenance.active -> AppGate.Maintenance(config.maintenance.message)
    isOlder(installedVersion, config.minAppVersion) -> AppGate.UpdateRequired
    else -> AppGate.Open
}

/**
 * True when [installed] is a lower version than [minimum]. Compared number by number, so 1.10 is
 * newer than 1.9, and a missing part counts as zero, so 1.0 and 1.0.0 are the same. A version that
 * cannot be read never blocks anyone.
 */
internal fun isOlder(installed: String, minimum: String): Boolean {
    val have = installed.toVersionParts() ?: return false
    val need = minimum.toVersionParts() ?: return false
    for (i in 0 until maxOf(have.size, need.size)) {
        val a = have.getOrElse(i) { 0 }
        val b = need.getOrElse(i) { 0 }
        if (a != b) return a < b
    }
    return false
}

private fun String.toVersionParts(): List<Int>? =
    split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: return null }.takeIf { it.isNotEmpty() }
