package org.openauto.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPresentationTest {
    @Test fun `garage summary reports connected vehicle`() {
        assertEquals("1 vehicle · connected now", garageSummary(1, true))
        assertEquals("2 vehicles · connected now", garageSummary(2, true))
    }

    @Test fun `garage summary reports waiting garage`() {
        assertEquals("No vehicles yet", garageSummary(0, false))
        assertEquals("No vehicles yet", garageSummary(0, true))
        assertEquals("1 vehicle · waiting", garageSummary(1, false))
        assertEquals("3 vehicles · waiting", garageSummary(3, false))
    }

    @Test fun `connected readiness is ready and positive`() {
        val result = readinessPresentation(CompanionStatus(connected = true, headUnitAvailable = true, ssid = "Prodigy_e57d"))
        assertEquals("Ready", result.title)
        assertEquals("Connected · sharing normally", result.detail)
        assertEquals(UiTone.Positive, result.tone)
    }

    @Test fun `connection issue takes warning precedence`() {
        val result = readinessPresentation(CompanionStatus(connected = false, headUnitAvailable = true, connectionMessage = "Authentication failed", ssid = "Prodigy_e57d"))
        assertEquals("Needs attention", result.title)
        assertEquals("Authentication failed", result.detail)
        assertEquals(UiTone.Warning, result.tone)
    }

    @Test fun `unavailable vehicle waits neutrally`() {
        val result = readinessPresentation(CompanionStatus(connected = false, headUnitAvailable = false, ssid = "Prodigy_e57d"))
        assertEquals("Waiting", result.title)
        assertEquals("Waiting for Prodigy_e57d", result.detail)
        assertEquals(UiTone.Neutral, result.tone)
    }

    @Test fun `sharing groups require live connection`() {
        val disconnected = CompanionStatus(connected = false, socks5Active = true)
        val connected = CompanionStatus(connected = true, socks5Active = true)
        val connectedWithoutSocks = CompanionStatus(connected = true, socks5Active = false)
        assertFalse(vehicleDataActive(disconnected))
        assertFalse(internetBridgeActive(disconnected))
        assertTrue(vehicleDataActive(connected))
        assertTrue(internetBridgeActive(connected))
        assertTrue(vehicleDataActive(connectedWithoutSocks))
        assertFalse(internetBridgeActive(connectedWithoutSocks))
    }
}
