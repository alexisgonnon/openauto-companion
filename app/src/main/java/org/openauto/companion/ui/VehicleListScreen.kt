package org.openauto.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openauto.companion.data.Vehicle
import org.openauto.companion.ui.theme.CockpitMint
import org.openauto.companion.ui.theme.CockpitMuted
import org.openauto.companion.ui.theme.OpenAutoCompanionTheme

@Composable
fun VehicleListScreen(
    vehicles: List<Vehicle>,
    connectedSsid: String?,
    onVehicleTap: (Vehicle) -> Unit,
    onAddVehicle: () -> Unit,
) {
    val connectedVehiclePresent = vehicles.any { it.ssid == connectedSsid }
    val displayedVehicles = vehicles.sortedByDescending { it.ssid == connectedSsid }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 22.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "PRODIGY",
                    style = MaterialTheme.typography.labelSmall,
                    color = CockpitMint,
                )
                Spacer(Modifier.height(12.dp))
                Text("Your garage", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = garageSummary(vehicles.size, connectedVehiclePresent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
            }

            items(displayedVehicles, key = { it.id }) { vehicle ->
                VehicleCard(
                    vehicle = vehicle,
                    connected = vehicle.ssid == connectedSsid,
                    onClick = { onVehicleTap(vehicle) },
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        Button(
            onClick = onAddVehicle,
            enabled = vehicles.size < Vehicle.MAX_VEHICLES,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Add vehicle")
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    connected: Boolean,
    onClick: () -> Unit,
) {
    CockpitCard(
        modifier = Modifier.fillMaxWidth(),
        highlighted = connected,
        onClick = onClick,
    ) {
        if (connected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionDot(CockpitMint, "Connected")
                Spacer(Modifier.width(10.dp))
                Text("CONNECTED", style = MaterialTheme.typography.labelSmall, color = CockpitMint)
            }
            Spacer(Modifier.height(18.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicle.name, style = MaterialTheme.typography.headlineLarge)
                if (vehicle.name != vehicle.ssid) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        vehicle.ssid,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (connected) "Data sharing active" else "Waiting for vehicle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (connected) CockpitMint else CockpitMuted,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun previewVehicles(): List<Vehicle> {
    val previewSecret = "00".repeat(32)
    return listOf(
    Vehicle(
        id = "miata",
        ssid = "Prodigy_e57d",
        name = "Miata",
        apiClientId = "preview-miata",
        apiSecretHex = previewSecret,
    ),
    Vehicle(
        id = "daily",
        ssid = "Prodigy_daily",
        name = "Daily driver",
        apiClientId = "preview-daily",
        apiSecretHex = previewSecret,
    ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun GarageConnectedPreview() {
    OpenAutoCompanionTheme {
        VehicleListScreen(
            vehicles = previewVehicles(),
            connectedSsid = "Prodigy_e57d",
            onVehicleTap = {},
            onAddVehicle = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun GarageEmptyPreview() {
    OpenAutoCompanionTheme {
        VehicleListScreen(
            vehicles = emptyList(),
            connectedSsid = null,
            onVehicleTap = {},
            onAddVehicle = {},
        )
    }
}
