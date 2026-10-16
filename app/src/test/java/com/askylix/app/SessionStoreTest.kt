package com.askylix.app

import com.askylix.app.data.remote.MeDto
import com.askylix.app.data.remote.QuotaDto
import com.askylix.app.data.remote.ToolFlagDto
import com.askylix.app.data.session.SessionStore
import com.askylix.app.domain.model.ChatSummary
import com.askylix.app.domain.model.StudyTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The in-memory model: what a deleted chat leaves behind, and what signing out takes with it. */
class SessionStoreTest {

    private val store = SessionStore()
    private fun chat(id: String, title: String = id) = ChatSummary(id, StudyTool.MathSolver, title, 1L)

    @Test
    fun `a deleted chat leaves every list and its thread is told it is gone`() {
        store.updateHistory { it.copy(items = listOf(chat("a"), chat("b"))) }
        store.updateDashboard { it.copy(recent = listOf(chat("a"), chat("b"))) }
        val thread = store.thread("a") // a screen is watching it

        store.forgetChat("a")

        assertEquals(listOf("b"), store.history.value.items.map { it.id })
        assertEquals(listOf("b"), store.dashboard.value.recent.map { it.id })
        assertTrue("the screen holding the thread learns the chat is gone", thread.value.gone)
        assertFalse("and asking for it again starts empty", store.thread("a").value.gone)
    }

    @Test
    fun `a rename shows everywhere at once, and says what the title was`() {
        store.updateHistory { it.copy(items = listOf(chat("a", "Old"))) }
        store.updateDashboard { it.copy(recent = listOf(chat("a", "Old"))) }
        store.thread("a").value = store.thread("a").value.copy(title = "Old")

        val previous = store.renameChat("a", "New")

        assertEquals("Old", previous)
        assertEquals("New", store.history.value.items.single().title)
        assertEquals("New", store.dashboard.value.recent.single().title)
        assertEquals("New", store.thread("a").value.title)
    }

    @Test
    fun `lists marked stale are fetched again by the next screen`() {
        store.updateHistory { it.copy(loadedAt = 123L) }
        store.updateDashboard { it.copy(loadedAt = 456L) }

        store.markListsStale()

        assertEquals(0L, store.history.value.loadedAt)
        assertEquals(0L, store.dashboard.value.loadedAt)
    }

    @Test
    fun `reading the account also sets the quota`() {
        store.setMe(MeDto(userId = "u", quota = QuotaDto(limit = 15, remaining = 11)), at = 5L)

        assertEquals(11, store.quota.value?.remaining)
        assertEquals(5L, store.meLoadedAt)
    }

    @Test
    fun `tool flags merge, and a later word from the server wins`() {
        store.mergeToolFlags(listOf(ToolFlagDto("CHATBOT", enabled = true), ToolFlagDto("MATH_SOLVER", enabled = true)))
        store.mergeToolFlags(listOf(ToolFlagDto("MATH_SOLVER", enabled = false)))

        assertTrue(store.toolFlags.value.getValue("CHATBOT").enabled)
        assertFalse(store.toolFlags.value.getValue("MATH_SOLVER").enabled)
    }

    @Test
    fun `signing out forgets the account but keeps the app's own settings`() {
        store.setMe(MeDto(userId = "u"), at = 1L)
        store.updateHistory { it.copy(items = listOf(chat("a"))) }
        store.mergeToolFlags(listOf(ToolFlagDto("CHATBOT")))
        store.setConfig(com.askylix.app.data.remote.ConfigDto(minAppVersion = "1.0.0"))

        store.clearAccount()

        assertNull(store.me.value)
        assertNull(store.quota.value)
        assertTrue(store.history.value.items.isEmpty())
        assertEquals("config is the app's, not the account's", "1.0.0", store.config.value?.minAppVersion)
        assertTrue(store.toolFlags.value.isNotEmpty())
    }
}
