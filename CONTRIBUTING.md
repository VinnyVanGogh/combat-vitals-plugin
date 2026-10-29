# Contributing to Combat Vitals

## Prerequisites

- A JDK capable of compiling targeting Java 11 bytecode (`options.release.set(11)` in `build.gradle`).
- No external runtime dependencies beyond `net.runelite:client` (pinned to `1.12.37` in `build.gradle` for local reproducibility). Do not add transitive or third-party dependencies to `build.gradle`.

## Commands

```bash
# Compile and run unit tests
./gradlew build

# Run unit tests only
./gradlew test

# Launch the RuneLite developer client (--developer-mode --debug) with Combat Vitals loaded
./gradlew run

# Launch a second isolated developer client with its own .runelite directory
./gradlew run -PaltHome=/path/to/alt-home

# Run the Phase 0.5 cross-account validation script self-test
python3 docs/phase-0.5/join_probe_csvs.py --selftest
```

To log in to the developer client with a Jagex account, follow the [RuneLite Using Jagex Accounts guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Architecture

- **Tick snapshot (`CombatVitalsPlugin`)**: Rebuilds immutable `Readout` snapshots (`readout` and `selfReadout`) once per `GameTick`. Overlays (`CombatVitalsPanelOverlay`, `CombatVitalsOverheadOverlay`) read the volatile snapshot each frame and perform no state calculations or scene scans.
- **Pure math (`HealthRecovery`, `HealthReadout`)**: Ratio-to-health inversion and readout formatting/colour interpolation live in pure classes with no RuneLite client dependency and are covered by JUnit tests (`HealthRecoveryTest`, `HealthReadoutTest`).
- **Exact self-health**: The local player's health is read from `Skill.HITPOINTS` via the client (`getBoostedSkillLevel` / `getRealSkillLevel`) and is never passed through `HealthRecovery`.

## Compliance and Engineering Rules

All changes must comply with [AGENTS.md](AGENTS.md) and [BRIEF.md](BRIEF.md):

1. **Single-opponent gate**: Only track the actor from `InteractingChanged` where `event.getSource() == client.getLocalPlayer()`. Never scan the scene or query hiscores for unengaged players.
2. **One cached hiscores lookup**: Player Hitpoints and stat comparison share a single `HiscoreManager.lookupAsync` call per engaged opponent and must remain switchable (`showPlayerHitpoints`, `showStatComparison`).
3. **Config stability**: The config group is `"combat-vitals"`. Never rename the config group or existing `@ConfigItem` keys without a migration.
4. **RuneLite conventions**:
   - Target Java 11; do not use reflection, JNI, or `Thread.sleep()`.
   - Use `net.runelite.api.gameval` constants (`VarbitID`, `VarPlayerID`, etc.) instead of magic numbers.
   - Clean up all overlays and state in `shutDown()`.
   - Use `log.debug()` for diagnostic logging; never log per-tick or per-frame at `INFO`.
5. **Testing**: Unit-test pure logic in `src/test/java/`. Never use automated input or screen-driving tools against the RuneScape client; verify in-game behaviour manually via `./gradlew run`.
