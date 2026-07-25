package org.openauto.companion.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openauto.companion.R
import org.openauto.companion.data.Vehicle
import org.openauto.companion.ui.theme.CockpitBlue
import org.openauto.companion.ui.theme.CockpitBlueText
import org.openauto.companion.ui.theme.CockpitMuted
import org.openauto.companion.ui.theme.OpenAutoCompanionTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    vehicles: List<Vehicle>,
    connectedSsid: String?,
    onRefresh: () -> Unit,
    onVehicleTap: (Vehicle) -> Unit,
    onAddVehicle: () -> Unit,
    onRemoveVehicle: (Vehicle) -> Unit,
) {
    val connectedVehiclePresent = vehicles.any { it.ssid == connectedSsid }
    val displayedVehicles = vehicles.sortedByDescending { it.ssid == connectedSsid }

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            try {
                onRefresh()
                withFrameNanos { }
            } finally {
                isRefreshing = false
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        modifier = Modifier.fillMaxSize(),
    ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 8.dp),
                        ) {
                            Text(
                                text = "Your garage",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = garageSummary(vehicles.size, connectedVehiclePresent),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.widthIn(min = 76.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.prodigy_logo),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(MaterialTheme.shapes.small),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "PRODIGY",
                                style = MaterialTheme.typography.labelSmall,
                                color = CockpitBlueText,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                items(displayedVehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        connected = vehicle.ssid == connectedSsid,
                        onClick = { onVehicleTap(vehicle) },
                        onRemove = { onRemoveVehicle(vehicle) },
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
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    connected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    CockpitCard(
        modifier = Modifier.fillMaxWidth(),
        highlighted = connected,
        onClick = onClick,
    ) {
        if (connected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionDot(CockpitBlue, "Connected")
                Spacer(Modifier.width(10.dp))
                Text("CONNECTED", style = MaterialTheme.typography.labelSmall, color = CockpitBlueText)
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
                    color = if (connected) CockpitBlueText else CockpitMuted,
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove vehicle",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove " + vehicle.name + "?") },
            text = { Text("This will unpair this vehicle. You can re-pair later with a new pairing code.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onRemove() }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                CockpitTextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
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
            onRefresh = {},
            onVehicleTap = {},
            onAddVehicle = {},
            onRemoveVehicle = {},
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
            onRefresh = {},
            onVehicleTap = {},
            onAddVehicle = {},
            onRemoveVehicle = {},
        )
    }
}
