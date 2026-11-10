# Combat Vitals, Research & Build Brief

A RuneLite Plugin Hub plugin. Standalone replacement for the stock **Opponent
Information** plugin, with a better overlay and overhead rendering, covering both
NPCs and PvP opponents.

Work through the phases in order. Phase 0 and Phase 0.5 gate everything else.
Report findings before writing plugin code.

---

## What this plugin is not

**It does not talk to questpath, or to any server, beyond the single hiscores
lookup described in Phase 0.**

This started as one idea, a RuneLite plugin for the questpath account, and was
split into two on purpose. Bundling an account-sync feature into a combat overlay
would be a bad trade in three separate ways:

- **Review surface.** This plugin's entire compliance argument is that it makes
  at most one hiscores request, in direct response to the user engaging one
  target. Adding an outbound data channel to a personal web app puts the whole
  plugin in a different review category.
- **Trust.** A combat overlay that also uploads your account state is the kind of
  thing users read the source of before installing, if they install it at all.
- **Audience.** A combat overlay is for everyone. An account-sync plugin is, for
  now, for one person.

The sync plugin lives in its own repo with its own brief. If you find yourself
adding an HTTP client here, stop: it belongs in the other one.

---

## Phase 0, Compliance research

Most of the binding constraints are already in this repo. **Read `AGENTS.md`
first**, the template ships RuneLite's own rules list, and it is more specific
than the wiki pages. The ones that decide this plugin's shape:

> - No level-based PvP player indicators (highlighting attackable players or
>   those within level range)
> - No opponent freeze duration indicators
> - No PvP target scouting information
> - No identifying an opponent's opponent
> - No player group summaries (attackable counts, prayer usage, etc.)
> - No attack counters *(listed under boss restrictions)*
> - No exposing player information over HTTP
> - No crowdsourcing data about other players

Then read, and summarise what is *new* relative to the above:

- `https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features`
- `https://oldschool.runescape.wiki/w/Update:Third_Party_Client_Guidelines`
- `https://legal.jagex.com/docs/rules/macro-and-client-features-not-permitted`
- `https://github.com/runelite/plugin-hub` (README + review guidelines)

### Out of scope: do not implement, do not propose workarounds

- Any highlighting, icon, or indicator based on combat level, wilderness level,
  or whether a player is attackable.
- Hiscores lookups for any player other than the single currently engaged
  opponent. Preemptive or vicinity-wide scanning violates Jagex's rule that each
  content request be in direct response to a user request.
- Opponent freeze timers; PK/skull warnings.
- Conditional menu entry removal or menu option changes on players.
- Any network transmission of other players' data.
- Reflection, JNI, subprocesses, or runtime-downloaded code. Java only, no
  Kotlin or Scala.

### In scope, with precedent

Hiscores HP-level lookup for the **single** actor set by `InteractingChanged`
where `event.getSource() == client.getLocalPlayer()`.

Precedent: `net.runelite.client.plugins.opponentinfo.OpponentInfoOverlay` in the
base client already does this for player opponents, resolving max HP from
`HiscoreSkill.HITPOINTS`. The lookup must be async and cached.

**Hard invariant:** exactly one hiscores lookup per engagement. If any design
would require a second concurrent lookup, stop and flag it rather than building
it. That single-target gate is the entire compliance basis for this feature.

### Phase 0 findings (completed 2026-08-26)

Sources read: RuneLite `Rejected-or-Rolled-Back-Features`; Jagex Third Party
Client Guidelines (via the OSRS Wiki mirror, `secure.runescape.com` returns
HTTP 403); `legal.jagex.com` macro-and-client-features rules; plugin-hub
README; RuneLite wiki `Plugin-Hub-Review` and
`Information-about-the-Plugin-Hub`; base-client `opponentinfo` and `hiscore`
sources.

- The clause this lookup rests on, from Jagex's rules: "each page/content
  request should only be in direct response to a user request each time",
  alongside a prohibition on "repeated page/content requests from our
  website". Not in `AGENTS.md`. It is the sentence a reviewer applies.
- The hiscores endpoint is `services.runescape.com` / `secure.runescape.com`, Jagex's own servers, not a third party. `AGENTS.md`'s mandatory warning
  string about "a 3rd-party server not controlled or verified by RuneLite
  developers" is therefore inaccurate for this feature. Do not paste it
  verbatim; write a warning that says what is actually sent.
- The base client already gates on
  `event.getSource() != client.getLocalPlayer()` in
  `OpponentInfoPlugin.onInteractingChanged`. This brief's gate matches
  precedent exactly.
- `HiscoreManager` is an `@Singleton` over a shared Guava cache
  (`maximumSize(128)`, `expireAfterWrite(1, HOURS)`, a `NONE` sentinel for
  unranked). The cache is shared with the stock plugin and the Hiscore
  plugin, so enabling both cannot double-request, and `lookupAsync` is safe
  to call per-frame. Unranked degrades to `null` cleanly, as required above.
- Jagex's list carries a catch-all: "Any features which act similarly to
  those described in the above list can also be considered unacceptable."
  Absence from the list is not permission.
- RuneLite rejects generic player highlighting, not only the level-based
  kind, citing harassment and private-message-mode privacy. Wider than the
  `AGENTS.md` wording implies.
- Plugin Hub review has explicit non-goals: it does not check functionality,
  performance, or the factual accuracy of displayed information. The "honest
  uncertainty" principle in this brief is a self-imposed quality bar, not a
  review requirement, and will not earn credit in review.
- Resources ship inside a jar. Use `Class.getResourceAsStream`, never
  `getResource`.
- Icon, if added: `icon.png` at the repository root, max 48x72 px.

Verdict: no part of Phase 2 requires a second concurrent lookup. The hard
invariant holds.

### Skilling vitals, compliance (added 2026-11-02)

The badge over a skilling NPC reads only the local player's own state and
sends nothing anywhere. Each element, the rule it lands nearest, and the
precedent that shows it is allowed:

| Element | Nearest rule | Precedent | Verdict |
|---|---|---|---|
| Own boosted level and `+N` per skill, from `Client.getBoostedSkillLevel` / `getRealSkillLevel` | None: own-account stat display | Base client `boosts` plugin shows exactly this, with `SkillIconManager` icons | Allowed |
| Seconds until the boost drops, Preserve-aware | None; not a boss mechanic, not PvP | Base client `BoostsPlugin.getChangeDownTicks` is the ported model | Allowed |
| Boost drop / expiry cue and optional notification | None | Base client `BoostsConfig.notifyOnBoost` notifies on the boost threshold | Allowed |
| Remaining special attack energy from `VarPlayerID.SA_ENERGY` | Interface Restrictions: "No unhiding hidden interface components (special attack bar)" | Nothing is unhidden; a number is drawn from a varp. Base client `statusbars` draws special attack energy as a bar, `specialcounter` reads `SA_ENERGY` | Allowed |
| Wielded tool identified from `InventoryID.WORN` | None | Base client `specialcounter` reads the weapon slot the same way | Allowed |
| Drink / Eat remembered from `MenuOptionClicked` | Menu Restrictions: adding or removing entries | The event is read, never modified or consumed; no entry is added | Allowed |
| Skill inferred from the NPC's menu options (`Net`, `Pickpocket`, …) | None | `NPCComposition.getActions()` is the same read the attackability gate uses | Allowed |
| Spot type from the spot's NPC id via `net.runelite.client.game.FishingSpot.findSpot` (v2, 2026-11-02) | None | Base client `fishing` plugin's overlay does exactly this, and `FishingSpot` is public API shipped for it | Allowed |
| Fish per method and level, a static table cited from the OSRS wiki ("Fishing", "Fishing spot") | None: own-account level display against public game data | Base client `fishing` plugin draws the spot's fish icon from the same table; many hub plugins (e.g. skill calculators) ship wiki level tables | Allowed |
| Menu option clicked on an NPC read from `MenuOptionClicked` (which method the spot is worked with) | Menu Restrictions: adding or removing entries | Read only, never modified or consumed; the same event the Drink/Eat read uses | Allowed |
| "You catch ..." read from `ChatMessage` (GAMEMESSAGE / SPAM) | None | Base client `fishing` plugin reads the same messages to drive its session timer | Allowed |
| Fish landing in the inventory read from `ItemContainerChanged` for `InventoryID.INV` | None | Base client `fishing` plugin and countless hub plugins read inventory changes this way; only the fish in the table are counted | Allowed |
| Item icons via `ItemManager.getImage` | None | Base client overlays everywhere | Allowed |
| Badge anchored on the local player (`Client.getLocalPlayer`), never the NPC | None | Same anchor as this plugin's own-health readout | Allowed |
| Object option clicked (`Chop down`, `Mine`, `Steal-from`, `Craft-rune`) read from `MenuOptionClicked`, with `Client.getObjectDefinition` for the name and footprint (task-fb282112, 2026-11-03) | Menu Restrictions: adding or removing entries | Read only, never modified or consumed; the same event and read this plugin already makes for NPC options and Drink/Eat. Base client `woodcutting` and `mining` plugins show session overlays for the same activity | Allowed |
| Own animation (`Player.getAnimation`) and own tile, read once per tick, to tell chopping from walking | None: own-player state | Base client `idlenotifier` reads the local player's animation to tell skilling from idle | Allowed |
| Pickpocket vs Attack on the same NPC decided by the clicked option (task-fb282112) | None | Same read-only `MenuOptionClicked` use as above | Allowed |

Not done, deliberately: no highlighting or marking of any tree, rock or
object, and nothing drawn over one; the badge stays over the player. No
guessing a boost source: a rise with nothing recently drunk, eaten or spec'd
shows no source, and a Drink or Eat is labelled `likely`. No automation of
any kind, and no change to any menu.

### Inventory fill and activity alerts, compliance (added 2026-11-03)

Board, 2026-11-03: "the bar to go from green to red as your inventory fills
up, and notify you when it gets full or when you stop fishing", and the same
for thieving and similar. Everything below reads the local player's own state
and sends a notification through RuneLite's `Notifier`; nothing acts on the
game, nothing is sent off the machine, no sound is the plugin's own.

| Element | Nearest rule | Precedent | Verdict |
|---|---|---|---|
| Inventory slots counted from `ItemContainerChanged` for `InventoryID.INV`, item definitions via `Client.getItemDefinition` | None | Base client `fishing` and countless hub plugins read the inventory this way | Allowed |
| Notification when you stop (no animation or experience for N ticks), make-X finished | Input Restrictions: nothing is clicked or queued; a notification only | Base client `idlenotifier` notifies on animation idle and interaction idle | Allowed |
| Inventory full notification | None | Inventory-full notifiers are common on the Plugin Hub; base `idlenotifier` covers the idle that follows | Allowed |
| Fishing spot moved, from `NpcDespawned` of the spot you are on | None | Base client `idlenotifier` "interaction idle" fires on the same moment, less specifically | Allowed |
| Low run energy | None | Base client `idlenotifier` `getLowEnergyThreshold` / "You have low run energy!" (read 2026-11-03 from runelite master) | Allowed |
| Dodgy necklace broke, from the game message | None | Base client `itemcharges` `DODGY_BREAK_PATTERN`, same regex (read 2026-11-03 from runelite master) | Allowed |
| Pickpocket stun from "You've been stunned!", coin pouch count from the inventory | None: own-account state, no automation | Hub thieving helpers notify on the same; unverified by name in this pass | Allowed |
| Bank opening read from `ItemContainerChanged` for `InventoryID.BANK`, your tile from `Player.getWorldLocation` | None | Read only; the same events the base client's bank and idle plugins read | Allowed |

Not done, deliberately: no sound of the plugin's own, no auto-anything, no
menu changes, no prompt to click. Woodcutting and mining alerts run on
experience drops, so they work before task-fb282112 brings the badge to trees
and rocks.

### Skilling badge round 3, compliance (added 2026-11-04)

Board's in-game test, 2026-11-04 (task-fe1a15fd): level locks as `Lv 15`,
an idle state, side pillars, and many more badge settings. All of it draws
the local player's own state; nothing acts on the game.

| Element | Nearest rule | Precedent | Verdict |
|---|---|---|---|
| `Lv 15` in red over a fish above your level, `Needs Fishing 15` on hover via `TooltipManager` | None | Base client `fishing` plugin shows spot contents; tooltips on overlays are standard (`OverlayUtil`, `TooltipManager`) | Allowed |
| "Not fishing" idle badge for N seconds or until another activity | None: a display of your own last activity | Base client `idlenotifier`, XP tracker's idle state | Allowed |
| Vertical bars beside the side panel, target health, XP to level, inventory fill | Interface Restrictions: nothing is hidden, moved, resized or made click-through; drawn over, like Status Bars | Base client `statusbars`, same placement (BSD-2, cited in `SidePillars`) | Allowed |
| Reading Status Bars' settings with `ConfigManager.getConfiguration("statusbars", ...)` and whether it is enabled with `PluginManager` | Reflection, cross-plugin access: no reflection, no plugin class touched; the config manager is the shared store | Config groups are readable by any plugin; Opponent Information check in this plugin already uses `PluginManager.isPluginEnabled` | Allowed |
| XP/hour, actions/hour and session count from `StatChanged` | None | Base client `xptracker` | Allowed |
| Badge over the spot, from the clicked NPC or object's tile | Boss & combat: not applicable, skilling objects only | Base client `fishing` and `woodcutting` draw over spots and trees | Allowed |
| `Needs: Fishing rod + bait` on a fishing click without the tool, bait or level (Board addition, 2026-11-04): own inventory and worn items, own Fishing level and Barbarian Training varbit `BRUT_FISHING_S`, read in `MenuOptionClicked` | Menu Restrictions: the event is read, never consumed or changed, so the click still reaches the game and the game's own dialog still shows | Same read-only `MenuOptionClicked` use as the rows above; base client `itemcharges`/`inventorytags` read own containers | Allowed |

### Path to next fishing spot, compliance (added 2026-11-04)

Board, 2026-11-04: "add shortest path as an option to give you the shortest
path to the next fishing spot that's active". Off by default.

| Element | Nearest rule | Precedent | Verdict |
|---|---|---|---|
| Spot NPCs in the loaded scene read from `WorldView.npcs()` and typed with `FishingSpot.findSpot` | None: public game state any player can see | Base client `fishing` plugin enumerates and highlights every spot NPC in the scene the same way | Allowed |
| Next spot marked until you fish again: its tile outlined, a straight line from you to it, an arrow at the game view's edge while it is off screen (1.1.0; Shortest Path drops its own path within its `finishDistance`, 5 tiles by default) | None: a drawing over public scene state, no input | Base client `fishing` plugin outlines every fishing spot; `groundmarkers` outlines tiles; Shortest Path draws lines to targets and Quest Helper arrows to them (from memory, not re-checked this pass) | Allowed |
| Target sent to Shortest Path via `PluginMessage` (`shortestpath` / `path`, `clear`) | Plugin Hub: reflection or reaching into another plugin is rejected | `PluginMessage` is RuneLite's own public, sanctioned plugin-to-plugin channel, shipped in `runelite-client`; Shortest Path documents and handles it. Same bridge as QuestPath's | Allowed |
| `Path to next spot` added to the right-click menu, only over the badge | Menu Restrictions: adding or removing entries | A `RUNELITE` entry of the plugin's own, nothing removed, swapped or reordered; base client `groundmarkers` ("Mark tile") and `npchighlight` ("Tag") add their own entries the same way | Allowed |
| Bundled list of spot locations from the OSRS wiki | None: public data | Shortest Path itself and many hub plugins ship wiki location tables | Allowed |

Not done, deliberately: no walking, clicking or queuing of any kind; the path
is drawn by Shortest Path and walked by the player. No route into the
Wilderness: those locations are left out of the bundled list.

The spot moving is read from the spot NPC's own tile changing between ticks
(1.1.0): public scene state, the way the base client `fishing` plugin resets
a minnow spot when `npc.getWorldLocation()` changes.

### Hide game health bar, compliance (added 2026-11-04, for the 1.1.0 Hub PR)

To quote in the Plugin Hub PR:

> **Hide game health bar** (`hideGameHealthBar`, **off by default**). Method:
> transparent sprite overrides. The 22 standard health bar sprites
> (`StandardHealth30` to `StandardHealth160`, front and back) are replaced in
> `client.getSpriteOverrides()` with fully transparent sprites of the same
> size, then `client.resetHealthBarCaches()`. This is the same map and the
> same ids the base client's Interface Styles plugin uses for its "HD health
> bars" (`InterfaceStylesPlugin`, `HealthbarOverride`); Interface Styles'
> sprites are remembered and handed back when the option is turned off or the
> plugin stops. No hooks, no draw listeners, no reflection; hitsplats, head
> icons and overhead chat use other sprites and are untouched. Every bar the
> override hides is drawn back by the plugin: your target and you get the
> plugin's readout, and every other player and NPC the game reports a bar for
> (`Actor#getHealthRatio` / `getHealthScale`) gets a plain green and red bar
> in the game's own place. No bar disappears, and none appears that the game
> was not showing.

| Element | Nearest rule | Precedent | Verdict |
|---|---|---|---|
| Blank sprites over the standard health bar ids, restored on toggle-off and shutdown | Interface Restrictions: nothing is moved, resized or made click-through; a cosmetic sprite swap of a non-interactive overlay | Base client Interface Styles overrides the same 22 ids for HD health bars; Entity Hider hides far more | Allowed |
| A plain bar redrawn for every other actor showing one, from its own ratio and scale, at the game's position | None: the same information the game shows at that moment, drawn in its place; nothing extra revealed | Interface Styles restyles these same bars for every actor; Opponent Information reads the same ratio and scale | Allowed |

Accurate scope (Board's in-game test, 2026-11-06, which rejected the first
version): the client looks sprites up by id alone, so the override hides
**every** standard health bar on screen (`docs/native-hp-bar.md`, "Can a
plugin hide only the health bar?": not per actor). 1.1.0 therefore draws
every one of them back (`GameHealthBars`, `GameHealthBarOverlay`). Known
differences from the game's own: one bar per actor where the game can stack
two, the plain look rather than Interface Styles' HD one, and an NPC whose
definition sets its own bar height is drawn a few pixels off.

### Thieving: stuns and hits (added 2026-11-06)

| Element | Nearest rule | Precedent | Verdict |
|---|---|---|---|
| A hit on you while pickpocketing or at a stall does not count as combat unless an attackable NPC or another player is interacting with you | None: reads public scene state (`Actor#getInteracting`) on a hitsplat on you | Base client Opponent Information and NPC Aggression Timer read `getInteracting` the same way | Allowed |
| Your own health shown above the badge for as long as you pickpocket | None: your own account's state | The plugin's own self readout | Allowed |

---

## Phase 0.5, Measure before building

On a PvP world, instrument and log `Actor.getHealthScale()` and
`Actor.getHealthRatio()` for player actors across a range of HP levels.

Determine whether `maxHealth <= healthScale` holds for typical PvP hitpoints
levels. The base client's recovery math is exact only under that condition;
above it, the result is an interval and the stock plugin silently returns the
midpoint.

Report the achievable precision before any overlay work. If the result is an
interval, the overlay must present a range or a percentage. It must never render
an estimated value formatted as an exact integer.

Note the testing rule in `AGENTS.md`: you cannot verify in-game behaviour
yourself, and must not try. This phase is instrumentation you write and the
**user** runs.

### Phase 0.5 findings (completed 2026-08-26)

Measured with HealthScaleProbe against live NPCs and the local player.
Raw data: `.runelite/combat-vitals/health-scale-probe.csv`.

**`healthScale` is 30 for every actor observed**. four distinct Guard ids, a
Man, and a level-105 player. It is a fixed health-bar resolution, not the
actor's max health. The condition for exact recovery therefore reduces to
`maxHealth <= 30`.

- **NPC targets: exact.** Guard (maxHealth 22, healthScale 30, ratio 20)
  recovers min 15 / max 15, a single value. `NPCManager.getHealth()` gives
  the max health, so the NPC readout can print an integer honestly. Bosses
  above 30 hitpoints are the exception and fall to the interval case.
- **Player targets: an interval, never exact.** Any account above 30
  hitpoints, which is all of them. Worked example at 99 hitpoints and
  ratio 15: min 48, max 51, a 4-wide band. The base client prints the
  midpoint, 50, formatted identically to a known value.

**Consequence for Phase 2, binding:** the player readout must render a range
or a percentage. It must never format a recovered player health as a bare
integer. The "honest uncertainty" line in this brief is the whole point of
the plugin, not a nicety, stock is actively misleading here and that is the
gap worth shipping into.

Confirmed against a live player opponent: a live opponent, combat 91, healthScale
30 across ratios 23, 20 and 19 during an actual fight. Every actor measured
(NPC, local player, and opposing player) reports scale 30, so nothing about
the verdict is left resting on inference.

Interval width is roughly `maxHealth / (healthScale - 1)`, so about 3 hitpoints
at 80 max and about 4 at 99. That is wide enough to matter: in PvP it is the
difference between a hit killing and not, which is exactly the judgement a
midpoint printed as an integer invites a user to make wrongly.

Observed live during the same fight. An existing plugin displayed `29/88`
for this opponent while the probe recorded `ratio=10`. The 88 is exact, it
is the hiscores Hitpoints level. The 29 is not: healths 28, 29 and 30 all
produce ratio 10, and 29 is merely the midpoint, rendered in the same
typeface as the number beside it that is genuinely known. At the same
fight's `ratio=5` sample the band is {13, 14, 15} and the display reads 14.

This is the plugin's reason to exist, stated concretely: not that stock
lacks a range, but that stock presents a guess as a measurement at exactly
the moment a player is deciding whether their next hit kills.

#### Cross-account validation, 2026-09-02

Two accounts, `VinnyWanGogh` (96 max) and `VinnyLanGogh` (99 max), fighting on
world 316 with a probe on each client. Each side records its own hitpoints from
the orb and the ratio it observes for the other, joined on wall clock. 386
scored pairs.

**The recovery is correct.** Not one reading in 386 fell below its recovered
range. The inverse behaves identically whether the actor being observed is
yourself or somebody else, which until now was assumed rather than tested.

**But quantisation is the smaller error.** 32 readings (8.3%) sat above their
range, every one of them because the observed health bar lags healing. Median
catch-up 1203ms, worst 2405ms, two game ticks typically, four at worst. The
worst single reading was 19 hitpoints stale.

The direction is the evidence: 32 above, 0 below. A stale-low bar produces
exactly that asymmetry; a broken inverse would scatter both ways.

So the readout carries two errors of very different size:

| Source | Magnitude | When |
|---|---|---|
| Ratio quantisation | ±1 to ±2 | always |
| Bar lag after healing | up to ~20 | for ~2 ticks after the target eats |

**Consequence for Phase 2.** The lag is invisible to any client, nothing in
the API says an opponent ate, so it cannot be detected, flagged, or corrected,
and the base client carries it identically. It cannot be designed away. What it
does settle is the honesty argument, which had been resting on ±1: the number
is most likely to be badly wrong precisely when an opponent is eating to
survive, which is the moment a player most wants to trust it. Whatever Phase 2
renders should not imply a precision the data cannot support at exactly that
moment.

The join script is under `docs/phase-0.5/`. The raw captures are gitignored
rather than committed: they name real accounts, including an opponent who
had no say in appearing in a public repository.

---

## Phase 1, Reference reading

**Base client** (`runelite/runelite`, BSD-licensed, reuse with credit):

- `plugins/opponentinfo/OpponentInfoPlugin.java`, `InteractingChanged`
  handling, the 5-second opponent timeout, boss HP HUD text override
- `plugins/opponentinfo/OpponentInfoOverlay.java`, the ratio-to-health
  recovery math, NPC max HP via `NPCManager`, player max HP via hiscores,
  `hasHpHud()` suppression
- `plugins/opponentinfo/PlayerComparisonOverlay.java`, how the stat comparison
  is gated behind a config flag
- `plugins/hiscore/HiscorePlugin.java`, how lookups are gated on user action
- `client/hiscore/HiscoreManager.java`, caching and async behavior

**Plugin Hub reference:**

- `devrat0/npc-health-text-plugin`, overhead HP text rendering, per-NPC
  position overrides, color gradient, whitelist/blacklist filtering

**API surface:** `Actor.getHealthRatio()`, `Actor.getHealthScale()`,
`NPCComposition`, `NPCManager.getHealth()`, `OverlayManager`, `OverlayPosition`.

### Phase 1 findings, completed 2026-08-26

**Reuse from the base client.**

- `hasHpHud()` is NPC-only, it returns false for players outright. It reads
  `VarbitID.HPBAR_HUD_BOSS_DISABLED` and `VarPlayerID.HPBAR_HUD_NPC` and
  compares against the NPC's composition id. There is no in-game HUD to
  suppress against for a player target, so the suppression rule in Phase 2
  applies to NPCs only.
- `HiscorePlugin` gates its lookup on an explicit "Lookup" menu entry added
  behind `config.playerOption()`. That is the cleanest available statement of
  "in direct response to a user request" and is worth citing in the PR.

**Reuse from `devrat0/npc-health-text-plugin`.**

- Overhead drawing is `OverlayPosition.DYNAMIC` + `OverlayLayer.UNDER_WIDGETS`.
- Vertical placement resolves off `Actor.getLogicalHeight()`: TOP is the full
  height, MIDDLE is half, BOTTOM is 0, plus a configurable pixel offset, fed
  to `getCanvasTextLocation()`.
- Per-target overrides are a single config string of comma-separated
  `pattern:top|middle|bottom` entries, matched with RuneLite's own
  `WildcardMatcher`. This satisfies the Phase 2 requirement with no new
  dependency, which matters because non-RuneLite dependencies need hash
  verification and slow review.

**Do not copy from it.** It is a useful reference, not a model.

- It reads the boss bar with `client.getWidget(303, 0)` and parses the text
  out of it. `AGENTS.md` forbids both the hardcoded interface id and the
  manual interface/component combination. The base client's varbit check is
  correct and cheaper; use that.
- It iterates every visible NPC every frame. `AGENTS.md` forbids scanning the
  scene per frame, and this plugin has no need to: we track exactly one
  opponent, already delivered by `InteractingChanged`.
- Its formatter builds a template then strips empty brackets and stray
  separators back out with a chain of regexes. If Phase 2 offers a format
  string, design the placeholders so that unset values do not need cleaning
  up afterwards.

**The gap is confirmed.** That plugin is NPC-only, it has no player rendering
at all. Overhead health for a player target is not something a hub plugin
currently does, which is the differentiator, and also why it was the one item
worth asking about rather than assuming.

---

## Phase 2, Scope

### The overlay

This is the core deliverable. Concrete improvements over stock Opponent
Information:

- **Overhead rendering option.** Stock is a fixed corner panel only. Support
  drawing at the actor, with per-target vertical position override
  (`Top` / `Middle` / `Bottom`) so text does not collide with the top-screen
  boss health bar on large NPCs.
- **Honest uncertainty.** Where the recovered health is an interval rather than
  an exact value, display it as such. Do not present the midpoint as exact.
- **Configurable display format.** Number, percentage, both, or a user-defined
  format string. Optional decimal precision on percentages.
- **Color gradient** keyed to remaining health fraction.
- **Configurable opponent timeout.** Stock hardcodes a 5-second `WAIT`.
- **Persist-after-timeout option**. keep the readout up until the target dies
  or despawns, rather than when the in-game bar expires.
- **Whitelist / blacklist** filtering by name.
- **Suppression when redundant.** Stand down when the in-game HP HUD is showing
  the same information, mirroring the existing `hasHpHud()` check. Also detect
  whether stock Opponent Information is enabled and avoid drawing a duplicate
  panel over it.

The config class is deliberately absent from the skeleton. It arrives here, once
the option list above survives Phase 0. Config group must be `combat-vitals`, 
specific, per `AGENTS.md`, and never renamed afterwards without a migration.

### NPC targets

Max HP from `NPCManager.getHealth()`. Exact recovery works here. No lookup, no
network, no compliance surface.

### Player targets (PvP)

- Percentage from the health ratio, no lookup required.
- Exact or interval HP via the single-opponent hiscores lookup described in
  Phase 0.
- Two config flags, matching the base client's actual shape:
  `showPlayerHitpoints` (the HP lookup) defaults **on**;
  `showStatComparison` (the stat table) defaults **off**. Decided 2026-08-26.

  Correction: an earlier draft of this brief said the HP lookup mirrors
  `lookupOnInteraction()`. It does not. In the base client the lookup in
  `OpponentInfoOverlay.render()` is ungated by any config item;
  `lookupOnInteraction` (default false) gates only `PlayerComparisonOverlay`.
  Default-on therefore *is* stock precedent. Give it an off switch anyway;
  stock has none, and having one is worth stating in the submission PR.
- Overhead rendering and name whitelist/blacklist apply to player targets,
  not NPC-only. Decided 2026-08-26, without a Discord ask. Build it behind an
  `overheadForPlayers` toggle so the conservative fallback is a default flip
  rather than surgery on the render path.
- Handle the unranked case: a player absent from the hiscores must degrade
  cleanly to percentage display, not to a wrong number or an error.
- Optional: a combat stat comparison panel from the same cached result.

### Cut

Cut 2026-08-26, cut, not deferred. These sit near the "impossible switches in
PvP" language in Jagex's 2022 statement, and near `AGENTS.md`'s "no attack
counters" and "no PvP target scouting information". Nothing in the overlay
needs them. Do not reintroduce without revisiting Phase 0.

- Time-to-kill or kill-threshold estimates against a player target
- Damage-dealt history or DPS tracking scoped to a PvP opponent

---

## Phase 3, Engineering quality bar

`AGENTS.md` carries RuneLite's own rules for logging, threading, HTTP, config
naming, and packaging, and they apply in full. What is specific to this plugin:

- RuneLite dependency stays pinned to an explicit version. Already done, 
  `build.gradle` pins `1.12.37`; bump deliberately, never to `latest.release`.
  Note (Phase 0): `runelite-plugin.properties` sets `build=standard`, and the
  plugin-hub packager replaces `build.gradle` and `settings.gradle` at
  submission. The pin governs local development only, it does not make the
  hub build reproducible. Keep it for local determinism; claim no more.
- No mutation of injected shared singletons. In particular, do not call
  `registerTypeAdapter` on an injected `GsonBuilder`, build any Gson instance
  once into a field.
- Thread safety: overlay and plugin state is touched from both the client thread
  and RuneLite's scheduler. Use appropriate synchronization or concurrent
  collections. Do not rely on defensive copies alone.
- `shutDown()` must fully reverse `startUp()`, every overlay removed, all
  cached state cleared, no residual markers or listeners.
- No `return` inside a `finally` block. No `Future.get()` without a timeout.
- Real unit tests for the ratio-to-health recovery math, including the
  interval case and the `healthScale == 1` edge case. This math is pure and
  needs no client, so there is no excuse for it being untested.

---

## Phase 4, Getting it installed

Adoption is downstream of the plugin being good, but these are the parts that
are cheap and get skipped:

- **The Plugin Hub listing is the whole shop window.** `displayName`,
  `description`, and `tags` in `runelite-plugin.properties` are what users
  search and read. Tags are a search index, not decoration.
- **A README with screenshots.** Overhead rendering versus the stock corner
  panel is a visual difference, and one image argues it better than a paragraph.
- **Say what it replaces.** "Opponent Information, but it can draw over the
  target and it doesn't lie about precision" is a clearer pitch than a feature
  list.
- **Ship useful defaults.** A plugin that does nothing until configured gets
  uninstalled before it gets configured. The default state should be the
  stock-equivalent readout, with the new behaviour discoverable.
- **Do not duplicate stock.** The suppression rules in Phase 2 are an adoption
  feature as much as a correctness one: a plugin that double-draws over
  Opponent Information reads as broken.

Submission, per the plugin-hub README: fork `runelite/plugin-hub`, add a file
under `plugins/` containing `repository=<https clone URL>` and
`commit=<full 40-char hash>`, and open a PR. Non-RuneLite dependencies need
Gradle dependency verification with cryptographic hashes; the cleanest way to
keep that painless is to add no non-RuneLite dependencies at all. A BSD 2-Clause
licence is required and is already in `LICENSE`.

---

## Reporting

At the end of Phase 0 and Phase 0.5, stop and report before continuing. If any
feature under consideration lands near a Phase 0 constraint, raise it rather
than building it and asking later.
