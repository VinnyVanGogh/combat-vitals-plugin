package com.vinnyvangogh.combatvitals;

import java.util.Locale;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ObjectID;

/**
 * Training methods that are not gathering but get the same badge: the
 * player operates an object, stands at it animating, and experience comes in
 * a skill the badge would otherwise never show (Board, 2026-11-14).
 *
 * Each row is what the badge needs: the name, the activity's verb, the skill
 * trained, the object and option that start it, and how long the game lets
 * it run from one click. More methods slot in as rows; the detection itself
 * is {@link ObjectSkilling}'s click-then-animate, shared with trees and rocks.
 *
 * <h3>Blast Furnace pump</h3>
 * Object {@link ObjectID#BLAST_FURNACE_PUMP} (9090), option "Operate". 2
 * Strength experience a tick, no Hitpoints; "The pump can be operated for up
 * to 100 minutes before automatically stopping."
 * (https://oldschool.runescape.wiki/w/Pump_(Blast_Furnace), read 2026-11-14).
 * The player's pumping animation is gameval
 * {@code AnimationID.BLAST_FURNACE_HUMAN_AIR_PUMP} (2432); detection takes any
 * animation beside the pump, as for every object, so a renamed or second
 * pumping animation does not lose the badge.
 */
enum TrainingMethod
{
	BLAST_FURNACE_PUMP("Blast Furnace", "pumping", Skill.STRENGTH, ObjectID.BLAST_FURNACE_PUMP, "Operate", 100);

	/** Where the method is, for the badge: "Pumping (Blast Furnace)". */
	private final String place;
	/** The activity in a sentence: "You've stopped pumping". */
	private final String verb;
	private final Skill skill;
	private final int objectId;
	private final String option;
	/** How long one click runs before the game stops it, in minutes; 0 when it never does. */
	private final int autoStopMinutes;

	TrainingMethod(String place, String verb, Skill skill, int objectId, String option, int autoStopMinutes)
	{
		this.place = place;
		this.verb = verb;
		this.skill = skill;
		this.objectId = objectId;
		this.option = option;
		this.autoStopMinutes = autoStopMinutes;
	}

	/** The method an option on an object starts, or null. Impostors are resolved by the caller. */
	static TrainingMethod forObject(int objectId, String option)
	{
		if (option == null)
		{
			return null;
		}
		for (TrainingMethod method : values())
		{
			if (method.objectId == objectId && method.option.equalsIgnoreCase(option))
			{
				return method;
			}
		}
		return null;
	}

	Skill skill()
	{
		return skill;
	}

	String verb()
	{
		return verb;
	}

	/** "Pumping (Blast Furnace)": the badge's name for the method while working. */
	String label()
	{
		return verb.substring(0, 1).toUpperCase(Locale.ROOT) + verb.substring(1) + " (" + place + ")";
	}

	/** Game ticks one click runs before the game stops it, or -1 when it never does. */
	int autoStopTicks()
	{
		return autoStopMinutes <= 0 ? -1 : autoStopMinutes * 100;
	}
}
