package org.openauto.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.openauto.companion.ui.theme.CockpitBlue
import org.openauto.companion.ui.theme.CockpitBlueText
import org.openauto.companion.ui.theme.CockpitMuted
import org.openauto.companion.ui.theme.OpenAutoCompanionTheme

data class CompanionStatus(
    val connected: Boolean = false,
    val headUnitAvailable: Boolean = false,
    val connectionMessage: String? = null,
    val sharingTime: Boolean = false,
    val sharingGps: Boolean = false,
    val sharingBattery: Boolean = false,
    val socks5Active: Boolean = false,
    val audioKeepAliveActive: Boolean = false,
    val ssid: String = "",
)

@Composable
fun StatusScreen(
    vehicleName: String,
    status: CompanionStatus,
    socks5Enabled: Boolean,
    onSocks5Toggle: (Boolean) -> Unit,
    audioKeepAlive: Boolean,
    onAudioKeepAliveToggle: (Boolean) -> Unit,
    onOpenSettingsPage: () -> Unit,
    onOpenThemeBuilder: () -> Unit,
    onUnpair: () -> Unit,
    onBack: () -> Unit,
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var showUnpairDialog by rememberSaveable { mutableStateOf(false) }
    val readiness = readinessPresentation(status)
    val readinessAccentColor = toneColor(readiness.tone)
    val readinessTextColor = toneTextColor(readiness.tone)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 22.dp),
    ) {
        CockpitTopBar(
            title = vehicleName,
            onBack = onBack,
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Vehicle menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Unpair vehicle") },
                            onClick = {
                                showMenu = false
                                showUnpairDialog = true
                            },
                        )
                    }
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        CockpitCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionDot(readinessAccentColor, readiness.title)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(readiness.title, style = MaterialTheme.typography.headlineLarge, color = readinessTextColor)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        readiness.detail,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = status.ssid,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CockpitMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    when (readiness.tone) {
                        UiTone.Positive -> "CONNECTED"
                        UiTone.Warning -> "ATTENTION"
                        UiTone.Neutral -> "WAITING"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = readinessTextColor,
                )
            }
        }

        ProdigySectionLabel("Sharing", modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
        CockpitCard(modifier = Modifier.fillMaxWidth()) {
            SharingRow(
                title = "Vehicle data",
                detail = "Time · GPS · Battery",
                active = vehicleDataActive(status),
                inactiveLabel = "WAITING",
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            SharingRow(
                title = "Internet bridge",
                detail = "SOCKS5 sharing",
                active = internetBridgeActive(status),
                inactiveLabel = if (status.connected) "OFF" else "WAITING",
            )
        }

        ProdigySectionLabel("Quick actions", modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                label = "Head unit",
                enabled = status.headUnitAvailable,
                onClick = onOpenSettingsPage,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                label = "Appearance",
                enabled = status.headUnitAvailable,
                onClick = onOpenThemeBuilder,
                modifier = Modifier.weight(1f),
            )
        }

        ProdigySectionLabel("Vehicle options", modifier = Modifier.padding(top = 28.dp, bottom = 10.dp))
        CockpitCard(modifier = Modifier.fillMaxWidth()) {
            OptionRow(
                title = "Internet sharing",
                detail = "Share phone data through SOCKS5",
                checked = socks5Enabled,
                onCheckedChange = onSocks5Toggle,
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            OptionRow(
                title = "Audio keep-alive",
                detail = "Play silence to keep AA audio open",
                checked = audioKeepAlive,
                onCheckedChange = onAudioKeepAliveToggle,
            )
        }
        Spacer(Modifier.height(28.dp))
    }

    if (showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairDialog = false },
            title = { Text("Unpair $vehicleName?") },
            text = { Text("This removes the saved vehicle. You can pair it again later with a new PIN.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnpairDialog = false
                        onUnpair()
                    },
                ) { Text("Unpair", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                CockpitTextButton(onClick = { showUnpairDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SharingRow(
    title: String,
    detail: String,
    active: Boolean,
    inactiveLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConnectionDot(if (active) CockpitBlue else CockpitMuted, if (active) "$title active" else "$title inactive")
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = CockpitMuted)
        }
        Text(
            if (active) "ACTIVE" else inactiveLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) CockpitBlueText else CockpitMuted,
        )
    }
}

@Composable
private fun QuickActionCard(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CockpitCard(
        modifier = modifier
            .heightIn(min = 96.dp)
            .alpha(if (enabled) 1f else 0.5f),
        onClick = onClick.takeIf { enabled },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CockpitMuted)
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = CockpitMuted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PreviewStatusScreen(status: CompanionStatus) {
    OpenAutoCompanionTheme {
        StatusScreen(
            vehicleName = "Miata",
            status = status,
            socks5Enabled = true,
            onSocks5Toggle = {},
            audioKeepAlive = false,
            onAudioKeepAliveToggle = {},
            onOpenSettingsPage = {},
            onOpenThemeBuilder = {},
            onUnpair = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun ConnectedStatusPreview() {
    PreviewStatusScreen(
        CompanionStatus(
            connected = true,
            headUnitAvailable = true,
            sharingTime = true,
            sharingGps = true,
            sharingBattery = true,
            socks5Active = true,
            ssid = "Prodigy_e57d",
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun WaitingStatusPreview() {
    PreviewStatusScreen(CompanionStatus(ssid = "Prodigy_e57d"))
}

@Preview(showBackground = true, backgroundColor = 0xFF0C100E)
@Composable
private fun IssueStatusPreview() {
    PreviewStatusScreen(
        CompanionStatus(
            headUnitAvailable = true,
            connectionMessage = "Authentication failed",
            ssid = "Prodigy_e57d",
        ),
    )
}
