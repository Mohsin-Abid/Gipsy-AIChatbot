package com.aitutor.chatbot.app

import com.aitutor.chatbot.app.domain.model.HistoryGroup
import com.aitutor.chatbot.app.ui.history.groupFor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class HistoryGroupingTest {

    private val now = TimeUnit.DAYS.toMillis(1000)

    private fun groupAfter(millisAgo: Long) = groupFor(updatedAt = now - millisAgo, nowMillis = now)

    @Test
    fun `a message from minutes ago is today`() {
        assertEquals(HistoryGroup.Today, groupAfter(TimeUnit.MINUTES.toMillis(12)))
    }

    @Test
    fun `the day boundary is exclusive at its lower edge`() {
        assertEquals(HistoryGroup.Today, groupAfter(TimeUnit.HOURS.toMillis(23)))
        assertEquals(HistoryGroup.Yesterday, groupAfter(TimeUnit.DAYS.toMillis(1)))
    }

    @Test
    fun `three days back lands in the same week`() {
        assertEquals(HistoryGroup.EarlierThisWeek, groupAfter(TimeUnit.DAYS.toMillis(3)))
    }

    @Test
    fun `a week or more back is older`() {
        assertEquals(HistoryGroup.Older, groupAfter(TimeUnit.DAYS.toMillis(7)))
        assertEquals(HistoryGroup.Older, groupAfter(TimeUnit.DAYS.toMillis(400)))
    }
}
