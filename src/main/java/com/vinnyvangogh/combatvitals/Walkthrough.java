package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The first-run tour of the sidebar: seven short steps, one per place a new
 * user needs to know about. It shows once after install, and again only when
 * {@link #REVISION} is raised because the tour itself changed; Skip and Done
 * both count as seen. "Show guides" and the "?" link on Live and Overlays
 * replay it. Pure: what is said and when it is due.
 */
final class Walkthrough
{
	private Walkthrough()
	{
	}

	/** Hidden setting: the revision of the tour the user has been through or skipped. */
	static final String SEEN_KEY = "sidebarTourSeen";

	/** Raise when the tour changes enough that everyone should see it again. */
	static final int REVISION = 1;

	/** One step: the tab it points at (null to stay where it is), a title and a short text. */
	static final class Step
	{
		final VitalsSidePanel.Main tab;
		final String title;
		final String text;

		Step(VitalsSidePanel.Main tab, String title, String text)
		{
			this.tab = tab;
			this.title = title;
			this.text = text;
		}
	}

	static final List<Step> STEPS = Collections.unmodifiableList(Arrays.asList(
		new Step(null, "What this shows",
			"While you fight, a card in the top left shows your target's name, level and health, and for monsters "
				+ "its wiki stats. A readout over the target shows its health in the world. Skilling gets a badge "
				+ "over your head. This tour takes a minute."),
		new Step(VitalsSidePanel.Main.LIVE, "Live",
			"What is happening right now: your session, XP per hour and the fight in progress. Open a card to "
				+ "see more."),
		new Step(VitalsSidePanel.Main.OVERLAYS, "Overlays",
			"One switch per overlay: the corner card, the readout over the target, the badge, the pillars. Turn "
				+ "off what you do not want."),
		new Step(VitalsSidePanel.Main.GOALS, "Goals",
			"Set a target, like a level or an amount of XP, and watch your progress and the time left at your "
				+ "current rate."),
		new Step(VitalsSidePanel.Main.MAX_HIT, "Max Hit",
			"Your max hit with the gear you wear now, and what it would be with another style or a different "
				+ "weapon."),
		new Step(VitalsSidePanel.Main.PRESETS, "Presets",
			"A whole setup in one click. Each preset shows what it changes before you apply it, and Undo puts "
				+ "it back."),
		new Step(VitalsSidePanel.Main.SETTINGS, "Settings",
			"Every setting, in tabs: Combat, Targets, Skilling, Alerts and Look. Alert sounds follow RuneLite's "
				+ "notification volume. Replay this tour any time with Show guides.")));

	/** Whether the tour has not yet been seen at this revision; {@code seen} is null while unset. */
	static boolean due(Integer seen)
	{
		return seen == null || seen < REVISION;
	}

	/** The index of the step for a tab, or 0 when no step points at it. */
	static int stepFor(VitalsSidePanel.Main tab)
	{
		for (int i = 0; i < STEPS.size(); i++)
		{
			if (STEPS.get(i).tab == tab)
			{
				return i;
			}
		}
		return 0;
	}
}
