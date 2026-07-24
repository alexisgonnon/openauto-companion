package org.openauto.companion.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiReconciliationPolicyTest {
    @Test
    fun automaticChecksNeverClearRuntime() {
        assertFalse(
            shouldClearRuntimeAfterNoMatch(
                clearWhenMissing = false,
                connected = false,
                wifiObserved = false,
                readableSsidObserved = false,
                inspectionFailed = false,
            )
        )
    }

    @Test
    fun explicitRefreshClearsRuntimeWhenDisconnected() {
        assertTrue(
            shouldClearRuntimeAfterNoMatch(
                clearWhenMissing = true,
                connected = false,
                wifiObserved = true,
                readableSsidObserved = false,
                inspectionFailed = true,
            )
        )
    }

    @Test
    fun explicitRefreshClearsConnectedRuntimeWhenWifiIsConfirmedAbsent() {
        assertTrue(
            shouldClearRuntimeAfterNoMatch(
                clearWhenMissing = true,
                connected = true,
                wifiObserved = false,
                readableSsidObserved = false,
                inspectionFailed = false,
            )
        )
    }

    @Test
    fun explicitRefreshClearsConnectedRuntimeAfterReadableNonPairedSsid() {
        assertTrue(
            shouldClearRuntimeAfterNoMatch(
                clearWhenMissing = true,
                connected = true,
                wifiObserved = true,
                readableSsidObserved = true,
                inspectionFailed = false,
            )
        )
    }

    @Test
    fun explicitRefreshPreservesConnectedRuntimeWhenWifiSsidIsUnreadable() {
        assertFalse(
            shouldClearRuntimeAfterNoMatch(
                clearWhenMissing = true,
                connected = true,
                wifiObserved = true,
                readableSsidObserved = false,
                inspectionFailed = false,
            )
        )
    }

    @Test
    fun explicitRefreshPreservesConnectedRuntimeWhenInspectionFails() {
        assertFalse(
            shouldClearRuntimeAfterNoMatch(
                clearWhenMissing = true,
                connected = true,
                wifiObserved = false,
                readableSsidObserved = false,
                inspectionFailed = true,
            )
        )
    }
}
