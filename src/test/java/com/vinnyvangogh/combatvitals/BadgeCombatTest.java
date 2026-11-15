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
	public void fightDoesNotEndAMovedBadgesIdle()
	{
		assertFalse(BadgeCombat.endsIdle(true, false));
	}

	@Test
	public void movedIdleTimerRunsNormallyThroughAFight()
	{
		final IdleBadge idle = new IdleBadge();
		idle.tick(Skill.WOODCUTTING, false, 0);
		idle.tick(null, BadgeCombat.endsIdle(false, false), 1_000);
		// A fight starts and goes on; the moved badge's idle is untouched by it.
		idle.tick(null, BadgeCombat.endsIdle(true, false), 2_000);
		idle.tick(null, BadgeCombat.endsIdle(true, false), 30_000);
		assertEquals(Skill.WOODCUTTING, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 30_000));
		// And it still runs out on its own timer, counted from the stop, not the fight.
		assertEquals(Skill.WOODCUTTING, idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 60_999));
		assertNull(idle.idleSkill(IdleBadgeMode.TIMED, SECONDS, 61_000));
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
