#!/usr/bin/env python3
"""Build src/main/resources/com/vinnyvangogh/combatvitals/npc_stats.tsv.

Developer tool, never shipped in the jar and never run by the plugin. It reads
the OSRS Wiki's `infobox_monster` bucket through the wiki's public API and
writes one line per NPC id (tab separated, "-" for a value the wiki leaves
blank):

    npcId combatLevel hitpoints maxHit attackStyle attackSpeed
    stabDef slashDef crushDef magicDef rangedDef weakness
    poisonImmune venomImmune name

The plugin reads this file at runtime and makes no request of its own.
Source: https://oldschool.runescape.wiki/w/Bucket:Infobox_monster (CC BY-NC-SA 3.0)

Usage: python3 tools/npcstats/fetch_npc_stats.py
"""
import json
import sys
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://oldschool.runescape.wiki/api.php"
PAGE = 500
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/com/vinnyvangogh/combatvitals/npc_stats.tsv"

FIELDS = ["page_name", "version_anchor", "id", "combat_level", "hitpoints", "max_hit", "attack_style",
          "attack_speed", "stab_defence_bonus", "slash_defence_bonus", "crush_defence_bonus",
          "magic_defence_bonus", "standard_range_defence_bonus", "range_defence_bonus",
          "elemental_weakness", "elemental_weakness_percent", "poison_resistance", "venom_resistance"]


def query(offset):
    q = "bucket('infobox_monster').select(%s).limit(%d).offset(%d).run()" % (
        ",".join("'%s'" % f for f in FIELDS), PAGE, offset)
    url = API + "?" + urllib.parse.urlencode({"action": "bucket", "format": "json", "query": q})
    req = urllib.request.Request(url, headers={"User-Agent": "osrs-plugin-data-fetch"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)["bucket"]


def clean(value):
    """One tab-free string; lists (repeated fields) joined with '/'; '-' when empty."""
    if value is None:
        return "-"
    if isinstance(value, list):
        value = "/".join(str(v) for v in value)
    value = str(value).replace("\t", " ").replace("\n", " ").strip()
    return value or "-"


def immune(value):
    """1 when the wiki text says immune. Unverified for numeric resistances: those count as not immune."""
    return 1 if value is not None and "immune" in str(value).lower() and "not immune" not in str(value).lower() else 0


def weakness(row):
    element = row.get("elemental_weakness")
    if not element:
        return "-"
    pct = row.get("elemental_weakness_percent")
    return "%s %s%%" % (element, pct) if pct is not None else str(element)


def display_name(row):
    name = row["page_name"]
    anchor = row.get("version_anchor")
    return ("%s (%s)" % (name, anchor) if anchor else name).replace("\t", " ")


def npc_ids(row):
    ids = row.get("id") or []
    if isinstance(ids, str):
        ids = [ids]
    return [int(npc) for npc in ids if str(npc).isdigit()]


def add_page(rows, page):
    for row in page:
        ranged = row.get("standard_range_defence_bonus", row.get("range_defence_bonus"))
        line = [clean(row.get("combat_level")), clean(row.get("hitpoints")), clean(row.get("max_hit")),
                clean(row.get("attack_style")), clean(row.get("attack_speed")),
                clean(row.get("stab_defence_bonus")), clean(row.get("slash_defence_bonus")),
                clean(row.get("crush_defence_bonus")), clean(row.get("magic_defence_bonus")), clean(ranged),
                clean(weakness(row)), str(immune(row.get("poison_resistance"))),
                str(immune(row.get("venom_resistance"))), clean(display_name(row))]
        for npc in npc_ids(row):
            rows.setdefault(npc, line)


def fetch_all():
    rows = {}
    offset = 0
    while True:
        page = query(offset)
        add_page(rows, page)
        if len(page) < PAGE:
            return rows
        offset += PAGE


def main():
    rows = fetch_all()
    with OUT.open("w", encoding="utf-8") as out:
        out.write("# npcId\tcombat\thp\tmaxHit\tstyle\tspeed\tstab\tslash\tcrush\tmagic\tranged\tweakness"
                  "\tpoisonImmune\tvenomImmune\tname. OSRS Wiki infobox_monster, CC BY-NC-SA 3.0.\n")
        for npc in sorted(rows):
            out.write("%d\t%s\n" % (npc, "\t".join(rows[npc])))
    print("wrote %d npc ids to %s" % (len(rows), OUT), file=sys.stderr)


if __name__ == "__main__":
    main()
