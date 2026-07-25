package org.openauto.companion.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.openauto.companion.data.Vehicle

class RuntimeVehicleCatalogTest {
    @Test
    fun replaceUsesLatestSettingsForTheSameSsid() {
        val initial = vehicle(socks5Enabled = false, audioKeepAlive = false)
        val catalog = RuntimeVehicleCatalog(listOf(initial))

        val before = requireNotNull(catalog.findBySsid(initial.ssid))
        assertFalse(before.socks5Enabled)
        assertFalse(before.audioKeepAlive)

        catalog.replace(
            listOf(
                initial.copy(
                    socks5Enabled = true,
                    audioKeepAlive = true,
                )
            )
        )

        val after = requireNotNull(catalog.findBySsid(initial.ssid))
        assertTrue(after.socks5Enabled)
        assertTrue(after.audioKeepAlive)
    }

    private fun vehicle(
        socks5Enabled: Boolean,
        audioKeepAlive: Boolean,
    ) = Vehicle(
        id = "vehicle-1",
        ssid = "Prodigy_e57d",
        name = "Miata",
        apiClientId = "client-1",
        apiSecretHex = "ab".repeat(32),
        socks5Enabled = socks5Enabled,
        audioKeepAlive = audioKeepAlive,
    )
}
