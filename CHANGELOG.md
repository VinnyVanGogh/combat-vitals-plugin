# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Overhead and corner-panel health readout for NPC and player targets, with Top/Middle/Bottom vertical placement, pixel offset, hitpoints/percentage/both display modes, optional decimal precision, configurable opponent timeout (1–60s), boss HP HUD suppression, and automatic panel suppression when stock Opponent Information is enabled (`1db1966`).
- Single-opponent asynchronous hiscores Hitpoints lookup with an opt-out toggle and automatic fallback to percentage display for unranked players (`1db1966`).
- `HealthRecovery` ratio-to-health range inversion arithmetic and JUnit property/unit test suite (`7d06bee`, `1db1966`).
- Three overhead display styles (`Bar behind text`, `Text above health bar`, `Text only`) with text-sized health bar rendering (`2857074`, `f61efb2`).
- Configurable bar background colour with alpha (`63ec155`) and three-stop health gradient colours (`healthFullColour`, `healthMidColour`, `healthLowColour`) with alpha (`69c8f9f`).
- Self-health overhead readout sourced directly from the hitpoints orb while engaged, and a player combat stat comparison table in the corner panel (`c61e77a`).
- Original 48×48 plugin icon (`0727b78`) and live PvP screenshots (`0ab85cf`, `0cee361`, `a04a5b0`).
- Phase 0.5 cross-account validation script (`docs/phase-0.5/join_probe_csvs.py`) and `-PaltHome` Gradle property on the `run` task for isolated dual-client testing (`06578a5`, `a6a1624`, `3706cc2`).

### Changed
- Renamed plugin, package (`com.vinnyvangogh.combatvitals`), config group (`combat-vitals`), classes, and Gradle project from Combat Info to Combat Vitals (`4673b53`).
- Raised `Text above health bar` (`ABOVE_BAR`) vertical clearance from 18 px to 34 px so overhead text clears the native health bar (`6da65b6`).

### Fixed
- Reordered Display config section items so health gradient colour pickers sit directly below `Colour by health`, and shortened the `Overhead style` label to `Style` so it does not truncate in the config panel (`5688c60`).
- Expired stale opponent references after the interaction timeout and skipped actors with negative health ratios so despawned NPCs are not sampled (`2980ee2`, `053a4b4`).
- Added `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED` on macOS in the Gradle `run` task so the developer client starts on Java 11+ toolchains without `IllegalAccessError` (`5bae9e1`).

### Removed
- Removed the experimental `Hide game's health bar` (`RenderableDrawListener`) option and `Hooks` injection because suppressing the actor's 2D layer also hid hitsplats, name plates, and overhead prayer icons (`7e9eac9`).
- Removed the temporary `HealthScaleProbe` instrumentation class after Phase 0.5 measurements concluded (`6f8f02d`).
