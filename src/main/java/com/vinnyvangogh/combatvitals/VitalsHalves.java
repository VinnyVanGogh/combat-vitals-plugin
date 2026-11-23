package com.vinnyvangogh.combatvitals;

/**
 * The plugin's two halves and their master switches, Combat vitals and
 * Skilling vitals (Board, 2026-11-14). A switch off takes its whole half away:
 * overlays, side pillars and alerts. The other half is left exactly as set.
 *
 * Every setting a switch has to override is read here and only here
 * (VitalsHalvesTest), so a half cannot keep running behind its switch.
 */
final class VitalsHalves
{
	private VitalsHalves()
	{
	}

	/** Target and own-health readouts: panel, over target, the game health bar swap, combat pillars. */
	static boolean combat(CombatVitalsConfig config)
	{
		return config.combatVitals();
	}

	/** The skilling badge, skilling pillars, the next fishing spot, activity and boost alerts. */
	static boolean skilling(CombatVitalsConfig config)
	{
		return config.skillingVitals();
	}

	static boolean skillingBadge(CombatVitalsConfig config)
	{
		return skilling(config) && config.skillingEnabled();
	}

	static boolean hideGameHealthBar(CombatVitalsConfig config)
	{
		return combat(config) && config.hideGameHealthBar();
	}

	static boolean nextSpotArrow(CombatVitalsConfig config)
	{
		return skilling(config) && config.nextSpotArrow();
	}

	/** The pillars the user chose, less the half that is switched off. */
	static PillarMode pillarMode(CombatVitalsConfig config)
	{
		final PillarMode chosen = config.pillarMode();
		final boolean combat = chosen.combat() && combat(config);
		final boolean skilling = chosen.skilling() && skilling(config);
		if (combat && skilling)
		{
			return PillarMode.BOTH;
		}
		if (combat)
		{
			return PillarMode.COMBAT;
		}
		return skilling ? PillarMode.SKILLING : PillarMode.OFF;
	}
}
