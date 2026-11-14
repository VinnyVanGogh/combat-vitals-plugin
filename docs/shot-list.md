# README shot list

One file drives every README picture, so a reshoot is one pass down this list.

**Rule for every shot:** no account names. Not ours (the Board's or a group
member's), not another player's. Blank, blur or replace them with `Player` /
`Groupmate` before the file is committed. RuneLite's **Entity Hider** (hide
other players) keeps strangers out of frame; for your own name, crop above it
or blur it. The window title also carries the name: capture the game view, not
the window.

## Drawn by the plugin (no game needed)

`./gradlew readmeShots` regenerates these into `docs/images/generated/` from
`ReadmeShotsTest`. Neutral gradient backdrop, no game capture. Budget 40 KB
each, checked by the test.

| File | Scene | Settings it draws with |
|---|---|---|
| `badge-woodcutting-basket.png` | Woodcutting 72, 8 free slots, log basket 12/28 free, 112 chopped in 14m | New profile defaults; session count, rates and time to level passed in |
| `badge-fishing-boost.png` | Fishing 79 boosted to 82 by an admiral pie, timer confirmed, 13 free | New profile defaults |
| `badge-idle-full.png` | `Not mining` after stopping, inventory full (red) | New profile defaults |
| `side-pillars-sheet.png` | XP 82%, inventory 10 free, inventory full, target 12/50 | Pillar style Status Bars, labels Icon and number |

## Taken in game

For what only the real game shows. The computer-use rig (RUNELITE-140) is not
in this repo, so these are taken by the Board for now; the rig can run this
list unchanged once it exists. Use a test account, or the Board's own with the
name blanked. Budget: 160 KB each (checked for README images by
`DocsLinksTest`); crop tight, PNG, then `pngquant --quality 70-90`.

Start every shot from a new RuneLite profile with only Combat Vitals (and the
plugins named below) on, GPU off, 100% UI scale, resizable modern layout.

| # | File | Status | Where | Plugin settings | What must be in frame |
|---|---|---|---|---|---|
| 1 | `combat-npc.png` | to take | Any mid-level NPC, e.g. a Hill Giant | Defaults | Readout over the NPC part-way down, green to amber, no player names in frame |
| 2 | `combat-both-sides.png` | to take (replaces the removed `both-sides.png`) | Fighting an NPC that hits back | Targets → Show your own health on | Your readout and the NPC's, same style; crop above your own name |
| 3 | `low-health.png` | in README, clean | Player target, low health | Defaults | Already has no name in frame |
| 4 | `woodcutting-badge.png` | in README, clean | Any tree | Session count on, XP and actions per hour on | Badge over the player |
| 5 | `badge-log-basket.png` | to take (after PR #33/#34 merge) | Willows with a log basket, checked once | Show containers on, Count containers as free on | `Log basket 12/28 free` row and `(+16 basket)` on the inventory line |
| 6 | `side-pillars-skilling.png`, `side-pillars-combat.png` | in README, clean | Fishing; then any fight | Show on side pillars: Both | The two pillars beside the inventory |
| 7 | `alert-voice.gif` | to take | Fishing; let the inventory fill | Inventory full sound: Voice line, Repeat on | Badge going red, the notification, then a click stopping the repeat. GIF under 1 MB |
| 8 | `badge-follows.gif` | to take | Walking between two trees | Defaults | The badge staying over the player as they move. GIF under 1 MB |
| 9 | `sidebar-panel.png` and a shot per preset | blocked on the sidebar panel task | Sidebar panel open | Each preset in turn (Skiller, PvM, Minimal) | The panel only; generated headless by the Swing harness once the panel exists |

## Removed for showing a name

`hero.png`, `both-sides.png`, `pvp-context.png` and `full-picture.jpg` were
deleted on 2026-11-14: each showed an account name over a player or in the
window title. Shots 1 and 2 replace them.
