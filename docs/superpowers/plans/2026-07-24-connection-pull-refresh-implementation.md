# Connection Pull-to-Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add pull-to-refresh on Garage and Vehicle Status so the user can reconcile paired Wi-Fi and Companion runtime state without closing the app.

**Architecture:** The application-owned monitor gains an explicit refresh lifecycle that reuses an existing monitor or starts one when absent. `WifiMonitor` reconciles the current Android Wi-Fi network, asks a disconnected runtime to bypass retry backoff, preserves an already healthy runtime, and clears stale service state when no paired network is present. Both Compose screens forward the same Activity callback through Material 3 `PullToRefreshBox`; existing service flows remain the only source of displayed connection truth.

**Tech Stack:** Kotlin 2.x, Android foreground services, `ConnectivityManager`, `WifiManager`, Kotlin `StateFlow`, Jetpack Compose Material 3 1.3.1 `PullToRefreshBox`, JUnit 4, Gradle.

## Global Constraints

- Preserve pairing credentials, External API v1 protocol behavior, reports, persistence, and Android Auto networking.
- Do not add periodic polling or a new background worker.
- Do not optimistically mutate UI connection state during refresh.
- Do not restart a healthy connected runtime.
- A disconnected matched runtime must bypass existing retry backoff on manual refresh.
- No paired network after reconciliation must stop stale Companion service state.
- Use the existing Compose BOM `2024.12.01`; do not add a new UI dependency.
- Before completion run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- Append verification evidence to `docs/session-handoffs.md`.

---

### Task 1: Application-Owned Monitor Refresh Lifecycle

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/MonitorSlot.kt`
- Modify: `app/src/main/java/org/openauto/companion/CompanionApp.kt`
- Test: `app/src/test/java/org/openauto/companion/MonitorSlotTest.kt`

**Interfaces:**
- Consumes: existing `MonitorLifecycle.start()`, `MonitorLifecycle.stop()`, and `MonitorSlot.startIfAbsent(next)`.
- Produces: `MonitorLifecycle.refresh()`, `MonitorSlot.refreshOrStart(next: T)`, and `CompanionApp.refreshWifiMonitor(vehicles: List<Vehicle>)`.

- [ ] **Step 1: Write failing monitor refresh tests**

Extend the fake monitor with `refreshCount`, then add these tests:

```kotlin
@Test
fun refreshOrStartRefreshesExistingMonitorWithoutReplacingIt() {
    val slot = MonitorSlot<FakeMonitor>()
    val first = FakeMonitor()
    val replacement = FakeMonitor()
    slot.startIfAbsent(first)

    slot.refreshOrStart(replacement)

    assertEquals(1, first.startCount)
    assertEquals(1, first.refreshCount)
    assertEquals(0, first.stopCount)
    assertEquals(0, replacement.startCount)
    assertEquals(0, replacement.refreshCount)
    assertSame(first, slot.current)
}

@Test
fun refreshOrStartStartsProvidedMonitorWhenSlotIsEmpty() {
    val slot = MonitorSlot<FakeMonitor>()
    val monitor = FakeMonitor()

    slot.refreshOrStart(monitor)

    assertEquals(1, monitor.startCount)
    assertEquals(0, monitor.refreshCount)
    assertSame(monitor, slot.current)
}
```

Update `FakeMonitor` with:

```kotlin
var refreshCount = 0

override fun refresh() {
    refreshCount += 1
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.openauto.companion.MonitorSlotTest
```

Expected: compilation fails because `MonitorLifecycle.refresh()` and `MonitorSlot.refreshOrStart()` do not exist.

- [ ] **Step 3: Implement the minimal lifecycle contract**

Add to `MonitorLifecycle`:

```kotlin
fun refresh()
```

Add to `MonitorSlot`:

```kotlin
fun refreshOrStart(next: T) {
    val existing = current
    if (existing != null) {
        existing.refresh()
        return
    }
    current = next
    next.start()
}
```

Add to `CompanionApp`:

```kotlin
fun refreshWifiMonitor(vehicles: List<Vehicle>) {
    wifiMonitorSlot.refreshOrStart(WifiMonitor(this, vehicles))
}
```

Do not change `startWifiMonitor()`: Activity recreation must still avoid an unsolicited rescan.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.openauto.companion.MonitorSlotTest
```

Expected: all `MonitorSlotTest` tests pass.

- [ ] **Step 5: Commit Task 1**

```bash
git add app/src/main/java/org/openauto/companion/MonitorSlot.kt app/src/main/java/org/openauto/companion/CompanionApp.kt app/src/test/java/org/openauto/companion/MonitorSlotTest.kt
git commit -m "feat: add monitor refresh lifecycle"
```

---

### Task 2: Wi-Fi Reconciliation and Immediate Disconnected Retry

**Files:**
- Create: `app/src/main/java/org/openauto/companion/service/RuntimeGenerationPolicy.kt`
- Create: `app/src/test/java/org/openauto/companion/service/RuntimeGenerationPolicyTest.kt`
- Modify: `app/src/main/java/org/openauto/companion/service/WifiMonitor.kt`
- Modify: `app/src/main/java/org/openauto/companion/service/CompanionService.kt`

**Interfaces:**
- Consumes: `MonitorLifecycle.refresh()`, `CompanionService.connected.value`, existing `WifiMonitor.checkCurrentNetwork()`, and generation token/cancellation safeguards.
- Produces: `shouldForceRuntimeReconnect(refreshRequested, connected)`, `shouldReuseRuntimeGeneration(forceReconnect, sameVehicle, sameNetwork, runtimeActive)`, `WifiMonitor.refresh()`, and `CompanionService.EXTRA_FORCE_RECONNECT`.

- [ ] **Step 1: Write failing runtime reuse policy tests**

Create `RuntimeGenerationPolicyTest.kt`:

```kotlin
package org.openauto.companion.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeGenerationPolicyTest {
    @Test
    fun refreshRequestForConnectedRuntimeDoesNotForceReconnect() {
        assertFalse(
            shouldForceRuntimeReconnect(
                refreshRequested = true,
                connected = true,
            )
        )
    }

    @Test
    fun refreshRequestForDisconnectedRuntimeForcesReconnect() {
        assertTrue(
            shouldForceRuntimeReconnect(
                refreshRequested = true,
                connected = false,
            )
        )
    }

    @Test
    fun healthyMatchingRuntimeIsReused() {
        assertTrue(
            shouldReuseRuntimeGeneration(
                forceReconnect = false,
                sameVehicle = true,
                sameNetwork = true,
                runtimeActive = true,
            )
        )
    }

    @Test
    fun manualReconnectBypassesMatchingActiveRuntimeReuse() {
        assertFalse(
            shouldReuseRuntimeGeneration(
                forceReconnect = true,
                sameVehicle = true,
                sameNetwork = true,
                runtimeActive = true,
            )
        )
    }

    @Test
    fun mismatchedOrInactiveRuntimeIsNeverReused() {
        assertFalse(shouldReuseRuntimeGeneration(false, false, true, true))
        assertFalse(shouldReuseRuntimeGeneration(false, true, false, true))
        assertFalse(shouldReuseRuntimeGeneration(false, true, true, false))
    }
}
```

- [ ] **Step 2: Run the focused policy test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.openauto.companion.service.RuntimeGenerationPolicyTest
```

Expected: compilation fails with unresolved `shouldForceRuntimeReconnect` and `shouldReuseRuntimeGeneration`.

- [ ] **Step 3: Implement the pure reuse policy**

Create `RuntimeGenerationPolicy.kt`:

```kotlin
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
```

- [ ] **Step 4: Run the focused policy test and verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.openauto.companion.service.RuntimeGenerationPolicyTest
```

Expected: all three policy tests pass.

- [ ] **Step 5: Teach the service to honor manual reconnect**

In `CompanionService.onStartCommand()`, read:

```kotlin
val refreshRequested = intent?.getBooleanExtra(EXTRA_FORCE_RECONNECT, false) == true
val forceReconnect = shouldForceRuntimeReconnect(
    refreshRequested = refreshRequested,
    connected = _connected.value,
)
```

Replace the inline same-runtime guard with:

```kotlin
if (shouldReuseRuntimeGeneration(
        forceReconnect = forceReconnect,
        sameVehicle = config.vehicleIdentity == currentVehicleKey,
        sameNetwork = config.wifiNetwork == currentWifiNetwork,
        runtimeActive = generationJob?.isActive == true,
    )
) {
    currentResources?.setInternetSharingEnabled(config.socks5Enabled)
    updateNotification(
        if (_connected.value) {
            "Connected to $currentVehicleName"
        } else {
            "Connecting to $currentVehicleName..."
        }
    )
    return START_STICKY
}
```

Add the public intent-extra key beside the existing service extras:

```kotlin
const val EXTRA_FORCE_RECONNECT = "force_reconnect"
```

When `forceReconnect` is true, existing `startRuntimeGeneration(config)` behavior cancels the previous generation, increments the token, resets runtime state, and starts retry attempt zero.

- [ ] **Step 6: Implement refresh reconciliation in `WifiMonitor`**

Add:

```kotlin
override fun refresh() {
    checkCurrentNetwork(
        forceReconnect = true,
        clearWhenMissing = true,
    )
}
```

Change the existing scanner signature to:

```kotlin
private fun checkCurrentNetwork(
    forceReconnect: Boolean = false,
    clearWhenMissing: Boolean = false,
)
```

Pass `forceReconnect` through both the `ConnectivityManager` match and `WifiManager` fallback:

```kotlin
startCompanionService(vehicle, forceReconnect)
```

At the no-match exit, reconcile stale ownership only for manual refresh:

```kotlin
if (clearWhenMissing) {
    wifiNetwork = null
    activeVehicle = null
    stopCompanionService()
}
Log.i(TAG, "No paired vehicle SSID found in current networks")
```

Change the service starter signature and include the flag:

```kotlin
private fun startCompanionService(
    vehicle: Vehicle,
    forceReconnect: Boolean = false,
) {
    // existing credential validation and extras
    intent.putExtra(CompanionService.EXTRA_FORCE_RECONNECT, forceReconnect)
    ContextCompat.startForegroundService(context, intent)
}
```

Existing network callback calls continue using the default `false`; only explicit refresh can bypass reuse. Initial `start()` continues calling `checkCurrentNetwork()` with both defaults.

- [ ] **Step 7: Run the service-focused tests and full unit suite**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.openauto.companion.MonitorSlotTest --tests org.openauto.companion.service.RuntimeGenerationPolicyTest
./gradlew :app:testDebugUnitTest
```

Expected: focused tests and the complete unit suite pass with zero failures.

- [ ] **Step 8: Commit Task 2**

```bash
git add app/src/main/java/org/openauto/companion/service/RuntimeGenerationPolicy.kt app/src/test/java/org/openauto/companion/service/RuntimeGenerationPolicyTest.kt app/src/main/java/org/openauto/companion/service/WifiMonitor.kt app/src/main/java/org/openauto/companion/service/CompanionService.kt
git commit -m "fix: reconcile connection state on refresh"
```

---

### Task 3: Pull Gesture on Garage and Vehicle Status

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt`
- Modify: `app/src/main/java/org/openauto/companion/ui/StatusScreen.kt`
- Modify: `app/src/main/java/org/openauto/companion/ui/MainActivity.kt`

**Interfaces:**
- Consumes: `CompanionApp.refreshWifiMonitor(vehicles)`, existing screen scroll containers, and Material 3 1.3.1 `PullToRefreshBox`.
- Produces: `onRefresh: () -> Unit` parameters on `VehicleListScreen` and `StatusScreen`.

- [ ] **Step 1: Add the Activity refresh callback**

Inside `setContent`, after the collected service flows and before screen routing, define:

```kotlin
val refreshConnection: () -> Unit = {
    (application as CompanionApp).refreshWifiMonitor(prefs.vehicles)
}
```

Pass `onRefresh = refreshConnection` to both `VehicleListScreen` and `StatusScreen`.

- [ ] **Step 2: Wrap Garage in Material pull-to-refresh**

Import:

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
```

Add `onRefresh: () -> Unit` to the screen signature, annotate the composable with `@OptIn(ExperimentalMaterial3Api::class)`, and wrap the current root `Column`:

```kotlin
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
        // existing LazyColumn and Add vehicle button unchanged
    }
}
```

Update both Garage previews with `onRefresh = {}`.

The controlled `isRefreshing` state is intentional. Device verification against Material 3 1.3.1 showed that a threshold-triggered pull remains at the indicator threshold when the value is permanently `false`; the pinned implementation hides only after observing a real `true` to `false` transition. `LaunchedEffect` dispatches reconciliation once after the `true` state is applied, holds it through one Compose frame, and clears it in `finally`. This is frame synchronization rather than an arbitrary timeout. Connection success remains driven by service flows.

- [ ] **Step 3: Wrap Vehicle Status in Material pull-to-refresh**

Use the same imports and opt-in. Add `onRefresh: () -> Unit` to the signature and wrap the existing scrollable `Column`:

```kotlin
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
            .verticalScroll(rememberScrollState())
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 22.dp),
    ) {
        // existing status content unchanged
    }
}
```

Update the Status preview with `onRefresh = {}`.

- [ ] **Step 4: Compile the UI integration**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: compilation succeeds against the resolved Material 3 1.3.1 API. No separate JVM assertion is added for Material nested-scroll mechanics; the production behavior under test is the refresh lifecycle from Tasks 1 and 2, while gesture recognition is verified on-device in Task 4.

- [ ] **Step 5: Run all unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all existing and new unit tests pass.

- [ ] **Step 6: Commit Task 3**

```bash
git add app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt app/src/main/java/org/openauto/companion/ui/StatusScreen.kt app/src/main/java/org/openauto/companion/ui/MainActivity.kt
git commit -m "feat: add connection pull refresh"
```

---

### Task 4: Verification, Device Evidence, and Handoff

**Files:**
- Modify: `docs/roadmap-current.md`
- Modify: `docs/session-handoffs.md`

**Interfaces:**
- Consumes: final debug APK, Pixel `39260DLJH000LX`, Windows ADB at `/mnt/e/Android/Sdk/platform-tools/adb.exe`, and the repository verification gate.
- Produces: reproducible verification evidence and an updated active reliability outcome.

- [ ] **Step 1: Run static scope checks**

```bash
git diff --check
git status --short --branch
rg -n "refreshOrStart|refreshWifiMonitor|EXTRA_FORCE_RECONNECT|PullToRefreshBox" app/src/main app/src/test
```

Expected: clean diff check; only planned source, test, and documentation files are changed; all required refresh links are present.

- [ ] **Step 2: Run the required full gate**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, all unit tests pass, and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Install the APK on the Pixel**

```bash
/mnt/e/Android/Sdk/platform-tools/adb.exe devices
/mnt/e/Android/Sdk/platform-tools/adb.exe -s 39260DLJH000LX install -r 'E:\claude\personal\openautopro\openauto-companion\app\build\outputs\apk\debug\app-debug.apk'
```

Expected: Pixel is listed as `device`; install returns `Success`.

- [ ] **Step 4: Verify both pull gestures without disrupting a healthy runtime**

On the Pixel:

1. Open Garage at scroll position zero and pull down past the Material threshold.
2. Confirm the indicator appears and the connected state remains correct after release.
3. Open Vehicle Status at scroll position zero and repeat.
4. Confirm no visible `Ready` → `Waiting` transition occurs during a healthy connected refresh.
5. Inspect filtered `WifiMonitor` and `CompanionService` logs to confirm reconciliation used the healthy runtime reuse path.

If a safe disconnected condition is available without interrupting Android Auto, pull once and confirm it starts an immediate attempt without closing the Activity. Otherwise record disconnected-path device validation as pending; the unit-tested policy and build gate may be claimed, but not the unexercised live transition.

- [ ] **Step 5: Update roadmap and handoff**

In `docs/roadmap-current.md`, update the active connection-reliability outcome to mention that Garage and Vehicle Status now expose user-triggered current-network reconciliation while automatic callback reliability remains under observation.

Append a dated `docs/session-handoffs.md` entry containing:

- what changed;
- why the stale state occurred;
- status and Companion-only dependency decision;
- next one to three steps;
- focused RED/GREEN evidence;
- full Gradle gate result and test total;
- APK path and SHA-256;
- install result;
- exact Garage/Status gesture observations;
- disconnected-path and Android Auto continuity status without overclaiming.

- [ ] **Step 6: Commit Task 4**

```bash
git add docs/roadmap-current.md docs/session-handoffs.md
git commit -m "docs: record connection refresh verification"
```

- [ ] **Step 7: Request final independent review**

Ask a fresh reviewer to inspect the complete implementation from design commit `4d1a528` through `HEAD` for:

- approved-spec compliance;
- healthy-runtime preservation;
- disconnected retry reset;
- stale-state clearing;
- callback and lifecycle regressions;
- Compose scroll/gesture integration;
- test quality;
- verification honesty.

Fix every Critical or Important finding through a new RED/GREEN cycle, rerun `./gradlew :app:testDebugUnitTest :app:assembleDebug`, and re-request review before declaring completion.
