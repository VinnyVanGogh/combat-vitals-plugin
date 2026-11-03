# Bug Log

Merged bug fixes in reverse chronological order (newest first). Pre-PR bootstrap commits on `main` are cited by short commit hash.

## 2026-11-03 — Pickpocketing an attackable NPC skipped the Thieving badge (#12)
- **What broke:** NPCs that offer both `Attack` and a skilling option such as `Pickpocket` (for example, a Man) were classified by `CombatState.isAttackable` before checking the clicked menu option, so pickpocketing them never showed the Thieving skilling badge.
- **How it was found:** Review of RUNELITE-40 (#7).
- **Fix:** Updated `SkillingActions` and `CombatVitalsPlugin` so the clicked menu option decides between skilling and combat on dual-option NPCs, while landing a combat hitsplat on the NPC still switches to the combat health bar (#12).

## 2026-11-03 — Consumed boost sources shown without uncertainty qualifier (#12)
- **What broke:** `BoostTracker` attributes a boost rise to any `Drink` or `Eat` menu action within three ticks, so an unrelated food or potion consumed just before a boost rose was displayed on the skilling badge as a definitive source.
- **How it was found:** Review of RUNELITE-40 (#7).
- **Fix:** Prefixed `Drink` and `Eat` boost attributions with `likely ` (for example, `likely Fishing potion`) while keeping recognised tool special attacks unqualified (#12).

## 2026-11-02 — Overhead readout and skilling badge painted over overhead chat (#10)
- **What broke:** The game draws an actor's overhead chat at the same logical height anchor as the plugin's overhead health readout and local-player skilling badge, so the overlay painted on top of active chat lines, and when the overlay redrew the chat above, the game's original line remained visible underneath.
- **How it was found:** In-game testing reported by the Board.
- **Fix:** Added `OverheadChat` and `OverheadStack.union` to redraw a covered actor's chat line once above the readout or bar-and-badge stack when `Actor#getOverheadCycle() > 0`, and added `OverheadChatHolder` (`Hide game chat under readout`, default off) to blank the game's underlying line with `" "` while covered and restore it when uncovered, disabled, or shut down (#10).

## 2026-11-02 — Skilling badge drew over the fishing spot instead of the player (#9)
- **What broke:** The initial skilling badge (#7) anchored its overlay to the target fishing spot NPC (`SkillingReadout#getActor`), drawing the player's own skill level over a small NPC that moves or despawns and separating it from the player's own overhead readout.
- **How it was found:** In-game testing of #7 with a screenshot from the Board showing `Fishing 10` drawn on the fishing spot.
- **Fix:** Removed the NPC actor from `SkillingReadout`, anchored the skilling badge to `client.getLocalPlayer()` at the same `zOffset` as the self-health readout, and added `OverheadStack` so the badge stacks above the self-health bar when both are active (#9).

## 2026-11-02 — Developer client on RuneLite 1.12.37 rejected by game server after update (#8)
- **What broke:** Launching `./gradlew run` against the pinned `1.12.37` RuneLite dependency failed before login with `error_game_js5connect_outofdate` after an Old School RuneScape game update.
- **How it was found:** Running `./gradlew run` to test in game.
- **Fix:** Bumped `runeLiteVersion` in `build.gradle` to `1.13.5` (#8).

## 2026-11-02 — Clicking skilling NPCs popped self-health bar and held stale opponents (#4)
- **What broke:** Interacting with a non-combat NPC such as a fishing spot triggered the player's overhead health bar and kept any previous opponent from expiring, because `onInteractingChanged` accepted any non-null target as combat and expiration waited while `getInteracting()` was non-null.
- **How it was found:** In-game testing reported by the Board when clicking a fishing spot.
- **Fix:** Added `CombatState` (`CombatStateTest`) so an opponent only engages when attackable (`Attack` menu option or `combatLevel > 0`; players always), self-health requires an engaged opponent or a recent combat hitsplat dealt by or to the local player (excluding poison, venom, disease, healing, and prayer drain), and opponent expiration waits only while still interacting with that same opponent (#4).

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
