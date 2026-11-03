package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.time.Instant;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The scenarios from the brief: chopping a tree shows Woodcutting, mining
 * shows Mining, walking away clears it, and combat still wins. Each drives
 * {@link ObjectSkilling} the way the plugin does and forwards its steps to
 * {@link SkillingState}, so the badge decision is tested end to end.
 */
public class ObjectSkillingTest
{
	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final Instant T0 = Instant.parse("2026-11-03T12:00:00Z");

	/** A 1x1 yew at (3200, 3200). */
	private static final int TREE_X = 3200;
	private static final int TREE_Y = 3200;

	private final ObjectSkilling objects = new ObjectSkilling();
	private final SkillingState skilling = new SkillingState();
	private final CombatState combat = new CombatState();

	/** What the plugin does with each step. */
	private void apply(ObjectSkilling.Step step, Instant now)
	{
		switch (step)
		{
			case ENGAGE:
				skilling.engage(objects.skill());
				break;
			case DISENGAGE:
				skilling.disengage(now);
				break;
			case RELEASE:
				skilling.release();
				break;
			default:
				break;
		}
	}

	private void tick(int tick, int x, int y, boolean animating)
	{
		apply(objects.tick(tick, x, y, 0, animating), T0);
	}

	/** The plugin's badge gate: a skill is engaged and no fight has the target. */
	private Skill badge()
	{
		return combat.showTarget() ? null : skilling.skill();
	}

	private void clickTree(int tick)
	{
		objects.clicked(SkillingActions.forOption("Chop down", "Yew tree"), TREE_X, TREE_Y, 0, 1, 1, tick);
	}

	@Test
	public void choppingATreeShowsWoodcutting()
	{
		clickTree(100);
		tick(101, 3195, 3200, false);
		assertNull("still walking over: nothing yet", badge());

		tick(105, TREE_X - 1, TREE_Y, true);
		assertEquals(Skill.WOODCUTTING, badge());
	}

	@Test
	public void miningARockShowsMining()
	{
		objects.clicked(SkillingActions.forOption("Mine", "Iron rocks"), 3300, 3300, 0, 1, 1, 100);
		tick(102, 3300, 3301, true);

		assertEquals(Skill.MINING, badge());
	}

	@Test
	public void aLargeObjectCountsFromItsWholeFootprint()
	{
		// A 2x2 tree: standing east of its far edge is still at the tree.
		objects.clicked(Skill.WOODCUTTING, TREE_X, TREE_Y, 0, 2, 2, 100);
		tick(102, TREE_X + 2, TREE_Y + 1, true);

		assertEquals(Skill.WOODCUTTING, badge());
	}

	@Test
	public void walkingAwayClearsIt()
	{
		clickTree(100);
		tick(102, TREE_X - 1, TREE_Y, true);
		assertEquals(Skill.WOODCUTTING, badge());

		tick(103, TREE_X - 1 - ObjectSkilling.REACH - 1, TREE_Y, false);
		assertNull("walked off: gone at once, no linger", badge());
		assertFalse(objects.held());
	}

	@Test
	public void walkHereClearsItAtOnce()
	{
		clickTree(100);
		tick(102, TREE_X - 1, TREE_Y, true);

		// The plugin's Walk here handler.
		if (objects.started())
		{
			skilling.release();
		}
		objects.forget();

		assertNull(badge());
	}

	@Test
	public void stoppingLingersForTheTimeoutThenExpires()
	{
		clickTree(100);
		tick(102, TREE_X - 1, TREE_Y, true);
		// The tree falls: the animation stops, the player stays put.
		tick(110, TREE_X - 1, TREE_Y, false);
		assertEquals("lingers", Skill.WOODCUTTING, badge());

		assertFalse(skilling.expire(T0.plus(TIMEOUT), TIMEOUT, objects.working()));
		assertTrue(skilling.expire(T0.plus(TIMEOUT).plusMillis(1), TIMEOUT, objects.working()));
		assertNull(badge());
	}

	@Test
	public void swingingAgainCancelsTheLinger()
	{
		clickTree(100);
		tick(102, TREE_X - 1, TREE_Y, true);
		tick(110, TREE_X - 1, TREE_Y, false);
		tick(111, TREE_X - 1, TREE_Y, true);

		assertFalse(skilling.expire(T0.plusSeconds(60), TIMEOUT, objects.working()));
		assertEquals(Skill.WOODCUTTING, badge());
	}

	@Test
	public void animatingAwayFromTheTreeIsNotChopping()
	{
		// Clicked a tree, then drank a potion on the way: no badge.
		clickTree(100);
		tick(101, TREE_X - 10, TREE_Y, true);

		assertNull(badge());
	}

	@Test
	public void aClickThatNeverStartsIsForgotten()
	{
		clickTree(100);
		tick(100 + ObjectSkilling.START_TICKS + 1, TREE_X - 1, TREE_Y, false);
		assertFalse(objects.held());

		tick(100 + ObjectSkilling.START_TICKS + 2, TREE_X - 1, TREE_Y, true);
		assertNull("an unrelated animation later is not chopping", badge());
	}

	@Test
	public void anUnknownOptionHoldsNothing()
	{
		objects.clicked(SkillingActions.forOption("Bank", "Bank booth"), TREE_X, TREE_Y, 0, 1, 1, 100);
		assertFalse(objects.held());
		tick(101, TREE_X - 1, TREE_Y, true);

		assertNull(badge());
	}

	@Test
	public void anotherPlaneIsNotTheTree()
	{
		clickTree(100);
		apply(objects.tick(102, TREE_X - 1, TREE_Y, 1, true), T0);

		assertNull(badge());
	}

	@Test
	public void combatStillWins()
	{
		clickTree(100);
		tick(102, TREE_X - 1, TREE_Y, true);
		assertEquals(Skill.WOODCUTTING, badge());

		// Attacking something: the plugin engages combat, releases the badge
		// and forgets the tree, as it does for a skilling NPC.
		combat.engage(CombatState.attackable(new String[] {"Attack"}, 2));
		skilling.release();
		objects.forget();

		assertTrue(combat.showTarget());
		assertNull(badge());
	}

	@Test
	public void pickpocketingAnAttackableNpcIsThievingUntilAHitLands()
	{
		final String[] man = {"Talk-to", "Attack", "Pickpocket", null, null};
		final Skill clicked = SkillingActions.forOption("Pickpocket", "Man");

		// The plugin: attackable, but the click was Pickpocket, so not a fight.
		combat.engage(CombatState.attackable(man, 2) && clicked == null);
		skilling.engage(clicked);
		assertEquals(Skill.THIEVING, badge());

		// Hitting the Man proves it a fight, and the health bar takes over.
		combat.hit(true, T0);
		assertNull(badge());
		assertTrue(combat.showTarget());
	}

	@Test
	public void attackingAnAttackableNpcIsStillAFight()
	{
		final String[] man = {"Talk-to", "Attack", "Pickpocket", null, null};
		final Skill clicked = SkillingActions.forOption("Attack", "Man");

		combat.engage(CombatState.attackable(man, 2) && clicked == null);
		assertTrue(combat.showTarget());
		assertNull(badge());
	}
}
