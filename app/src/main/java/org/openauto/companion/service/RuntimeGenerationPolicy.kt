package org.openauto.companion.service

internal fun shouldForceRuntimeReconnect(
    refreshRequested: Boolean,
    connected: Boolean,
): Boolean = refreshRequested && !connected

internal fun shouldReuseRuntimeGeneration(
    forceReconnect: Boolean,
    sameVehicle: Boolean,
    sameNetwork: Boolean,
    runtimeActive: Boolean,
): Boolean = !forceReconnect && sameVehicle && sameNetwork && runtimeActive
