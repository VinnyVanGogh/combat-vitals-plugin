# Bug Log

Merged bug fixes in reverse chronological order (newest first). Pre-PR bootstrap commits on `main` are cited by short commit hash.

## 2026-11-18 — Splash setup-changed alert bypassed sidebar mute and Mute alert sounds (#52)
- **What broke:** `SplashWatcher` called `Notifier.notify` directly when the splashing setup changed, so muting alerts from the sidebar (`AlertMute`, #51) or enabling `Mute alert sounds` (`alertMuteSounds`, #50) never silenced the splashing setup-broke notification.
- **How it was found:** Merging `origin/main` (#50, #51) into the splashing mode branch (#52).
- **Fix:** Changed `SplashWatcher.tick()` to return the setup-changed alert text and dispatched it from `CombatVitalsPlugin` through `alertMute.muted()` and `muted(config.splashAlertBroke())` (#52).

## 2026-11-18 — Plugin alert sounds ignored in-game sound effects mute and had no mute-all toggle (#50)
- **What broke:** Custom and bundled alert sounds played through `AudioPlayer` at the plugin's configured volume without checking the game's sound effects volume (`Preferences#getSoundEffectVolume`), and there was no single setting to silence all alert audio (both plugin sounds and RuneLite's notification sound) while keeping tray and chat notifications.
- **How it was found:** In-game testing reported by the Board.
- **Fix:** Added `alertFollowGameVolume` (`Follow game volume`, default on) to scale the plugin's own alert sounds by the game's 0–127 sound effects volume (playing nothing at 0) and `alertMuteSounds` (`Mute alert sounds`, default off) to silence both plugin sounds and RuneLite's notification sound in a new `Muting` card on the Alerts tab (#50).

## 2026-11-16 — Voice-line concatenation imported `javax.sound.sampled` prohibited by Plugin Hub (#49)
- **What broke:** `AlertSounds` used `javax.sound.sampled.AudioSystem` and `AudioInputStream` to decode and concatenate voice-line WAV clips in memory before handing the combined stream to RuneLite's `AudioPlayer`.
- **How it was found:** Automated and reviewer check on Plugin Hub PR (`runelite/plugin-hub#20795`).
- **Fix:** Replaced `javax.sound.sampled` with a pure-Java 16 kHz mono 16-bit PCM RIFF/WAVE reader and 44-byte header writer (`AlertSounds.Wav`) that plays through `AudioPlayer.play(InputStream, gain)`, and added `HubRulesTest` to fail the build if `javax.sound` appears in `src/main` (#49).

## 2026-11-15 — Strength XP drops on the Blast Furnace pump replaced the pump badge with an XP-drop badge (#47)
- **What broke:** Earning Strength XP while operating the Blast Furnace pump triggered `XpSkilling`, which overwrote the active `Pumping (Blast Furnace)` training-method badge.
- **How it was found:** In-game testing of the Blast Furnace pump checks.
- **Fix:** Kept the active training-method session (`TrainingSession`) ahead of `XpSkilling` on Strength XP drops and folded each alert row in the sidebar Alerts tab into its own card (#47).

## 2026-11-14 — False "stopped" alerts and XP drops fired on login and after connection-lost reconnects (#46)
- **What broke:** Logging in near the Blast Furnace pump or reconnecting after `CONNECTION_LOST` (which does not fire a logout or world-hop reset) left stop detectors and repeating alerts armed and treated the login XP sync as freshly gained XP, firing `"You've stopped pumping"` immediately on login.
- **How it was found:** In-game testing reported by the Board at the Blast Furnace.
- **Fix:** Added `LoginSync` so XP reported until the second game tick after login, hop, or reconnect is recorded as baseline only, armed stopped-activity alerts only after the activity is observed in the current login session, and reset `TrainingSession` and `ActivityAlerts` across `CONNECTION_LOST` reconnects (#46).

## 2026-11-14 — Sidebar guide and setting labels ran past the panel's right edge (#45)
- **What broke:** Guide paragraphs and long labels used a fixed CSS `width:170px` hint that did not match the sidebar panel's actual width, clipping text against the right border.
- **How it was found:** Review and testing of the sidebar panel reported by the Board.
- **Fix:** Added `WrapLabel` to wrap text dynamically at the container's allocated width as part of the card-based sidebar redesign (#45).

## 2026-11-14 — Pump badge and Live section showed raw idle counter instead of status and countdowns (#44)
- **What broke:** While actively pumping at the Blast Furnace, the timer row showed a raw `Re-click in` countdown even on the tick of input instead of an `Active` status chip and clear `AFK logout in` / `Pump stops in` labels.
- **How it was found:** Review of the sidebar Live section and Blast Furnace pump badge.
- **Fix:** Updated the pump timer chips and badge readout to show `Active` while input is fresh, `AFK logout in <m:ss>`, and `Pump stops in <mm:ss>` (#44).

## 2026-11-14 — Sidebar tab bar scrolled away and setting rows stretched vertically (#42)
- **What broke:** Putting the sidebar tab bar inside the scrollable column let the tabs scroll out of view on long tabs, and `BoxLayout` stretched individual setting rows vertically when a tab had few items.
- **How it was found:** Visual review of the sidebar panel (#37).
- **Fix:** Pinned the tab bar above the scroll pane and constrained setting row maximum heights in `Column` so rows keep their natural height (#42).

## 2026-11-14 — Emptying a log basket or fish barrel failed to zero the tracked count when chat differed or bank was open (#36)
- **What broke:** `ContainerContents` missed the period-terminated `"You empty your basket."` message and did not zero the container when `Empty` was clicked with the bank open if the chat line was filtered or varied.
- **How it was found:** In-game testing of log basket and fish barrel emptying.
- **Fix:** Matched both `"You empty your basket"` chat variants and zeroed the tracked container when `Empty` is clicked with the bank open and the bank item container changes on the following tick (#36).

## 2026-11-14 — Moving the skilling badge to fixed or over-spot still hid it during combat (#32, #36)
- **What broke:** `skillingHideInCombat` hid the skilling badge and cleared its `Not <activity>` idle state whenever combat started, even when the badge was positioned at `OVER_SPOT` or `FIXED` where it never collides with the player's overhead health bar.
- **How it was found:** In-game testing of fixed and over-spot badge positions during combat.
- **Fix:** Added `BadgeCombat` so `skillingHideInCombat` (`Hide in combat (over head)`) only hides the badge when positioned over the player's head, while fixed and over-spot badges stay visible and show `| under attack` or `| in combat` in the header (#32, #36).

## 2026-11-14 — Log basket `Check` dialog and fill-on-catch states were lost across ticks (#31)
- **What broke:** Checking a log basket or fish barrel reports its count in a sprite dialog widget rather than a `GAMEMESSAGE` chat event, and filling the final slot on a catch did not mark the container full or retain known counts across skill switches.
- **How it was found:** In-game testing with the log basket.
- **Fix:** Read container `Check` text from the sprite dialog widget each tick, marked open containers full when a catch lands directly into them at capacity, and preserved known counts until logout (#31).

## 2026-11-06 — Pickpocket stun and target hits hid the Thieving badge as combat (#26)
- **What broke:** Being stunned or taking a hit from the NPC being pickpocketed triggered `CombatState` hitsplat combat detection, hiding the Thieving skilling badge when `Hide during combat` was enabled.
- **How it was found:** In-game Thieving testing reported by the Board.
- **Fix:** Added `ThievingHits` so a hit on the local player while thieving counts as combat only when an attackable NPC other than the pickpocket target (such as a guard) or another player is interacting with the player (#26).

## 2026-11-06 — Slow `Not fishing` transition, overlapping catch icons, and dark Thieving session icon (#22)
- **What broke:** Walking away from a fishing spot waited out the full 5-second linger timeout before switching to `Not fishing`; catch icons overlapped the word `free` on the inventory bar at certain slot counts; and the dark Thieving skill icon disappeared against the session row's dark background.
- **How it was found:** In-game testing and screenshots from the Board.
- **Fix:** Added `SkillingStop` to switch to idle on the first tick the player's tile changes after the interaction ends (or after 2 stationary non-animating ticks off a fishing spot), reserved the widest-digit label width in `InventoryLine` before right-aligning catch icons, and drew a subtle light backing behind every skill's session icon in `SkillingBadgeRenderer.drawSessionIcon` (#22).

## 2026-11-06 — `Next spot path: On spot move` never fired when a fishing spot relocated in place (#20)
- **What broke:** Fishing spots in Old School RuneScape move by changing the existing NPC's world location without firing `NpcDespawned` or `NpcChanged`, so `SpotPathing` never triggered automatic pathing when the active spot moved and Shortest Path cleared paths within its default 5-tile `finishDistance`.
- **How it was found:** In-game testing reported by the Board where only right-click paths appeared in the log.
- **Fix:** Tracked the fished spot's `WorldPoint` each tick via `SpotPathing.Locator` so a tile change while fishing (or within 2 ticks of the interaction ending) triggers pathing, and added `NextSpotOverlay` (`EdgeArrow`) to draw the plugin's own tile outline, line, and off-screen edge arrow until fishing resumes (#20).

## 2026-11-05 — Missing health bar sprite retries caused per-tick cache decode lag spikes and despawned NPCs threw in overlay rendering (#19)
- **What broke:** With `Hide game health bar` enabled, any uncached health bar sprite was re-read from the game cache on every client tick (up to 22 decodes per frame); separately, calling `getCanvasTextLocation` on an NPC in the middle of despawning after death threw an exception during overlay rendering.
- **How it was found:** Frame-path benchmarking (`FramePathBenchmarkTest`) and client log inspection after combat kills.
- **Fix:** Rate-limited missing sprite retries in `NativeHealthBarHider` to once per `RETRY_CALLS` (~1 second), cleared the tracked opponent on `NpcDespawned`, and guarded `CombatVitalsOverheadOverlay.onScreen` against client lookup exceptions (#19).

## 2026-11-04 — Opening plugin settings caused a black screen because `FishingSpotPath` enum was package-private (#18)
- **What broke:** RuneLite's config panel requires `@ConfigItem` return-type enums to be `public`; `FishingSpotPath` was declared package-private in #16, throwing an access error when opening the settings panel.
- **How it was found:** Opening the plugin configuration panel in the developer client after #16 merged.
- **Fix:** Declared `public enum FishingSpotPath` (#18).

## 2026-11-04 — Overhead chat rendered twice when `Chat above readout` was off or on the first frame of a message (#14)
- **What broke:** When `Chat above readout` (`hideGameOverheadChat`) was turned off, the overlay still drew its own lifted copy above the readout while the game drew the original line; when turned on, blanking waited until the tick after render so the game's line flashed for a frame under the readout.
- **How it was found:** In-game testing reported by the Board.
- **Fix:** Stopped drawing the overlay's lifted chat copy when `Chat above readout` is off, and blanked covered messages immediately inside `onOverheadTextChanged` via `OverheadChatHolder.hold` before the first frame renders (#14).

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
