# Calm Cockpit UI Refresh Design

## Summary

Refresh the OpenAuto Companion presentation layer around a low-glare, dark
"Calm Cockpit" design language. The refresh makes the app answer three user
questions quickly: which vehicle is selected, whether it is ready, and whether
anything needs attention. Existing pairing, persistence, service, networking,
theme-transfer, and web-configuration behavior remains unchanged.

This work is intentionally based on the local `dev` branch. That branch still
uses the six-digit manual PIN contract, so the refresh must preserve those
types and callbacks rather than importing the newer pairing-code behavior from
another branch.

## Vision Alignment

The design supports the project principles in `docs/project-vision.md`:

- **Reliability over novelty:** runtime state remains authoritative and the UI
  does not simulate connectivity or add animation-driven state.
- **Low-friction UX:** QR pairing becomes the obvious primary action while the
  existing manual path remains available as a disclosure.
- **Clear connection and sharing state:** the vehicle screen promotes one
  primary readiness state and groups supporting telemetry beneath it.
- **Per-vehicle configuration:** existing vehicle-scoped toggles and callbacks
  are retained.
- **No-stream interruption:** no service, socket, Wi-Fi monitor, or foreground
  lifecycle behavior changes.

The user has explicitly promoted the UI polish work ahead of its previous
`Later` roadmap position. The roadmap should record that sequencing change.

## Approved Direction

The approved visual direction is **Calm Cockpit**:

- fixed low-glare dark presentation;
- deep charcoal background and elevated surfaces;
- mint green used only for connected, active, or primary-action states;
- muted gray for inactive or unavailable state;
- warm amber reserved for recoverable warnings;
- generous spacing and rounded Material 3 surfaces;
- no gradients inside app screens, automotive dashboard imitation, car
  photography, or decorative motion.

Reference palette:

| Role | Value | Purpose |
| --- | --- | --- |
| Background | `#0C100E` | Main app background |
| Surface | `#151B18` | Cards and grouped controls |
| Surface raised | `#1B231F` | Prominent interactive surfaces |
| Outline | `#2A342E` | Quiet component boundaries |
| Primary mint | `#7DE3A6` | Connected, active, primary action |
| Primary content | `#0A2114` | Text/icon on mint |
| On background | `#F4F7F3` | Primary text |
| On surface variant | `#9BA69F` | Supporting text |
| Warning amber | `#F1B86C` | Recoverable attention state |
| Error | Material dark error role | Destructive and failure state |

## Alternatives Considered

### 1. Branded presentation layer over current architecture — selected

Create a dedicated Compose theme and small reusable UI components while
retaining the current sealed `Screen` routing and callback/data flow. This
delivers the approved appearance with the smallest runtime risk.

### 2. Light Material polish

Keep the default Material theme and adjust spacing, cards, and typography. This
would be smaller, but it would not deliver the approved low-glare identity or
make active state visually distinctive enough.

### 3. Navigation and state architecture rewrite

Introduce Navigation Compose and screen view models while redesigning the UI.
This could improve long-term structure, but it unnecessarily combines runtime
architecture work with a visual refresh. It is deferred.

## Scope

### Included

- A branded `OpenAutoCompanionTheme` with dark color scheme, typography,
  shapes, status-bar treatment, and reusable design tokens.
- Reusable section labels, status indicators, and action surfaces where doing
  so removes duplication across the refreshed screens.
- Garage refresh for connected and waiting vehicles.
- Vehicle-status refresh with a readiness hero, grouped sharing state, quick
  actions, and contained vehicle options.
- QR-first add-vehicle screen with the manual form collapsed by default.
- QR scanner styling with a top bar, guidance, framed camera preview, and
  existing permission/error states.
- Responsive vertical scrolling and safe insets for compact phones and large
  font settings.
- Accessibility semantics, minimum touch targets, state conveyed by text as
  well as color, and sufficient contrast.
- Previews or renderable sample states for visual inspection where practical.

### Excluded

- Networking, pairing protocol, service, Wi-Fi monitor, or persistence changes.
- Navigation Compose or view-model migration.
- New vehicle capabilities or settings.
- Reworking the internal theme-builder workflow beyond inheriting the new
  global theme.
- Light mode or user-selectable appearance in this first slice.
- Motion beyond normal Material interaction feedback.

## Architecture

### Theme

Add a focused theme package under
`app/src/main/java/org/openauto/companion/ui/theme/`. `MainActivity` wraps all
screens in `OpenAutoCompanionTheme` instead of the default `MaterialTheme`.
The package owns the dark color scheme, typography, shapes, and small semantic
colors not represented directly by Material roles.

The Android XML launch theme becomes dark and sets status/navigation bar colors
that match the Compose background, preventing a light flash during startup.

### Components

Add a small component file for visual primitives shared by two or more screens:

- `ProdigySectionLabel` for uppercase section hierarchy;
- `ConnectionDot` for textual and visual state pairing;
- `CockpitCard` defaults for shape, color, and outline;
- `TopBar` for consistent back/title layout.

Screen-specific components stay private to their screen files. The refresh
must not create a generic design-system abstraction for one-off layouts.

### Presentation helpers

Pure Kotlin presentation helpers convert existing runtime models to explicit UI
copy and semantic tones. They provide testable rules such as:

- connected vehicle -> `Ready` and `Connected · sharing normally`;
- current vehicle with a connection issue -> issue copy and warning tone;
- unavailable vehicle -> `Waiting for <ssid>` and neutral tone;
- vehicle-data group is active only while connected;
- internet bridge is active only while connected and SOCKS5 is active.

These helpers do not own runtime state and must not duplicate service logic.

## Screen Designs

### Garage

- Brand label `PRODIGY`, title `Your garage`, and a concise vehicle count and
  connection summary replace the oversized centered logo treatment.
- The connected vehicle, when present, appears first as a visually promoted
  card with explicit `CONNECTED`, vehicle name, SSID when distinct, and
  `Data sharing active`.
- Other vehicles use quieter cards with `Waiting for vehicle`.
- Cards navigate to vehicle status. Removal moves into the vehicle screen's
  overflow/destructive section instead of appearing on every garage row.
- A full-width `Add vehicle` button remains anchored after the scrolling list
  and respects `Vehicle.MAX_VEHICLES`.
- The existing hidden logo tap/easter-egg behavior is removed with the logo;
  it is non-product functionality and does not fit the new hierarchy.

### Vehicle status

- Top bar contains back, vehicle name, and an overflow action for destructive
  vehicle management.
- The readiness hero provides the primary state using icon/dot, title, detail,
  SSID, and explicit state color.
- `Sharing` groups vehicle data (`Time · GPS · Battery`) and internet bridge
  (`SOCKS5 sharing`) into one card. Each row includes text status, not color
  alone.
- `Quick actions` contains equal-weight `Head unit` and `Appearance` cards,
  mapped to the existing web-config and theme-builder callbacks.
- `Vehicle options` contains the existing Internet Sharing and Audio Keep Alive
  switches, with explanatory text. It remains on the same screen in an
  expandable or grouped card; no new navigation destination is required.
- `Unpair` remains behind an explicit confirmation dialog.
- The complete screen scrolls so content is reachable on compact displays and
  with larger accessibility font scales.

### Add vehicle

- The QR path is presented first with the heading `Pair in seconds`, short
  head-unit guidance, a QR illustration/surface, and a full-width `Scan QR`
  button.
- `Can't scan?` expands the existing SSID, optional vehicle name, and six-digit
  PIN form in place. Manual input behavior and validation remain unchanged.
- Pairing progress disables both QR and manual actions and retains the existing
  progress copy.
- Pairing failures appear in a contained error surface near the relevant
  actions and remain readable after configuration changes handled by the
  existing activity state.
- Cancel/back behavior remains unchanged when paired vehicles already exist.

### QR scanner

- Use a dark top bar and concise instruction.
- Place the camera preview inside a large rounded frame with mint corner guides
  overlaid without obstructing the scanned area.
- Preserve the current one-shot scan, camera lifecycle, permission request,
  denial fallback, and error callbacks.
- Permission-denied and startup-error states use the same page hierarchy rather
  than visually unrelated centered text.

## Data Flow and Behavior Preservation

`MainActivity` remains the owner of screen selection, collected service state,
pairing state, and callbacks. Refreshed composables continue to receive plain
models and lambdas. No screen directly reads preferences or service flows.

The only data-shaping addition is pure presentation mapping. User actions still
flow through the same callbacks:

1. Garage selects a `Vehicle`.
2. Status toggles update preferences and the existing static service control.
3. QR scan returns the existing `PairingPayload`.
4. Manual pair returns the existing SSID, display name, and six-digit PIN.
5. Web config, theme builder, back, cancel, and unpair routes remain unchanged.

## Error and Transitional States

- Connected: mint readiness treatment and explicit `Ready`/`Active` copy.
- Waiting: neutral treatment and `Waiting for <ssid>` copy.
- Recoverable connection issue: amber treatment and the existing issue message.
- Pairing in progress: disable conflicting actions and show progress.
- Pairing failed: error container with the existing failure message.
- Camera permission pending: progress and request copy within scanner layout.
- Camera permission denied: explanation plus `Go back` action.
- Camera failure: visible error surface while preserving cancel/back.
- Maximum vehicles reached: disabled Add action with accessible disabled state;
  no new business rule.

## Accessibility and Responsive Rules

- Use at least 48 dp touch targets.
- Do not rely on color alone; every status has text.
- Use Material content colors for disabled and error states.
- Add content descriptions to meaningful icons and mark decorative icons as
  such.
- Use `WindowInsets.safeDrawing` or equivalent screen padding.
- Keep major pages vertically scrollable where fixed content could clip.
- Let text wrap rather than forcing fixed-height cards around dynamic copy.
- Preserve logical traversal order: page title, primary state/action, details,
  secondary actions, destructive actions.

## Testing Strategy

### Test-driven unit coverage

Add JVM tests for pure presentation rules before implementation:

- garage summary copy for zero, one, and multiple vehicles;
- readiness presentation for connected, waiting, and connection-issue states;
- sharing group active/inactive rules;
- manual pairing default disclosure state and existing six-digit validation if
  logic is extracted.

Each helper follows red-green-refactor and avoids testing Compose internals.

### Compile and regression verification

- Run focused UI/presentation unit tests during implementation.
- Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` before completion.
- Build previews and inspect representative connected, waiting, issue, pairing,
  and camera-permission states.
- When the Pixel is available, install with the Windows SDK ADB binary and
  inspect garage, status, QR/manual pairing, scrolling, and font scaling.
- Confirm no service reconnect or Android Auto interruption is triggered by
  screen navigation; this refresh must not touch runtime lifecycle code.

## Acceptance Criteria

- The app launches into the branded dark theme without a light startup flash.
- Garage, status, add-vehicle, and QR scanner screens match the Calm Cockpit
  hierarchy and palette.
- A user can identify connected versus waiting state without relying on color.
- QR is the obvious primary pairing route and manual six-digit PIN pairing is
  still fully available.
- Existing status actions, toggles, back/cancel behavior, web config, theme
  builder, QR scanning, and unpair behavior remain wired to their current
  callbacks.
- Core screens remain usable on compact phones and with larger font settings.
- No networking, service, pairing protocol, or persistence behavior changes.
- `./gradlew :app:testDebugUnitTest :app:assembleDebug` passes.

