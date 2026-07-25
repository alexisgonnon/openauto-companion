# Brand Blue and Star Header Design

**Status:** Approved visual direction

**Date:** 2026-07-24

## Goal

Align the Calm Cockpit refresh with the established Prodigy identity by
replacing the mint accent with the canonical brand blue and restoring the
existing star artwork to the garage header. The change remains presentation
only: networking, pairing, persistence, navigation, and runtime callbacks do
not change.

## Approved Direction

The user selected the compact header-tile treatment and then approved its
refinement:

- `Your garage` and the vehicle summary remain left-aligned.
- The existing `prodigy_logo.png` tile sits at the upper-right.
- The uppercase `PRODIGY` label is centered directly beneath the star tile.
- The logo and label form a single compact lockup rather than competing with
  the page title.

The star tile is approximately 60 dp square with the existing rounded-corner
language. It is decorative because the adjacent visible `PRODIGY` label
already communicates the brand; assistive technology should not announce the
same name twice.

## Color Tokens

Replace the mint-specific tokens and names throughout the refreshed UI:

| Role | Value | Use |
| --- | --- | --- |
| Brand blue | `#534FFD` | Primary actions, active/connected state, focus accents |
| On brand blue | `#FFFFFF` | Text and icons on filled blue controls |
| Blue container | `#2F2B72` | Subtle primary containers on the dark theme |
| On blue container | `#E4E2FF` | Content on blue containers |

The dark backgrounds, neutral surfaces, text colors, and amber warning color
remain unchanged. Every active state continues to include explicit text, so
the design does not rely on blue alone.

Rename `CockpitMint`/`CockpitOnMint` to `CockpitBlue`/`CockpitOnBlue` rather
than keeping misleading aliases. `UiTone.Positive` maps to `CockpitBlue`, and
all scanner guides, QR illustration accents, connected labels, dots, and
primary Material roles inherit the blue treatment consistently.

## Garage Header Layout

The first `LazyColumn` item becomes a responsive header row:

- a weighted left column contains `Your garage` and its summary;
- a fixed-width right column contains the clipped star tile and centered
  `PRODIGY` label;
- spacing prevents long or large-font summary copy from overlapping the
  lockup;
- the page remains vertically scrollable and retains safe-drawing insets.

The existing connected-first sorting, vehicle cards, maximum-vehicle rule,
and Add Vehicle callback remain unchanged.

## Verification

This is a purely visual token and layout amendment. No new business-rule unit
test is warranted; automated verification uses the existing 173-test suite and
debug assembly gate. This is an explicit exception to test-first development
for the visual-only placement and token substitution.

Before completion:

1. Run `git diff --check`.
2. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
3. Inspect the updated Garage previews and, when an unlocked device is
   available, confirm the header, blue contrast, system bars, and compact-font
   behavior on hardware.
4. Append the required session handoff with exact results.

## Acceptance Criteria

- No mint accent remains in the refreshed application UI or token names.
- Brand blue `#534FFD` is the consistent primary and positive-state accent.
- The Garage header shows the approved star-right/`PRODIGY`-below lockup.
- Title, summary, and logo do not overlap at supported phone widths or larger
  font settings.
- Existing callbacks, state mapping, and runtime behavior are unchanged.
- The repository-required Gradle gate passes.
