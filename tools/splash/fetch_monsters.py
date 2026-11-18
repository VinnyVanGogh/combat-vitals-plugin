#!/usr/bin/env python3
"""Build src/main/resources/com/vinnyvangogh/combatvitals/monster_magic.tsv.

Developer tool, never shipped in the jar and never run by the plugin. It reads
the OSRS Wiki's `infobox_monster` bucket (Monster infobox: "Magic" level and
the defensive "Magic" bonus) through the wiki's public API, and writes one
line per NPC id:

    npcId <TAB> magicLevel <TAB> magicDefenceBonus <TAB> name

Usage: python3 tools/splash/fetch_monsters.py
Source: https://oldschool.runescape.wiki/w/Bucket:Infobox_monster (CC BY-NC-SA 3.0)
"""
import json
import sys
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://oldschool.runescape.wiki/api.php"
PAGE = 500
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/com/vinnyvangogh/combatvitals/monster_magic.tsv"


def query(offset):
    q = ("bucket('infobox_monster').select('page_name','version_anchor','magic_level',"
         "'magic_defence_bonus','id').limit(%d).offset(%d).run()" % (PAGE, offset))
    url = API + "?" + urllib.parse.urlencode({"action": "bucket", "format": "json", "query": q})
    req = urllib.request.Request(url, headers={"User-Agent": "osrs-plugin-data-fetch"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)["bucket"]


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
        level = row.get("magic_level")
        bonus = row.get("magic_defence_bonus")
        if level is None or bonus is None:
            continue
        for npc in npc_ids(row):
            rows.setdefault(npc, (int(level), int(bonus), display_name(row)))


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
        out.write("# npcId\tmagicLevel\tmagicDefenceBonus\tname. OSRS Wiki infobox_monster, CC BY-NC-SA 3.0.\n")
        for npc in sorted(rows):
            level, bonus, name = rows[npc]
            out.write("%d\t%d\t%d\t%s\n" % (npc, level, bonus, name))
    print("wrote %d npc ids to %s" % (len(rows), OUT), file=sys.stderr)


if __name__ == "__main__":
    main()
