# Prodigy Brand Blue and Star Header Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Calm Cockpit mint accent with Prodigy brand blue and add the approved star-right/`PRODIGY`-below lockup to the Garage header.

**Architecture:** Keep the existing Compose screen and theme boundaries. Rename the semantic accent tokens at the theme boundary, update their direct consumers mechanically, and change only the first Garage `LazyColumn` item to render the existing drawable in a responsive header row. Runtime models, callbacks, navigation, persistence, pairing, and service code remain untouched.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android resources, JUnit 4, Gradle, existing `prodigy_logo.png` asset.

---

## File Structure

- Modify `app/src/main/java/org/openauto/companion/ui/theme/OpenAutoTheme.kt`: own the canonical blue Material roles and renamed semantic tokens.
- Modify `app/src/main/java/org/openauto/companion/ui/CockpitComponents.kt`: map positive presentation tone to the renamed blue token.
- Modify `app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt`: consume the blue token and render the approved Garage logo lockup.
- Modify `app/src/main/java/org/openauto/companion/ui/StatusScreen.kt`: use blue for active sharing state.
- Modify `app/src/main/java/org/openauto/companion/ui/PairingScreen.kt`: use blue for the QR illustration accents.
- Reuse `app/src/main/res/drawable-xxhdpi/prodigy_logo.png`: no asset conversion or duplication.
- Modify `docs/session-handoffs.md`: record the visual amendment and exact verification results.

The approved design is `docs/superpowers/specs/2026-07-24-brand-blue-star-design.md`.

### Task 1: Replace Mint Theme Tokens with Brand Blue

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/ui/theme/OpenAutoTheme.kt:22-53`
- Modify: `app/src/main/java/org/openauto/companion/ui/CockpitComponents.kt:28,124`
- Modify: `app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt:32,113-135`
- Modify: `app/src/main/java/org/openauto/companion/ui/StatusScreen.kt:45,238-247`
- Modify: `app/src/main/java/org/openauto/companion/ui/PairingScreen.kt:53,281-303`

The approved spec explicitly exempts this visual-only token substitution from a new test-first unit seam. Existing presentation tests and the full build remain the automated regression gate.

- [ ] **Step 1: Rename direct token consumers**

Apply this mechanical rename in the five Compose files listed above:

```text
CockpitMint   -> CockpitBlue
CockpitOnMint -> CockpitOnBlue
```

Do not retain compatibility aliases; the old names should disappear from application source.

- [ ] **Step 2: Install the exact Prodigy palette in the theme**

Replace the mint declarations and Material primary mappings with:

```kotlin
val CockpitBlue = Color(0xFF534FFD)
val CockpitOnBlue = Color(0xFFFFFFFF)
val CockpitText = Color(0xFFF4F7F3)
val CockpitMuted = Color(0xFF9BA69F)
val CockpitWarning = Color(0xFFF1B86C)

private val CockpitColorScheme = darkColorScheme(
    primary = CockpitBlue,
    onPrimary = CockpitOnBlue,
    primaryContainer = Color(0xFF2F2B72),
    onPrimaryContainer = Color(0xFFE4E2FF),
    secondary = CockpitBlue,
    onSecondary = CockpitOnBlue,
    tertiary = CockpitWarning,
    onTertiary = Color(0xFF2A1700),
    background = CockpitBackground,
    onBackground = CockpitText,
    surface = CockpitSurface,
    onSurface = CockpitText,
    surfaceVariant = CockpitSurfaceRaised,
    onSurfaceVariant = CockpitMuted,
    outline = CockpitOutline,
    outlineVariant = Color(0xFF222B26),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)
```

Keep the background, surface, outline, text, warning, and error colors unchanged.

- [ ] **Step 3: Verify semantic consumers are consistently blue**

Run:

```bash
rg -n 'CockpitMint|CockpitOnMint|7DE3A6|0A2114|244B39|CFF8DE' app/src/main
rg -n 'CockpitBlue|CockpitOnBlue|534FFD|2F2B72|E4E2FF' app/src/main
```

Expected: the first command returns no matches; the second reports the theme declarations plus Garage, Status, Pairing, and shared-component consumers.

- [ ] **Step 4: Compile the theme substitution**

Run:

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, including all existing presentation tests.

- [ ] **Step 5: Commit the blue token change**

```bash
git add app/src/main/java/org/openauto/companion/ui/theme/OpenAutoTheme.kt \
  app/src/main/java/org/openauto/companion/ui/CockpitComponents.kt \
  app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt \
  app/src/main/java/org/openauto/companion/ui/StatusScreen.kt \
  app/src/main/java/org/openauto/companion/ui/PairingScreen.kt
git commit -m "style: adopt Prodigy brand blue"
```

### Task 2: Add the Approved Garage Star Lockup

**Files:**
- Modify: `app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt:3-34,56-72`
- Reuse: `app/src/main/res/drawable-xxhdpi/prodigy_logo.png`

- [ ] **Step 1: Add the logo rendering imports**

Add these imports while keeping the file's explicit layout imports:

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import org.openauto.companion.R
```

- [ ] **Step 2: Replace the Garage heading with the approved responsive row**

Replace the current standalone `PRODIGY`, title, and summary block inside the first lazy-list item with:

```kotlin
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
            Text("Your garage", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = garageSummary(vehicles.size, connectedVehiclePresent),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.width(76.dp),
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
                color = CockpitBlue,
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}
```

The image is intentionally decorative because the visible label immediately beneath it supplies the brand name. Keep vehicle sorting, card callbacks, and the Add Vehicle button unchanged.

- [ ] **Step 3: Check the focused diff for unintended behavior changes**

Run:

```bash
git diff -- app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt
```

Expected: imports, blue-token rename from Task 1, and only the first header item change. No callback, sorting, vehicle limit, or preview fixture changes.

- [ ] **Step 4: Build and inspect representative Garage states**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. Inspect both `GarageConnectedPreview` and `GarageEmptyPreview` in Android Studio when available. On an unlocked attached device, verify that the title/summary remain readable beside the fixed 76 dp lockup and that the star tile is not separately announced by TalkBack.

- [ ] **Step 5: Commit the Garage lockup**

```bash
git add app/src/main/java/org/openauto/companion/ui/VehicleListScreen.kt
git commit -m "style: add Prodigy star to garage header"
```

### Task 3: Full Verification and Handoff

**Files:**
- Modify: `docs/session-handoffs.md`

- [ ] **Step 1: Confirm the complete implementation matches the amendment**

Run:

```bash
git diff --check
rg -n 'CockpitMint|CockpitOnMint|7DE3A6|0A2114' app/src/main
git diff 384ad04..HEAD -- app/src/main
```

Expected: clean whitespace, no old mint symbols/values in application source, and only theme/direct-consumer/header presentation changes.

- [ ] **Step 2: Run the repository-required verification gate**

Run:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, all 173 or more unit tests passing, and `app/build/outputs/apk/debug/app-debug.apk` generated.

- [ ] **Step 3: Perform device visual QA when possible**

Use the Windows Android SDK ADB binary:

```bash
/mnt/e/Android/Sdk/platform-tools/adb.exe devices -l
/mnt/e/Android/Sdk/platform-tools/adb.exe -s 39260DLJH000LX install -r app/build/outputs/apk/debug/app-debug.apk
```

If the Pixel is unlocked, inspect the Garage header, connected/waiting states, primary buttons, QR illustration, scanner guides, safe insets, and system bars. If the device remains locked, record install success and mark visual inspection pending rather than passed.

- [ ] **Step 4: Append the required handoff**

First run `date +"%Y-%m-%d %H:%M %Z"` and use its literal output for the entry heading. Append an entry to `docs/session-handoffs.md` that records these exact facts:

- What changed: Calm Cockpit mint was replaced by Prodigy blue, and the approved star-right/PRODIGY-below Garage lockup was added.
- Why: the refreshed companion UI now matches canonical Prodigy branding.
- Status: use `complete and device-inspected` only if the unlocked-device pass ran; otherwise use `implementation complete; unlocked-device visual QA pending`.
- Dependency decision: `Companion-only: Yes`.
- Wishlist promotion: continuation of the active UI polish pass; no roadmap resequencing.
- Next steps: device tap-through if pending, user acceptance, then branch integration.
- Verification: copy the observed Gradle result and test count, diff checks, APK install result, visual-QA result, and `Android Auto continuity not exercised because runtime behavior was untouched`.

- [ ] **Step 5: Re-run doc and repository checks**

Run:

```bash
git diff --check
./gradlew :app:testDebugUnitTest :app:assembleDebug
git status --short --branch
```

Expected: clean formatting, a fresh successful full gate, and only the intended handoff file uncommitted.

- [ ] **Step 6: Commit verification documentation**

```bash
git add docs/session-handoffs.md
git commit -m "docs: record Prodigy branding refinement"
```

