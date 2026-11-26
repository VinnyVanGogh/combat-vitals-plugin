# Credits and licences

Back to the [README](../README.md).

Licensed under the **BSD 2-Clause License** ([LICENSE](../LICENSE); full
third-party notices in [NOTICE](../NOTICE)).

- **Code and tables adapted from (BSD 2-Clause):**
  - **[RuneLite](https://github.com/runelite/runelite)** (BSD 2-Clause):
    health-bar ratio inversion, single-opponent tracking, player Hitpoints
    lookup, and boss HUD check (`OpponentInfoPlugin` / `OpponentInfoOverlay`);
    Preserve drain math (`BoostsPlugin#getChangeDownTicks`); degraded essence
    pouch table (`EssencePouchOverlay`); side-pillar geometry (`StatusBarsOverlay`
    / `BarRenderer`); decibel gain (`Notifier`); Forestry spawn IDs and ritual
    circle / flowering bush / sapling solvers, pet chat lines, and random-event
    NPC IDs (`WoodcuttingPlugin`, `ScreenshotPlugin`, `RandomEventPlugin`).
  - **[GP Per Hour](https://github.com/MosheBenZacharia/GP-Per-Hour)** by Moshe
    Ben-Zacharia (BSD 2-Clause): container check/empty chat wording and
    Fill/Empty inventory-delta tracking (`ContainerContents`).
- **Bundled audio and game data:**
  - **Sound packs** (*Soft chime*, *Music box*, *Retro blip*): synthesised
    in-repo by `tools/sounds/generate_packs.py` (BSD 2-Clause).
  - **Voice clips** (`voice/*.wav`): rendered offline with
    **[Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M)** by hexgrad
    (Apache-2.0) and
    **[kokoro-onnx](https://github.com/thewh1teagle/kokoro-onnx)** by
    thewh1teagle (MIT).
  - **Game tables and formulas** (`fishing_spots.tsv`, `monster_magic.tsv`, boss
    IDs, fishing/impling levels, Blast Furnace stove stages, Forestry rules,
    spell rune costs, and splashing / max-hit formulas): from the
    **[Old School RuneScape Wiki](https://oldschool.runescape.wiki)**
    (CC BY-NC-SA 3.0), plus autocast varbit numbers from
    `jcarbelbide/autocasting`.
- **Inspired by (independent re-implementations, no code copied):**
  [NPC Health Text](https://runelite.net/plugin-hub/show/npc-health-text),
  [Monster HP Percentage](https://runelite.net/plugin-hub/show/monster-hp-percentage),
  [Custom HP Bar](https://runelite.net/plugin-hub/show/custom-hp-bar),
  [Gemstone Crab](https://runelite.net/plugin-hub/show/gemstone-crab-timer),
  [Gemstone Crab Alert](https://runelite.net/plugin-hub/show/gemstone-crab-alert),
  [Remaining Casts](https://runelite.net/plugin-hub/show/remaining-casts),
  [Inventory Notifier](https://runelite.net/plugin-hub/show/inventory-notifier),
  [Fish Barrel](https://runelite.net/plugin-hub/show/fish-barrel),
  [Log Basket](https://runelite.net/plugin-hub/show/log-basket),
  [Ent Trimmer](https://runelite.net/plugin-hub/show/ent-trimmer),
  [Idle Alarm](https://runelite.net/plugin-hub/show/idle-alarm),
  [Clue Scroll Notifier](https://runelite.net/plugin-hub/show/clue-scroll-notifier),
  [Sound Alerts Expanded](https://runelite.net/plugin-hub/show/sound-alerts-expanded),
  [XP Goalz](https://runelite.net/plugin-hub/show/xp-goalz),
  [Goal Tracker](https://runelite.net/plugin-hub/show/goal-tracker),
  [Max Hit](https://runelite.net/plugin-hub/show/maxhitplugin),
  RuneLite's built-in **NPC Aggression Timer**, and `PluginMessage` routing to
  [Shortest Path](https://runelite.net/plugin-hub/show/shortest-path)
  (`Skretzo`, BSD 2-Clause).
