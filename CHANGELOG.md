# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Skilling badge round 3 (Board's in-game test, 2026-11-04, after #14):
  - A fish above your level reads `Lv 15` in red instead of a bare `15`, with `Needs Fishing 15` on hover (`SkillingText.lockedLabel`, `lockedTooltip`, `LOCKED_LEVEL`).
  - Idle state: when you stop, the badge stays with a red `Not fishing` header (generic `Not <activity>`) and keeps the level, XP and inventory rows. New `skillingIdleMode` (Off / Show for a while / Until another activity, default Show for a while), `skillingIdleSeconds` (60), `skillingIdleColour`. Another activity or a fight ends it (`IdleBadge`).
  - Side pillars, like RuneLite's Status Bars: target health while fighting, XP to level and inventory fill while skilling (`pillarMode`, default Off; `pillarCombatSide`, `pillarXpSide`). Placement follows Status Bars' offsets for each side-panel layout; with Status Bars drawing, ours stack to the left of its bars, never over them (`SidePillars`, `PillarOverlay`).
  - New `Skilling badge` config section: per-skill on/off and colour overrides (defaulting to the palette), header and XP rows on/off, XP as percent / XP left / both, inventory count free or used, session count and XP and actions per hour with a reset tick box (`SkillingSession`), badge position over your head / over the spot / fixed on screen (draggable `SkillingBadgeOverlay`), vertical offset, size, dim or hide fish above your level, hide during combat. Defaults keep the previous look.
- `SkillingBadgeRenderer` draws the badge for every position; `Bars` is the one bar painter.

### Fixed
- Overhead chat drawn twice (Board's in-game test, 2026-11-04). With `Hide game chat under readout` off the plugin no longer draws its own copy, so the game's line is the only one. With it on, a new message over you or your opponent is blanked inside `OverheadTextChanged`, before any frame is drawn, instead of on the next client tick, so the game's copy no longer flashes under the readout. The overlay now marks every actor it draws a readout over, chat or not, and only draws a line the plugin is holding (`OverheadChatHolder.hold`, `heldText`, `OverheadChat.baseline`).

### Changed
- `Hide game chat under readout` and `Hide game health bar` default on. Saved values are kept: the key names are unchanged (`ConfigDefaultsTest`).
- Skilling badge v3: coloured by skill (`SkillColours`, each colour at least 3:1 against white and black); a second row with XP progress to the next level, `Lv 12 -> 13: 64% (1,234 xp)`, counting toward 200m at 99 (`XpProgress`); the bottom row is labelled as the inventory, `Inv 26/28 free`, on the fill bar; the catch icons move into that row and show every fish the method can give, the ones above your level dimmed with the level they need (`FishingCatches.options`). The three-icon cap and `+N` are gone.

### Added
- Skilling badge on objects: chopping a tree, mining a rock, stealing from a stall or crafting runes at an altar shows the same badge (skill icon, level, boost, timer, tool spec energy). Objects never fire `InteractingChanged`, so the badge needs both the clicked option (`Chop down`, `Chop`, `Mine`, `Steal-from`, `Craft-rune`) and the player animating within reach of the object; it lingers for the timeout when the animation stops in place, and clears at once on walking away, `Walk here`, a non-skilling object option, or engaging any NPC. Attacking still wins. Pure, unit-tested `ObjectSkilling` (#12).
- Inventory fill bar under the skilling badge, green through yellow to red by the share of free capacity filled; stackables, noted items, tools and wieldable or wearable items hold their slot and do not count (`InventoryFill`, `skillingInventoryBar`, #11).
- Activity alerts, a new config section of RuneLite notifications, each with its own toggle: inventory full while gathering or thieving, stopped gathering (no animation or experience for `alertStopSeconds`, default 5; never on walking off, banking, dropping or depositing, logout or hop), fishing spot moved, stopped thieving (stuns excluded), pickpocket stun (off), coin pouches at cap (`alertCoinPouchCap`, default 28), dodgy necklace broke, make-X finished for cooking, fletching, crafting and smithing (off), and low run energy (off, `alertRunEnergyThreshold` default 10). Pure, unit-tested `ActivityAlerts` state machine (#11).
- Skilling badge v2: the badge is drawn over your own head, at the same anchor as your own health bar, never over the spot or NPC; when both are up it stacks above the health bar (`OverheadStack`). On a fishing spot it shows the fish the clicked method (Net, Bait, Lure, Cage, Harpoon, Big Net, Use-rod, Fish) can land at your current boosted level, as small item icons beside the level, up to three then `+N`, with the last catch (from the `You catch ...` message or the inventory) leading and the rest faded. Spot type from the base client's `FishingSpot` NPC id table; levels and methods from the OSRS wiki, in `FishingCatches`; unknown spots or methods fall back to the skill and level. New `skillingCatches` toggle. Pure, unit-tested `FishingCatches`, `InventoryCatches`, `OverheadStack` (#9).
- Skilling vitals badge: working a fishing spot or another non-attackable skilling NPC draws a badge over it in the health bar's style, with the skill's icon, your boosted level and boost (`Fishing 79 (+3)`), the seconds until the boost drops by one (Preserve-aware, marked `~` until an observed drop confirms the drain phase), the remaining special attack energy when a dragon/infernal/crystal/3rd age axe, pickaxe or harpoon is wielded, and the boost's source when it was just drunk, eaten or spec'd. A drop or expiry paints the badge red for a moment, with an optional RuneLite notification (off by default). New `Skilling vitals` config section (`skillingEnabled`, `skillingBoostTimer`, `skillingBoostSource`, `skillingSpecEnergy`, `skillingBoostNotify`). Pure, unit-tested `SkillingActions`, `SkillingTools`, `BoostTracker`, `BoostTimer`, `SkillingState` and `SkillingText` (#7).
- Overhead and corner-panel health readout for NPC and player targets, with Top/Middle/Bottom vertical placement, pixel offset, hitpoints/percentage/both display modes, optional decimal precision, configurable opponent timeout (1–60s), boss HP HUD suppression, and automatic panel suppression when stock Opponent Information is enabled (`1db1966`).
- Single-opponent asynchronous hiscores Hitpoints lookup with an opt-out toggle and automatic fallback to percentage display for unranked players (`1db1966`).
- `HealthRecovery` ratio-to-health range inversion arithmetic and JUnit property/unit test suite (`7d06bee`, `1db1966`).
- Three overhead display styles (`Bar behind text`, `Text above health bar`, `Text only`) with text-sized health bar rendering (`2857074`, `f61efb2`).
- Configurable bar background colour with alpha (`63ec155`) and three-stop health gradient colours (`healthFullColour`, `healthMidColour`, `healthLowColour`) with alpha (`69c8f9f`).
- Self-health overhead readout sourced directly from the hitpoints orb while engaged, and a player combat stat comparison table in the corner panel (`c61e77a`).
- Original 48×48 plugin icon (`0727b78`) and live PvP screenshots (`0ab85cf`, `0cee361`, `a04a5b0`).
- Phase 0.5 cross-account validation script (`docs/phase-0.5/join_probe_csvs.py`) and `-PaltHome` Gradle property on the `run` task for isolated dual-client testing (`06578a5`, `a6a1624`, `3706cc2`).
- `Hide game health bar` option, reinstated by the Board after the `7e9eac9` cut, now built on transparent sprite overrides for the 22 standard bar sprites rather than the 2D draw hook: global (yours included), reasserted each client tick so Interface Styles' HD bars are handed back rather than cleared, restored on toggle-off and shutdown (`NativeHealthBarHider`, `NativeHealthBarHiderTest`, `docs/native-hp-bar.md`, `5f65626`, `7fcb8e1`, #10).
- `Hide game chat under readout` option (default off): while the overlay redraws an actor's overhead chat above the readout, the game's own line is blanked the way the base client's Chat Filter blanks a message, countdown untouched, and handed back when the readout moves clear, the option is off, or the plugin shuts down (`OverheadChatHolder`, `OverheadChatHolderTest`, `docs/overhead-chat.md`, `0aad7b8`, #10).

### Changed
- Bumped the pinned RuneLite dependency from 1.12.37 to 1.13.5 so the developer client connects after the game update (#8).
- Enabled `Show your own health` (`showSelf`) by default so your own health bar stays visible when `Hide game health bar` is turned on (`92eacfd`, #10).
- Renamed plugin, package (`com.vinnyvangogh.combatvitals`), config group (`combat-vitals`), classes, and Gradle project from Combat Info to Combat Vitals (`4673b53`).
- Raised `Text above health bar` (`ABOVE_BAR`) vertical clearance from 18 px to 34 px so overhead text clears the native health bar (`6da65b6`).

### Fixed
- Pickpocketing an attackable NPC (a Man) now shows the Thieving badge: the clicked option decides before attackability, and a hit you land on it still makes it a fight (#12).
- The boost source for a Drink or Eat is labelled `likely` (`likely Fishing potion`), since any item consumed within three ticks of a rise is named (#12).
- The overhead readout no longer paints over the target's overhead chat: while an actor's chat is showing (`Actor#getOverheadCycle() > 0`) and the readout covers the game's line, the overlay redraws the line just above the readout in the game's bold font; the readout keeps its configured position and nothing is drawn when the two do not overlap, so a message never shows twice. Only actors this plugin draws over qualify: the opponent (readout) and you (readout, skilling badge, or both stacked), with one copy of your chat lifted above the whole stack (`OverheadStack.union`) so the badge and a chat line never overlap. A skilling NPC, or any bystander, is never touched (`OverheadChat`, `OverheadChatTest`, `88b5d70`, `1358522`, #10).
- Clicking a skilling NPC (fishing spot, rock, banker, shopkeeper, pet) no longer pops your own health bar or holds a stale opponent: a target now counts only if it is attackable (`Attack` menu option or a combat level), your own bar also shows for `opponentTimeout` after any hitsplat on you or dealt by you, and the opponent expires unless you are still interacting with that same actor (`CombatState`, `CombatStateTest`, #4).
- Reordered Display config section items so health gradient colour pickers sit directly below `Colour by health`, and shortened the `Overhead style` label to `Style` so it does not truncate in the config panel (`5688c60`).
- Expired stale opponent references after the interaction timeout and skipped actors with negative health ratios so despawned NPCs are not sampled (`2980ee2`, `053a4b4`).
- Added `--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED` on macOS in the Gradle `run` task so the developer client starts on Java 11+ toolchains without `IllegalAccessError` (`5bae9e1`).

### Removed
- Removed the experimental `Hide game's health bar` (`RenderableDrawListener`) option and `Hooks` injection because suppressing the actor's 2D layer also hid hitsplats, name plates, and overhead prayer icons (`7e9eac9`). Reinstated on sprite overrides in `5f65626`; see Added.
- Removed the temporary `HealthScaleProbe` instrumentation class after Phase 0.5 measurements concluded (`6f8f02d`).
