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
 * The scenarios from the brief: the badge comes up for a fishing spot and
 * not for a goblin, a drop fires the warning, and the source is only ever
 * something that was actually seen.
 */
public class SkillingStateTest
{
	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final Instant T0 = Instant.parse("2026-11-02T12:00:00Z");

	private static final String[] FISHING_SPOT = {"Net", "Bait", null, null, null};
	private static final String[] GOBLIN = {"Attack", null, "Talk-to", null, null};

	@Test
	public void fishingSpotRaisesTheFishingBadgeAndNoHealthBar()
	{
		CombatState combat = new CombatState();
		SkillingState skilling = new SkillingState();

		combat.engage(CombatState.attackable(FISHING_SPOT, -1));
		skilling.engage(SkillingActions.skill(FISHING_SPOT, "Fishing spot"));

		assertFalse("no health bar for a fishing spot", combat.showTarget());
		assertEquals(Skill.FISHING, skilling.skill());
	}

	@Test
	public void combatTargetRaisesTheHealthBarAndNoBadge()
	{
		CombatState combat = new CombatState();
		SkillingState skilling = new SkillingState();

		combat.engage(CombatState.attackable(GOBLIN, 2));
		// The plugin only engages the badge for a non-attackable NPC with a
		// recognised option, so for a goblin it is never engaged at all.
		assertNull(SkillingActions.skill(GOBLIN, "Goblin"));

		assertTrue(combat.showTarget());
		assertNull(skilling.skill());
	}

	@Test
	public void badgeLingersForTheTimeoutThenExpires()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.disengage(T0);

		assertFalse(state.expire(T0.plus(TIMEOUT), TIMEOUT, false));
		assertEquals(Skill.FISHING, state.skill());

		assertTrue(state.expire(T0.plus(TIMEOUT).plusMillis(1), TIMEOUT, false));
		assertNull(state.skill());
	}

	@Test
	public void badgeStaysWhileStillOnTheSpot()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.disengage(T0);

		assertFalse(state.expire(T0.plusSeconds(60), TIMEOUT, true));
		assertEquals(Skill.FISHING, state.skill());
	}

	@Test
	public void reEngagingCancelsAPendingExpiry()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.disengage(T0);
		state.engage(Skill.FISHING);

		assertFalse(state.expire(T0.plusSeconds(60), TIMEOUT, false));
	}

	@Test
	public void boostDroppingFiresTheWarningWhileTheBadgeIsUp()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);

		assertEquals(SkillingState.Warning.DROPPED,
			state.boostChanged(Skill.FISHING, BoostTracker.Change.DROP, 100));
		assertEquals("cue shows", SkillingState.Warning.DROPPED, state.warning(100));
		assertEquals("still inside the window", SkillingState.Warning.DROPPED,
			state.warning(100 + SkillingState.DROP_CUE_TICKS - 1));
		assertEquals("then clears", SkillingState.Warning.NONE,
			state.warning(100 + SkillingState.DROP_CUE_TICKS));
	}

	@Test
	public void boostExpiringFiresTheExpiryWarning()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);

		assertEquals(SkillingState.Warning.EXPIRED,
			state.boostChanged(Skill.FISHING, BoostTracker.Change.EXPIRED, 100));
		assertEquals(SkillingState.Warning.EXPIRED, state.warning(100 + SkillingState.EXPIRY_CUE_TICKS - 1));
		assertEquals(SkillingState.Warning.NONE, state.warning(100 + SkillingState.EXPIRY_CUE_TICKS));
	}

	@Test
	public void aDropInAnotherSkillIsNotThisBadgesWarning()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);

		assertEquals(SkillingState.Warning.NONE,
			state.boostChanged(Skill.WOODCUTTING, BoostTracker.Change.DROP, 100));
		assertEquals(SkillingState.Warning.NONE, state.warning(100));
	}

	@Test
	public void aDropWithNoBadgeUpIsNoWarning()
	{
		SkillingState state = new SkillingState();

		assertEquals(SkillingState.Warning.NONE,
			state.boostChanged(Skill.FISHING, BoostTracker.Change.DROP, 100));
	}

	@Test
	public void drinkingAPotionNamesIt()
	{
		SkillingState state = new SkillingState();
		state.consumed("Fishing potion(3)", 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 51);

		// Drink first, click the spot second: the usual order.
		state.engage(Skill.FISHING);
		assertEquals("Fishing potion", state.source());
	}

	@Test
	public void eatingAPieNamesIt()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.consumed("Admiral pie", 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 50);

		assertEquals("Admiral pie", state.source());
	}

	@Test
	public void aSpecialAttackNamesTheTool()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.specUsed("Dragon harpoon", Skill.FISHING, 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 51);

		assertEquals("Dragon harpoon special", state.source());
	}

	@Test
	public void aSpecialAttackIsOnlyCreditedToItsOwnSkill()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.specUsed("Dragon axe", Skill.WOODCUTTING, 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 51);

		assertNull("a Lumber Up does not explain a fishing boost", state.source());
	}

	@Test
	public void releaseDropsTheBadgeAtOnceButKeepsTheSource()
	{
		SkillingState state = new SkillingState();
		state.consumed("Fishing potion(3)", 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 50);
		state.engage(Skill.FISHING);
		state.release();

		assertNull("clicked a banker: no linger", state.skill());
		assertFalse(state.expire(T0.plusSeconds(60), TIMEOUT, false));

		state.engage(Skill.FISHING);
		assertEquals("back on the spot, the potion is still the source", "Fishing potion", state.source());
	}

	@Test
	public void theMoreRecentOfPotionAndSpecialWins()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.consumed("Fishing potion(4)", 48);
		state.specUsed("Dragon harpoon", Skill.FISHING, 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 50);

		assertEquals("Dragon harpoon special", state.source());
	}

	@Test
	public void aRiseWithNothingRecentHasNoSource()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.consumed("Fishing potion(3)", 10);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 10 + SkillingState.ATTRIBUTION_TICKS + 1);

		assertNull("too long ago to credit", state.source());
	}

	@Test
	public void expiryClearsTheSource()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.consumed("Fishing potion(3)", 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.DROP, 150);
		assertEquals("a drop keeps the source", "Fishing potion", state.source());

		state.boostChanged(Skill.FISHING, BoostTracker.Change.EXPIRED, 250);
		assertNull(state.source());
	}

	@Test
	public void sourceIsPerSkill()
	{
		SkillingState state = new SkillingState();
		state.consumed("Super energy(4)", 50);
		state.boostChanged(Skill.AGILITY, BoostTracker.Change.RISE, 50);

		state.engage(Skill.FISHING);
		assertNull("the fishing badge does not inherit another skill's source", state.source());
	}

	@Test
	public void theClickedOptionIsTheMethod()
	{
		SkillingState state = new SkillingState();
		// Menu click first, then the interaction starts on the next tick.
		state.clicked("Bait");
		state.engage(Skill.FISHING);

		assertEquals("Bait", state.method());
	}

	@Test
	public void aNewClickReplacesTheMethod()
	{
		SkillingState state = new SkillingState();
		state.clicked("Net");
		state.engage(Skill.FISHING);
		state.clicked("Bait");
		state.engage(Skill.FISHING);

		assertEquals("Bait", state.method());
	}

	@Test
	public void engagingWithoutAClickHasNoMethod()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);

		assertNull("no guess: the badge falls back to skill and level", state.method());
	}

	@Test
	public void aClickIsUsedOnce()
	{
		// Click Net, fish, the spot moves, and the game re-engages the new
		// spot without a click: the method is kept from the last click. But
		// a click that was never followed by an engagement on a skilling NPC
		// (clicked Bait, then clicked a banker) is forgotten.
		SkillingState state = new SkillingState();
		state.clicked("Net");
		state.engage(Skill.FISHING);
		state.engage(Skill.FISHING);
		assertEquals("Net", state.method());

		state.clicked("Bait");
		state.release();
		state.engage(Skill.FISHING);
		assertNull(state.method());
	}

	@Test
	public void theLastCatchIsRemembered()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		assertEquals(-1, state.lastCatch());

		state.caught(317);
		assertEquals(317, state.lastCatch());
		state.caught(321);
		assertEquals(321, state.lastCatch());
	}

	@Test
	public void theLastCatchSurvivesTheSpotMoving()
	{
		SkillingState state = new SkillingState();
		state.clicked("Net");
		state.engage(Skill.FISHING);
		state.caught(317);
		state.disengage(T0);
		state.engage(Skill.FISHING);

		assertEquals(317, state.lastCatch());
	}

	@Test
	public void releaseForgetsTheCatchAndMethod()
	{
		SkillingState state = new SkillingState();
		state.clicked("Net");
		state.engage(Skill.FISHING);
		state.caught(317);
		state.release();

		assertEquals(-1, state.lastCatch());
		assertNull(state.method());
	}

	@Test
	public void dosesAreStripped()
	{
		assertEquals("Fishing potion", SkillingState.stripDoses("Fishing potion(3)"));
		assertEquals("Fishing potion", SkillingState.stripDoses("Fishing potion (1)"));
		assertEquals("Admiral pie", SkillingState.stripDoses("Admiral pie"));
	}

	@Test
	public void clearForgetsEverything()
	{
		SkillingState state = new SkillingState();
		state.engage(Skill.FISHING);
		state.consumed("Fishing potion(3)", 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.RISE, 50);
		state.boostChanged(Skill.FISHING, BoostTracker.Change.DROP, 51);
		state.clicked("Net");
		state.caught(317);
		state.clear();

		assertNull(state.skill());
		assertNull(state.source());
		assertNull(state.method());
		assertEquals(-1, state.lastCatch());
		assertEquals(SkillingState.Warning.NONE, state.warning(51));
	}
}
