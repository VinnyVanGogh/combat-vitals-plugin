package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The short guide at the top of each tab of the sidebar panel: what the tab
 * does, how to use it, and a walkthrough. README.md's "Sidebar panel" section
 * carries the same pages, with screenshots.
 */
final class PanelGuide
{
	private PanelGuide()
	{
	}

	/** One guide page. */
	static final class Page
	{
		final String what;
		final String how;
		final List<String> walkthrough;

		Page(String what, String how, String... walkthrough)
		{
			this.what = what;
			this.how = how;
			this.walkthrough = Collections.unmodifiableList(Arrays.asList(walkthrough));
		}
	}

	static final Page PRESETS = new Page(
		"A whole setup in one click. A preset only changes the few settings it lists; everything else stays as "
			+ "you set it.",
		"Click a preset to see exactly what it would change, then Apply. Undo puts those settings back as they "
			+ "were.",
		"Click Skiller.",
		"Read the list: each line is a setting, what it is now, and what it becomes.",
		"Click Apply. Changed your mind? Click Undo.");

	static Page of(PanelLayout.Tab tab)
	{
		switch (tab)
		{
			case COMBAT:
				return new Page(
					"Your target's health while you fight: in a small panel, and over the target itself. Also your "
						+ "own health, and the game's own health bar and chat.",
					"Combat vitals at the top turns this whole half on or off. Under it, pick what shows and how "
						+ "the numbers read.",
					"Leave Combat vitals ticked.",
					"Tick Show over target for the readout over the monster, Show panel for the box.",
					"Pick Display: hitpoints, percentage, or both.",
					"Tick Hide game health bar to see only this plugin's bar.");
			case TARGETS:
				return new Page(
					"Who gets a readout: monsters, players, and how big targets like bosses show.",
					"Tick the kinds of target you want. Big targets can show differently from the rest.",
					"Tick NPC targets for monsters; Player targets for PvP.",
					"Set Big target is to Bosses only, then Big targets to Both for hitpoints and percent on bosses.");
			case SKILLING:
				return new Page(
					"A badge over your head while you fish, cut, mine, thieve, runecraft or hunt: XP to level, "
						+ "inventory, catches, boosts, and the next fishing spot.",
					"Skilling vitals at the top turns this whole half on or off. Under it, pick the skills and "
						+ "the badge's rows.",
					"Leave Skilling vitals and Skilling badge ticked.",
					"Untick any skill you do not want a badge for.",
					"Under Rows, untick rows you do not read; Badge layout on the Look tab makes it smaller.",
					"Once you stop: choose how long the badge stays after you stop.");
			case ALERTS:
				return new Page(
					"A notification when something needs you: inventory full, you stopped gathering or thieving, "
						+ "the fishing spot moved, and more.",
					"Each alert is a row: tick it on or off, and Test plays it. 'Same for every alert' gives "
						+ "them all one sound; untick it to set each alert's own.",
					"Tick Same for every alert.",
					"Pick Voice line as the sound and set the volume, e.g. 40.",
					"Tick Repeat every alert if you want it again until you are back.",
					"Click Test on Inventory full to hear it.");
			case LOOK:
			default:
				return new Page(
					"How things look: the readout's style and position, health colours, the badge's size, place "
						+ "and colours, and the side pillars.",
					"The preview at the top redraws as you change things, before you see it in game.",
					"Pick a Style for the readout over your target.",
					"Change the three health colours; the preview bar shows the fade.",
					"Move the badge with Badge position and Badge offset; the preview shows where.",
					"Side pillars: pick Combat, Skilling or Both.");
		}
	}
}
