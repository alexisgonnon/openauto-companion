package org.openauto.companion.ui

enum class UiTone { Positive, Neutral, Warning }

data class ReadinessPresentation(
    val title: String,
    val detail: String,
    val tone: UiTone,
)

internal fun garageSummary(vehicleCount: Int, connected: Boolean): String = when {
    vehicleCount == 0 -> "No vehicles yet"
    connected -> "${vehicleCount} ${if (vehicleCount == 1) "vehicle" else "vehicles"} · connected now"
    else -> "${vehicleCount} ${if (vehicleCount == 1) "vehicle" else "vehicles"} · waiting"
}

internal fun readinessPresentation(status: CompanionStatus): ReadinessPresentation = when {
    status.connected -> ReadinessPresentation("Ready", "Connected · sharing normally", UiTone.Positive)
    !status.connectionMessage.isNullOrBlank() ->
        ReadinessPresentation("Needs attention", status.connectionMessage, UiTone.Warning)
    else -> ReadinessPresentation("Waiting", "Waiting for ${status.ssid}", UiTone.Neutral)
}

internal fun vehicleDataActive(status: CompanionStatus): Boolean = status.connected

internal fun internetBridgeActive(status: CompanionStatus): Boolean =
    status.connected && status.socks5Active

internal fun canSubmitManualPairing(
    ssid: String,
    pin: String,
    pairingInProgress: Boolean,
): Boolean =
    !pairingInProgress && ssid.trim().isNotEmpty() && pin.length == 6 && pin.all(Char::isDigit)
