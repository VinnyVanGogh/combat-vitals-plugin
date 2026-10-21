# The game's own health bar (RUNELITE-26)

Why the native overhead health bar moves up when the opponent talks, what a
plugin can and cannot do about it, and how the *Hide game health bar* option
works. Researched against RuneLite `runelite-parent-1.13.2`, the version
`build.gradle` pins.

## Why the bar moves

The client draws every actor's 2D extras (overhead chat, health bars, player
name, head icons) in one routine that keeps a running vertical offset and
stacks each element above the previous one. Overhead chat is placed first; if
the actor is talking, the offset grows by 12 px before any health bar is
placed, so the bar is drawn 12 px higher.

From the community deobfuscation of the Jagex client in
[jbx5/devious-client `runescape-client/src/main/java/UserComparator10.java`](https://github.com/jbx5/devious-client/blob/36c5a587421e12b7362905d68e155e818b267261/runescape-client/src/main/java/UserComparator10.java#L106-L229)
(`@Export("drawActor2d")`, last changed 2024-06-26), with `var10` as the
running offset:

- L123: `int var10 = -2;`
- L124-L139: if `overheadText != null`, queue the chat at `viewportTempY - var10` (L130), then `var10 += 12;` (L137)
- L144-L226: for each health bar, `var10 += backSprite.subHeight;` (L198), draw at `viewportTempY - var10`, then `var10 += 2;`
- Hitsplats are positioned separately afterwards (L309 on), not on this stack.

Caveat on the source: this is a third-party deobfuscation, not RuneLite or
Jagex code, and the field names are community labels. It matches what the
Board saw in game (bar higher only while chat is up), which is why it is
treated as confirmed. Nothing in this plugin touches the native bar; PR #2 only
draws on top.

## Can a plugin move it?

No. The RuneLite API exposes nothing that positions an actor's health bar.

- `Actor` offers only `getHealthRatio()` / `getHealthScale()` (read) and
  `getOverheadText()` / `setOverheadText()` / `getOverheadCycle()` /
  `setOverheadCycle()` (checked with `javap` against `runelite-api-1.13.2.jar`).
- `HealthBarConfig` (`runelite-api/.../HealthBarConfig.java` L27-L36), handed
  out by the `PostHealthBarConfig` event, allows only `setPadding(int)`: the
  fill padding inside the bar, not its position.
- Clearing an actor's overhead text would move the bar back, but it would also
  remove the chat, and editing another player's chat is not something to do.

## Can a plugin hide only the health bar?

Not per actor. Globally, yes.

| Route | Result |
| --- | --- |
| Sprite overrides: `client.getSpriteOverrides()` (`Client.java` L1700) + `client.resetHealthBarCaches()` (L1947) | Works. Replaces the bar's front/back sprites by sprite id, so it hits **every** actor's standard bar, your own included. Hitsplats, chat and head icons use other sprites and stay. |
| `Client.setDraw2DMask` (`Client.java` L2139-L2166) | Too coarse. Only `DRAW_2D_OVERHEAD_TEXT` is a separate bit; health bars fall under `DRAW_2D_OTHERS` (L2151) together with hitsplats. |
| `Hooks.registerRenderableDrawListener` (`Hooks.java` L145, L603), as Entity Hider uses it (`EntityHiderPlugin.java` L128, L141) | Hides an actor's model and/or all of its 2D extras together. No health-bar-only option. |
| `PostHealthBarConfig` | Padding only (above). |

The sprite route is the one the base client already uses: Interface Styles'
"HD health bars" puts replacement sprites into the same map and resets the
cache (`InterfaceStylesPlugin.java` L414-L432, through `SpriteManager.java`
L155-L185), for these exact ids (`HealthbarOverride.java` L35-L58:
`StandardHealth30` to `StandardHealth160`, `FRONT` and `BACK`, 22 sprites).
The reset matters because `HealthBarDefinition` caches its sprites
(devious-client `HealthBarDefinition.java`, `getFrontSprite` / `getBackSprite`).

A transparent sprite is an `int[]` of zeros: the client's sprite rasteriser
treats pixel value `0` as transparent (`ImageUtil.java` L477-L495, comment at
L487-L488: "the sprite draw routines check for == 0, not actual transparency").

Not covered: the special head bars (`HeadbarShield100`, `HeadbarBlood*`,
`HeadbarHeat*`, `HeadbarIce*`, Olm timer, shooting star). Those carry boss and
activity mechanics, so they are left alone on purpose. Bars defined without
sprites fall back to plain green/red rectangles in the same routine (L214-L224)
and cannot be hidden this way. Unverified: whether any current bar definition
still has no sprites; the in-game test will show it.

## The option

*Display > Hide game health bar*, default **off**.

- **Global**, not per actor (see above). The plugin still shows HP for the
  tracked opponent and you; every other player and NPC shows no bar while it
  is on. The config description says so.
- Each transparent sprite is built at the size of the sprite it replaces, read
  from the cache, so the stack above it (head icons, player name) does not
  shift.
- Applied on `ClientTick`. Turning the option off, or stopping the plugin,
  removes our sprites and resets the cache; shutdown does this on the client
  thread through `ClientThread.invoke`.
- PR #2's chat redraw is unaffected: it works from the actor's logical height
  and the readout's own box, neither of which depends on the native bar.

Code: `NativeHealthBarHider.java` (map logic, unit tested in
`NativeHealthBarHiderTest.java`), wired in `CombatVitalsPlugin.onClientTick`
and `shutDown`.

### With Interface Styles

Both plugins write the same 22 keys. The hider:

1. Remembers whatever override it replaced (Interface Styles' HD sprite) and
   puts it back when turned off.
2. Re-checks every client tick. If Interface Styles writes its HD sprites
   (any change to its config re-runs `updateAllOverrides`, L130-L136) or
   clears them (`restoreHealthBars`, L428-L432, removes the keys, ours
   included), ours goes back in and the displaced record follows the latest
   state.
3. On restore, leaves alone any key another plugin has overwritten since.

Net effect: with both on, bars stay hidden, and with ours off, HD bars come
back if Interface Styles still has them on. Known limit: when Interface Styles
rewrites the map, a single frame can show its bar before the next client tick
reasserts ours.

## Plugin Hub rules

- Jagex / RuneLite rules (`AGENTS.md` L138-L140, "Interface Restrictions"):
  the nearest line is "No unhiding hidden interface components". This goes the
  other way, it hides something, and it gives the player no extra information.
  The compliance skill's restricted table lists "leaving them hidden" as the
  allowed side of that rule.
- Precedent in the base client: Interface Styles replaces these exact sprites
  for every user; Entity Hider hides whole actors including their bars.
- No Hub plugin found that hides health bars (searched `runelite/plugin-hub`
  PR titles for "health bar", "healthbar", "hide health", "hide hp"; GitHub code
  search for `getSpriteOverrides` with `StandardHealth`). So, against the
  compliance skill's scale: **allowed, base-client precedent, no Hub
  precedent**.
- **Open item for the Board:** the local `runelite-compliance` skill's notes
  list "hiding the game's own health bar" as a Combat Vitals cut under
  "unhiding/altering interface components". `BRIEF.md`'s cut list (Phase 2,
  "Cut") does not contain it, and the rule as written is about unhiding. The
  option ships default off so it can be pulled without touching anything else
  if the Board keeps the earlier cut; worth naming in the Hub PR body.
- No reflection: the hider is generic over the sprite type so its tests use
  plain objects rather than a `Proxy`.
