# Calm Cockpit UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved Calm Cockpit visual refresh for the garage, vehicle status, add-vehicle, and QR scanner screens without changing runtime, network, persistence, or pairing protocol behavior.

**Architecture:** Keep `MainActivity` as the existing screen/state owner, introduce a focused dark Compose theme and small shared UI primitives, and map runtime status to testable presentation values with pure Kotlin helpers. Screen composables continue to receive the same models and callbacks; only layout, hierarchy, accessibility, and presentation copy change.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, CameraX, ML Kit barcode scanning, JUnit 4, Android SDK 35.

---

## File Structure

- Create `app/src/main/java/org/openauto/companion/ui/UiPresentation.kt`: pure, JVM-testable copy and semantic-tone mapping.
- Create `app/src/test/java/org/openauto/companion/ui/UiPresentationTest.kt`: presentation mapping and manual-pairing validation tests.
- Create `app/src/main/java/org/openauto/companion/ui/theme/OpenAutoTheme.kt`: dark palette, typography, shapes, system-bar treatment, semantic colors.
- Create `app/src/main/java/org/openauto/companion/ui/CockpitComponents.kt`: shared top bar, section label, card, and state indicator.
- Modify `app/src/main/java/org/openauto/companion/ui/MainActivity.kt`: install the branded theme and adjust refreshed screen signatures only.
- Modify `app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt`: garage hierarchy and promoted connected vehicle.
- Modify `app/src/main/java/org/openauto/companion/ui/StatusScreen.kt`: readiness hero, grouped status, quick actions, options, and unpair overflow.
- Modify `app/src/main/java/org/openauto/companion/ui/PairingScreen.kt`: QR-first hierarchy and collapsible manual PIN form.
- Modify `app/src/main/java/org/openauto/companion/ui/QrScanScreen.kt`: scanner hierarchy, clipped preview, guides, and consistent errors.
- Modify `app/src/main/res/values/themes.xml`: dark launch window and matching system bars.
- Modify `docs/roadmap-current.md`: promote the approved UI refresh from Later to Now/recent completion as appropriate.
- Modify `docs/session-handoffs.md`: record the behavior-changing UI handoff and fresh verification evidence.

### Task 1: Presentation Rules — RED

**Files:**
- Create: `app/src/test/java/org/openauto/companion/ui/UiPresentationTest.kt`

- [ ] **Step 1: Write the failing presentation tests**

Create the following tests before production code:

```kotlin
package org.openauto.companion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPresentationTest {
    @Test
    fun `garage summary reports connected vehicle`() {
        assertEquals("1 vehicle · connected now", garageSummary(1, true))
        assertEquals("2 vehicles · connected now", garageSummary(2, true))
    }

    @Test
    fun `garage summary reports waiting garage`() {
        assertEquals("No vehicles yet", garageSummary(0, false))
        assertEquals("1 vehicle · waiting", garageSummary(1, false))
        assertEquals("3 vehicles · waiting", garageSummary(3, false))
    }

    @Test
    fun `connected readiness is ready and positive`() {
        val result = readinessPresentation(
            CompanionStatus(connected = true, headUnitAvailable = true, ssid = "Prodigy_e57d")
        )

        assertEquals("Ready", result.title)
        assertEquals("Connected · sharing normally", result.detail)
        assertEquals(UiTone.Positive, result.tone)
    }

    @Test
    fun `connection issue takes warning precedence`() {
        val result = readinessPresentation(
            CompanionStatus(
                connected = false,
                headUnitAvailable = true,
                connectionMessage = "Authentication failed",
                ssid = "Prodigy_e57d"
            )
        )

        assertEquals("Needs attention", result.title)
        assertEquals("Authentication failed", result.detail)
        assertEquals(UiTone.Warning, result.tone)
    }

    @Test
    fun `unavailable vehicle waits neutrally`() {
        val result = readinessPresentation(
            CompanionStatus(connected = false, headUnitAvailable = false, ssid = "Prodigy_e57d")
        )

        assertEquals("Waiting", result.title)
        assertEquals("Waiting for Prodigy_e57d", result.detail)
        assertEquals(UiTone.Neutral, result.tone)
    }

    @Test
    fun `sharing groups require live connection`() {
        val disconnected = CompanionStatus(connected = false, socks5Active = true)
        val connected = CompanionStatus(connected = true, socks5Active = true)

        assertFalse(vehicleDataActive(disconnected))
        assertFalse(internetBridgeActive(disconnected))
        assertTrue(vehicleDataActive(connected))
        assertTrue(internetBridgeActive(connected))
    }

    @Test
    fun `manual pairing requires ssid six digits and idle state`() {
        assertTrue(canSubmitManualPairing("Prodigy", "123456", false))
        assertFalse(canSubmitManualPairing("", "123456", false))
        assertFalse(canSubmitManualPairing("Prodigy", "12345", false))
        assertFalse(canSubmitManualPairing("Prodigy", "12345x", false))
        assertFalse(canSubmitManualPairing("Prodigy", "123456", true))
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.openauto.companion.ui.UiPresentationTest
```

Expected: compilation fails because `garageSummary`, `readinessPresentation`,
`UiTone`, `vehicleDataActive`, `internetBridgeActive`, and
`canSubmitManualPairing` do not exist.

- [ ] **Step 3: Commit the failing tests**

```bash
git add app/src/test/java/org/openauto/companion/ui/UiPresentationTest.kt
git commit -m "test: define calm cockpit presentation rules"
```

### Task 2: Presentation Rules — GREEN

**Files:**
- Create: `app/src/main/java/org/openauto/companion/ui/UiPresentation.kt`
- Test: `app/src/test/java/org/openauto/companion/ui/UiPresentationTest.kt`

- [ ] **Step 1: Add the minimal presentation implementation**

Create:

```kotlin
package org.openauto.companion.ui

enum class UiTone { Positive, Neutral, Warning }

data class ReadinessPresentation(
    val title: String,
    val detail: String,
    val tone: UiTone
)

internal fun garageSummary(vehicleCount: Int, connected: Boolean): String = when {
    vehicleCount == 0 -> "No vehicles yet"
    connected -> "${vehicleCount} ${if (vehicleCount == 1) "vehicle" else "vehicles"} · connected now"
    else -> "${vehicleCount} ${if (vehicleCount == 1) "vehicle" else "vehicles"} · waiting"
}

internal fun readinessPresentation(status: CompanionStatus): ReadinessPresentation = when {
    status.connected -> ReadinessPresentation(
        title = "Ready",
        detail = "Connected · sharing normally",
        tone = UiTone.Positive
    )
    !status.connectionMessage.isNullOrBlank() -> ReadinessPresentation(
        title = "Needs attention",
        detail = status.connectionMessage,
        tone = UiTone.Warning
    )
    else -> ReadinessPresentation(
        title = "Waiting",
        detail = "Waiting for ${status.ssid}",
        tone = UiTone.Neutral
    )
}

internal fun vehicleDataActive(status: CompanionStatus): Boolean = status.connected

internal fun internetBridgeActive(status: CompanionStatus): Boolean =
    status.connected && status.socks5Active

internal fun canSubmitManualPairing(
    ssid: String,
    pin: String,
    pairingInProgress: Boolean
): Boolean = !pairingInProgress &&
    ssid.trim().isNotEmpty() &&
    pin.length == 6 &&
    pin.all(Char::isDigit)
```

- [ ] **Step 2: Run the focused test and verify GREEN**

Run the Task 1 focused command. Expected: all `UiPresentationTest` tests pass.

- [ ] **Step 3: Run all unit tests**

Run: `./gradlew :app:testDebugUnitTest`

Expected: existing tests plus `UiPresentationTest` pass.

- [ ] **Step 4: Commit the implementation**

```bash
git add app/src/main/java/org/openauto/companion/ui/UiPresentation.kt
git commit -m "feat: add calm cockpit presentation mapping"
```

### Task 3: Branded Theme Foundation

**Files:**
- Create: `app/src/main/java/org/openauto/companion/ui/theme/OpenAutoTheme.kt`
- Create: `app/src/main/java/org/openauto/companion/ui/CockpitComponents.kt`
- Modify: `app/src/main/java/org/openauto/companion/ui/MainActivity.kt`
- Modify: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Add the theme implementation**

Create `OpenAutoTheme.kt` with named colors for `CockpitBackground` (`0xFF0C100E`),
`CockpitSurface` (`0xFF151B18`), `CockpitSurfaceRaised` (`0xFF1B231F`),
`CockpitOutline` (`0xFF2A342E`), `CockpitMint` (`0xFF7DE3A6`),
`CockpitOnMint` (`0xFF0A2114`), `CockpitText` (`0xFFF4F7F3`),
`CockpitMuted` (`0xFF9BA69F`), and `CockpitWarning` (`0xFFF1B86C`).

Define a `darkColorScheme` mapping primary/onPrimary, background/onBackground,
surface/onSurface, surfaceVariant/onSurfaceVariant, outline/outlineVariant,
secondary, tertiary, and Material dark error roles. Define rounded shapes with
12 dp small, 18 dp medium, and 26 dp large corners. Define typography using the
platform sans-serif with bold 34 sp display, 26 sp headline, 20 sp title, 16 sp
body, 14 sp supporting, and 11 sp uppercase-label styles.

Expose:

```kotlin
@Composable
fun OpenAutoCompanionTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = CockpitBackground.toArgb()
            window.navigationBarColor = CockpitBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = CockpitColorScheme,
        typography = CockpitTypography,
        shapes = CockpitShapes,
        content = content
    )
}
```

- [ ] **Step 2: Add shared cockpit components**

Create `CockpitComponents.kt` containing:

- `CockpitTopBar(title, onBack, actions)` with a 48 dp back target;
- `ProdigySectionLabel(text)` using the uppercase label style and muted color;
- `CockpitCard(modifier, highlighted, content)` using raised/surface colors,
  large shape, and a one-dp outline;
- `ConnectionDot(color, contentDescription)` with semantics;
- `toneColor(UiTone)` mapping positive to mint, warning to amber, and neutral
  to muted.

- [ ] **Step 3: Install the theme at the app root**

Replace `MaterialTheme {` in `MainActivity.setContent` with:

```kotlin
OpenAutoCompanionTheme {
```

and import `org.openauto.companion.ui.theme.OpenAutoCompanionTheme`.

- [ ] **Step 4: Make the launch window dark**

Replace `themes.xml` with a dark, no-action-bar theme that sets
`android:windowBackground`, `android:statusBarColor`,
`android:navigationBarColor`, `android:windowLightStatusBar`, and
`android:windowLightNavigationBar` consistently to the cockpit background.

- [ ] **Step 5: Compile the theme foundation**

Run: `./gradlew :app:assembleDebug`

Expected: debug APK compiles.

- [ ] **Step 6: Commit the theme foundation**

```bash
git add app/src/main/java/org/openauto/companion/ui/theme/OpenAutoTheme.kt app/src/main/java/org/openauto/companion/ui/CockpitComponents.kt app/src/main/java/org/openauto/companion/ui/MainActivity.kt app/src/main/res/values/themes.xml
git commit -m "feat: add calm cockpit design system"
```

### Task 4: Garage Screen

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt`
- Modify: `app/src/main/java/org/openauto/companion/ui/MainActivity.kt`

- [ ] **Step 1: Replace the garage hierarchy**

Rewrite `VehicleListScreen` to:

- use `WindowInsets.safeDrawing` padding;
- show `PRODIGY`, `Your garage`, and `garageSummary(vehicles.size, connectedSsid != null)`;
- sort the connected vehicle first without changing stored order;
- render a promoted connected `CockpitCard` with `CONNECTED`, vehicle name,
  SSID when distinct, `Data sharing active`, and a chevron;
- render waiting cards with vehicle name, optional SSID, and
  `Waiting for vehicle`;
- use a `LazyColumn` with content padding and a full-width `Add vehicle` button;
- retain the maximum-vehicle disabled rule;
- remove logo, external-video easter egg, inline delete icon, delete dialog,
  and `onRemoveVehicle` parameter.

Add previews for one connected/two-vehicle state and an empty state using a
private sample `Vehicle` with a valid 64-character secret string.

- [ ] **Step 2: Update the call site**

Remove `onRemoveVehicle` from the `VehicleListScreen` call in `MainActivity`.
Do not alter the existing unpair callback on `StatusScreen`.

- [ ] **Step 3: Compile and run unit tests**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: tests pass and debug APK compiles.

- [ ] **Step 4: Commit the garage screen**

```bash
git add app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt app/src/main/java/org/openauto/companion/ui/MainActivity.kt
git commit -m "feat: refresh garage screen"
```

### Task 5: Vehicle Status Screen

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/ui/StatusScreen.kt`

- [ ] **Step 1: Build the readiness hierarchy**

Use `readinessPresentation(status)` to render a highlighted hero containing:

- tone-colored dot/icon;
- title and detail;
- SSID;
- textual `CONNECTED`, `WAITING`, or `ATTENTION` state.

The whole page uses `verticalScroll`, safe drawing padding, and spacing that
does not depend on fixed card heights.

- [ ] **Step 2: Group sharing and actions**

Add a `Sharing` card with two rows:

- Vehicle data / `Time · GPS · Battery`, active from `vehicleDataActive`;
- Internet bridge / `SOCKS5 sharing`, active from `internetBridgeActive`.

Add equal-weight `Head unit` and `Appearance` action cards wired to the existing
callbacks. Disabled actions remain visible when the head unit is unavailable.

- [ ] **Step 3: Contain vehicle options and destructive action**

Render Internet Sharing and Audio Keep Alive switches in a single options card
with explanatory copy. Add an overflow menu in the top bar with `Unpair`, then
show the existing confirmation meaning before invoking `onUnpair`.

- [ ] **Step 4: Add representative previews**

Add connected, waiting, and issue previews inside `OpenAutoCompanionTheme`.
Callbacks are empty lambdas and previews must compile without Android runtime
state.

- [ ] **Step 5: Compile and verify tests**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: all tests pass and the status previews compile.

- [ ] **Step 6: Commit the status screen**

```bash
git add app/src/main/java/org/openauto/companion/ui/StatusScreen.kt
git commit -m "feat: refresh vehicle status screen"
```

### Task 6: QR-First Add Vehicle Screen

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/ui/PairingScreen.kt`

- [ ] **Step 1: Make QR the primary path**

Replace the centered all-at-once form with a safe-inset, vertically scrollable
page containing:

- optional back/cancel top bar;
- `Pair in seconds` heading and External API guidance;
- decorative QR illustration card;
- full-width primary `Scan QR` button;
- `Can't scan?` disclosure card.

Use `rememberSaveable { mutableStateOf(false) }` so the manual form is collapsed
by default and survives activity recreation.

- [ ] **Step 2: Preserve manual behavior in the disclosure**

When expanded, render the existing SSID, optional vehicle name, and numeric PIN
fields. Continue limiting PIN input to six digits and use
`canSubmitManualPairing(ssid, pin, pairing)` for the Pair button. Preserve the
exact callback fallback `name.trim().ifEmpty { ssid.trim() }`.

- [ ] **Step 3: Style progress and failure states**

Disable QR, disclosure, inputs, cancel, and pair actions while pairing. Render
pairing progress inside the Pair button and render `PairingUiState.Failed` in a
rounded Material error container near the pairing actions.

- [ ] **Step 4: Add idle, manual-expanded, and failed previews**

Add previews inside `OpenAutoCompanionTheme`. Add the internal parameter
`manualExpandedInitially: Boolean = false` to `PairingScreen`, initialize the
saveable disclosure state from that parameter, and pass `true` only from the
expanded preview. Production call sites continue using the default `false`.

- [ ] **Step 5: Run focused and full checks**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.openauto.companion.ui.UiPresentationTest
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: manual validation test and full build pass.

- [ ] **Step 6: Commit the add-vehicle screen**

```bash
git add app/src/main/java/org/openauto/companion/ui/PairingScreen.kt
git commit -m "feat: make vehicle pairing QR first"
```

### Task 7: QR Scanner Presentation

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/ui/QrScanScreen.kt`

- [ ] **Step 1: Add the scanner page hierarchy**

Use `CockpitTopBar("Scan pairing code", onCancel)`, a concise instruction, and
a content area that preserves the existing permission state machine.

- [ ] **Step 2: Frame the existing camera preview**

Clip `CameraPreviewWithScanner` to a large rounded shape, add a subtle outline,
and overlay a pointer-transparent `ScanFrameOverlay`. Implement the overlay with
Compose `Canvas`, drawing four mint corner paths with round caps. Do not alter
the analyzer, one-shot atomic guard, ML Kit options, lifecycle binding, or
executor cleanup.

- [ ] **Step 3: Unify permission and camera errors**

Render permission-pending, permission-denied, and camera-error messages inside
cockpit cards with explicit text and back action. Keep `Go Back` and `Cancel`
semantics available.

- [ ] **Step 4: Compile and verify regressions**

Run: `./gradlew :app:testDebugUnitTest :app:assembleDebug`

Expected: existing QR parser and API tests pass; debug APK compiles.

- [ ] **Step 5: Commit the scanner presentation**

```bash
git add app/src/main/java/org/openauto/companion/ui/QrScanScreen.kt
git commit -m "feat: refresh QR scanner presentation"
```

### Task 8: Visual QA, Roadmap, and Handoff

**Files:**
- Modify: `docs/roadmap-current.md`
- Modify: `docs/session-handoffs.md`

- [ ] **Step 1: Inspect the complete diff against the spec**

Run:

```bash
git diff dev...HEAD -- app/src/main docs/superpowers
git diff --check
```

Confirm every acceptance criterion in the design spec has a corresponding
implementation. Correct missing semantics, clipped layouts, accidental runtime
changes, or inconsistent color use before proceeding.

- [ ] **Step 2: Run the repository-required full verification gate**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, zero failing tests, and a generated debug APK.

- [ ] **Step 3: Perform Pixel visual QA when the device is attached**

Run:

```bash
/mnt/e/Android/Sdk/platform-tools/adb.exe devices
/mnt/e/Android/Sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```

Inspect garage, connected/waiting status, QR-first/manual pairing, scanner
permission/error handling, scroll reachability, and system bars. Capture a
screenshot with `adb exec-out screencap -p` when the device is attached. If no
device is attached, record that device QA remains pending instead of treating
it as passed.

- [ ] **Step 4: Update the roadmap**

Move `Evaluate UI polish pass for status and pairing screens` out of `Later`.
Record Calm Cockpit as active while implementation is in progress, or in
Recently Completed only after the full gate and visual inspection are complete.
Do not change unrelated SOCKS5 priorities.

- [ ] **Step 5: Append the required handoff**

Add a dated entry to `docs/session-handoffs.md` containing:

- what changed and why;
- done/in-progress status;
- Companion-only dependency decision;
- wishlist promotion source as `UI polish pass`;
- next one to three steps;
- exact Gradle results;
- Pixel/ADB result if attempted;
- Android Auto continuity marked not exercised because runtime behavior was
  intentionally untouched, unless it was directly observed.

- [ ] **Step 6: Re-run doc and final verification checks**

Run:

```bash
git diff --check
./gradlew :app:testDebugUnitTest :app:assembleDebug
git status --short --branch
```

Expected: clean diff formatting, fresh successful full gate, and only intended
UI/docs changes.

- [ ] **Step 7: Commit verification documentation**

```bash
git add docs/roadmap-current.md docs/session-handoffs.md
git commit -m "docs: record calm cockpit UI refresh"
```

