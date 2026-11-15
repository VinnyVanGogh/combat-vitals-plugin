package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Where every setting lives: which tab of the sidebar panel, under which
 * heading, in what order. RuneLite's settings list follows the same table:
 * each tab is one config section with the same settings in the same order
 * (PanelLayoutTest), so the panel and the list never disagree about where
 * something is. Only keyNames appear here; names, descriptions, types and
 * ranges come from the config interface itself, through RuneLite's
 * {@code ConfigDescriptor}.
 */
final class PanelLayout
{
	private PanelLayout()
	{
	}

	/** The panel's tabs, each the twin of one config section. */
	enum Tab
	{
		COMBAT("Combat", CombatVitalsConfig.displaySection, "combatVitals"),
		TARGETS("Targets", CombatVitalsConfig.targetSection, null),
		SKILLING("Skilling", CombatVitalsConfig.skillingSection, "skillingVitals"),
		ALERTS("Alerts", CombatVitalsConfig.alertsSection, null),
		LOOK("Look", CombatVitalsConfig.pillarsSection, null);

		final String label;
		final String section;
		/** The half's master switch shown at the top of the tab, or null. */
		final String master;

		Tab(String label, String section, String master)
		{
			this.label = label;
			this.section = section;
			this.master = master;
		}
	}

	/** A heading in a tab and the settings under it. */
	static final class Group
	{
		final String title;
		final List<String> keys;

		Group(String title, String... keys)
		{
			this.title = title;
			this.keys = Collections.unmodifiableList(Arrays.asList(keys));
		}
	}

	/** One activity alert's settings: a row in the Alerts tab. */
	static final class AlertRow
	{
		final ActivityAlerts.Type type;
		final String label;
		final String toggle;
		final String sound;
		final String volume;
		final String repeat;
		/** A setting only this alert has (its delay, cap or threshold), or null. */
		final String extra;

		AlertRow(ActivityAlerts.Type type, String label, String toggle, String sound, String volume, String repeat,
			String extra)
		{
			this.type = type;
			this.label = label;
			this.toggle = toggle;
			this.sound = sound;
			this.volume = volume;
			this.repeat = repeat;
			this.extra = extra;
		}
	}

	static final String SOUND_SHARED = "alertSoundShared";

	/** The one sound for every alert, shown while {@link #SOUND_SHARED} is on. */
	static final Group SHARED_SOUND = new Group("Sound for every alert", SOUND_SHARED, "alertSoundAll",
		"alertVolumeAll", "alertRepeatAll");

	static final Group REPEATS = new Group("Repeats", "repeatEverySeconds", "repeatMaxMinutes");

	static final List<AlertRow> ALERTS = Collections.unmodifiableList(Arrays.asList(
		row(ActivityAlerts.Type.INVENTORY_FULL, "Inventory full", "InventoryFull", "alertInventoryFull", null),
		row(ActivityAlerts.Type.GATHERING_STOPPED, "Stopped gathering", "GatheringStopped", "alertGatheringStopped",
			"alertStopSeconds"),
		row(ActivityAlerts.Type.SPOT_MOVED, "Fishing spot moved", "SpotMoved", "alertSpotMoved", null),
		row(ActivityAlerts.Type.THIEVING_STOPPED, "Stopped thieving", "ThievingStopped", "alertThievingStopped",
			null),
		row(ActivityAlerts.Type.STUNNED, "Pickpocket stun", "Stunned", "alertStunned", null),
		row(ActivityAlerts.Type.COIN_POUCH_CAP, "Coin pouches at cap", "CoinPouch", "alertCoinPouch",
			"alertCoinPouchCap"),
		row(ActivityAlerts.Type.NECKLACE_BROKE, "Dodgy necklace broke", "NecklaceBroke", "alertNecklaceBroke",
			null),
		row(ActivityAlerts.Type.PRODUCTION_FINISHED, "Make-X finished", "MakeFinished", "alertMakeFinished", null),
		row(ActivityAlerts.Type.LOW_RUN_ENERGY, "Low run energy", "LowRunEnergy", "alertLowRunEnergy",
			"alertRunEnergyThreshold")));

	/** In the settings list only: the panel says where to find them. */
	static final List<String> ADVANCED = Collections.unmodifiableList(Arrays.asList(
		"opponentTimeout", "hideWhenHpHud", "hideWhenStockEnabled", "showUpdateNotes"));

	private static AlertRow row(ActivityAlerts.Type type, String label, String suffix, String toggle, String extra)
	{
		return new AlertRow(type, label, toggle, "sound" + suffix, "volume" + suffix, "repeat" + suffix, extra);
	}

	/** The headings and settings of a tab other than Alerts, which is drawn from {@link #ALERTS}. */
	static List<Group> groups(Tab tab)
	{
		switch (tab)
		{
			case COMBAT:
				return Arrays.asList(
					new Group("What shows", "showPanel", "showOverhead", "displayMode", "percentageDecimals"),
					new Group("Your own health", "showOwnHealth"),
					new Group("The game's own", "hideGameHealthBar", "hideGameOverheadChat"));
			case TARGETS:
				return Arrays.asList(
					new Group("Who gets a readout", "showForNpcs", "showForPlayers", "overheadForPlayers",
						"showPlayerHitpoints", "showStatComparison"),
					new Group("Big targets", "bigTargetDisplay", "otherTargetDisplay", "bigTargetRule",
						"bigTargetMaxHp"));
			case SKILLING:
				return skillingGroups();
			case LOOK:
				return lookGroups();
			case ALERTS:
			default:
				return Collections.emptyList();
		}
	}

	private static List<Group> skillingGroups()
	{
		return Arrays.asList(
			new Group("Badge", "skillingEnabled", "skillingHideInCombat"),
			new Group("Skills", "skillingFishing", "skillingWoodcutting", "skillingMining", "skillingThieving",
				"skillingRunecraft", "skillingHunter"),
			new Group("Rows", "skillingShowHeader", "skillingShowXp", "skillingXpDisplay", "skillingTimeToLevel",
				"skillingCatches", "skillingLockedFish", "skillingGearWarning"),
			new Group("Inventory", "skillingInventoryBar", "skillingInventoryCount", "skillingContainers",
				"skillingContainersAsInventory"),
			new Group("Session", "skillingSessionCount", "skillingRates", "skillingSessionReset"),
			new Group("Boosts", "skillingBoostTimer", "skillingBoostSource", "skillingSpecEnergy",
				"skillingBoostNotify"),
			new Group("Once you stop", "skillingIdleMode", "skillingIdleSeconds"),
			new Group("Next fishing spot", "fishingSpotPath", "fishingSpotShortestPath"),
			new Group("More skills", "skillingHerblore", "skillingCooking", "skillingFletching", "skillingCrafting",
				"skillingSmithing", "skillingFiremaking", "skillingPrayer", "skillingMagic", "skillingAgility",
				"skillingConstruction", "skillingSailing"));
	}

	private static List<Group> lookGroups()
	{
		return Arrays.asList(
			new Group("Readout", "overheadStyle", "overheadPosition", "overheadOffset", "barBackground"),
			new Group("Health colours", "colourGradient", "healthFullColour", "healthMidColour", "healthLowColour"),
			new Group("Badge", "skillingBadgeLayout", "skillingPosition", "skillingOffset", "skillingScale",
				"skillingIdleColour"),
			new Group("Skill colours", "skillingFishingColour", "skillingWoodcuttingColour", "skillingMiningColour",
				"skillingThievingColour", "skillingRunecraftColour", "skillingHunterColour"),
			new Group("Side pillars", "pillarMode", "pillarCombatSide", "pillarXpSide", "pillarLabels",
				"pillarStyle"));
	}

	/**
	 * A tab's settings in the order RuneLite's list shows them in its section.
	 * Alerts: each alert's switch (and its own delay, cap or threshold), then
	 * the shared sound, the repeat timing, then each alert's own sound.
	 */
	static List<String> listOrder(Tab tab)
	{
		final List<String> out = new ArrayList<>();
		if (tab != Tab.ALERTS)
		{
			for (Group g : groups(tab))
			{
				out.addAll(g.keys);
			}
			return out;
		}
		for (AlertRow r : ALERTS)
		{
			out.add(r.toggle);
			if (r.extra != null)
			{
				out.add(r.extra);
			}
		}
		out.addAll(SHARED_SOUND.keys);
		out.addAll(REPEATS.keys);
		for (AlertRow r : ALERTS)
		{
			out.addAll(Arrays.asList(r.sound, r.volume, r.repeat));
		}
		return out;
	}
}
