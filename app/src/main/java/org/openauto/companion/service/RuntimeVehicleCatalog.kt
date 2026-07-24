package org.openauto.companion.service

import org.openauto.companion.data.Vehicle
import org.openauto.companion.net.api.ApiCrypto

internal fun hasValidRuntimeCredentials(vehicle: Vehicle): Boolean =
    vehicle.apiClientId.isNotBlank() &&
        ApiCrypto.decodeSecretHex(vehicle.apiSecretHex) != null

internal class RuntimeVehicleCatalog(vehicles: List<Vehicle>) {
    @Volatile
    private var snapshot = Snapshot.from(vehicles)

    val runtimeVehicles: List<Vehicle>
        get() = snapshot.vehicles

    fun findBySsid(ssid: String): Vehicle? = snapshot.bySsid[ssid]

    fun replace(vehicles: List<Vehicle>) {
        snapshot = Snapshot.from(vehicles)
    }

    private data class Snapshot(
        val vehicles: List<Vehicle>,
        val bySsid: Map<String, Vehicle>,
    ) {
        companion object {
            fun from(vehicles: List<Vehicle>): Snapshot {
                val runtimeVehicles = vehicles.filter(::hasValidRuntimeCredentials)
                return Snapshot(
                    vehicles = runtimeVehicles,
                    bySsid = runtimeVehicles.associateBy { it.ssid },
                )
            }
        }
    }
}
