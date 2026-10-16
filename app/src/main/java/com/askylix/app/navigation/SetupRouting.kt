package com.askylix.app.navigation

/** Where the app opens, or goes after someone signs in or skips. */
enum class Start { Language, SignIn, Setup, Home }

/**
 * Where to open the app.
 *
 * [firstRunComplete] is whether this phone has been through setup. [skipped] is the "Skip for now"
 * choice, which makes the app a browse mode with no account. [signedIn] is whether Firebase still
 * holds a signed-in person.
 *
 * - **Signed in:** Home once setup is done; otherwise setup resumes, which covers an app closed in the
 *   middle of it.
 * - **Not signed in, set up before:** Home if they chose to skip, otherwise sign-in is the only way
 *   back, which is where someone who logged out lands.
 * - **Brand new:** the start of first run.
 */
fun startOnLaunch(firstRunComplete: Boolean, skipped: Boolean, signedIn: Boolean): Start = when {
    signedIn -> if (firstRunComplete) Start.Home else Start.Setup
    firstRunComplete -> if (skipped) Start.Home else Start.SignIn
    else -> Start.Language
}

/**
 * Where "Skip for now" leads: straight to Home if this phone has been set up already, otherwise through
 * the setup wizard, whose answers are then kept on the phone until the person signs in and they are sent.
 */
fun afterSkip(firstRunComplete: Boolean): Start = if (firstRunComplete) Start.Home else Start.Setup
