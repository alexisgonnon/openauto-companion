package org.openauto.companion.service

internal fun shouldClearRuntimeAfterNoMatch(
    clearWhenMissing: Boolean,
    connected: Boolean,
    wifiObserved: Boolean,
    readableSsidObserved: Boolean,
    inspectionFailed: Boolean,
): Boolean {
    if (!clearWhenMissing) return false
    if (!connected) return true

    val networkStateIsIndeterminate =
        inspectionFailed || (wifiObserved && !readableSsidObserved)
    return !networkStateIsIndeterminate
}
