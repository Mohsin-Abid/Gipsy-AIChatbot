package com.aitutor.chatbot.app

import com.aitutor.chatbot.app.data.auth.AuthUser
import com.aitutor.chatbot.app.navigation.Start
import com.aitutor.chatbot.app.navigation.afterSignIn
import com.aitutor.chatbot.app.navigation.shouldClaimSetup
import com.aitutor.chatbot.app.navigation.startOnLaunch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The three cases of the sign-in scenario, and the awkward ones around them. */
class SetupRoutingTest {

    // ---- 1. Signs in and already has an account: straight to Home ----

    @Test
    fun `an account whose setup is done goes straight to Home`() {
        assertEquals(Start.Home, afterSignIn(setupComplete = true, setupOwner = "A", uid = "A"))
    }

    // ---- 2. A new user: setup ----

    @Test
    fun `a person who has never set up goes to setup`() {
        assertEquals(Start.Setup, afterSignIn(setupComplete = false, setupOwner = null, uid = "A"))
    }

    @Test
    fun `a different account on the same phone is a new user, not the previous person`() {
        // The phone has A's finished setup. B signs in. B must not land on Home under A's profile.
        assertEquals(Start.Setup, afterSignIn(setupComplete = true, setupOwner = "A", uid = "B"))
    }

    // ---- 3. Skips sign-in ----

    @Test
    fun `a guest who has set up carries on to Home`() {
        assertEquals(Start.Home, afterSignIn(setupComplete = true, setupOwner = null, uid = null))
    }

    @Test
    fun `a guest on a phone that belongs to an account is a new person`() {
        assertEquals(Start.Setup, afterSignIn(setupComplete = true, setupOwner = "A", uid = null))
    }

    // ---- setup nobody owns yet ----

    @Test
    fun `a setup nobody owns is claimed by the first account that signs in`() {
        // Done as a guest, or before accounts existed. Signing in must keep what was filled in.
        assertEquals(Start.Home, afterSignIn(setupComplete = true, setupOwner = null, uid = "A"))
        assertTrue(shouldClaimSetup(setupComplete = true, setupOwner = null, uid = "A"))
    }

    @Test
    fun `nothing is claimed when there is nothing to claim`() {
        assertFalse(shouldClaimSetup(setupComplete = false, setupOwner = null, uid = "A")) // not set up
        assertFalse(shouldClaimSetup(setupComplete = true, setupOwner = "A", uid = "A")) // already owned
        assertFalse(shouldClaimSetup(setupComplete = true, setupOwner = null, uid = null)) // nobody to claim
    }

    // ---- opening the app ----

    @Test
    fun `a brand new install starts at the language screen`() {
        assertEquals(Start.Language, startOnLaunch(false, null, uid = null))
    }

    @Test
    fun `an interrupted setup resumes once an account exists`() {
        assertEquals(Start.Setup, startOnLaunch(false, null, uid = "A"))
    }

    @Test
    fun `someone who set up but is now logged out must sign in`() {
        assertEquals(Start.SignIn, startOnLaunch(true, "A", uid = null))
        assertEquals(Start.SignIn, startOnLaunch(true, null, uid = null))
    }

    @Test
    fun `a signed in account with finished setup opens on Home`() {
        assertEquals(Start.Home, startOnLaunch(true, "A", uid = "A"))
        assertEquals(Start.Home, startOnLaunch(true, null, uid = "A"))
    }

    // ---- the suggested name ----

    private fun user(name: String?) = AuthUser("u", "a@b.c", name, null, isAnonymous = false)

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
