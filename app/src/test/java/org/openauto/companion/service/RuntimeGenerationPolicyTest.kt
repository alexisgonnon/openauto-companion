package org.openauto.companion.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeGenerationPolicyTest {
    @Test
    fun refreshRequestForConnectedRuntimeDoesNotForceReconnect() {
        assertFalse(
            shouldForceRuntimeReconnect(
                refreshRequested = true,
                connected = true,
            )
        )
    }

    @Test
    fun refreshRequestForDisconnectedRuntimeForcesReconnect() {
        assertTrue(
            shouldForceRuntimeReconnect(
                refreshRequested = true,
                connected = false,
            )
        )
    }

    @Test
    fun healthyMatchingRuntimeIsReused() {
        assertTrue(
            shouldReuseRuntimeGeneration(
                forceReconnect = false,
                sameVehicle = true,
                sameNetwork = true,
                runtimeActive = true,
            )
        )
    }

    @Test
    fun manualReconnectBypassesMatchingActiveRuntimeReuse() {
        assertFalse(
            shouldReuseRuntimeGeneration(
                forceReconnect = true,
                sameVehicle = true,
                sameNetwork = true,
                runtimeActive = true,
            )
        )
    }

    @Test
    fun mismatchedOrInactiveRuntimeIsNeverReused() {
        assertFalse(shouldReuseRuntimeGeneration(false, false, true, true))
        assertFalse(shouldReuseRuntimeGeneration(false, true, false, true))
        assertFalse(shouldReuseRuntimeGeneration(false, true, true, false))
    }
}
