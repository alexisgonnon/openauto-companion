package org.openauto.companion

internal interface MonitorLifecycle {
    fun start()
    fun stop()
    fun refresh() {}
}

internal class MonitorSlot<T : MonitorLifecycle> {
    var current: T? = null
        private set

    fun startIfAbsent(next: T) {
        if (current != null) return
        current = next
        next.start()
    }

    fun refreshOrStart(next: T) {
        val existing = current
        if (existing != null) {
            existing.refresh()
            return
        }
        current = next
        next.start()
    }

    fun stopCurrent() {
        current?.stop()
        current = null
    }
}
