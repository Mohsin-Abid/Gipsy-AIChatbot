package com.askylix.app

import com.askylix.app.data.remote.ConfigDto
import com.askylix.app.data.remote.MaintenanceDto
import com.askylix.app.ui.gate.AppGate
import com.askylix.app.ui.gate.gateFor
import com.askylix.app.ui.gate.isOlder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the server's settings stop the app, and when they do not. */
class AppGateTest {

    @Test
    fun `versions compare number by number`() {
        assertTrue(isOlder("1.0.0", "1.0.1"))
        assertTrue(isOlder("1.9", "1.10"))
        assertFalse(isOlder("1.10", "1.9"))
        assertFalse(isOlder("2.0", "1.9.9"))
    }

    @Test
    fun `a missing part counts as zero, so 1_0 and 1_0_0 are the same`() {
        assertFalse(isOlder("1.0", "1.0.0"))
        assertFalse(isOlder("1.0.0", "1.0"))
        assertTrue(isOlder("1.0", "1.0.1"))
    }

    @Test
    fun `a version that cannot be read never blocks anyone`() {
        assertFalse(isOlder("beta", "1.0.0"))
        assertFalse(isOlder("1.0.0", ""))
        assertFalse(isOlder("1.0.0", "next"))
    }

    @Test
    fun `a suffix on a number is ignored`() {
        assertFalse(isOlder("1.2.0-debug", "1.2.0"))
        assertTrue(isOlder("1.1.9-rc1", "1.2.0"))
    }

    @Test
    fun `no settings yet leaves the app open`() {
        assertEquals(AppGate.Open, gateFor(null, "1.0"))
    }

    @Test
    fun `an old version is stopped`() {
        assertEquals(AppGate.UpdateRequired, gateFor(ConfigDto(minAppVersion = "2.0.0"), "1.0"))
    }

    @Test
    fun `a current version is let in`() {
        assertEquals(AppGate.Open, gateFor(ConfigDto(minAppVersion = "1.0.0"), "1.0"))
    }

    @Test
    fun `maintenance stops everyone, whatever their version, with the server's own words`() {
        val config = ConfigDto(minAppVersion = "1.0.0", maintenance = MaintenanceDto(active = true, message = "Back at noon"))

        assertEquals(AppGate.Maintenance("Back at noon"), gateFor(config, "9.9"))
    }

    @Test
    fun `an update outranks nothing, and maintenance outranks an update`() {
        val both = ConfigDto(minAppVersion = "5.0.0", maintenance = MaintenanceDto(active = true))

        assertTrue(gateFor(both, "1.0") is AppGate.Maintenance)
    }
}
