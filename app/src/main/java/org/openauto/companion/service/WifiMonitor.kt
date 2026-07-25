package org.openauto.companion.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import org.openauto.companion.MonitorLifecycle
import org.openauto.companion.data.Vehicle

class WifiMonitor(
    private val context: Context,
    vehicles: List<Vehicle>
) : MonitorLifecycle {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)
    private var registered = false
    @Volatile
    private var wifiNetwork: Network? = null
    @Volatile
    private var activeVehicle: Vehicle? = null
    private val vehicleCatalog = RuntimeVehicleCatalog(vehicles)

    private val networkCallback = object : ConnectivityManager.NetworkCallback(
        FLAG_INCLUDE_LOCATION_INFO
    ) {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val wifiInfo = caps.transportInfo as? WifiInfo ?: return
            val ssid = readableSsid(wifiInfo.ssid) ?: return

            val vehicle = vehicleCatalog.findBySsid(ssid) ?: return
            if (activeVehicle?.ssid == ssid && wifiNetwork == network) return

            Log.i(TAG, "Matched vehicle '${vehicle.name}' on SSID: $ssid")
            wifiNetwork = network
            activeVehicle = vehicle
            startCompanionService(vehicle)
        }

        override fun onLost(network: Network) {
            if (network != wifiNetwork) return
            Log.i(TAG, "WiFi network lost")
            wifiNetwork = null
            activeVehicle = null
            stopCompanionService()
        }
    }

    override fun start() {
        if (registered) return
        val runtimeVehicles = vehicleCatalog.runtimeVehicles
        if (runtimeVehicles.isEmpty()) {
            Log.i(TAG, "WiFi monitor has no vehicles with valid External API v1 credentials")
            return
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        registered = true
        Log.i(
            TAG,
            "WiFi monitor started, watching for ${runtimeVehicles.size} External API vehicle(s): " +
                runtimeVehicles.map { it.ssid }
        )
        checkCurrentNetwork()
    }

    override fun refresh() {
        checkCurrentNetwork(
            forceReconnect = true,
            clearWhenMissing = true,
        )
    }

    fun refresh(vehicles: List<Vehicle>) {
        vehicleCatalog.replace(vehicles)
        refresh()
    }

    private fun checkCurrentNetwork(
        forceReconnect: Boolean = false,
        clearWhenMissing: Boolean = false,
    ) {
        var wifiObserved = false
        var readableSsidObserved = false
        var inspectionFailed = false

        try {
            @Suppress("DEPRECATION")
            for (network in connectivityManager.allNetworks) {
                val caps = connectivityManager.getNetworkCapabilities(network)
                if (caps == null) {
                    inspectionFailed = true
                    continue
                }
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
                wifiObserved = true
                val wifiInfo = caps.transportInfo as? WifiInfo
                val ssid = readableSsid(wifiInfo?.ssid)
                if (ssid != null) readableSsidObserved = true
                Log.i(TAG, "Network check: transport=WiFi, ssid=$ssid")
                val vehicle = ssid?.let(vehicleCatalog::findBySsid)
                if (vehicle != null) {
                    Log.i(TAG, "Already connected to vehicle '${vehicle.name}' via ConnectivityManager")
                    wifiNetwork = network
                    activeVehicle = vehicle
                    startCompanionService(vehicle, forceReconnect)
                    return
                }
                if (wifiNetwork == null) wifiNetwork = network
            }
        } catch (e: Exception) {
            inspectionFailed = true
            Log.w(TAG, "ConnectivityManager network scan failed", e)
        }

        // Fallback: WifiManager.connectionInfo
        try {
            val wifiManager = context.getSystemService(WifiManager::class.java)
            @Suppress("DEPRECATION")
            val info = wifiManager.connectionInfo
            val ssid = readableSsid(info?.ssid)
            if (ssid != null) readableSsidObserved = true
            Log.i(TAG, "WifiManager fallback: ssid=$ssid")
            val vehicle = ssid?.let(vehicleCatalog::findBySsid)
            if (vehicle != null) {
                Log.i(TAG, "Already connected to vehicle '${vehicle.name}' via WifiManager")
                activeVehicle = vehicle
                startCompanionService(vehicle, forceReconnect)
                return
            }
        } catch (e: Exception) {
            inspectionFailed = true
            Log.w(TAG, "WifiManager fallback failed", e)
        }

        val shouldClearRuntime = shouldClearRuntimeAfterNoMatch(
            clearWhenMissing = clearWhenMissing,
            connected = CompanionService.connected.value,
            wifiObserved = wifiObserved,
            readableSsidObserved = readableSsidObserved,
            inspectionFailed = inspectionFailed,
        )
        if (shouldClearRuntime) {
            wifiNetwork = null
            activeVehicle = null
            stopCompanionService()
        } else if (clearWhenMissing) {
            Log.i(TAG, "Preserving connected runtime because WiFi state is indeterminate")
        }
        Log.i(TAG, "No paired vehicle SSID found in current networks")
    }

    override fun stop() {
        if (registered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            registered = false
        }
        wifiNetwork = null
        activeVehicle = null
        stopCompanionService()
    }

    fun getWifiNetwork(): Network? = wifiNetwork

    private fun startCompanionService(
        vehicle: Vehicle,
        forceReconnect: Boolean = false,
    ) {
        val clientId = vehicle.apiClientId.trim()
        val secretHex = vehicle.apiSecretHex.trim()
        if (!hasValidRuntimeCredentials(vehicle)) {
            Log.w(TAG, "Refusing to start service for vehicle without valid API v1 credentials")
            return
        }
        val intent = Intent(context, CompanionService::class.java).apply {
            putExtra(CompanionService.EXTRA_VEHICLE_NAME, vehicle.name)
            putExtra(CompanionService.EXTRA_VEHICLE_ID, vehicle.id)
            putExtra(CompanionService.EXTRA_VEHICLE_SSID, vehicle.ssid)
            putExtra(CompanionService.EXTRA_API_CLIENT_ID, clientId)
            putExtra(CompanionService.EXTRA_API_SECRET_HEX, secretHex)
            putExtra(CompanionService.EXTRA_SERVER_ID, vehicle.serverId)
            putExtra(CompanionService.EXTRA_HEAD_UNIT_HOST, vehicle.settingsHost)
            putExtra(CompanionService.EXTRA_API_TCP_PORT, vehicle.apiTcpPort)
            putExtra(CompanionService.EXTRA_SOCKS5_ENABLED, vehicle.socks5Enabled)
            putExtra(CompanionService.EXTRA_AUDIO_KEEP_ALIVE, vehicle.audioKeepAlive)
            putExtra(CompanionService.EXTRA_FORCE_RECONNECT, forceReconnect)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopCompanionService() {
        context.stopService(Intent(context, CompanionService::class.java))
    }

    private fun readableSsid(rawSsid: String?): String? =
        rawSsid
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID }

    companion object {
        private const val TAG = "WifiMonitor"
    }
}
