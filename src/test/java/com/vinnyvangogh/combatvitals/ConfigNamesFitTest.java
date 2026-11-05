package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.lang.reflect.Method;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/**
 * Every setting's name fits beside its control in RuneLite's config panel,
 * so none ends in "..." (Board, in-game 2026-11-04: "Path to next fi...",
 * "Once you...", and three more outside the skilling sections).
 *
 * The panel is {@code PluginPanel.PANEL_WIDTH} (225 px) less its borders,
 * and each row is a {@code BorderLayout}: the name in the centre, the
 * control on the east at its natural width. An enum's box is as wide as
 * its longest choice. Measured in characters of the RuneScape font, the two
 * names the Board saw cut off give the budget: "Path to next fi..." beside
 * "Right-click only" and "Once you..." beside "Until another activity" both
 * filled 33 characters. A name plus its control must stay within
 * {@link #BUDGET}, a few characters short of that, so a wide letter or two
 * cannot tip it over.
 */
public class ConfigNamesFitTest
{
	static final int BUDGET = 30;

	/** Characters a checkbox, number spinner and colour button take. */
	private static final int CHECKBOX = 3;
	private static final int SPINNER = 12;
	private static final int COLOUR = 9;

	@Test
	public void theBudgetWouldHaveCaughtWhatTheBoardSaw()
	{
		assertTrue("Path to next fishing spot".length() + "Right-click only".length() > BUDGET);
		assertTrue("Once you stop".length() + "Until another activity".length() > BUDGET);
		assertTrue("Hide game chat under readout".length() + CHECKBOX > BUDGET);
		assertTrue("Opponent timeout (s)".length() + SPINNER > BUDGET);
		assertTrue("Hide panel if stock plugin is on".length() + CHECKBOX > BUDGET);
	}

	/** Every section, and any setting outside one. */
	@Test
	public void everyNameFits()
	{
		final StringBuilder tooLong = new StringBuilder();
		for (Method method : CombatVitalsConfig.class.getMethods())
		{
			final ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item == null)
			{
				continue;
			}
			final int used = item.name().length() + control(method.getReturnType());
			if (used > BUDGET)
			{
				tooLong.append("\n'").append(item.name()).append("' (").append(item.keyName())
					.append(") takes ").append(used).append(" of ").append(BUDGET);
			}
		}
		assertTrue(tooLong.toString(), tooLong.length() == 0);
	}

	private static int control(Class<?> type)
	{
		if (type.isEnum())
		{
			int widest = 0;
			for (Object choice : type.getEnumConstants())
			{
				widest = Math.max(widest, choice.toString().length());
			}
			return widest;
		}
		if (type == Color.class)
		{
			return COLOUR;
		}
		if (type == int.class || type == double.class)
		{
			return SPINNER;
		}
		return CHECKBOX;
	}
}
