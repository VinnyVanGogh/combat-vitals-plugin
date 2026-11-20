package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import net.runelite.api.Skill;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

/**
 * Config group is "combat-vitals" and must never be renamed without a migration —
 * renaming silently resets everyone's saved settings.
 */
@ConfigGroup(CombatVitalsConfig.GROUP)
public interface CombatVitalsConfig extends Config
{
	String GROUP = "combat-vitals";

	/*
	 * The two master switches sit above every section (positions 0 and 1);
	 * six sections follow, all collapsed, one per tab of the plugin's own
	 * sidebar panel plus Advanced. Which section a setting is in, and its order
	 * there, is PanelLayout's (PanelLayoutTest), so the list and the panel read
	 * the same. Section names and positions are display only: every keyName is
	 * frozen (ConfigKeyFreezeTest), and the section key strings are kept.
	 */

	@ConfigItem(
		keyName = "combatVitals",
		name = "Combat vitals",
		description = "Health for your target and for you: the panel, the readout over the target, your own "
			+ "health, the game health bar swap and the combat side pillar. Off turns all of it off.",
		position = 0
	)
	default boolean combatVitals()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingVitals",
		name = "Skilling vitals",
		description = "The skilling badge, the skilling side pillars, the next fishing spot, and the activity "
			+ "and boost alerts. Off turns all of it off.",
		position = 1
	)
	default boolean skillingVitals()
	{
		return true;
	}

	@ConfigSection(
		name = "Combat readout",
		description = "The panel and the readout over your target, your own health, and the game's health bar. "
			+ "Same as the sidebar panel's Combat tab.",
		position = 2,
		closedByDefault = true
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Combat targets",
		description = "Which opponents get a readout, and how big targets show. Same as the Targets tab.",
		position = 3,
		closedByDefault = true
	)
	String targetSection = "targets";

	@ConfigSection(
		name = "Skilling badge",
		description = "The badge while you skill: which skills, its rows, boosts, once you stop, and the next "
			+ "fishing spot. Same as the Skilling tab.",
		position = 4,
		closedByDefault = true
	)
	String skillingSection = "skilling";

	@ConfigSection(
		name = "Alerts and sounds",
		description = "Inventory full, stopped, and more: on or off, what each sounds like, and repeats. Same as "
			+ "the Alerts tab.",
		position = 5,
		closedByDefault = true
	)
	String alertsSection = "alerts";

	@ConfigSection(
		name = "Look",
		description = "Colours, styles, positions and sizes of the readout and badge, and the side pillars. Same "
			+ "as the Look tab.",
		position = 6,
		closedByDefault = true
	)
	String pillarsSection = "pillars";

	@ConfigSection(
		name = "Advanced",
		description = "Timeouts and update notes",
		position = 7,
		closedByDefault = true
	)
	String advancedSection = "advanced";

	@ConfigItem(
		keyName = "showPanel",
		name = "Show panel",
		description = "Draw the opponent's health in a corner panel, like the stock plugin.",
		section = displaySection,
		position = 0
	)
	default boolean showPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverhead",
		name = "Show over target",
		description = "Draw the opponent's health above them in the world.",
		section = displaySection,
		position = 1
	)
	default boolean showOverhead()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overheadOnlyAfterDamage",
		name = "Only after damage",
		description = "Show the readout over a target (or over you) only for a few seconds after it takes a hit, "
			+ "like the game's own health bar. Off: it stays up for as long as it is shown today.",
		section = displaySection,
		position = 9
	)
	default boolean overheadOnlyAfterDamage()
	{
		return false;
	}

	@ConfigItem(
		keyName = "displayMode",
		name = "Display",
		description = "Show hitpoints, a percentage, or both.",
		section = displaySection,
		position = 2
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.BOTH;
	}

	@ConfigItem(
		keyName = "percentageDecimals",
		name = "Decimal percentage",
		description = "Show one decimal place on percentages.",
		section = displaySection,
		position = 3
	)
	default boolean percentageDecimals()
	{
		return false;
	}

	@ConfigItem(
		keyName = "colourGradient",
		name = "Colour by health",
		description = "Fade the text from green to red as the target loses health.",
		section = pillarsSection,
		position = 4
	)
	default boolean colourGradient()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overheadPosition",
		name = "Overhead position",
		description = "Where above the target the text sits. Large NPCs often read better on Bottom.",
		section = pillarsSection,
		position = 1
	)
	default OverheadPosition overheadPosition()
	{
		return OverheadPosition.TOP;
	}

	@Alpha
	@ConfigItem(
		keyName = "healthFullColour",
		name = "Colour at full",
		description = "Colour of the readout at full health, used when Colour by health is on.",
		section = pillarsSection,
		position = 5
	)
	default Color healthFullColour()
	{
		return new Color(0, 146, 54, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "healthMidColour",
		name = "Colour at half",
		description = "Colour the readout passes through at half health.",
		section = pillarsSection,
		position = 6
	)
	default Color healthMidColour()
	{
		return new Color(255, 193, 7, 230);
	}

	@Alpha
	@ConfigItem(
		keyName = "healthLowColour",
		name = "Colour at empty",
		description = "Colour of the readout as health approaches zero.",
		section = pillarsSection,
		position = 7
	)
	default Color healthLowColour()
	{
		return new Color(199, 26, 26, 230);
	}

	@ConfigItem(
		keyName = "overheadStyle",
		name = "Style",
		description =
			"Bar behind text draws a health bar sized to fit the text, carrying the colour so the text "
				+ "can stay white. Text above health bar lifts the text clear of the game's own bar "
				+ "instead. Text only draws coloured text where it falls, which will overlap the "
				+ "game's bar.",
		section = pillarsSection,
		position = 0
	)
	default OverheadStyle overheadStyle()
	{
		return OverheadStyle.BAR;
	}

	@Alpha
	@ConfigItem(
		keyName = "barBackground",
		name = "Bar background",
		description =
			"Colour behind the health fill, used by the Bar behind text style. Opaque by default so "
				+ "the game's own health bar does not show through it. Lower the alpha to let it "
				+ "show again.",
		section = pillarsSection,
		position = 3
	)
	default Color barBackground()
	{
		return Color.BLACK;
	}

	@Range(min = -200, max = 200)
	@ConfigItem(
		keyName = "overheadOffset",
		name = "Overhead offset",
		description = "Extra height in pixels, applied after the position above.",
		section = pillarsSection,
		position = 2
	)
	default int overheadOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "hideGameHealthBar",
		name = "Hide game health bar",
		description =
			"Replace the game's own green and red health bar with the plugin's: your target and you get the "
				+ "readout, and every other player and NPC that shows a bar gets a plain one drawn in the "
				+ "game's place, so no bar goes missing. Hitsplats and overhead chat stay. Overrides "
				+ "Interface Styles' HD health bars while on, and gives them back when off.",
		section = displaySection,
		position = 5
	)
	default boolean hideGameHealthBar()
	{
		// Off by default for the Hub release (Board, 2026-11-04 night): it
		// changes the game's own sprites, so it is something to opt into. A
		// saved value is kept under the unchanged key, so only profiles that
		// never set it change.
		return false;
	}

	@ConfigItem(
		keyName = "hideGameOverheadChat",
		name = "Chat above readout",
		description =
			"While the readout or skilling badge is over you or your opponent, take the game's own overhead "
				+ "chat line off and draw it above the readout instead, so it is never hidden under it. "
				+ "The message keeps its full time on screen and goes back to the game when the readout goes. "
				+ "Off: the game draws its own line and the plugin adds nothing. A skilling NPC is never touched.",
		section = displaySection,
		position = 6
	)
	default boolean hideGameOverheadChat()
	{
		// On by default (Board, 2026-11-04, after the in-game test). It edits
		// the actor's chat text, the way the base Chat Filter does.
		return true;
	}

	@ConfigItem(
		keyName = "aggressionTimer",
		name = "NPC aggression timer",
		description = "Count down the ten minutes after which nearby NPCs stop being aggressive, in the panel "
			+ "and the skilling badge's corner. Starts when an NPC attacks you; resets when you leave the "
			+ "21x21 area, hop worlds or change floors. This plugin's own timer: it does not read the "
			+ "base NPC Aggression Timer plugin.",
		section = displaySection,
		position = 7
	)
	default boolean aggressionTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "aggressionAreaLines",
		name = "Aggression area lines",
		description = "Draw the 21x21 area on the ground while the timer runs. Off by default: RuneLite's "
			+ "NPC Aggression Timer plugin draws the same lines.",
		section = displaySection,
		position = 8
	)
	default boolean aggressionAreaLines()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOwnHealth",
		name = "Show your own health",
		description =
			"Draw your own health over your character while you are engaged, in the same style as your "
				+ "opponent's. Taken from the hitpoints orb, so it is exact rather than recovered.",
		section = displaySection,
		position = 4
	)
	default boolean showOwnHealth()
	{
		// On by default: "Hide game health bar" hides your own bar too (Board, 2026-10-20).
		return true;
	}

	@ConfigItem(
		keyName = "showForNpcs",
		name = "NPC targets",
		description = "Show the readout when fighting NPCs.",
		section = targetSection,
		position = 0
	)
	default boolean showForNpcs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showForPlayers",
		name = "Player targets",
		description = "Show the readout when fighting other players.",
		section = targetSection,
		position = 1
	)
	default boolean showForPlayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overheadForPlayers",
		name = "Draw over players",
		description = "Draw over player targets as well as NPCs. Turn off to keep players in the panel only.",
		section = targetSection,
		position = 2
	)
	default boolean overheadForPlayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlayerHitpoints",
		name = "Look up player hitpoints",
		description =
			"Fetch the opponent's Hitpoints level from the hiscores so their health can be shown as a "
				+ "number rather than a percentage. One lookup per opponent, cached. Sends their display "
				+ "name to Jagex's hiscores service. The stock Opponent Information plugin does this with "
				+ "no way to turn it off.",
		section = targetSection,
		position = 3
	)
	default boolean showPlayerHitpoints()
	{
		return true;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(
		keyName = "showStatComparison",
		name = "Stat comparison panel",
		description =
			"Compare your combat stats against a player opponent's, in the panel. Reads the same "
				+ "cached hiscores result as the hitpoints lookup, so it costs no extra request.",
		section = targetSection,
		position = 4
	)
	default boolean showStatComparison()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bigTargetDisplay",
		name = "Big targets",
		description =
			"Display for bosses and high-HP NPCs, as set by Big target is. Same as Display follows the "
				+ "Display setting above. Players always use Display.",
		section = targetSection,
		position = 5
	)
	default TargetDisplay bigTargetDisplay()
	{
		// Same as Display, so a profile that never sets it is unchanged.
		return TargetDisplay.SAME;
	}

	@ConfigItem(
		keyName = "otherTargetDisplay",
		name = "Other targets",
		description =
			"Display for every NPC that is not a big target. Same as Display follows the Display "
				+ "setting above.",
		section = targetSection,
		position = 6
	)
	default TargetDisplay otherTargetDisplay()
	{
		return TargetDisplay.SAME;
	}

	@ConfigItem(
		keyName = "bigTargetRule",
		name = "Big target is",
		description =
			"What counts as a big target. A boss is any NPC the game's boss health bar is showing, or one "
				+ "on the plugin's list of bosses. Max HP uses Big target max HP; an NPC whose max HP is "
				+ "unknown counts as an other target.",
		section = targetSection,
		position = 7
	)
	default BigTargetRule bigTargetRule()
	{
		return BigTargetRule.BOSSES_AND_HP;
	}

	@Range(min = 1, max = 10000)
	@ConfigItem(
		keyName = "bigTargetMaxHp",
		name = "Big target max HP",
		description = "An NPC with at least this much max HP is a big target, when Big target is uses max HP.",
		section = targetSection,
		position = 8
	)
	default int bigTargetMaxHp()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "splashMode",
		name = "Splashing mode",
		description = "For AFK Magic training: when you attack an NPC with a combat spell, show your Magic "
			+ "attack bonus and your chance to hit it, from the OSRS wiki's accuracy formula, and warn when "
			+ "the bonus is too high to splash. Information only: nothing is cast, clicked or changed for you.",
		section = targetSection,
		position = 9
	)
	default boolean splashMode()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "splashMaxChance",
		name = "Splash: warn at %",
		description = "Splashing mode warns in red while your chance to hit the target is above this. At 0 any "
			+ "chance to hit warns; a Magic attack bonus of -64 or lower is a 0% chance against anything.",
		section = targetSection,
		position = 10
	)
	default int splashMaxChance()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "autoSplash",
		name = "Auto-detect splashing",
		description = "Switch Splashing mode on by itself while your worn Magic attack bonus is at or under the "
			+ "threshold below, and back off above it. Switching the mode by hand wins until your gear next "
			+ "crosses the threshold.",
		section = targetSection,
		position = 30
	)
	default boolean autoSplash()
	{
		return true;
	}

	@Range(min = -200, max = 200)
	@ConfigItem(
		keyName = "autoSplashThreshold",
		name = "Splash at bonus",
		description = "Splashing mode switches on at this Magic attack bonus or lower (-40 by default).",
		section = targetSection,
		position = 31
	)
	default int autoSplashThreshold()
	{
		return -40;
	}

	@ConfigItem(
		keyName = "splashAlertBroke",
		name = "Splash: setup broke",
		description = "Splashing mode: notify when you change your gear (anything worn), your spell, or the "
			+ "NPC you are casting at while splashing.",
		section = targetSection,
		position = 11
	)
	default Notification splashAlertBroke()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "goalAlert",
		name = "Goal reached",
		description = "Notify when a goal on the Goals page is reached (a level, experience, item, kill count, "
			+ "daily or weekly goal). Obeys the sidebar mute.",
		section = displaySection,
		position = 60
	)
	default Notification goalAlert()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "castsLeft",
		name = "Show casts left",
		description = "Show an overlay with the spell, the runes it costs and how many casts you have left, from "
			+ "the runes in your inventory and rune pouch, counting combination runes and a worn staff or "
			+ "charged tome of fire or water that stands in for a rune. By default it shows while you autocast "
			+ "with a staff, or for a few seconds after you cast a spell by hand. Information only.",
		section = targetSection,
		position = 12
	)
	default boolean castsLeft()
	{
		return true;
	}

	@ConfigItem(
		keyName = "castsAlways",
		name = "Always show",
		description = "Keep the overlay up all the time instead of only while you cast, for the spell you pick "
			+ "below, or the last spell you cast if none is picked. Replaces a separate casts plugin.",
		section = targetSection,
		position = 13
	)
	default boolean castsAlways()
	{
		return false;
	}

	@ConfigItem(
		keyName = "castsSpell",
		name = "Always show: spell",
		description = "The spell to count when Always show is on and you are not casting, by name, e.g. Fire "
			+ "Surge or Ice Barrage. Leave empty for the last spell you cast.",
		section = targetSection,
		position = 14
	)
	default String castsSpell()
	{
		return "";
	}

	@Range(min = 0, max = 10000)
	@ConfigItem(
		keyName = "castsOnlyBelow",
		name = "Show only below",
		description = "Show the overlay only when you have fewer than this many casts left. 0 shows it always. "
			+ "Unlimited casts never show while this is set. The low-casts alert is not affected.",
		section = targetSection,
		position = 15
	)
	default int castsOnlyBelow()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "castsMerge",
		name = "Merge into splash card",
		description = "Show the casts line on the combat card while Splashing mode is on. Off keeps that card to the "
				+ "Magic bonus and hit chance; the casts are never shown on a second card.",
		section = targetSection,
		position = 16
	)
	default boolean castsMerge()
	{
		return true;
	}

	@Range(min = 0, max = 1000)
	@ConfigItem(
		keyName = "castsWarnAt",
		name = "Casts: warn at",
		description = "Turn the casts red and send the alert below when you have this many casts or fewer left. "
			+ "0 turns the warning off.",
		section = targetSection,
		position = 17
	)
	default int castsWarnAt()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "castsAlertLow",
		name = "Casts: running low",
		description = "Notify when your casts drop to the warn level. It fires once, and again after you restock "
			+ "above it. Follows the sidebar mute and Mute alert sounds.",
		section = targetSection,
		position = 18
	)
	default Notification castsAlertLow()
	{
		return Notification.ON;
	}

	@Range(min = 0, max = 300)
	@ConfigItem(
		keyName = "splashLingerSeconds",
		name = "Card stays (s)",
		description = "How many seconds the splashing card stays up, dimmed with 'Stopped Xs ago', after you stop "
			+ "casting. 0 hides it at once.",
		section = targetSection,
		position = 19
	)
	default int splashLingerSeconds()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "splashLingerForever",
		name = "Keep until new activity",
		description = "Keep the dimmed splashing card up after you stop until you start casting again, "
			+ "instead of for the seconds above.",
		section = targetSection,
		position = 20
	)
	default boolean splashLingerForever()
	{
		return false;
	}

	@ConfigItem(
		keyName = "splashAlertStopSoon",
		name = "Splash: stops soon",
		description = "Notify a little before splashing stops by itself: the OSRS wiki says the player stops "
			+ "attacking after 20 minutes without input. Any mouse or keyboard input restarts the count. "
			+ "Information only: nothing is clicked for you.",
		section = targetSection,
		position = 21
	)
	default Notification splashAlertStopSoon()
	{
		return Notification.ON;
	}

	@Range(min = 5, max = 600)
	@ConfigItem(
		keyName = "splashWarnSeconds",
		name = "Splash: warn (s)",
		description = "How long before splashing stops by itself the alert above fires.",
		section = targetSection,
		position = 22
	)
	default int splashWarnSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "splashAlertStopped",
		name = "Splash: stopped",
		description = "Notify when the 20 minutes without input have passed and splashing has stopped.",
		section = targetSection,
		position = 23
	)
	default Notification splashAlertStopped()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "gemstoneCrab",
		name = "Gemstone Crab mode",
		description = "At the Gemstone Crab: show its health and how long until it burrows (its health bar is a "
			+ "timer of about ten minutes, not damage), then how long its shell stays. Information only.",
		section = targetSection,
		position = 24
	)
	default boolean gemstoneCrab()
	{
		return true;
	}

	@Range(min = 0, max = 120)
	@ConfigItem(
		keyName = "gemstoneCrabWarnSeconds",
		name = "Crab: warn at (s)",
		description = "The about-to-burrow alert and the cave arrow start this many seconds before the crab "
			+ "burrows. At 0 the arrow and the alert wait for the burrow itself.",
		section = targetSection,
		position = 25
	)
	default int gemstoneCrabWarnSeconds()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "gemstoneCrabArrow",
		name = "Crab: cave arrow",
		description = "Draw an arrow and an outline over the cave to crawl through, from the burrow warning until "
			+ "the shell is gone. Drawing only: nothing is clicked for you.",
		section = targetSection,
		position = 26
	)
	default boolean gemstoneCrabArrow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gemstoneCrabAlertSpawn",
		name = "Crab: spawned",
		description = "Notify when a Gemstone Crab appears, once per crab. Follows the sidebar mute and Mute "
			+ "alert sounds.",
		section = targetSection,
		position = 27
	)
	default Notification gemstoneCrabAlertSpawn()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "gemstoneCrabAlertBurrow",
		name = "Crab: about to burrow",
		description = "Notify once when the crab is about to burrow (the warn-before setting). Follows the "
			+ "sidebar mute and Mute alert sounds.",
		section = targetSection,
		position = 28
	)
	default Notification gemstoneCrabAlertBurrow()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "gemstoneCrabAlertGo",
		name = "Crab: go through the cave",
		description = "Notify once when the crab has burrowed and the cave is ready to crawl through. Follows "
			+ "the sidebar mute and Mute alert sounds.",
		section = targetSection,
		position = 29
	)
	default Notification gemstoneCrabAlertGo()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "skillingEnabled",
		name = "Skilling badge",
		description =
			"When you work a fishing spot or another skilling NPC, draw a badge over your head, where "
				+ "your own health bar sits, in the skill's colour: the skill's icon, your level, your "
				+ "boost, and a line with your progress to the next level.",
		section = skillingSection,
		position = 0
	)
	default boolean skillingEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingCatches",
		name = "Show what you can catch",
		description =
			"On a fishing spot, show every fish the method you clicked (Net, Bait, Lure, Cage, Harpoon...) "
				+ "can give, as icons in the badge's inventory line: the ones your boosted level can catch "
				+ "now at full strength, the rest dimmed with the level they need. The fish you last caught "
				+ "leads. Spots the plugin does not know show no icons.",
		section = skillingSection,
		position = 12
	)
	default boolean skillingCatches()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingBoostTimer",
		name = "Show boost timer",
		description =
			"While boosted, show the seconds until the boost drops by one. The game never reports where "
				+ "it is in its drain cycle, so the timer is an estimate (marked ~) from login until a drop "
				+ "is seen, and exact after that. Preserve is accounted for.",
		section = skillingSection,
		position = 22
	)
	default boolean skillingBoostTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingBoostSource",
		name = "Show boost source",
		description =
			"Name what the boost came from, when it can be known: the potion or food you just drank or "
				+ "ate, or the special attack of the tool you are wielding. Otherwise nothing is shown "
				+ "rather than a guess.",
		section = skillingSection,
		position = 23
	)
	default boolean skillingBoostSource()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingSpecEnergy",
		name = "Show spec energy",
		description =
			"When the wielded tool has a special attack that boosts the skill (dragon, infernal, crystal "
				+ "and 3rd age axes, pickaxes and harpoons), show the remaining special attack energy.",
		section = skillingSection,
		position = 24
	)
	default boolean skillingSpecEnergy()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingBoostNotify",
		name = "Notify on boost drop",
		description =
			"Send a RuneLite notification when the badge's boost drops by one or wears off. The badge "
				+ "itself always shows the cue.",
		section = skillingSection,
		position = 25
	)
	default Notification skillingBoostNotify()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "skillingInventoryBar",
		name = "Show inventory fill",
		description =
			"Draw an inventory line under the badge, \"Inv 26/28 free\", on a bar that fills from green "
				+ "to yellow to red as your inventory fills. Stackables (feathers, bait, coin pouches), tools "
				+ "and equipment hold their slot, so they are not free, but do not count as filling; red "
				+ "means no free slot is left.",
		section = skillingSection,
		position = 15
	)
	default boolean skillingInventoryBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingContainers",
		name = "Show containers",
		description =
			"Under the inventory line, a line for each container you carry that holds what you are gathering: "
				+ "\"Log basket 12/28 free\" while woodcutting, the fish barrel while fishing, the coal bag while "
				+ "mining, essence pouches while runecrafting. A basket, barrel or coal bag shows \"?\" until you "
				+ "Check it once after logging in; the plugin never guesses what is inside.",
		section = skillingSection,
		position = 17
	)
	default boolean skillingContainers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingContainersAsInventory",
		name = "Count containers as free",
		description =
			"Add a container's free space to the inventory line, \"Inv 27/28 free (+16 basket)\", and only "
				+ "notify Inventory full once the container is full too. The coal bag takes coal only, so it is "
				+ "never counted; a container not yet checked counts as no space.",
		section = skillingSection,
		position = 18
	)
	default boolean skillingContainersAsInventory()
	{
		return false;
	}

	@ConfigItem(
		keyName = "skillingIdleMode",
		name = "Once idle",
		description =
			"What the badge does when you stop: Off lets it go as before; Show for a while keeps it up for the "
				+ "seconds below with the header turned to \"Not fishing\"; Until next activity keeps it until "
				+ "you start something else or a fight starts. The level, XP and inventory rows stay.",
		section = skillingSection,
		position = 26
	)
	default IdleBadgeMode skillingIdleMode()
	{
		return IdleBadgeMode.TIMED;
	}

	@Range(min = 1, max = 600)
	@ConfigItem(
		keyName = "skillingIdleSeconds",
		name = "Idle badge for (s)",
		description = "How long the \"Not fishing\" badge stays up, with Show for a while.",
		section = skillingSection,
		position = 27
	)
	default int skillingIdleSeconds()
	{
		return 60;
	}

	@Alpha
	@ConfigItem(
		keyName = "skillingIdleColour",
		name = "Idle header colour",
		description = "The header's colour while the badge says \"Not fishing\".",
		section = pillarsSection,
		position = 12
	)
	default Color skillingIdleColour()
	{
		return new Color(199, 26, 26, 230);
	}

	@ConfigItem(
		keyName = "skillingFishing",
		name = "Fishing",
		description = "Show the badge while fishing.",
		section = skillingSection,
		position = 2
	)
	default boolean skillingFishing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingWoodcutting",
		name = "Woodcutting",
		description = "Show the badge while chopping trees.",
		section = skillingSection,
		position = 3
	)
	default boolean skillingWoodcutting()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingMining",
		name = "Mining",
		description = "Show the badge while mining rocks.",
		section = skillingSection,
		position = 4
	)
	default boolean skillingMining()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingThieving",
		name = "Thieving",
		description = "Show the badge while pickpocketing or stealing from stalls.",
		section = skillingSection,
		position = 5
	)
	default boolean skillingThieving()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingRunecraft",
		name = "Runecraft",
		description = "Show the badge while crafting runes at an altar.",
		section = skillingSection,
		position = 6
	)
	default boolean skillingRunecraft()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingHunter",
		name = "Hunter",
		description = "Show the badge while catching implings and butterflies.",
		section = skillingSection,
		position = 7
	)
	default boolean skillingHunter()
	{
		return true;
	}

	// The skills raised by their experience drops (XpSkilling). Position 8,
	// like Hunter: equal positions sort by name, so they sit alphabetically
	// among the skill switches without renumbering the rows below.

	@ConfigItem(
		keyName = "skillingHerblore",
		name = "Herblore",
		description = "Show the badge while cleaning herbs, making potions or crushing.",
		section = skillingSection,
		position = 30
	)
	default boolean skillingHerblore()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingCooking",
		name = "Cooking",
		description = "Show the badge while cooking, with the raw food left.",
		section = skillingSection,
		position = 31
	)
	default boolean skillingCooking()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingFletching",
		name = "Fletching",
		description = "Show the badge while fletching, with the logs, shafts or bows left.",
		section = skillingSection,
		position = 32
	)
	default boolean skillingFletching()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingCrafting",
		name = "Crafting",
		description = "Show the badge while crafting, with the gems, hides or glass left.",
		section = skillingSection,
		position = 33
	)
	default boolean skillingCrafting()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingSmithing",
		name = "Smithing",
		description = "Show the badge while smithing, with the bars left.",
		section = skillingSection,
		position = 34
	)
	default boolean skillingSmithing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingFiremaking",
		name = "Firemaking",
		description = "Show the badge while lighting logs, with the logs left.",
		section = skillingSection,
		position = 35
	)
	default boolean skillingFiremaking()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingPrayer",
		name = "Prayer",
		description = "Show the badge while burying or offering bones, with the bones left.",
		section = skillingSection,
		position = 36
	)
	default boolean skillingPrayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingMagic",
		name = "Magic",
		description = "Show the badge while casting skilling spells (alchemy, enchant, superheat, plank make), never combat spells.",
		section = skillingSection,
		position = 37
	)
	default boolean skillingMagic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingAgility",
		name = "Agility",
		description = "Show the badge on agility courses, with laps, lap time and marks of grace.",
		section = skillingSection,
		position = 38
	)
	default boolean skillingAgility()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingConstruction",
		name = "Construction",
		description = "Show the badge while building, with the planks left.",
		section = skillingSection,
		position = 39
	)
	default boolean skillingConstruction()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingSailing",
		name = "Sailing",
		description = "Show the badge while training Sailing.",
		section = skillingSection,
		position = 40
	)
	default boolean skillingSailing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingShowHeader",
		name = "Header row",
		description = "The top row: the skill's icon, your level, boost, timer and spec energy.",
		section = skillingSection,
		position = 8
	)
	default boolean skillingShowHeader()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingShowXp",
		name = "XP row",
		description = "The row with your progress to the next level. The inventory row and catch list have their "
			+ "own switches in Skilling: badge.",
		section = skillingSection,
		position = 9
	)
	default boolean skillingShowXp()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingXpDisplay",
		name = "XP shown as",
		description = "The XP row as a percentage, the XP left, or both.",
		section = skillingSection,
		position = 10
	)
	default XpDisplay skillingXpDisplay()
	{
		return XpDisplay.BOTH;
	}

	@ConfigItem(
		keyName = "skillingInventoryCount",
		name = "Inventory count",
		description = "Count free slots (\"Inv 26/28 free\") or used ones (\"Inv 2/28 used\").",
		section = skillingSection,
		position = 16
	)
	default InventoryCount skillingInventoryCount()
	{
		return InventoryCount.FREE;
	}

	@ConfigItem(
		keyName = "skillingSessionCount",
		name = "Session count",
		description = "A row with how many you caught, chopped or mined this session: one per experience drop. "
			+ "Resets on logout or with Reset session below.",
		section = skillingSection,
		position = 19
	)
	default boolean skillingSessionCount()
	{
		return false;
	}

	@ConfigItem(
		keyName = "skillingRates",
		name = "XP and actions per hour",
		description = "XP per hour and actions per hour for the skill, this session, from your first experience "
			+ "drop. Shown after a minute.",
		section = skillingSection,
		position = 20
	)
	default boolean skillingRates()
	{
		return false;
	}

	@Range(min = 10, max = 600)
	@ConfigItem(
		keyName = "pauseAfterIdleSeconds",
		name = "Idle pause (s)",
		description = "Pause after this many idle seconds (10 to 600). The session timer and the XP and action rates pause when no experience has come in for "
			+ "this long, and resume with the next gain. Paused time is left out of the rates.",
		section = skillingSection,
		position = 22
	)
	default int pauseAfterIdleSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "skillingSessionReset",
		name = "Reset session",
		description = "Tick to zero the session count and rates; it unticks itself.",
		section = skillingSection,
		position = 21
	)
	default boolean skillingSessionReset()
	{
		return false;
	}

	@ConfigItem(
		keyName = "skillingPosition",
		name = "Badge position",
		description = "Over your head; over the spot, tree or rock you are working (over your head when idle); or "
			+ "fixed on screen, where Alt-drag moves it like any other overlay.",
		section = pillarsSection,
		position = 9
	)
	default BadgePosition skillingPosition()
	{
		return BadgePosition.OVER_HEAD;
	}

	@Range(min = -200, max = 200)
	@ConfigItem(
		keyName = "skillingOffset",
		name = "Badge offset",
		description = "Pixels to raise the badge (negative lowers it), over your head or the spot.",
		section = pillarsSection,
		position = 10
	)
	default int skillingOffset()
	{
		return 0;
	}

	@Range(min = 50, max = 200)
	@ConfigItem(
		keyName = "skillingScale",
		name = "Badge size (%)",
		description = "Scale the whole badge, text and icons included.",
		section = pillarsSection,
		position = 11
	)
	default int skillingScale()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "skillingLockedFish",
		name = "Fish above your level",
		description = "Dim the fish you cannot catch yet and mark them with the level they need, or leave them out.",
		section = skillingSection,
		position = 13
	)
	default LockedFish skillingLockedFish()
	{
		return LockedFish.DIM;
	}

	@ConfigItem(
		keyName = "skillingHideInCombat",
		name = "Hide in combat (over head)",
		description = "Over your head only: hide the badge while you are being hit or hitting back. Fighting a target"
			+ " always replaces it there. Fixed or over the spot, a fight never hides it; it says 'in combat' instead.",
		section = skillingSection,
		position = 1
	)
	default boolean skillingHideInCombat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "skillingFishingColour",
		name = "Fishing colour",
		description = "The badge's colour for Fishing.",
		section = pillarsSection,
		position = 13
	)
	default Color skillingFishingColour()
	{
		return SkillColours.of(Skill.FISHING);
	}

	@ConfigItem(
		keyName = "skillingWoodcuttingColour",
		name = "Woodcutting colour",
		description = "The badge's colour for Woodcutting.",
		section = pillarsSection,
		position = 14
	)
	default Color skillingWoodcuttingColour()
	{
		return SkillColours.of(Skill.WOODCUTTING);
	}

	@ConfigItem(
		keyName = "skillingMiningColour",
		name = "Mining colour",
		description = "The badge's colour for Mining.",
		section = pillarsSection,
		position = 15
	)
	default Color skillingMiningColour()
	{
		return SkillColours.of(Skill.MINING);
	}

	@ConfigItem(
		keyName = "skillingThievingColour",
		name = "Thieving colour",
		description = "The badge's colour for Thieving.",
		section = pillarsSection,
		position = 16
	)
	default Color skillingThievingColour()
	{
		return SkillColours.of(Skill.THIEVING);
	}

	@ConfigItem(
		keyName = "skillingRunecraftColour",
		name = "Runecraft colour",
		description = "The badge's colour for Runecraft.",
		section = pillarsSection,
		position = 17
	)
	default Color skillingRunecraftColour()
	{
		return SkillColours.of(Skill.RUNECRAFT);
	}

	@ConfigItem(
		keyName = "skillingHunterColour",
		name = "Hunter colour",
		description = "The badge's colour for Hunter.",
		section = pillarsSection,
		position = 18
	)
	default Color skillingHunterColour()
	{
		return SkillColours.of(Skill.HUNTER);
	}

	@ConfigItem(
		keyName = "skillingGearWarning",
		name = "Warn if missing gear",
		description = "When you click a fishing spot without the tool, bait or level the option needs,"
			+ " say what is missing on the badge straight away. The click itself is never changed.",
		section = skillingSection,
		position = 14
	)
	default boolean skillingGearWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingTimeToLevel",
		name = "Time to level",
		description = "With XP and actions per hour on, add how long the next level takes at this session's "
			+ "rate, \"lvl 18 in 6m\", to the session row.",
		section = skillingSection,
		position = 11
	)
	default boolean skillingTimeToLevel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skillingBadgeLayout",
		name = "Badge layout",
		description = "Full: every session stat on, wrapping to two lines when the row would be wider than the "
			+ "badge. Compact: one short line, \"24 caught | 10.6k xp/h\".",
		section = pillarsSection,
		position = 8
	)
	default BadgeLayout skillingBadgeLayout()
	{
		return BadgeLayout.FULL;
	}

	@ConfigItem(
		keyName = "pillarMode",
		name = "Show on side pillars",
		description =
			"Draw vitals on the bars beside the side panel, where RuneLite's Status Bars draws hitpoints and "
				+ "prayer. Combat: your target's health. Skilling: XP to the next level and inventory fill. "
				+ "If Status Bars is drawing, these sit beside its bars rather than over them.",
		section = pillarsSection,
		position = 19
	)
	default PillarMode pillarMode()
	{
		return PillarMode.OFF;
	}

	@ConfigItem(
		keyName = "pillarCombatSide",
		name = "Target health on",
		description = "Which pillar carries your target's health.",
		section = pillarsSection,
		position = 20
	)
	default PillarSide pillarCombatSide()
	{
		return PillarSide.LEFT;
	}

	@ConfigItem(
		keyName = "pillarXpSide",
		name = "Skilling XP on",
		description = "Which pillar carries XP to the next level; inventory fill takes the other.",
		section = pillarsSection,
		position = 21
	)
	default PillarSide pillarXpSide()
	{
		return PillarSide.LEFT;
	}

	@ConfigItem(
		keyName = "pillarLabels",
		name = "Pillar labels",
		description = "What each pillar says at its top, as Status Bars does: an icon for what it is (the skill, "
			+ "a backpack, a heart) and the number under it (XP percent or XP left, free or used slots, "
			+ "your target's hitpoints), the icon alone, or nothing. Hover a pillar for the whole story.",
		section = pillarsSection,
		position = 22
	)
	default PillarLabels pillarLabels()
	{
		return PillarLabels.ICON_AND_NUMBER;
	}

	@ConfigItem(
		keyName = "pillarStyle",
		name = "Pillar style",
		description = "Status Bars: a dark frame with the fill inside, a shine down its left edge, a mark every "
			+ "25% on XP and one per slot on the inventory, to match RuneLite's own bars beside them. "
			+ "Flat: one colour on the bar background.",
		section = pillarsSection,
		position = 23
	)
	default PillarStyle pillarStyle()
	{
		return PillarStyle.STATUS_BARS;
	}

	@ConfigItem(
		keyName = "pillarContainers",
		name = "Containers on the pillar",
		description = "With a log basket or fish barrel carried open and its contents known, the inventory "
			+ "pillar shows both as one bar: the container fills first, at the bottom in its own colour, then "
			+ "the inventory on top, and the number is the free space of the two together. Off, or with the "
			+ "container closed or not yet checked, the pillar shows the inventory alone.",
		section = pillarsSection,
		position = 24
	)
	default boolean pillarContainers()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "pillarContainerColour",
		name = "Container colour",
		description = "The open log basket's or fish barrel's part of the inventory pillar.",
		section = pillarsSection,
		position = 25
	)
	default Color pillarContainerColour()
	{
		return new Color(142, 92, 222, 230);
	}

	@ConfigItem(
		keyName = "fishingSpotPath",
		name = "Next spot path",
		description =
			"Find the nearest fishing spot of the same kind (net/bait, lure/bait, cage/harpoon...) that you can "
				+ "catch something at, and mark it: its tile outlined, a line from you to it, and an arrow at "
				+ "the screen's edge while it is off screen. The mark stays until you start fishing again. "
				+ "'On spot move' does it when your spot moves; either way, right-clicking the badge offers "
				+ "'Path to next spot'. With no spot of that kind nearby, the nearest known location is used.",
		section = skillingSection,
		position = 28
	)
	default FishingSpotPath fishingSpotPath()
	{
		return FishingSpotPath.OFF;
	}

	@ConfigItem(
		keyName = "fishingSpotShortestPath",
		name = "Next spot: Shortest Path",
		description =
			"Also send the next spot to the Shortest Path plugin, when it is installed and on, for a walking "
				+ "path. Shortest Path ends its own path once you are within its 'Finish distance' (5 tiles by "
				+ "default), which is why the plugin's own mark is drawn as well.",
		section = skillingSection,
		position = 29
	)
	default boolean fishingSpotShortestPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alertInventoryFull",
		name = "Inventory full",
		description = "Notify when the last free slot fills while you are fishing, woodcutting, mining, hunting "
			+ "or thieving.",
		section = alertsSection,
		position = 0
	)
	default Notification alertInventoryFull()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertGatheringStopped",
		name = "Stopped gathering",
		description = "Notify when you have had no gathering animation or experience for the delay below. "
			+ "Not sent when you walk away, bank, drop or deposit, log out, or when the inventory filled "
			+ "or the spot moved (those have their own alerts).",
		section = alertsSection,
		position = 1
	)
	default Notification alertGatheringStopped()
	{
		return Notification.ON;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(
		keyName = "alertStopSeconds",
		name = "Stopped after (s)",
		description = "How long with no animation or experience counts as stopped, for gathering, thieving and "
			+ "make-X.",
		section = alertsSection,
		position = 2
	)
	default int alertStopSeconds()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "alertSpotMoved",
		name = "Fishing spot moved",
		description = "Notify when the fishing spot you are working moves away.",
		section = alertsSection,
		position = 3
	)
	default Notification alertSpotMoved()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertThievingStopped",
		name = "Stopped thieving",
		description = "Notify when pickpocketing or stealing from stalls has stopped for the delay above. A "
			+ "stun does not count as stopping.",
		section = alertsSection,
		position = 4
	)
	default Notification alertThievingStopped()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertStunned",
		name = "Pickpocket stun",
		description = "Notify when a failed pickpocket stuns you.",
		section = alertsSection,
		position = 5
	)
	default Notification alertStunned()
	{
		// Off: at most levels it fires every few pickpockets.
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "alertCoinPouch",
		name = "Coin pouches at cap",
		description = "Notify when the coin pouches you hold reach the number below, so you open them before "
			+ "pickpocketing stops paying out.",
		section = alertsSection,
		position = 6
	)
	default Notification alertCoinPouch()
	{
		return Notification.ON;
	}

	@Range(min = 1, max = 140)
	@ConfigItem(
		keyName = "alertCoinPouchCap",
		name = "Coin pouch cap",
		description = "Pouches held before the alert. 28 is the cap without Ardougne Diary rewards; raise this "
			+ "to match yours if the diary has lifted it.",
		section = alertsSection,
		position = 7
	)
	default int alertCoinPouchCap()
	{
		return 28;
	}

	@ConfigItem(
		keyName = "alertNecklaceBroke",
		name = "Dodgy necklace broke",
		description = "Notify when your dodgy necklace uses its last charge. RuneLite's Item Charges plugin can "
			+ "send the same alert; turn one of them off if you get two.",
		section = alertsSection,
		position = 8
	)
	default Notification alertNecklaceBroke()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertMakeFinished",
		name = "Make-X finished",
		description = "Notify when cooking, fletching, crafting or smithing has stopped for the delay above, "
			+ "usually because you ran out of materials or levelled up.",
		section = alertsSection,
		position = 9
	)
	default Notification alertMakeFinished()
	{
		// Off: RuneLite's Idle Notifier already covers this for many users.
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "alertLowRunEnergy",
		name = "Low run energy",
		description = "Notify when run energy drops below the threshold.",
		section = alertsSection,
		position = 10
	)
	default Notification alertLowRunEnergy()
	{
		return Notification.OFF;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "alertRunEnergyThreshold",
		name = "Low run energy (%)",
		description = "Run energy below which the low run energy alert fires.",
		section = alertsSection,
		position = 11
	)
	default int alertRunEnergyThreshold()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "alertMethodStopped",
		name = "Stopped pumping",
		description = "Notify when operating the Blast Furnace pump has had no animation or Strength experience for "
			+ "the stopped delay above. Not sent when you walk away or a fight starts.",
		section = alertsSection,
		position = 12
	)
	default Notification alertMethodStopped()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertReclickSoon",
		name = "Idle logout soon",
		description = "While pumping: notify a little before the game logs you out for no activity. Any input counts "
			+ "(a click anywhere, moving the camera, typing), not only clicking the pump. The badge shows 'Active' "
			+ "while you are, and counts down 'AFK logout in' once you've been idle 5 seconds.",
		section = alertsSection,
		position = 13
	)
	default Notification alertReclickSoon()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertAutoStopSoon",
		name = "Pump stops soon",
		description = "Notify a little before the Blast Furnace pump stops by itself, 100 minutes after you clicked "
			+ "it; click the pump again to restart it. The badge counts it down as 'Pump stops in'.",
		section = alertsSection,
		position = 14
	)
	default Notification alertAutoStopSoon()
	{
		return Notification.ON;
	}

	@Range(min = 5, max = 300)
	@ConfigItem(
		keyName = "alertMethodWarnSeconds",
		name = "Warn before (s)",
		description = "How long before the idle logout or the pump's own stop the two alerts above fire.",
		section = alertsSection,
		position = 15
	)
	default int alertMethodWarnSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "alertBirdNest",
		name = "Bird nest",
		description = "Notify when a bird's nest falls out of the tree you are chopping. A clue nest goes to the "
			+ "Clue drop alert instead, or comes here when that one is off.",
		section = alertsSection,
		position = 16
	)
	default Notification alertBirdNest()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertClueDrop",
		name = "Clue drop",
		description = "Notify, with the tier, when skilling gives you a clue: a clue nest, a clue geode while "
			+ "mining, a clue bottle while fishing, or a scroll box.",
		section = alertsSection,
		position = 17
	)
	default Notification alertClueDrop()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertForestryEvent",
		name = "Forestry event",
		description = "Notify when a Forestry event (Rising Roots, Struggling Sapling, Flowering Bush, Leprechaun, "
			+ "Poachers, Pheasant Control, Bee Hive, Enchantment Ritual, Friendly Ent) starts near you while you "
			+ "are chopping. RuneLite's Woodcutting plugin has its own per-event alerts; turn one off if you get two.",
		section = alertsSection,
		position = 18
	)
	default Notification alertForestryEvent()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertPet",
		name = "Pet",
		description = "Notify when you get a pet, or would have got one you already have.",
		section = alertsSection,
		position = 19
	)
	default Notification alertPet()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertLevelUp",
		name = "Level up",
		description = "Notify when you gain a level, in the skills chosen below.",
		section = alertsSection,
		position = 20
	)
	default Notification alertLevelUp()
	{
		// Off: the game already shows a level-up dialogue.
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "alertLevelUpSkills",
		name = "Level up skills",
		description = "All skills, or only the non-combat ones (everything but Attack, Strength, Defence, "
			+ "Hitpoints, Ranged, Prayer and Magic).",
		section = alertsSection,
		position = 21
	)
	default LevelUpSkills alertLevelUpSkills()
	{
		return LevelUpSkills.ALL;
	}

	@ConfigItem(
		keyName = "alertRandomEvent",
		name = "Random event",
		description = "Notify when a random event is here for you: only when it is talking to you, never "
			+ "someone else's. RuneLite's Random Events plugin can send the same alert.",
		section = alertsSection,
		position = 22
	)
	default Notification alertRandomEvent()
	{
		return Notification.OFF;
	}

	// The pump checks share position 22 with Random event: RuneLite orders a
	// tie by name, and these names sort after it and in this order.
	@ConfigItem(
		keyName = "alertCokeLow",
		name = "Stove: coke runs low",
		description = "While pumping: notify a little before the stove's coke runs out ('Warn before' above). "
			+ "The badge counts it down as 'Coke runs out in' once it has timed the stove burning down a step, "
			+ "from how the stove looks.",
		section = alertsSection,
		position = 22
	)
	default Notification alertCokeLow()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertNoStoking",
		name = "Stove: no one stoking",
		description = "While pumping: notify when the pump gives no Strength experience for a few seconds, which "
			+ "means the stove is out of coke and nobody is refuelling it. The badge says 'Stove: no one stoking'.",
		section = alertsSection,
		position = 22
	)
	default Notification alertNoStoking()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "alertWrongWorld",
		name = "Wrong world",
		description = "Notify once when you start pumping on a world that is not in 'Pumping worlds'. The badge "
			+ "says 'World 420: not a pumping world' while you pump there.",
		section = alertsSection,
		position = 22
	)
	default Notification alertWrongWorld()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "pumpWorlds",
		name = "Wrong world: pumping worlds",
		description = "The worlds where people keep the Blast Furnace stoked for pumping, separated by commas. "
			+ "World 302 is the usual one, 303 the other (OSRS Wiki). Empty: no world check.",
		section = alertsSection,
		position = 22
	)
	default String pumpWorlds()
	{
		return PumpChecks.DEFAULT_WORLDS;
	}

	@ConfigItem(
		keyName = "alertSkillingEvent",
		name = "Mining and fishing events",
		description = "Notify on a gem found while mining, or a clue bottle or big catch while fishing. "
			+ "Off by default; the voice line is not bundled yet, so it falls back to RuneLite's sound.",
		section = alertsSection,
		position = 23
	)
	default Notification alertSkillingEvent()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "autoResetSession",
		name = "Auto-reset on new activity",
		description = "Reset the session when the activity changes (yew logs to maple logs, or another skill or "
			+ "resource). The previous session's summary stays on the badge for two minutes.",
		section = skillingSection,
		position = 41
	)
	default boolean autoResetSession()
	{
		return false;
	}

	@ConfigItem(
		keyName = "forestryHighlights",
		name = "Highlight Forestry events",
		description = "Outline the parts of a Forestry event in the game view (glowing roots, the sapling, the "
			+ "flowering bush, the leprechaun and the rest), as RuneLite's Woodcutting plugin does. Not covered "
			+ "yet: mulch ingredients, fox trap, pheasant nest, ritual circle and the leprechaun's rainbow, so keep "
			+ "RuneLite's Woodcutting plugin on for those for now.",
		section = skillingSection,
		position = 42
	)
	default boolean forestryHighlights()
	{
		return true;
	}

	@ConfigItem(
		keyName = "forestryHighlightColour",
		name = "Highlight colour",
		description = "The outline colour for Forestry event highlights.",
		section = skillingSection,
		position = 43
	)
	default Color forestryHighlightColour()
	{
		return new Color(0x00FF7F);
	}

	@ConfigItem(
		keyName = "highlightRisingRoots",
		name = "Rising Roots",
		description = "Highlight the glowing roots.",
		section = skillingSection,
		position = 44
	)
	default boolean highlightRisingRoots()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightStrugglingSapling",
		name = "Struggling Sapling",
		description = "Highlight the sapling.",
		section = skillingSection,
		position = 45
	)
	default boolean highlightStrugglingSapling()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightFloweringBush",
		name = "Flowering Bush",
		description = "Highlight the flowering bush.",
		section = skillingSection,
		position = 46
	)
	default boolean highlightFloweringBush()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightLeprechaun",
		name = "Leprechaun",
		description = "Highlight the Woodcutting leprechaun.",
		section = skillingSection,
		position = 47
	)
	default boolean highlightLeprechaun()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightPoachers",
		name = "Poachers",
		description = "Highlight the poachers.",
		section = skillingSection,
		position = 48
	)
	default boolean highlightPoachers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightPheasantControl",
		name = "Pheasant Control",
		description = "Highlight the pheasant control event.",
		section = skillingSection,
		position = 49
	)
	default boolean highlightPheasantControl()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightBeeHive",
		name = "Bee Hive",
		description = "Highlight the bee hive.",
		section = skillingSection,
		position = 50
	)
	default boolean highlightBeeHive()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightEnchantmentRitual",
		name = "Enchantment Ritual",
		description = "Highlight the ritual.",
		section = skillingSection,
		position = 51
	)
	default boolean highlightEnchantmentRitual()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightFriendlyEnt",
		name = "Friendly Ent",
		description = "Highlight the friendly ent.",
		section = skillingSection,
		position = 52
	)
	default boolean highlightFriendlyEnt()
	{
		return true;
	}

	String SOUND_HELP = " RuneLite: your RuneLite notification sound, as before. Soft chime, Music box, Retro blip: "
		+ "bundled sounds. Voice line: says what happened. My sounds: your own WAV in "
		+ ".runelite/plugin-data/combat-vitals/sounds/";
	String VOLUME_HELP = "Volume of this alert's own sounds. RuneLite's sound keeps RuneLite's volume.";
	String REPEAT_HELP = " again every 'Repeat every (s)', softly, until you click or press a key in the client, "
		+ "start again, log out, or 'Repeat for (min)' runs out. Only listens; never sends any input.";
	String SHARED_HELP = " Used by every alert while 'Same for every alert' is on; each alert's own is kept for when "
		+ "it is off.";

	@ConfigItem(
		keyName = "alertSoundShared",
		name = "Same for every alert",
		description = "One sound, volume and repeat for every alert, set just below. Off: each alert has its own, "
			+ "further down. Turning an alert on or off still happens per alert.",
		section = alertsSection,
		position = 23
	)
	default boolean alertSoundShared()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alertSoundAll",
		name = "Sound for all",
		description = "What every alert sounds like." + SOUND_HELP + "<alert>.wav." + SHARED_HELP,
		section = alertsSection,
		position = 24
	)
	default AlertSound alertSoundAll()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "alertVolumeAll",
		name = "Volume for all",
		description = VOLUME_HELP + SHARED_HELP,
		section = alertsSection,
		position = 25
	)
	default int alertVolumeAll()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "alertRepeatAll",
		name = "Repeat every alert",
		description = "Sound every alert" + REPEAT_HELP + SHARED_HELP,
		section = alertsSection,
		position = 26
	)
	default boolean alertRepeatAll()
	{
		return false;
	}

	@Range(min = 5, max = 120)
	@ConfigItem(
		keyName = "repeatEverySeconds",
		name = "Repeat every (s)",
		description = "For alerts with Repeat on: seconds between repeats.",
		section = alertsSection,
		position = 27
	)
	default int repeatEverySeconds()
	{
		return 15;
	}

	@Range(min = 1, max = 60)
	@ConfigItem(
		keyName = "repeatMaxMinutes",
		name = "Repeat for (min)",
		description = "For alerts with Repeat on: stop repeating after this many minutes even if you are not back.",
		section = alertsSection,
		position = 28
	)
	default int repeatMaxMinutes()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "soundInventoryFull",
		name = "Full inv sound",
		description = "What the inventory full alert sounds like." + SOUND_HELP + "inventory-full.wav",
		section = alertsSection,
		position = 29
	)
	default AlertSound soundInventoryFull()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeInventoryFull",
		name = "Full inv volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 30
	)
	default int volumeInventoryFull()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatInventoryFull",
		name = "Repeat full inv",
		description = "Sound the inventory full alert" + REPEAT_HELP,
		section = alertsSection,
		position = 31
	)
	default boolean repeatInventoryFull()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundGatheringStopped",
		name = "Gathering sound",
		description = "What the stopped gathering alert sounds like." + SOUND_HELP + "gathering-stopped.wav",
		section = alertsSection,
		position = 32
	)
	default AlertSound soundGatheringStopped()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeGatheringStopped",
		name = "Gathering volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 33
	)
	default int volumeGatheringStopped()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatGatheringStopped",
		name = "Repeat gathering",
		description = "Sound the stopped gathering alert" + REPEAT_HELP,
		section = alertsSection,
		position = 34
	)
	default boolean repeatGatheringStopped()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundSpotMoved",
		name = "Spot moved sound",
		description = "What the fishing spot moved alert sounds like." + SOUND_HELP + "spot-moved.wav",
		section = alertsSection,
		position = 35
	)
	default AlertSound soundSpotMoved()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeSpotMoved",
		name = "Spot moved volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 36
	)
	default int volumeSpotMoved()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatSpotMoved",
		name = "Repeat spot moved",
		description = "Sound the fishing spot moved alert" + REPEAT_HELP,
		section = alertsSection,
		position = 37
	)
	default boolean repeatSpotMoved()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundThievingStopped",
		name = "Thieving sound",
		description = "What the stopped thieving alert sounds like." + SOUND_HELP + "thieving-stopped.wav",
		section = alertsSection,
		position = 38
	)
	default AlertSound soundThievingStopped()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeThievingStopped",
		name = "Thieving volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 39
	)
	default int volumeThievingStopped()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatThievingStopped",
		name = "Repeat thieving",
		description = "Sound the stopped thieving alert" + REPEAT_HELP,
		section = alertsSection,
		position = 40
	)
	default boolean repeatThievingStopped()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundStunned",
		name = "Stunned sound",
		description = "What the stunned alert sounds like." + SOUND_HELP + "stunned.wav",
		section = alertsSection,
		position = 41
	)
	default AlertSound soundStunned()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeStunned",
		name = "Stunned volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 42
	)
	default int volumeStunned()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatStunned",
		name = "Repeat stunned",
		description = "Sound the stunned alert" + REPEAT_HELP,
		section = alertsSection,
		position = 43
	)
	default boolean repeatStunned()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundCoinPouch",
		name = "Coin pouch sound",
		description = "What the coin pouch alert sounds like." + SOUND_HELP + "coin-pouch-cap.wav",
		section = alertsSection,
		position = 44
	)
	default AlertSound soundCoinPouch()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeCoinPouch",
		name = "Coin pouch volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 45
	)
	default int volumeCoinPouch()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatCoinPouch",
		name = "Repeat coin pouch",
		description = "Sound the coin pouch alert" + REPEAT_HELP,
		section = alertsSection,
		position = 46
	)
	default boolean repeatCoinPouch()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundNecklaceBroke",
		name = "Necklace sound",
		description = "What the dodgy necklace alert sounds like." + SOUND_HELP + "necklace-broke.wav",
		section = alertsSection,
		position = 47
	)
	default AlertSound soundNecklaceBroke()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeNecklaceBroke",
		name = "Necklace volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 48
	)
	default int volumeNecklaceBroke()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatNecklaceBroke",
		name = "Repeat necklace",
		description = "Sound the dodgy necklace alert" + REPEAT_HELP,
		section = alertsSection,
		position = 49
	)
	default boolean repeatNecklaceBroke()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundMakeFinished",
		name = "Make-X sound",
		description = "What the make-X finished alert sounds like." + SOUND_HELP + "production-finished.wav",
		section = alertsSection,
		position = 50
	)
	default AlertSound soundMakeFinished()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeMakeFinished",
		name = "Make-X volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 51
	)
	default int volumeMakeFinished()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatMakeFinished",
		name = "Repeat make-X",
		description = "Sound the make-X finished alert" + REPEAT_HELP,
		section = alertsSection,
		position = 52
	)
	default boolean repeatMakeFinished()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundLowRunEnergy",
		name = "Low energy sound",
		description = "What the low run energy alert sounds like." + SOUND_HELP + "low-run-energy.wav",
		section = alertsSection,
		position = 53
	)
	default AlertSound soundLowRunEnergy()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeLowRunEnergy",
		name = "Low energy volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 54
	)
	default int volumeLowRunEnergy()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatLowRunEnergy",
		name = "Repeat low energy",
		description = "Sound the low run energy alert" + REPEAT_HELP,
		section = alertsSection,
		position = 55
	)
	default boolean repeatLowRunEnergy()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundMethodStopped",
		name = "Pumping sound",
		description = "What the stopped pumping alert sounds like." + SOUND_HELP + "method-stopped.wav",
		section = alertsSection,
		position = 56
	)
	default AlertSound soundMethodStopped()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeMethodStopped",
		name = "Pumping volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 57
	)
	default int volumeMethodStopped()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatMethodStopped",
		name = "Repeat pumping",
		description = "Sound the stopped pumping alert" + REPEAT_HELP,
		section = alertsSection,
		position = 58
	)
	default boolean repeatMethodStopped()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundReclickSoon",
		name = "Idle logout sound",
		description = "What the idle logout soon alert sounds like." + SOUND_HELP + "reclick-soon.wav",
		section = alertsSection,
		position = 59
	)
	default AlertSound soundReclickSoon()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeReclickSoon",
		name = "Idle logout volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 60
	)
	default int volumeReclickSoon()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatReclickSoon",
		name = "Repeat idle logout",
		description = "Sound the idle logout soon alert" + REPEAT_HELP,
		section = alertsSection,
		position = 61
	)
	default boolean repeatReclickSoon()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundAutoStopSoon",
		name = "Pump stops sound",
		description = "What the pump stops soon alert sounds like." + SOUND_HELP + "auto-stop-soon.wav",
		section = alertsSection,
		position = 62
	)
	default AlertSound soundAutoStopSoon()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeAutoStopSoon",
		name = "Pump stops volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 63
	)
	default int volumeAutoStopSoon()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatAutoStopSoon",
		name = "Repeat pump stops",
		description = "Sound the pump stops soon alert" + REPEAT_HELP,
		section = alertsSection,
		position = 64
	)
	default boolean repeatAutoStopSoon()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundBirdNest",
		name = "Bird nest sound",
		description = "What the bird nest alert sounds like." + SOUND_HELP + "bird-nest.wav",
		section = alertsSection,
		position = 65
	)
	default AlertSound soundBirdNest()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeBirdNest",
		name = "Bird nest volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 66
	)
	default int volumeBirdNest()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatBirdNest",
		name = "Repeat bird nest",
		description = "Sound the bird nest alert" + REPEAT_HELP,
		section = alertsSection,
		position = 67
	)
	default boolean repeatBirdNest()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundClueDrop",
		name = "Clue drop sound",
		description = "What the clue drop alert sounds like." + SOUND_HELP + "clue-drop.wav",
		section = alertsSection,
		position = 68
	)
	default AlertSound soundClueDrop()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeClueDrop",
		name = "Clue drop volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 69
	)
	default int volumeClueDrop()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatClueDrop",
		name = "Repeat clue drop",
		description = "Sound the clue drop alert" + REPEAT_HELP,
		section = alertsSection,
		position = 70
	)
	default boolean repeatClueDrop()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundForestryEvent",
		name = "Forestry sound",
		description = "What the Forestry event alert sounds like." + SOUND_HELP + "forestry-event.wav",
		section = alertsSection,
		position = 71
	)
	default AlertSound soundForestryEvent()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeForestryEvent",
		name = "Forestry volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 72
	)
	default int volumeForestryEvent()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatForestryEvent",
		name = "Repeat Forestry",
		description = "Sound the Forestry event alert" + REPEAT_HELP,
		section = alertsSection,
		position = 73
	)
	default boolean repeatForestryEvent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundPet",
		name = "Pet sound",
		description = "What the pet alert sounds like." + SOUND_HELP + "pet.wav",
		section = alertsSection,
		position = 74
	)
	default AlertSound soundPet()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumePet",
		name = "Pet volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 75
	)
	default int volumePet()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatPet",
		name = "Repeat pet",
		description = "Sound the pet alert" + REPEAT_HELP,
		section = alertsSection,
		position = 76
	)
	default boolean repeatPet()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundLevelUp",
		name = "Level up sound",
		description = "What the level up alert sounds like." + SOUND_HELP + "level-up.wav",
		section = alertsSection,
		position = 77
	)
	default AlertSound soundLevelUp()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeLevelUp",
		name = "Level up volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 78
	)
	default int volumeLevelUp()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatLevelUp",
		name = "Repeat level up",
		description = "Sound the level up alert" + REPEAT_HELP,
		section = alertsSection,
		position = 79
	)
	default boolean repeatLevelUp()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundRandomEvent",
		name = "Random sound",
		description = "What the random event alert sounds like." + SOUND_HELP + "random-event.wav",
		section = alertsSection,
		position = 80
	)
	default AlertSound soundRandomEvent()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeRandomEvent",
		name = "Random volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 81
	)
	default int volumeRandomEvent()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatRandomEvent",
		name = "Repeat random",
		description = "Sound the random event alert" + REPEAT_HELP,
		section = alertsSection,
		position = 82
	)
	default boolean repeatRandomEvent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundCokeLow",
		name = "Coke low sound",
		description = "What the coke runs low alert sounds like." + SOUND_HELP + "coke-low.wav",
		section = alertsSection,
		position = 83
	)
	default AlertSound soundCokeLow()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeCokeLow",
		name = "Coke low volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 84
	)
	default int volumeCokeLow()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatCokeLow",
		name = "Repeat coke low",
		description = "Sound the coke runs low alert" + REPEAT_HELP,
		section = alertsSection,
		position = 85
	)
	default boolean repeatCokeLow()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundNoStoking",
		name = "No stoking sound",
		description = "What the no one stoking alert sounds like." + SOUND_HELP + "no-stoking.wav",
		section = alertsSection,
		position = 86
	)
	default AlertSound soundNoStoking()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeNoStoking",
		name = "No stoking volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 87
	)
	default int volumeNoStoking()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatNoStoking",
		name = "Repeat no stoking",
		description = "Sound the no one stoking alert" + REPEAT_HELP,
		section = alertsSection,
		position = 88
	)
	default boolean repeatNoStoking()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundWrongWorld",
		name = "Wrong world sound",
		description = "What the wrong world alert sounds like." + SOUND_HELP + "wrong-world.wav",
		section = alertsSection,
		position = 89
	)
	default AlertSound soundWrongWorld()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeWrongWorld",
		name = "Wrong world volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 90
	)
	default int volumeWrongWorld()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatWrongWorld",
		name = "Repeat wrong world",
		description = "Sound the wrong world alert" + REPEAT_HELP,
		section = alertsSection,
		position = 91
	)
	default boolean repeatWrongWorld()
	{
		return false;
	}

	@ConfigItem(
		keyName = "soundSkillingEvent",
		name = "Gem/bottle sound",
		description = "What the mining and fishing events alert sounds like." + SOUND_HELP + "skilling-event.wav",
		section = alertsSection,
		position = 92
	)
	default AlertSound soundSkillingEvent()
	{
		return AlertSound.RUNELITE;
	}

	@Range(min = 1, max = 100)
	@ConfigItem(
		keyName = "volumeSkillingEvent",
		name = "Gem/bottle volume",
		description = VOLUME_HELP,
		section = alertsSection,
		position = 93
	)
	default int volumeSkillingEvent()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "repeatSkillingEvent",
		name = "Repeat gem/bottle",
		description = "Sound the mining and fishing events alert" + REPEAT_HELP,
		section = alertsSection,
		position = 94
	)
	default boolean repeatSkillingEvent()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alertMuteSounds",
		name = "Mute alert sounds",
		description = "No sound from any alert: not the plugin's own, and not RuneLite's notification sound "
			+ "(the tray notification and chat message still come). RuneLite plays sound inside its own "
			+ "process, so muting one app in your system's volume mixer may not silence it: use this "
			+ "instead.",
		section = alertsSection,
		position = 95
	)
	default boolean alertMuteSounds()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alertFollowGameVolume",
		name = "Follow game volume",
		description = "Scale the plugin's own alert sounds by the game's sound effects volume, and play nothing "
			+ "while that is muted. RuneLite's sound keeps RuneLite's notification volume.",
		section = alertsSection,
		position = 96
	)
	default boolean alertFollowGameVolume()
	{
		return false;
	}

	@ConfigItem(
		keyName = "opponentTimeout",
		name = "Timeout (s)",
		description = "How long, in seconds, the readout stays up after combat ends. The stock plugin fixes this at 5.",
		section = advancedSection,
		position = 0
	)
	default int opponentTimeout()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "hideWhenHpHud",
		name = "Hide behind boss HP bar",
		description = "Stand down when the game's own boss health bar is already showing this target.",
		section = advancedSection,
		position = 1
	)
	default boolean hideWhenHpHud()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideWhenStockEnabled",
		name = "Hide if Opponent Info on",
		description =
			"Hide this plugin's panel while Opponent Information is enabled, so the two do not draw "
				+ "over each other. The overhead text is unaffected.",
		section = advancedSection,
		position = 2
	)
	default boolean hideWhenStockEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showUpdateNotes",
		name = "Show update notes in chat",
		description = "After the plugin updates, say what is new in the game chat once, on your next login.",
		section = advancedSection,
		position = 3
	)
	default boolean showUpdateNotes()
	{
		return true;
	}

	/** The last version whose update notes were handled; written by the plugin, never shown. */
	@ConfigItem(
		keyName = LAST_SEEN_VERSION,
		name = "",
		description = "",
		hidden = true
	)
	default String lastSeenVersion()
	{
		return "";
	}

	String LAST_SEEN_VERSION = "lastSeenVersion";

	/** Set once the old default-on Follow game volume was cleared (AlertSounds); never shown. */
	@ConfigItem(
		keyName = "followGameVolumeReset",
		name = "",
		description = "",
		hidden = true
	)
	default boolean followGameVolumeReset()
	{
		return false;
	}

	/** The sidebar's Live section is open ({@link LiveSection}); written by its header, never listed. */
	@ConfigItem(
		keyName = LiveSection.OPEN_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default boolean sidebarLiveOpen()
	{
		return true;
	}

	/** The sidebar guides the user folded, "PRESETS,COMBAT" ({@link VitalsSidePanel}); written there, never listed. */
	@ConfigItem(
		keyName = VitalsSidePanel.GUIDES_CLOSED_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String sidebarGuidesClosed()
	{
		return "";
	}

	/** Every sidebar guide hidden from the panel's Hide guides; written there, never listed. */
	@ConfigItem(
		keyName = VitalsSidePanel.GUIDES_HIDDEN_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default boolean sidebarGuidesHidden()
	{
		return false;
	}

	/** The sidebar's last open tab, a {@link VitalsSidePanel.Main} name; written there, never listed. */
	@ConfigItem(
		keyName = VitalsSidePanel.TAB_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String sidebarTab()
	{
		return "";
	}

	/** Which card the Live tab shows, a {@link LiveSection.View} name; written there, never listed. */
	@ConfigItem(
		keyName = LiveSection.VIEW_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String sidebarLiveView()
	{
		return "";
	}
}
