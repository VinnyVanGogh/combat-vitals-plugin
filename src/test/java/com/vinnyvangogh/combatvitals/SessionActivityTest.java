package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

public class SessionActivityTest
{
	private SessionActivity activity;

	@Before
	public void setUp()
	{
		activity = new SessionActivity();
	}

	@Test
	public void firstActivityIsNotAChange()
	{
		assertFalse(activity.observe(Skill.WOODCUTTING, "Yew logs", false));
	}

	@Test
	public void sameActivityNeverChanges()
	{
		activity.observe(Skill.WOODCUTTING, "Yew logs", true);
		assertFalse(activity.observe(Skill.WOODCUTTING, "Yew logs", true));
	}

	@Test
	public void newResourceIsAChangeOnlyWhenAutoResetIsOn()
	{
		activity.observe(Skill.WOODCUTTING, "Yew logs", true);
		assertTrue(activity.observe(Skill.WOODCUTTING, "Maple logs", true));
		assertFalse(activity.observe(Skill.WOODCUTTING, "Yew logs", false));
	}

	@Test
	public void newSkillIsAChange()
	{
		activity.observe(Skill.WOODCUTTING, "Yew logs", true);
		assertTrue(activity.observe(Skill.MINING, "Iron ore", true));
	}

	@Test
	public void unknownResourceDoesNotChangeIt()
	{
		activity.observe(Skill.WOODCUTTING, "Yew logs", true);
		assertFalse(activity.observe(Skill.WOODCUTTING, null, true));
		assertFalse(activity.observe(Skill.WOODCUTTING, "Yew logs", true));
	}

	@Test
	public void previousSummaryIsKeptThenExpires()
	{
		assertNull(activity.previous(0));
		activity.keepSummary("Yew logs: 120 chopped, 40k xp", 1000);
		assertEquals("Yew logs: 120 chopped, 40k xp", activity.previous(1000));
		assertEquals("Yew logs: 120 chopped, 40k xp", activity.previous(1000 + SessionActivity.SUMMARY_MILLIS - 1));
		assertNull(activity.previous(1000 + SessionActivity.SUMMARY_MILLIS));
	}

	@Test
	public void readsTheSkillAndResourceFromGatheringLines()
	{
		assertEquals(Skill.WOODCUTTING, SessionActivity.skillOf("You get some yew logs."));
		assertEquals("yew logs", SessionActivity.resourceOf("You get some yew logs."));
		assertEquals(Skill.MINING, SessionActivity.skillOf("You manage to mine some iron."));
		assertEquals("iron", SessionActivity.resourceOf("You manage to mine some iron."));
		assertEquals(Skill.FISHING, SessionActivity.skillOf("You catch a trout."));
		assertEquals("trout", SessionActivity.resourceOf("You catch a trout."));
		assertNull(SessionActivity.skillOf("You eat the shark."));
		assertNull(SessionActivity.resourceOf(null));
	}

	@Test
	public void clearForgetsTheActivity()
	{
		activity.observe(Skill.WOODCUTTING, "Yew logs", true);
		activity.clear();
		assertFalse(activity.observe(Skill.MINING, "Iron ore", true));
	}
}
