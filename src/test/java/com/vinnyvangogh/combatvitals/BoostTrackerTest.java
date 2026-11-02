package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

public class BoostTrackerTest
{
	@Test
	public void firstUpdateOnlySeeds()
	{
		BoostTracker tracker = new BoostTracker();
		assertFalse(tracker.isSeeded(Skill.FISHING));

		assertEquals(BoostTracker.Change.NONE, tracker.observe(Skill.FISHING, 76, 79));
		assertTrue(tracker.isSeeded(Skill.FISHING));
	}

	@Test
	public void drinkingAPotionIsARise()
	{
		BoostTracker tracker = new BoostTracker();
		tracker.seed(Skill.FISHING, 76);

		assertEquals(BoostTracker.Change.RISE, tracker.observe(Skill.FISHING, 76, 79));
	}

	@Test
	public void theTimerDroppingOneIsADrop()
	{
		BoostTracker tracker = new BoostTracker();
		tracker.seed(Skill.FISHING, 79);

		assertEquals(BoostTracker.Change.DROP, tracker.observe(Skill.FISHING, 76, 78));
		assertEquals(BoostTracker.Change.DROP, tracker.observe(Skill.FISHING, 76, 77));
		assertEquals("the last drop reaches base", BoostTracker.Change.EXPIRED,
			tracker.observe(Skill.FISHING, 76, 76));
	}

	@Test
	public void noChangeIsNothing()
	{
		BoostTracker tracker = new BoostTracker();
		tracker.seed(Skill.FISHING, 79);

		assertEquals(BoostTracker.Change.NONE, tracker.observe(Skill.FISHING, 76, 79));
	}

	@Test
	public void aFallBelowBaseIsNotTheDrainTimer()
	{
		BoostTracker tracker = new BoostTracker();
		tracker.seed(Skill.FISHING, 76);

		assertEquals("drained by something", BoostTracker.Change.NONE,
			tracker.observe(Skill.FISHING, 76, 75));
		assertEquals("restoring up from below base is not a boost", BoostTracker.Change.NONE,
			tracker.observe(Skill.FISHING, 76, 76));
	}

	@Test
	public void aLargerFallIsNotTheDrainTimer()
	{
		// A world hop to free-to-play resets a boost in one step.
		BoostTracker tracker = new BoostTracker();
		tracker.seed(Skill.FISHING, 79);

		assertEquals(BoostTracker.Change.NONE, tracker.observe(Skill.FISHING, 76, 76));
	}

	@Test
	public void clearForgetsTheLevels()
	{
		BoostTracker tracker = new BoostTracker();
		tracker.seed(Skill.FISHING, 79);
		tracker.clear();

		assertFalse(tracker.isSeeded(Skill.FISHING));
		assertEquals(BoostTracker.Change.NONE, tracker.observe(Skill.FISHING, 76, 78));
	}
}
