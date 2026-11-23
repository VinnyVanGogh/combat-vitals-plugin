package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class NextSpotArrowTest
{
	private static final NextSpotArrow.Target TREE = NextSpotArrow.Target.tile(3200, 3200, 0);
	private static final NextSpotArrow.Target OTHER_TREE = NextSpotArrow.Target.tile(3210, 3200, 0);

	private FakeHintArrow game;
	private boolean enabled;
	private int distance;
	private NextSpotArrow arrow;

	@Before
	public void setUp()
	{
		game = new FakeHintArrow();
		enabled = true;
		distance = 15;
		arrow = new NextSpotArrow(game, () -> enabled, () -> distance);
	}

	@Test
	public void pointsAtATarget()
	{
		assertTrue(arrow.point(TREE));

		assertSame(TREE, game.shown);
		assertTrue(arrow.active());
		assertTrue(arrow.targetsTile(3200, 3200, 0));
	}

	@Test
	public void pointsAtAnNpcAndFollowsIt()
	{
		final Object spot = new Object();
		assertTrue(arrow.point(NextSpotArrow.Target.npc(spot, 3000, 3000, 0)));

		assertTrue(arrow.targets(spot));
		assertFalse(arrow.targetsTile(3000, 3000, 0));
	}

	@Test
	public void neverOverridesAForeignArrow()
	{
		game.foreign = true;

		assertFalse(arrow.point(TREE));

		assertNull(game.shown);
		assertEquals(0, game.shows);
		assertFalse(arrow.active());
	}

	@Test
	public void neverClearsAForeignArrow()
	{
		game.foreign = true;

		arrow.point(TREE);
		arrow.clear();
		arrow.tick(0, 0, 0);

		assertEquals(0, game.hides);
		assertTrue(game.foreign);
	}

	@Test
	public void anArrowTheGameTookOverIsForgottenNotCleared()
	{
		arrow.point(TREE);
		game.takenOverByQuest();

		arrow.tick(3200, 3200, 0);
		arrow.clear();

		assertFalse(arrow.active());
		assertEquals("the quest's arrow is left alone", 0, game.hides);
		assertTrue(game.foreign);
	}

	@Test
	public void ourOwnArrowIsReplacedByTheNextOne()
	{
		arrow.point(TREE);

		assertTrue(arrow.point(OTHER_TREE));

		assertSame(OTHER_TREE, game.shown);
	}

	@Test
	public void clearTakesDownOurArrow()
	{
		arrow.point(TREE);

		arrow.clear();

		assertNull(game.shown);
		assertEquals(1, game.hides);
		assertFalse(arrow.active());
	}

	@Test
	public void clearWithNothingSetDoesNothing()
	{
		arrow.clear();

		assertEquals(0, game.hides);
	}

	@Test
	public void settingOffPointsNowhere()
	{
		enabled = false;

		assertFalse(arrow.point(TREE));

		assertNull(game.shown);
	}

	@Test
	public void turningTheSettingOffTakesTheArrowDown()
	{
		arrow.point(TREE);
		enabled = false;

		arrow.tick(3200, 3200, 0);

		assertNull(game.shown);
	}

	@Test
	public void walkingMoreThanTheDistanceAwayAfterArrivingClears()
	{
		arrow.point(TREE);

		arrow.tick(3205, 3200, 0);
		assertSame("near: stays", TREE, game.shown);
		arrow.tick(3200 + distance + 1, 3200, 0);

		assertNull(game.shown);
		assertFalse(arrow.active());
	}

	@Test
	public void exactlyTheDistanceAwayStays()
	{
		arrow.point(TREE);

		arrow.tick(3200, 3200, 0);
		arrow.tick(3200 + distance, 3200, 0);

		assertSame(TREE, game.shown);
	}

	@Test
	public void aFarTargetIsNotDroppedBeforeThePlayerEverGetsNear()
	{
		arrow.point(TREE);

		arrow.tick(3200 + 200, 3200, 0);

		assertSame(TREE, game.shown);
	}

	@Test
	public void theDistanceIsTheSetting()
	{
		distance = 5;
		arrow.point(TREE);

		arrow.tick(3202, 3200, 0);
		arrow.tick(3206, 3200, 0);

		assertNull(game.shown);
	}

	@Test
	public void anotherPlaneNeverClears()
	{
		arrow.point(TREE);

		arrow.tick(3200, 3200, 0);
		arrow.tick(3200, 3200, 1);

		assertSame(TREE, game.shown);
	}
}
