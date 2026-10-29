# Bug Log

Merged bug fixes in reverse chronological order (newest first). Pre-PR bootstrap commits on `main` are cited by short commit hash.

## 2026-09-02 — Native health bar suppression hid hitsplats and overhead icons (`7e9eac9`)
- **What broke:** Enabling the experimental `Hide game's health bar` option suppressed the target actor's entire 2D UI layer via `Hooks` (`RenderableDrawListener`), hiding hitsplats, name plates, and overhead prayer icons alongside the native health bar.
- **How it was found:** In-game testing and review of rejected Plugin Hub submissions (`Add Combat Entity Hider` / `Add PVP Entity Hider`).
- **Fix:** Removed the config item, `RenderableDrawListener`, and `Hooks` injection (`7e9eac9`).

## 2026-09-02 — Truncated `Overhead style` label and orphaned colour pickers in settings (`5688c60`)
- **What broke:** In the RuneLite settings panel, `Overhead style` rendered truncated as `Overhead...` next to its dropdown, and the three health gradient colour pickers (`positions 41–43`) sat separated at the bottom of the Display section away from the `Colour by health` toggle that governs them.
- **How it was found:** Inspecting a screenshot of the plugin settings panel.
- **Fix:** Shortened the setting label to `Style` and renumbered the Display section positions so the colour pickers sit immediately below `Colour by health` (`5688c60`).

## 2026-09-02 — `Text above health bar` style overlapped the native health bar (`6da65b6`)
- **What broke:** The initial 18 px vertical clearance for `OverheadStyle.ABOVE_BAR` was too small, leaving the text sitting on top of the game's health bar.
- **How it was found:** In-game testing of the overhead styles.
- **Fix:** Raised `ABOVE_BAR` vertical clearance from 18 px to 34 px (`6da65b6`).

## 2026-09-02 — Translucent bar background let the native health bar bleed through (`63ec155`)
- **What broke:** The `Bar behind text` background was hardcoded to black at alpha 150, allowing the narrower native green/red health bar underneath to show through and smear the readout.
- **How it was found:** In-game testing of the `Bar behind text` overhead style.
- **Fix:** Changed the default bar background to opaque black (`Color.BLACK`) and exposed `barBackground` as an `@Alpha` colour setting (`63ec155`).

## 2026-09-02 — Overhead coloured text collided with the native health bar (`2857074`, `f61efb2`)
- **What broke:** Drawing health-tinted text directly at the target's overhead anchor overlapped the narrower native health bar (for example, red low-health text on top of the native bar), making the readout hard to read against cave-floor terrain.
- **How it was found:** In-game testing.
- **Fix:** Added `OverheadStyle` (`Bar behind text` with white text inside a text-sized health bar, `Text above health bar`, and `Text only`) (`2857074`, `f61efb2`).

## 2026-09-02 — Despawned NPCs sampled indefinitely after death (`053a4b4`)
- **What broke:** After an NPC died and despawned, the retained `opponent` reference continued to report a stale positive `healthScale` while `healthRatio` was `-1` (with `combatLevel` and `npcId` also `-1`), causing the plugin to keep sampling a despawned actor indefinitely.
- **How it was found:** Live combat data capture during Phase 0.5 testing.
- **Fix:** Restored the post-interaction expiration timeout and rejected actors with `healthRatio < 0` or `healthScale <= 0` (`053a4b4`, carried into `CombatVitalsPlugin` in `1db1966`).

## 2026-08-26 — Opponent cleared immediately when interaction ended (`2980ee2`)
- **What broke:** `onInteractingChanged` overwrote `opponent` with `null` as soon as `event.getTarget()` became `null`, discarding the target while its health bar was still lingering in-game.
- **How it was found:** Phase 0.5 live probe run.
- **Fix:** Kept the last engaged `opponent` when `event.getTarget() == null` and retired it on a timer instead (`2980ee2`).

## 2026-08-26 — Developer client crashed at startup on macOS with `IllegalAccessError` (`5bae9e1`)
- **What broke:** Running `./gradlew run` on macOS with a Java 11+ toolchain threw `IllegalAccessError` because RuneLite's `OSXFullScreenAdapter` extends `com.apple.eawt.FullScreenAdapter`, which `java.desktop` stopped exporting to the unnamed module after Java 8.
- **How it was found:** Launching `./gradlew run` locally on macOS.
- **Fix:** Added `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED` gated on macOS in the Gradle `run` task (`5bae9e1`).
