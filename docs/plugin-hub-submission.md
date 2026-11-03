# Plugin Hub submission

How everything on `main` (combat readout, skilling badge, inventory fill,
activity alerts, hide game health bar, hide game chat) reaches the Plugin Hub
as one pull request.

Audited 2026-11-03 against `runelite/plugin-hub` at commit
`e6ee1f284dd89ab7839c8e598d333c8a4f273367` (master, committed
2026-11-03T21:40:25Z), reading its `README.md`.

## This is an update, not a new submission

`plugins/combat-vitals` already exists on the hub and points at this repo:

```
repository=https://github.com/VinnyVanGogh/combat-vitals-plugin.git
commit=7e9eac9c07548875c4ae09b17f0813f072661cd8
```

`7e9eac9` is "feat: drop the hide game's health bar option". `main` is 35
commits past it. So the hub PR edits that one existing file and changes only
the `commit=` line. Do not create a second file under a new name.

## The `plugins/combat-vitals` file

After this branch and anything else meant for the release are merged to
`main`, the file in your plugin-hub fork becomes:

```
repository=https://github.com/VinnyVanGogh/combat-vitals-plugin.git
commit=<FULL 40-CHARACTER SHA OF THE FINAL MERGE COMMIT ON main>
```

Get the sha with `git rev-parse origin/main` after `git fetch`. The hub
wants the full 40-character hash, not the short one.

## Checklist

Results of the audit. Items marked *Board* need a human.

| # | Check | Result |
|---|---|---|
| 1 | `runelite-plugin.properties`: `displayName`, `author`, `description`, `tags`, `plugins`, `build=standard` | Pass. Description and tags updated in this PR to cover skilling and alerts, matching `@PluginDescriptor` |
| 2 | `icon.png` at repo root, no larger than 48x72 px | Pass: 48x48 RGBA |
| 3 | LICENSE BSD 2-Clause | Pass: `LICENSE` is BSD 2-Clause, (c) 2026 VinnyVanGogh |
| 4 | No reflection, JNI, subprocesses, dynamic classloading | Pass: no `java.lang.reflect`, `setAccessible`, `getDeclared*`, `Runtime.getRuntime`, `ProcessBuilder`, `ClassLoader`, `native`, `System.load` in `src/main` |
| 5 | No input automation | Pass: overlays, sprite overrides and notifications only; nothing sends clicks, keys or menu actions |
| 6 | No external network calls | Pass with one sanctioned exception: player Hitpoints lookup goes through RuneLite's own `HiscoreManager` to Jagex's hiscores, opt-out via *Look up player hitpoints*, documented in the README's *Privacy and network use*. No `OkHttpClient`/`URLConnection`/`HttpClient` of our own |
| 7 | No third-party dependencies | Pass: only `net.runelite:client` (compileOnly) and Lombok; no dependency verification metadata needed |
| 8 | Config group and keyNames frozen since the published commit | Pass: group `combat-vitals` unchanged; every `keyName` in `CombatVitalsConfig` at `7e9eac9` still exists on this branch. New keys are additions only |
| 9 | Every config item has a `description` | Pass: 47 `description =` entries for 42 `keyName`s (sections carry descriptions too) |
| 10 | No identifier collision with other hub plugins | Pass: `plugins/combat-vitals` is ours (above); package `com.vinnyvangogh.combatvitals` is author-namespaced. Not checked: a full code search of every hub plugin's package, which needs an authenticated GitHub code search |
| 11 | Builds on the hub's Java version (11) | `options.release.set(11)` in `build.gradle` compiles against the Java 11 API. `./gradlew clean test` on this branch: see the PR for the run result |
| 12 | README explains every feature, with a screenshot list | Pass: *Features* covers each feature; new *Screenshots* section lists every feature and its image |
| 13 | Missing screenshots taken in game | *Board*: the eight images marked *to take* in the README's *Screenshots* table |
| 14 | In-game check of the whole release on the developer client | *Board*: `./gradlew run`, then the test list on the PR |
| 15 | Feature review risk | *Board*, see below |

### Review risk worth knowing about

**Hide game health bar** (`NativeHealthBarHider`) was removed in `7e9eac9`,
the published commit, and reinstated by the Board on sprite overrides (#10).
It hides the game's own health bars by replacing the 22 standard bar sprites
with transparent ones, the same mechanism the base client's Interface Styles
plugin uses to swap those sprites. It is off by default. A reviewer may still
ask about it, because hiding information the game shows is the kind of thing
the hub's review looks at. If they object, the fallback is to remove that
option in a follow-up commit on the same PR; its `keyName` would then become
unused, which is harmless.

## What the Board pastes into the hub PR

1. In your fork of `runelite/plugin-hub`, on a new branch, edit
   `plugins/combat-vitals` so `commit=` is the final `main` sha. Change nothing
   else.
2. Commit as `Update Combat Vitals` and open the PR against
   `runelite/plugin-hub:master`.
3. PR title: `Update Combat Vitals`
4. PR body:

```
Updates Combat Vitals.

New since the last version:
- Skilling badge: while you fish, chop, mine, thieve or craft runes, a badge
  over your head shows the skill, your boosted level, the boost, seconds until
  it drops, what you can catch on a fishing spot, and how full your inventory is.
- Activity alerts (RuneLite notifications, each with its own toggle):
  inventory full, stopped gathering, fishing spot moved, stopped thieving,
  coin pouches at cap, dodgy necklace broke; pickpocket stun, make-X finished
  and low run energy are off by default.
- Hide game health bar (off by default): replaces the standard health bar
  sprites with transparent ones, the same sprite override Interface Styles uses.
- Hide game chat under readout (off by default).

No new dependencies. No new network use: the only request remains the
existing, opt-out player Hitpoints lookup through RuneLite's HiscoreManager.
Existing config keys are unchanged.
```

5. Watch the `RuneLite Plugin Hub Checks` result on the PR. If it says
   *Changes are needed*, fix on `main` here, merge, and push a new commit to
   the same hub branch with the new sha. Never open a second PR.
