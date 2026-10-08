package com.aitutor.chatbot.app.navigation

/** Where the app opens, or goes after someone signs in. */
enum class Start { Language, SignIn, Setup, Home }

/**
 * Where to go once someone is signed in: Home if setup is already done **for them**, otherwise setup.
 *
 * "Done for them" is the part that needs care. Setup is saved on the phone, so a device flag alone
 * would send a second person who signs in on the same phone straight to Home, under the first
 * person's profile. So the saved setup remembers whose it is ([setupOwner], an account `uid`):
 *
 * - **Same account:** Home.
 * - **A different account:** setup, as a new user.
 * - **Nobody's yet** (set up as a guest, or before accounts existed): Home, and the next account to
 *   sign in claims it. Someone who skipped sign-in and signs in later keeps what they filled in.
 *
 * Once the server holds the profile this is replaced by its `setupCompleted` flag, which also covers
 * the same person on a new phone — which this cannot, and so repeats setup.
 */
fun afterSignIn(setupComplete: Boolean, setupOwner: String?, uid: String?): Start = when {
    !setupComplete -> Start.Setup
    setupOwner == null || setupOwner == uid -> Start.Home
    else -> Start.Setup
}

/** Where to open the app. [uid] is null when nobody is signed in. */
fun startOnLaunch(setupComplete: Boolean, setupOwner: String?, uid: String?): Start = when {
    // Never set up: a new person starts at the beginning, unless an account already exists and
    // setup was simply interrupted, in which case it resumes.
    !setupComplete -> if (uid != null) Start.Setup else Start.Language
    // Set up before but nobody is signed in now (they logged out): sign-in is the only way back.
    uid == null -> Start.SignIn
    else -> afterSignIn(setupComplete = true, setupOwner = setupOwner, uid = uid)
}

/** True when a signed-in account should take ownership of a setup nobody owns yet. */
fun shouldClaimSetup(setupComplete: Boolean, setupOwner: String?, uid: String?): Boolean =
    setupComplete && setupOwner == null && uid != null
