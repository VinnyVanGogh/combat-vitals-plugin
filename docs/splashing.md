# Splashing mode

For AFK Magic training. Turn on **Splashing mode** (Targets tab). When you cast
a combat spell at an NPC, a small overlay shows:

- your **Magic attack bonus**, the Equipment stats window's number;
- your **chance to hit** the NPC you are casting at.

Green means you are splashing (a Magic bonus of **-64 or lower**, a 0% chance
against anything). Amber is a small chance. Red is a chance above **Splash:
warn at %**. If the NPC is not in the wiki table and your bonus is too
high, it says so rather than guessing.

**Splash: setup broke** notifies when you change anything you wear, your spell,
or the NPC you are casting at while splashing.

It only reads and shows. It never casts, clicks or changes a menu.

## The card, the clocks and casts left

The read-out is one compact card like the skilling badge: the spell, the runes
it costs per cast, **casts left**, then the Magic bonus and hit chance. Two
clocks sit under them, like the pump's:

- **Active** while you use the mouse or keyboard, otherwise **Logout in m:ss**,
  the game's own idle logout counting down (any input resets it), with a thin
  bar for how much is left.
- **Casting m:ss**, since your first cast. It restarts after ten ticks with no
  cast.

**Show casts left** (Targets tab, on by default, works with Splashing mode off)
counts casts from the runes in your inventory and rune pouch. A combination
rune pays for either of its two elements, and a worn staff (staff of air, an
air battlestaff, a mystic or combination staff) or a charged tome of fire or
water stands in for that rune without limit. **Casts: warn at** turns the line
red and **Casts: running low** notifies once, and again after a restock; both
mutes apply. A tome counts as unlimited whatever its charges.

A spell you click is read by name. For autocast the game only gives a number,
and the table from that number to the spell (strike to wave) is **unverified in
game**: if the spell name on the card is wrong, tell us.

## The formula

From the OSRS Wiki, [Damage per second/Magic](https://oldschool.runescape.wiki/w/Damage_per_second/Magic)
(steps two to five, read 2026-11-18):

```
effective level = floor(floor(magic level * prayer bonus) + 9)
accuracy roll   = effective level * (equipment magic bonus + 64)
defence roll    = (9 + NPC magic level) * (NPC magic defence + 64)
accuracy > defence:  hit chance = 1 - (defence + 2) / (2 * (accuracy + 1))
otherwise:           hit chance = accuracy / (2 * (defence + 1))
```

Splashing, [OSRS Wiki, Splashing](https://oldschool.runescape.wiki/w/Splashing):
"wearing items with bonuses that total -64 Magic attack bonus or lower". At -64
the accuracy roll is 0, so no NPC can be hit. (The Board's brief said -65; the
wiki's number is -64, and -65 only makes the roll negative, which is the same
0%.)

The prayer bonus is the wiki's table: Mystic Will 1.05, Mystic Lore 1.10, Mystic
Might 1.15, Mystic Vigour 1.18, Augury 1.25. Your level is the boosted Magic level.

`SplashMathTest` works each case by hand from these lines.

### Not modelled

Void magic, the +2 for Accurate on powered staves, the slayer helm and the salve
amulet. Each only raises your accuracy, and none matters at a splash setup, so
the green state is exact. Above -64 the amber and red chances can read a little
lower than the game's for a player using one of those.

## Monster data

`src/main/resources/com/vinnyvangogh/combatvitals/monster_magic.tsv`: one line per
NPC id, `id, Magic level, Magic defence bonus, name`, from the wiki's Monster
infobox (`infobox_monster` bucket; text CC BY-NC-SA 3.0). Built offline by
`python3 tools/splash/fetch_monsters.py`; the plugin never fetches anything.
Re-run it to refresh. An NPC whose id is not in the table has no hit chance
shown unless you are already at -64.

## Casting detected from

- Autocast on: varbit `AUTOCAST_SET`; the spell is `AUTOCAST_SPELL`.
- A spell you click on the NPC: the `MenuOptionClicked` event for
  `WIDGET_TARGET_ON_NPC`, read only, counted for 8 ticks.
