# Connection Pull-to-Refresh Design

## Context

The Garage and Vehicle Status screens observe `CompanionService` state through lifecycle-aware `StateFlow` collection. That UI path updates correctly when the service publishes a new value. The unreliable boundary is earlier: `WifiMonitor` scans current networks only when it is created, then depends on Android network callbacks for later transitions. `CompanionApp.startWifiMonitor()` intentionally reuses the application-owned monitor across Activity recreation, so an open app has no way to request a new current-network reconciliation when a callback is missed or delayed.

Closing the process and reopening it creates a new monitor, performs a fresh current-network scan, and resets any runtime retry backoff. The manual refresh behavior will expose those useful reconciliation semantics without requiring the user to close the app.

This work aligns with the project vision outcomes of predictable reconnection, clear connection status, low-friction operation, and no interruption to an already healthy Android Auto session.

## Goals

- Add pull-to-refresh to both Garage and Vehicle Status.
- Reconcile the current Android Wi-Fi state against stored paired vehicles on demand.
- Start or reconnect the Companion runtime when a paired vehicle network is present.
- Clear stale connection state when no paired vehicle network is present.
- Preserve an already healthy Companion runtime instead of restarting it unnecessarily.
- Keep displayed connection status authoritative: the UI must not optimistically claim success before `CompanionService.connected` becomes true.

## Non-Goals

- Add periodic background polling.
- Replace the existing Android network callback flow.
- Change pairing, stored credentials, protocol messages, report contents, or Android Auto networking.
- Claim that a refresh guarantees the head unit is reachable.
- Add a separate refresh button when the pull gesture is available.

## Architecture

### Refresh lifecycle contract

`MonitorLifecycle` will gain a `refresh()` operation. `MonitorSlot` will gain `refreshOrStart(next)`, with two behaviors:

- When a monitor exists, call `refresh()` on that existing application-owned monitor and do not stop or replace it.
- When no monitor exists, install and start `next` so refresh also works after lifecycle recovery.

`CompanionApp.refreshWifiMonitor(vehicles)` will create the fallback `WifiMonitor` and delegate to this slot operation. Normal Activity creation will continue using `startWifiMonitor()` and will continue reusing an existing monitor without rescanning.

### Wi-Fi reconciliation

`WifiMonitor.refresh()` will perform a current-network reconciliation using the same paired-vehicle SSID and credential rules as initial startup:

1. Inspect `ConnectivityManager.allNetworks` for a Wi-Fi network whose reported SSID matches a runtime-capable paired vehicle.
2. Use `WifiManager.connectionInfo` as the existing fallback when the primary scan cannot identify a match.
3. When a match is found, update `wifiNetwork` and `activeVehicle`, then request the Companion service for that vehicle.
4. When no match is found, clear `wifiNetwork` and `activeVehicle` and stop the Companion service so stale connected state cannot remain visible.

Every explicit pull will mark its service request as a manual refresh. `CompanionService` will evaluate that request against `_connected.value` on the service main thread: it bypasses the same-identity/same-network live-generation reuse guard only when the service is still disconnected, then begins a fresh runtime generation and resets retry backoff. If the service is connected when it handles the request, it uses the normal idempotent start path and preserves the healthy generation. This service-side decision removes the race between a monitor-side state read and intent handling.

The existing network callbacks remain the automatic path. Pull-to-refresh is a deterministic user-triggered fallback.

### UI integration

`VehicleListScreen` and `StatusScreen` will each accept an `onRefresh: () -> Unit` callback and wrap their existing vertical scrolling content in the Material 3 pull-to-refresh container available through the current Compose BOM.

`MainActivity` will provide one callback that invokes `CompanionApp.refreshWifiMonitor(prefs.vehicles)`. Both screens will use that callback.

The standard pull indicator will communicate gesture progress and retract after the reconciliation request is dispatched. It does not imply connection success. Existing service flows remain the only source for `CONNECTED`, `Ready`, `Waiting`, sharing-active states, and error copy.

Pull-to-refresh must remain reachable only when the scrollable content is at its top, preserving normal vertical scrolling and taps.

## Error Handling

- Missing runtime-capable paired vehicles: refresh leaves the app disconnected and does not start the service.
- No matching current Wi-Fi network: refresh clears monitor ownership and stops the service.
- Android network inspection failure: log the failure, attempt the existing `WifiManager` fallback, and resolve to disconnected if no valid match is found.
- Service authentication or identity failure: existing `connectionIssue` handling remains authoritative.
- Repeated pulls: each pull reconciles current state; a healthy runtime is reused, while a disconnected runtime may start a new generation and cancel the prior retry generation through existing token/cancellation safeguards.

## Testing Strategy

### Unit tests

- `MonitorSlotTest`: an existing monitor receives exactly one refresh and is neither stopped nor replaced.
- `MonitorSlotTest`: refresh with an empty slot starts the provided monitor.
- Runtime reconnect decision test: a refresh request received while connected does not force a new generation.
- Runtime reconnect decision test: a refresh request received while disconnected forces a new generation.
- Runtime reuse decision test: a forced reconnect bypasses reuse and starts a fresh generation.

Tests will be written and observed failing before production changes.

### Automated gate

Run the repository-required command:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

### Device verification

On Pixel `39260DLJH000LX`:

1. Install the new debug APK.
2. Confirm pull-to-refresh is reachable at the top of both Garage and Vehicle Status.
3. Pull while already connected and confirm the screen remains connected without a visible service teardown.
4. Exercise a disconnected state when it can be done without disrupting an active Android Auto session, pull once, and confirm the UI follows the resulting real service state without closing the app.
5. Record any part of the disconnected transition that cannot be safely exercised as pending rather than claiming it passed.

## Acceptance Criteria

- Both Garage and Vehicle Status expose the pull gesture.
- A pull invokes application-level monitor reconciliation.
- Refresh starts monitoring when no monitor exists.
- Refresh requests an immediate new runtime generation when a paired vehicle network is present but the service is disconnected.
- Refresh does not restart a healthy connected runtime.
- Refresh stops stale service state when no paired vehicle network is present.
- Connection labels change only in response to existing service flows.
- Unit tests and debug APK assembly pass.
- The behavior and verification result are appended to `docs/session-handoffs.md`.
