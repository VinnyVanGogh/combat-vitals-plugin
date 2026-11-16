# How exact the opponent health number is

**Opponent health is never sent to your client.** The server broadcasts a health
*bar*: a ratio out of a fixed scale. Every plugin that shows a number, this one
and RuneLite's built-in included, is inverting that ratio to guess.

The scale is **30**. That was measured on a live world, not assumed.

## What that means

**NPCs at or below 30 max hitpoints are exact.** A Guard on 22 resolves to one
value, always. No guessing involved.

**Players never are.** Above 30 max hitpoints the ratio maps to a band of three
or four possible values, and the number shown is the middle of that band.

For an 88 hitpoint opponent showing ratio 10, the true health is 28, 29 or 30.
The readout says 29. That is the best single guess available, never wrong by
more than one, but it is still a guess, and RuneLite's built-in plugin renders it
identically to the maximum beside it, which it genuinely knows.

| Max hitpoints | Midpoint exactly right | Worst error |
|---:|---:|---:|
| 45 | 67% | 1 |
| 88 | 34% | 1 |
| 99 | 30% | 2 |

## The error that actually matters

Quantisation is the small problem. This is the large one:

> **After your opponent eats, the health bar you can see lags behind.**

Measured across 386 cross-checked readings from a real fight between two
accounts, each logging its own true hitpoints and the ratio it observed for the
other:

- Every reading that was wrong was wrong in the **same direction**, showing
  less health than the target actually had.
- Typical catch-up: **two game ticks**. Worst observed: **four ticks**.
- Worst single reading: **19 hitpoints stale.**

Nothing in the client API reports that an opponent ate. This cannot be detected,
flagged, or corrected. Not by this plugin, and not by RuneLite's built-in one,
which carries the identical error silently.

**The practical version:** trust the number while you are trading hits. Distrust
it for a second or two after they eat, which is exactly when you most want to
trust it. That is a property of the game's networking, not of any plugin.

Method, numbers and the cross-account analysis script are in
[BRIEF.md](../BRIEF.md) under Phase 0.5 and [`docs/phase-0.5/`](phase-0.5/).
The raw captures are deliberately not committed: they name real accounts.

## How it works

The server computes the health bar as:

```
healthRatio = 1 + (healthScale - 1) * health / maxHealth
```

with integer division, and forces `healthRatio = 0` when health is 0. Inverting
that gives a *range* of healths consistent with what you can see, which collapses
to a single value only when `maxHealth <= healthScale`.

Maximum health comes from the NPC cache for monsters, and from the hiscores for
players. Without it, only the fraction of the bar is knowable, which is why an
unranked player degrades to a percentage rather than a wrong number.

**Your own health skips all of this.** The client knows it exactly, from the
hitpoints orb, so it is never put through the recovery. The readout is exact for
you, exact for small NPCs, and banded only where the data genuinely is.

The recovery is pure arithmetic with no client dependency, and is covered by
unit tests including exhaustive checks that the recovered range always contains
the true health, and that the midpoint is never off by more than half the range
width, across every `(maxHealth, health)` pair up to 120.

## Compared with Opponent Information

| | Opponent Information | Combat Vitals |
|---|---|---|
| Corner panel | Yes | Yes |
| Drawn at the target | No | Yes |
| Overhead for players | No | Yes |
| Colour by health | No | Yes, three configurable stops |
| Opponent timeout | Fixed 5s | 1–60s |
| Player hiscores lookup | Always on, no switch | On by default, switchable |
| Documents its own accuracy | No | Yes |
| Combat stat comparison panel | Yes | Yes |
| Shows your own health too | No | Yes |
