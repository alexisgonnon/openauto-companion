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

    fun refreshOrStart(
        next: T,
        refreshExisting: (T) -> Unit = { it.refresh() },
    ) {
        val existing = current
        if (existing != null) {
            refreshExisting(existing)
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
