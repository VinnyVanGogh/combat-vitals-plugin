package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

/**
 * The skilling badge's colour per skill, so the skill reads at a glance.
 *
 * Every colour sits in the middle of the luminance range (WCAG relative
 * luminance about 0.15 to 0.25), which keeps a contrast of at least 3:1
 * against both white and black: readable as text over snow or a cave, and
 * under the badge's white text when used as the bar fill.
 * {@code SkillColoursTest} checks every skill.
 */
final class SkillColours
{
	/** Slate grey, for any skill without its own colour. */
	static final Color DEFAULT = new Color(0x6E7681);

	private static final Map<Skill, Color> COLOURS = new EnumMap<>(Skill.class);

	static
	{
		COLOURS.put(Skill.FISHING, new Color(0x3A7BD5));
		COLOURS.put(Skill.WOODCUTTING, new Color(0x2E8B3A));
		COLOURS.put(Skill.MINING, new Color(0x8C6E52));
		COLOURS.put(Skill.THIEVING, new Color(0x9050C8));
		COLOURS.put(Skill.HUNTER, new Color(0xB0602A));
		COLOURS.put(Skill.RUNECRAFT, new Color(0xA88418));
	}

	private SkillColours()
	{
	}

	static Color of(Skill skill)
	{
		return skill == null ? DEFAULT : COLOURS.getOrDefault(skill, DEFAULT);
	}

	/** WCAG 2 contrast ratio between two colours, 1 to 21. */
	static double contrast(Color a, Color b)
	{
		final double la = luminance(a);
		final double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static double luminance(Color c)
	{
		return 0.2126 * linear(c.getRed()) + 0.7152 * linear(c.getGreen()) + 0.0722 * linear(c.getBlue());
	}

	private static double linear(int channel)
	{
		final double s = channel / 255.0;
		return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
	}
}
