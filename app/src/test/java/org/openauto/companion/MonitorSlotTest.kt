package org.openauto.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class MonitorSlotTest {
    @Test
    fun startIfAbsentReusesTheExistingMonitor() {
        val slot = MonitorSlot<FakeMonitor>()
        val first = FakeMonitor()
        val second = FakeMonitor()

        slot.startIfAbsent(first)
        slot.startIfAbsent(second)

        assertEquals(1, first.startCount)
        assertEquals(0, first.stopCount)
        assertEquals(0, second.startCount)
        assertEquals(0, second.stopCount)
        assertSame(first, slot.current)
    }

    @Test
    fun refreshOrStartRefreshesExistingMonitorWithoutReplacingIt() {
        val slot = MonitorSlot<FakeMonitor>()
        val first = FakeMonitor()
        val replacement = FakeMonitor()
        slot.startIfAbsent(first)

        slot.refreshOrStart(replacement)

        assertEquals(1, first.startCount)
        assertEquals(1, first.refreshCount)
        assertEquals(0, first.stopCount)
        assertEquals(0, replacement.startCount)
        assertEquals(0, replacement.refreshCount)
        assertSame(first, slot.current)
    }

    @Test
    fun refreshOrStartAppliesLatestInputToExistingMonitor() {
        val slot = MonitorSlot<FakeMonitor>()
        val first = FakeMonitor()
        val replacement = FakeMonitor()
        slot.startIfAbsent(first)

        slot.refreshOrStart(replacement) { monitor ->
            monitor.appliedConfig = "latest"
            monitor.refresh()
        }

        assertEquals("latest", first.appliedConfig)
        assertNull(replacement.appliedConfig)
        assertEquals(1, first.refreshCount)
        assertSame(first, slot.current)
    }

    @Test
    fun refreshOrStartStartsProvidedMonitorWhenSlotIsEmpty() {
        val slot = MonitorSlot<FakeMonitor>()
        val monitor = FakeMonitor()

        slot.refreshOrStart(monitor)

        assertEquals(1, monitor.startCount)
        assertEquals(0, monitor.refreshCount)
        assertSame(monitor, slot.current)
    }

    @Test
    fun stopCurrentAlsoStopsTheCompanionService() {
        val slot = MonitorSlot<FakeMonitor>()
        val monitor = FakeMonitor()
        slot.startIfAbsent(monitor)

        slot.stopCurrent()

        assertEquals(1, monitor.stopCount)
        assertNull(slot.current)
    }

    private class FakeMonitor : MonitorLifecycle {
        var startCount = 0
        var stopCount = 0
        var refreshCount = 0
        var appliedConfig: String? = null

        override fun start() {
            startCount += 1
        }

        override fun stop() {
            stopCount += 1
        }

        override fun refresh() {
            refreshCount += 1
        }
    }
}
