# Thieving parity: Pyramid Plunder, Rogues' Den, Timers & Buffs, House Thieving

Task RUNELITE-464. Goal: turn the original plugins off and lose nothing.
This file is the compliance verdict (Phase 0) and the feature map. Status is
honest: only rows marked **done** have code and tests in this branch.

## Compliance verdict

- All four are read-only overlays/timers driven by game state the client
  already shows. No input, no automation, no outside host. Precedents: the
  stock RuneLite Pyramid Plunder, Rogues' Den and Timers & Buffs plugins
  (BSD-2). Nothing here is on the Rejected-or-Rolled-Back list or in the
  Jagex third-party client rules.
- House Thieving (Varlamore) is a Plugin Hub plugin. **Its licence has not
  been checked** (the lookup was blocked in this run). Rule applied: nothing
  is copied from it; behaviour is reimplemented from the game and the OSRS
  wiki. Re-check the licence before any port.
- Reflection, subprocesses, outside HTTP: none used. Voice clips go through
  the existing alert system.

## Constants verified in runelite-api 1.13.6 (`net.runelite.api.gameval`)

`VarbitID.NTK_ROOM_NUMBER`, `NTK_CURRENT_ROOM_LEVEL`, `NTK_PLAYER_TIMER_COUNT`,
`NTK_THIEVING_REQUIRED`, `NTK_URN1_STATE`..`NTK_URN15_STATE`,
`NTK_SARCOPHAGUS_STATE`, `NTK_GOLDEN_CHEST_STATE`, `NTK_TRAP_ACTIVE`,
`NTK_DOOR1_STATE`..`4`, `InterfaceID.NtkOverlay`, `ObjectID.NTK_GOLDEN_CHEST_*`,
`NpcID.NTK_MUMMY_*`; `VarbitID.STAMINA_ACTIVE`, `STAMINA_DURATION`,
`ANTIFIRE_POTION`, `TELEBLOCK_CYCLES`, `VENGEANCE_TIMELIMIT`,
`NZONE_OVERLOAD_POTION_EFFECTS`. Their *meaning* (what each value encodes)
is **unverified** until read in game; the first wiring step logs them at
debug level for the Board to confirm.

## Feature map

| Original feature | Our equivalent | Status |
|---|---|---|
| Pyramid Plunder: room number, thieving level per room | `PyramidPlunder.roomText/levelForRoom/nextRoomOpen` | **wired** (room text on the Thieving badge via `Varbits.PYRAMID_PLUNDER_ROOM`; level per room not shown; unverified in game) |
| Pyramid Plunder: timer + urgent warning | `PyramidPlunder.timerText/urgent` | **done** (logic + tests, not yet wired) |
| Pyramid Plunder: per-room loot and xp | `PyramidPlunder.roomLoot/roomXp/roomItems` | **done** (logic + tests, not yet wired) |
| Pyramid Plunder: highlight spear traps, urns, chests, sarcophagus | scene overlay on `NTK_*` objects | todo |
| Rogues' Den: obstacle highlights and overlays | scene overlay | todo |
| Timers & Buffs: stamina, antifire, antipoison/venom, divine, overload, prayer enhance, teleblock, freeze, vengeance, thralls, cannon, home/minigame teleport, imbued heart | timer catalogue feeding pillar sources, merged with `BoostTimer` | todo |
| House Thieving: owner away/returning timer, lockpick/door state, valuables, alerts, voice clips | new tracker + `AlertSounds` | todo |

## Which plugins can be turned off

None yet. Turn nothing off until a row above reads **done** end to end
(wired, configurable, verified in game) for the plugin in question.
