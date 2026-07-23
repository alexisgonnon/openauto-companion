package org.openauto.companion.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openauto.companion.ui.theme.CockpitMint
import org.openauto.companion.ui.theme.CockpitMuted
import org.openauto.companion.ui.theme.OpenAutoCompanionTheme

sealed interface PairingUiState {
    data object Idle : PairingUiState
    data object Pairing : PairingUiState
    data class Failed(val message: String) : PairingUiState
}

@Composable
fun PairingScreen(
    suggestedSsid: String = "",
    state: PairingUiState,
    onPair: (ssid: String, name: String, pin: String) -> Unit,
    onScanQr: () -> Unit,
    onCancel: (() -> Unit)? = null,
    manualExpandedInitially: Boolean = false,
) {
    var ssid by rememberSaveable { mutableStateOf(suggestedSsid) }
    var name by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var manualExpanded by rememberSaveable { mutableStateOf(manualExpandedInitially) }
    val pairing = state is PairingUiState.Pairing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 22.dp),
    ) {
        if (onCancel != null) {
            CockpitTopBar(
                title = "Add vehicle",
                onBack = onCancel,
                backEnabled = !pairing,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Add vehicle", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Pair in seconds", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            "Open External API on your head unit, then scan its pairing code.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))

        CockpitCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
            PairingQrIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            )
            Text(
                "Point your camera at the code on the head unit",
                style = MaterialTheme.typography.bodyMedium,
                color = CockpitMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onScanQr,
            enabled = !pairing,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            if (pairing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Pairing…")
            } else {
                Text("Scan QR")
            }
        }
        if (pairing) {
            Text(
                "Connecting to your head unit…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Spacer(Modifier.height(14.dp))
        CockpitCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = !pairing) { manualExpanded = !manualExpanded }
                    .semantics {
                        role = Role.Button
                        stateDescription = if (manualExpanded) "Expanded" else "Collapsed"
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Can't scan?", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Enter Wi-Fi and pairing PIN manually",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CockpitMuted,
                    )
                }
                Icon(
                    if (manualExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CockpitMuted,
                )
            }

            if (manualExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("Wi-Fi SSID") },
                    placeholder = { Text("e.g. OpenAutoProdigy-A3F2") },
                    singleLine = true,
                    enabled = !pairing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Vehicle name (optional)") },
                    placeholder = { Text("e.g. Miata") },
                    singleLine = true,
                    enabled = !pairing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        if (value.length <= 6 && value.all(Char::isDigit)) pin = value
                    },
                    label = { Text("Six-digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    enabled = !pairing,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onCancel != null) {
                        TextButton(onClick = onCancel, enabled = !pairing) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            onPair(
                                ssid.trim(),
                                name.trim().ifEmpty { ssid.trim() },
                                pin,
                            )
                        },
                        enabled = canSubmitManualPairing(ssid, pin, pairing),
                    ) {
                        if (pairing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Pairing…")
                        } else {
                            Text("Pair")
                        }
                    }
                }
            }
        }

        if (state is PairingUiState.Failed) {
            Spacer(Modifier.height(14.dp))
            CockpitCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Text("Pairing failed", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
                Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PairingQrIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val side = size.minDimension * 0.22f
        val stroke = side * 0.16f
        val inset = size.minDimension * 0.1f
        fun finder(topLeft: Offset) {
            drawRect(
                color = CockpitMint,
                topLeft = topLeft,
                size = Size(side, side),
                style = Stroke(width = stroke),
            )
            drawCircle(
                color = CockpitMint,
                radius = side * 0.14f,
                center = Offset(topLeft.x + side / 2f, topLeft.y + side / 2f),
            )
        }
        finder(Offset(inset, inset))
        finder(Offset(size.width - inset - side, inset))
        finder(Offset(inset, size.height - inset - side))
        drawLine(
            color = CockpitMint,
            start = Offset(size.width * 0.55f, size.height * 0.65f),
            end = Offset(size.width * 0.78f, size.height * 0.65f),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
        drawLine(
            color = CockpitMint,
            start = Offset(size.width * 0.67f, size.height * 0.53f),
            end = Offset(size.width * 0.67f, size.height * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Square,
        )
    }
}

@Composable
private fun PairingPreview(state: PairingUiState, manualExpanded: Boolean = false) {
    OpenAutoCompanionTheme {
        PairingScreen(
            state = state,
            onPair = { _, _, _ -> },
            onScanQr = {},
            onCancel = {},
            manualExpandedInitially = manualExpanded,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun PairingIdlePreview() {
    PairingPreview(PairingUiState.Idle)
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun PairingManualPreview() {
    PairingPreview(PairingUiState.Idle, manualExpanded = true)
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun PairingFailedPreview() {
    PairingPreview(PairingUiState.Failed("Unable to reach the head unit."), manualExpanded = true)
}
