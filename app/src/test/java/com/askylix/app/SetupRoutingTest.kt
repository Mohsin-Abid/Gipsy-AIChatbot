package com.askylix.app

import com.askylix.app.data.auth.AuthUser
import com.askylix.app.navigation.Start
import com.askylix.app.navigation.afterSkip
import com.askylix.app.navigation.startOnLaunch
import org.junit.Assert.assertEquals
import org.junit.Test

/** Where the app opens, and where "Skip for now" leads. */
class SetupRoutingTest {

    // ---- Opening the app ----

    @Test
    fun `a brand new phone starts at the beginning`() {
        assertEquals(Start.Language, startOnLaunch(firstRunComplete = false, skipped = false, signedIn = false))
    }

    @Test
    fun `a signed in person with setup done goes straight to Home`() {
        assertEquals(Start.Home, startOnLaunch(firstRunComplete = true, skipped = false, signedIn = true))
    }

    @Test
    fun `signed in with setup unfinished resumes setup`() {
        // The app was closed after sign-in and before the last wizard step.
        assertEquals(Start.Setup, startOnLaunch(firstRunComplete = false, skipped = false, signedIn = true))
    }

    @Test
    fun `someone who logged out is asked to sign in`() {
        assertEquals(Start.SignIn, startOnLaunch(firstRunComplete = true, skipped = false, signedIn = false))
    }

    @Test
    fun `someone who skipped goes straight to Home, with no sign-in screen at every launch`() {
        assertEquals(Start.Home, startOnLaunch(firstRunComplete = true, skipped = true, signedIn = false))
    }

    @Test
    fun `being signed in outranks an old skip`() {
        assertEquals(Start.Home, startOnLaunch(firstRunComplete = true, skipped = true, signedIn = true))
    }

    @Test
    fun `a skip before setup was finished does not skip first run`() {
        // They chose Skip, then the app closed in the wizard. Nothing is set up, and nobody is signed in.
        assertEquals(Start.Language, startOnLaunch(firstRunComplete = false, skipped = true, signedIn = false))
    }

    // ---- Skipping ----

    @Test
    fun `skipping on a phone that has been set up goes to Home`() {
        assertEquals(Start.Home, afterSkip(firstRunComplete = true))
    }

    @Test
    fun `skipping on a new phone still runs the wizard, kept on the phone`() {
        assertEquals(Start.Setup, afterSkip(firstRunComplete = false))
    }

    // ---- The suggested name ----

    private fun user(name: String?) = AuthUser("u", "a@b.c", name, null)

    @Test
    fun `a full Google name suggests just the first name`() {
        assertEquals("Alex", user("Alex Morgan").suggestedFirstName())
        assertEquals("Mary", user("Mary Ann Smith").suggestedFirstName())
    }

    @Test
    fun `a single name is used whole, and a missing name suggests nothing`() {
        assertEquals("Mohsin", user("Mohsin").suggestedFirstName())
        assertEquals("", user(null).suggestedFirstName())
        assertEquals("", user("   ").suggestedFirstName())
    }

    @Test
    fun `stray spaces around the name do not leak into the suggestion`() {
        assertEquals("Alex", user("  Alex   Morgan ").suggestedFirstName())
    }
}
