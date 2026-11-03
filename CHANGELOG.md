# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Skilling badge v2: the badge is drawn over your own head, at the same anchor as your own health bar, never over the spot or NPC; when both are up it stacks above the health bar (`OverheadStack`). On a fishing spot it shows the fish the clicked method (Net, Bait, Lure, Cage, Harpoon, Big Net, Use-rod, Fish) can land at your current boosted level, as small item icons beside the level, up to three then `+N`, with the last catch (from the `You catch ...` message or the inventory) leading and the rest faded. Spot type from the base client's `FishingSpot` NPC id table; levels and methods from the OSRS wiki, in `FishingCatches`; unknown spots or methods fall back to the skill and level. New `skillingCatches` toggle. Pure, unit-tested `FishingCatches`, `InventoryCatches`, `OverheadStack`.
- Skilling vitals badge: working a fishing spot or another non-attackable skilling NPC draws a badge over it in the health bar's style, with the skill's icon, your boosted level and boost (`Fishing 79 (+3)`), the seconds until the boost drops by one (Preserve-aware, marked `~` until an observed drop confirms the drain phase), the remaining special attack energy when a dragon/infernal/crystal/3rd age axe, pickaxe or harpoon is wielded, and the boost's source when it was just drunk, eaten or spec'd. A drop or expiry paints the badge red for a moment, with an optional RuneLite notification (off by default). New `Skilling vitals` config section (`skillingEnabled`, `skillingBoostTimer`, `skillingBoostSource`, `skillingSpecEnergy`, `skillingBoostNotify`). Pure, unit-tested `SkillingActions`, `SkillingTools`, `BoostTracker`, `BoostTimer`, `SkillingState` and `SkillingText`.
- Overhead and corner-panel health readout for NPC and player targets, with Top/Middle/Bottom vertical placement, pixel offset, hitpoints/percentage/both display modes, optional decimal precision, configurable opponent timeout (1–60s), boss HP HUD suppression, and automatic panel suppression when stock Opponent Information is enabled (`1db1966`).
- Single-opponent asynchronous hiscores Hitpoints lookup with an opt-out toggle and automatic fallback to percentage display for unranked players (`1db1966`).
- `HealthRecovery` ratio-to-health range inversion arithmetic and JUnit property/unit test suite (`7d06bee`, `1db1966`).
- Three overhead display styles (`Bar behind text`, `Text above health bar`, `Text only`) with text-sized health bar rendering (`2857074`, `f61efb2`).
- Configurable bar background colour with alpha (`63ec155`) and three-stop health gradient colours (`healthFullColour`, `healthMidColour`, `healthLowColour`) with alpha (`69c8f9f`).
- Self-health overhead readout sourced directly from the hitpoints orb while engaged, and a player combat stat comparison table in the corner panel (`c61e77a`).
- Original 48×48 plugin icon (`0727b78`) and live PvP screenshots (`0ab85cf`, `0cee361`, `a04a5b0`).
- Phase 0.5 cross-account validation script (`docs/phase-0.5/join_probe_csvs.py`) and `-PaltHome` Gradle property on the `run` task for isolated dual-client testing (`06578a5`, `a6a1624`, `3706cc2`).
- `Hide game health bar` option, reinstated by the Board after the `7e9eac9` cut, now built on transparent sprite overrides for the 22 standard bar sprites rather than the 2D draw hook: global (yours included), reasserted each client tick so Interface Styles' HD bars are handed back rather than cleared, restored on toggle-off and shutdown (`NativeHealthBarHider`, `NativeHealthBarHiderTest`, `docs/native-hp-bar.md`, `5f65626`, `7fcb8e1`).
- `Hide game chat under readout` option (default off): while the overlay redraws an actor's overhead chat above the readout, the game's own line is blanked the way the base client's Chat Filter blanks a message, countdown untouched, and handed back when the readout moves clear, the option is off, or the plugin shuts down (`OverheadChatHolder`, `OverheadChatHolderTest`, `docs/overhead-chat.md`, `0aad7b8`).

### Changed
- Renamed plugin, package (`com.vinnyvangogh.combatvitals`), config group (`combat-vitals`), classes, and Gradle project from Combat Info to Combat Vitals (`4673b53`).
- Raised `Text above health bar` (`ABOVE_BAR`) vertical clearance from 18 px to 34 px so overhead text clears the native health bar (`6da65b6`).

### Fixed
- The overhead readout no longer paints over the target's overhead chat: while an actor's chat is showing (`Actor#getOverheadCycle() > 0`) and the readout covers the game's line, the overlay redraws the line just above the readout in the game's bold font; the readout keeps its configured position and nothing is drawn when the two do not overlap, so a message never shows twice. Only actors this plugin draws over qualify: the opponent (readout) and you (readout, skilling badge, or both stacked), with one copy of your chat lifted above the whole stack (`OverheadStack.union`) so the badge and a chat line never overlap. A skilling NPC, or any bystander, is never touched (`OverheadChat`, `OverheadChatTest`, `88b5d70`, `1358522`).
- Clicking a skilling NPC (fishing spot, rock, banker, shopkeeper, pet) no longer pops your own health bar or holds a stale opponent: a target now counts only if it is attackable (`Attack` menu option or a combat level), your own bar also shows for `opponentTimeout` after any hitsplat on you or dealt by you, and the opponent expires unless you are still interacting with that same actor (`CombatState`, `CombatStateTest`, #4).
- Reordered Display config section items so health gradient colour pickers sit directly below `Colour by health`, and shortened the `Overhead style` label to `Style` so it does not truncate in the config panel (`5688c60`).
- Expired stale opponent references after the interaction timeout and skipped actors with negative health ratios so despawned NPCs are not sampled (`2980ee2`, `053a4b4`).
- Added `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED` on macOS in the Gradle `run` task so the developer client starts on Java 11+ toolchains without `IllegalAccessError` (`5bae9e1`).

### Removed
- Removed the experimental `Hide game's health bar` (`RenderableDrawListener`) option and `Hooks` injection because suppressing the actor's 2D layer also hid hitsplats, name plates, and overhead prayer icons (`7e9eac9`). Reinstated on sprite overrides in `5f65626`; see Added.
- Removed the temporary `HealthScaleProbe` instrumentation class after Phase 0.5 measurements concluded (`6f8f02d`).
