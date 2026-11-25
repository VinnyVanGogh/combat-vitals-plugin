package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * When the worn Magic bonus is too high to splash: which worn items to take off,
 * ranked by how much each one lowers the bonus, and which of them (taken off
 * together with the ones above it) reach the splash bonus. No client types, so
 * it is unit tested. Information only: it reads what is worn and says nothing
 * about how to change it.
 */
final class GearAdvice
{
	/** How many items the card lists. */
	static final int MAX_ROWS = 3;

	/** A worn item and its Magic attack bonus. */
	static final class Worn
	{
		final String name;
		final int magicBonus;

		Worn(String name, int magicBonus)
		{
			this.name = name;
			this.magicBonus = magicBonus;
		}
	}

	/** One item to remove. */
	static final class Step
	{
		final String name;
		/** How much taking it off lowers the Magic bonus. */
		final int lowersBy;
		/** The bonus once this and every item above it in the list is off. */
		final int bonusAfter;
		/** Taking off this and the items above it reaches the splash bonus. */
		final boolean reaches;

		Step(String name, int lowersBy, int bonusAfter, boolean reaches)
		{
			this.name = name;
			this.lowersBy = lowersBy;
			this.bonusAfter = bonusAfter;
			this.reaches = reaches;
		}
	}

	private GearAdvice()
	{
	}

	/**
	 * The items to take off, biggest first, at most {@link #MAX_ROWS}. Empty when the bonus already splashes or
	 * nothing worn adds to it. Items worn with a negative bonus are never listed: taking them off would raise it.
	 */
	static List<Step> remove(List<Worn> worn, int magicBonus)
	{
		final List<Step> steps = new ArrayList<>();
		if (SplashMath.splashes(magicBonus))
		{
			return steps;
		}
		final List<Worn> positive = new ArrayList<>();
		for (Worn w : worn)
		{
			if (w.magicBonus > 0)
			{
				positive.add(w);
			}
		}
		positive.sort(Comparator.comparingInt((Worn w) -> w.magicBonus).reversed().thenComparing(w -> w.name));
		int bonus = magicBonus;
		for (Worn w : positive)
		{
			bonus -= w.magicBonus;
			steps.add(new Step(w.name, w.magicBonus, bonus, SplashMath.splashes(bonus)));
			if (steps.size() == MAX_ROWS)
			{
				break;
			}
		}
		return steps;
	}
}
