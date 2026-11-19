package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * A fight and the skilling badge (Board, 2026-11-14): a badge moved off your
 * head never vanishes because of a fight; it says so instead, and its
 * "Not woodcutting" runs out on its own timer. Over your head, Hide during
 * combat keeps its meaning.
 */
public class BadgeCombatTest
{
	private static final int SECONDS = 60;

	// --- Moved badge: a fight never hides it --------------------------------

	@Test
	public void fixedBadgeStaysThroughAFightWithHideDuringCombatOn()
	{
		assertFalse(BadgeCombat.hides(true, false, true, false));
		assertFalse(BadgeCombat.hides(true, false, true, true));
	}

	@Test
	public void movedBadgeSaysInCombatWhileIdle()
	{
		assertEquals("in combat", BadgeCombat.tag(true, true));
		assertEquals("Not woodcutting | in combat",
			SkillingText.withCombatTag(SkillingText.idleHeader(Skill.WOODCUTTING), BadgeCombat.tag(true, true)));
	}

	@Test
	public void movedBadgeSaysUnderAttackWhileStillGathering()
	{
		assertEquals("under attack", BadgeCombat.tag(true, false));
		assertEquals("Woodcutting 70 | under attack",
			SkillingText.withCombatTag("Woodcutting 70", BadgeCombat.tag(true, false)));
	}

	@Test
	public void noTagOutOfCombat()
	{
		assertNull(BadgeCombat.tag(false, true));
		assertNull(BadgeCombat.tag(false, false));
		assertEquals("Not fishing", SkillingText.withCombatTag("Not fishing", null));
	}

	@Test
	public void fightEndsAMovedBadgesIdleToo()
	{
		assertTrue(BadgeCombat.endsIdle(true, false));
	}

	@Test
	public void movedIdleBadgeGoesWhenAFightStarts()
	{
		final IdleBadge idle = new IdleBadge();
		idle.tick(Skill.HUNTER, false, 0);
		idle.tick(null, BadgeCombat.endsIdle(false, false), 1_000);
		assertEquals(Skill.HUNTER, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 2_000));
		// No "Not hunting | in combat": the fight ends the idle badge.
		idle.tick(null, BadgeCombat.endsIdle(true, false), 3_000);
		assertNull(idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 3_000));
	}

	// --- Over your head: unchanged -------------------------------------------

	@Test
	public void overHeadWithHideDuringCombatStillHides()
	{
		assertTrue(BadgeCombat.hides(true, true, true, false));
	}

	@Test
	public void overHeadWithoutHideStaysWhileBeingHit()
	{
		assertFalse(BadgeCombat.hides(true, true, false, false));
	}

	@Test
	public void overHeadTargetsHealthBarTakesOver()
	{
		assertTrue(BadgeCombat.hides(true, true, false, true));
	}

	@Test
	public void overHeadFightStillEndsIdle()
	{
		assertTrue(BadgeCombat.endsIdle(true, true));
		assertFalse(BadgeCombat.endsIdle(false, true));
	}

	@Test
	public void noFightNeverHides()
	{
		assertFalse(BadgeCombat.hides(false, true, true, false));
		assertFalse(BadgeCombat.hides(false, false, true, false));
	}
}
