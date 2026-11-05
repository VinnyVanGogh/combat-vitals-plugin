package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether an NPC target is a big target (a boss, or an NPC with a
 * high max HP), and so which display it gets.
 *
 * Pure: the plugin passes in whether the game's boss health HUD is showing
 * this NPC, its name and its max HP, so the rules are unit testable.
 */
final class BigTargets
{
	/**
	 * Fallback for when the game's boss HUD is not showing (off in the game's
	 * settings, or not yet set for this NPC). Combat bosses from the OSRS wiki's
	 * Boss page (https://oldschool.runescape.wiki/w/Boss, read 2026-11-04):
	 * the main list, plus minigame and raid bosses. Skilling bosses (Tempoross,
	 * Wintertodt, Zalcano) have no opponent health to show, and quest bosses
	 * are left out because many share names with ordinary monsters ("Black
	 * demon"); Bosses + max HP still catches the big ones. Royal Titans and
	 * Grotesque Guardians are the wiki's names for pairs, so their NPC names
	 * (Branda, Eldric, Dusk, Dawn) are listed too.
	 */
	private static final Set<String> BOSSES = new HashSet<>(Arrays.asList(
		"ahrim the blighted", "karil the tainted", "dharok the wretched", "guthan the infested",
		"torag the corrupted", "verac the defiled", "gemstone crab", "scurrius", "giant mole",
		"deranged archaeologist", "dagannoth supreme", "dagannoth rex", "dagannoth prime", "sarachnis",
		"blood moon", "blue moon", "eclipse moon", "kalphite queen", "kree'arra", "commander zilyana",
		"general graardor", "k'ril tsutsaroth", "the hueycoatl", "corporeal beast", "nex", "chaos fanatic",
		"crazy archaeologist", "scorpia", "king black dragon", "chaos elemental", "revenant maledictus",
		"calvar'ion", "vet'ion", "spindel", "venenatis", "artio", "callisto", "brutus", "demonic brutus",
		"obor", "bryophyta", "amoxliatl", "branda the fire queen", "eldric the ice king",
		"doom of mokhaiotl", "mad angel", "zulrah", "vorkath", "phantom muspah", "maggot king",
		"the nightmare", "phosani's nightmare", "yama", "duke sucellus", "the leviathan", "the scar",
		"the whisperer", "vardorvis", "the mimic", "hespori", "skotizo", "shellbane gryphon",
		"grotesque guardians", "dusk", "dawn", "abyssal sire", "kraken", "cerberus", "araxxor",
		"thermonuclear smoke devil", "alchemical hydra", "crystalline hunllef", "corrupted hunllef",
		"tztok-jad", "tzkal-zuk", "sol heredit", "tekton", "vanguard", "vespula", "vasa nistirio",
		"muttadile", "great olm", "the maiden of sugadinti", "pestilent bloat", "nylocas vasilias",
		"sotetseg", "xarpus", "verzik vitur", "akkha", "ba-ba", "kephri", "zebak", "tumeken's warden",
		"elidinis' warden"));

	private BigTargets()
	{
	}

	/**
	 * @param onHud true when the game's boss health HUD is showing this NPC
	 * @param name  the NPC's name, tags removed, or null
	 */
	static boolean isBoss(boolean onHud, String name)
	{
		return onHud || (name != null && BOSSES.contains(name.toLowerCase(Locale.ROOT)));
	}

	/**
	 * @param maxHealth the NPC's max HP, or -1 when unknown. Unknown is never
	 *                  over the threshold, so it falls to everything else.
	 */
	static boolean isBig(BigTargetRule rule, boolean boss, int maxHealth, int threshold)
	{
		final boolean high = maxHealth > 0 && maxHealth >= threshold;
		switch (rule)
		{
			case BOSSES:
				return boss;
			case HP_ONLY:
				return high;
			case BOSSES_AND_HP:
			default:
				return boss || high;
		}
	}

	static DisplayMode mode(boolean big, TargetDisplay bigDisplay, TargetDisplay otherDisplay, DisplayMode display)
	{
		return (big ? bigDisplay : otherDisplay).resolve(display);
	}
}
