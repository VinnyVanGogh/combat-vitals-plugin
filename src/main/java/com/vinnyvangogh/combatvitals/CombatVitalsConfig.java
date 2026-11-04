package com.vinnyvangogh.combatvitals;

import java.awt.Color;
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

	@ConfigSection(
		name = "Display",
		description = "What the readout shows and where",
		position = 0
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Targets",
		description = "Which opponents the readout appears for",
		position = 1
	)
	String targetSection = "targets";

	@ConfigSection(
		name = "Skilling vitals",
		description = "A badge over your head while you work a fishing spot or other skilling NPC",
		position = 2
	)
	String skillingSection = "skilling";

	@ConfigSection(
		name = "Activity alerts",
		description = "RuneLite notifications for gathering, thieving and make-X: inventory full, stopped, and more",
		position = 3
	)
	String alertsSection = "alerts";

	@ConfigSection(
		name = "Advanced",
		description = "Timeouts and diagnostics",
		position = 4,
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
		section = displaySection,
		position = 8
	)
	default boolean colourGradient()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overheadPosition",
		name = "Overhead position",
		description = "Where above the target the text sits. Large NPCs often read better on Bottom.",
		section = displaySection,
		position = 5
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
		section = displaySection,
		position = 9
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
		section = displaySection,
		position = 10
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
		section = displaySection,
		position = 11
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
		section = displaySection,
		position = 4
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
		section = displaySection,
		position = 7
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
		section = displaySection,
		position = 6
	)
	default int overheadOffset()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "hideGameHealthBar",
		name = "Hide game health bar",
		description =
			"Hide the game's own green and red health bar over every player and NPC, yours included, "
				+ "so it no longer jumps up when someone talks. Hitsplats and overhead chat stay. "
				+ "Overrides Interface Styles' HD health bars while on, and gives them back when off.",
		section = displaySection,
		position = 12
	)
	default boolean hideGameHealthBar()
	{
		// On by default (Board, 2026-11-04). A saved value is kept under the
		// unchanged key, so only profiles that never set it change.
		return true;
	}

	@ConfigItem(
		keyName = "hideGameOverheadChat",
		name = "Hide game chat under readout",
		description =
			"While the readout or skilling badge is over you or your opponent, take the game's own overhead "
				+ "chat line off and draw it above the readout instead, so it is never hidden under it. "
				+ "The message keeps its full time on screen and goes back to the game when the readout goes. "
				+ "Off: the game draws its own line and the plugin adds nothing. A skilling NPC is never touched.",
		section = displaySection,
		position = 13
	)
	default boolean hideGameOverheadChat()
	{
		// On by default (Board, 2026-11-04, after the in-game test). It edits
		// the actor's chat text, the way the base Chat Filter does.
		return true;
	}

	@ConfigItem(
		keyName = "showOwnHealth",
		name = "Show your own health",
		description =
			"Draw your own health over your character while you are engaged, in the same style as your "
				+ "opponent's. Taken from the hitpoints orb, so it is exact rather than recovered.",
		section = targetSection,
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
		position = 5
	)
	default boolean showStatComparison()
	{
		return false;
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
		position = 1
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
		position = 2
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
		position = 3
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
		position = 4
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
		position = 5
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
		position = 6
	)
	default boolean skillingInventoryBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fishingSpotPath",
		name = "Path to next fishing spot",
		description =
			"Find the nearest fishing spot of the same kind (net/bait, lure/bait, cage/harpoon...) that you can "
				+ "catch something at, and send it to the Shortest Path plugin; the path clears when you start "
				+ "fishing again. 'On spot move' does it when your spot moves; either way, right-clicking the badge "
				+ "offers 'Path to next spot'. With no spot of that kind nearby, the nearest known location is "
				+ "used. Without Shortest Path installed, the next spot is highlighted instead.",
		section = skillingSection,
		position = 7
	)
	default FishingSpotPath fishingSpotPath()
	{
		return FishingSpotPath.OFF;
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
		keyName = "opponentTimeout",
		name = "Opponent timeout (s)",
		description = "How long the readout stays up after combat ends. The stock plugin fixes this at 5.",
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
		name = "Hide panel if stock plugin is on",
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

}
