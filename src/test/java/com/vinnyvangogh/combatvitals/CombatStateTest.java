package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.time.Instant;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import org.junit.Test;

/**
 * The four scenarios from the bug report, plus the attackability rule they
 * rest on. Pure: instants and booleans in, decisions out, no client.
 */
public class CombatStateTest
{
	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private static final Instant T0 = Instant.parse("2026-11-02T12:00:00Z");

	/** A fishing spot's menu options: Net, Bait, nothing else. Combat level -1. */
	private static final String[] FISHING_SPOT = {"Net", "Bait", null, null, null};

	/** A goblin: Attack, Talk-to. Combat level 2. */
	private static final String[] GOBLIN = {"Attack", null, "Talk-to", null, null};

	private static final String[] BANKER = {"Bank", "Talk-to", "Collect", null, null};

	@Test
	public void fishingSpotIsNotAttackable()
	{
		assertFalse(CombatState.attackable(FISHING_SPOT, -1));
	}

	@Test
	public void bankerIsNotAttackable()
	{
		assertFalse(CombatState.attackable(BANKER, -1));
	}

	@Test
	public void goblinIsAttackable()
	{
		assertTrue(CombatState.attackable(GOBLIN, 2));
	}

	@Test
	public void attackOptionAloneMakesAnNpcAttackable()
	{
		// Some attackable NPCs carry no combat level; the menu option still decides.
		assertTrue(CombatState.attackable(GOBLIN, -1));
	}

	@Test
	public void combatLevelAloneMakesAnNpcAttackable()
	{
		// A transformed or scripted NPC may hide Attack behind a different slot
		// or casing; a combat level is still proof it can be fought.
		assertTrue(CombatState.attackable(new String[] {null, null, null, null, null}, 50));
		assertTrue(CombatState.attackable(new String[] {"attack", null, null, null, null}, -1));
	}

	@Test
	public void nullActionsAreNotAttackable()
	{
		assertFalse(CombatState.attackable(null, -1));
		assertFalse(CombatState.attackable(null, 0));
	}

	@Test
	public void fishingSpotInteractionShowsNothing()
	{
		CombatState state = new CombatState();
		state.engage(CombatState.attackable(FISHING_SPOT, -1));

		assertFalse("no opponent readout for a skilling NPC", state.showTarget());
		assertFalse("no self bar for a skilling NPC", state.inCombat(T0, TIMEOUT));
		assertFalse("still nothing a few ticks later", state.inCombat(T0.plusSeconds(3), TIMEOUT));
	}

	@Test
	public void attackingAGoblinShowsTheBar()
	{
		CombatState state = new CombatState();
		state.engage(CombatState.attackable(GOBLIN, 2));

		assertTrue(state.showTarget());
		assertTrue(state.inCombat(T0, TIMEOUT));
	}

	@Test
	public void beingAttackedWhileSkillingShowsTheSelfBarOnly()
	{
		CombatState state = new CombatState();
		state.engage(CombatState.attackable(FISHING_SPOT, -1));
		state.hit(false, T0.plusSeconds(1));

		assertTrue("a hit on me is combat", state.inCombat(T0.plusSeconds(1), TIMEOUT));
		assertFalse("the fishing spot is still not an opponent", state.showTarget());
	}

	@Test
	public void beingAttackedWithNoTargetShowsTheSelfBar()
	{
		CombatState state = new CombatState();
		state.hit(false, T0);

		assertTrue(state.inCombat(T0.plusSeconds(4), TIMEOUT));
		assertFalse(state.showTarget());
	}

	@Test
	public void hitEvidenceExpiresAfterTheTimeout()
	{
		CombatState state = new CombatState();
		state.hit(false, T0);

		assertTrue(state.inCombat(T0.plus(TIMEOUT), TIMEOUT));
		assertFalse(state.inCombat(T0.plus(TIMEOUT).plusMillis(1), TIMEOUT));
	}

	@Test
	public void myHitOnTheTargetProvesItIsAttackable()
	{
		// Composition said no, but my damage landed on it: the evidence wins.
		CombatState state = new CombatState();
		state.engage(false);
		state.hit(true, T0.plusSeconds(1));

		assertTrue(state.showTarget());
	}

	@Test
	public void barExpiresAfterOpponentTimeout()
	{
		CombatState state = new CombatState();
		state.engage(true);
		state.disengage(T0.plusSeconds(1));

		Instant beforeTimeout = T0.plusSeconds(1).plus(TIMEOUT);
		assertFalse("not yet", state.expire(beforeTimeout, TIMEOUT, false));
		assertTrue("readout lingers until the timeout", state.showTarget());
		assertTrue(state.inCombat(beforeTimeout, TIMEOUT));

		Instant afterTimeout = beforeTimeout.plusMillis(1);
		assertTrue("the caller must drop the actor", state.expire(afterTimeout, TIMEOUT, false));
		assertFalse(state.showTarget());
		assertFalse(state.inCombat(afterTimeout, TIMEOUT));
	}

	@Test
	public void expiryWaitsWhileStillInteractingWithTheTarget()
	{
		CombatState state = new CombatState();
		state.engage(true);
		state.disengage(T0);

		Instant late = T0.plus(TIMEOUT).plusSeconds(10);
		assertFalse("the game still has us on the target", state.expire(late, TIMEOUT, true));
		assertTrue(state.showTarget());
	}

	@Test
	public void expiryDoesNotWaitForAnUnrelatedInteraction()
	{
		// The original bug: any interaction, a fishing spot included, kept the
		// old opponent alive. Only interacting with the target itself counts,
		// and that is the caller's comparison, so here it is simply false.
		CombatState state = new CombatState();
		state.engage(true);
		state.disengage(T0);

		assertTrue(state.expire(T0.plus(TIMEOUT).plusMillis(1), TIMEOUT, false));
		assertFalse(state.showTarget());
	}

	@Test
	public void reEngagingCancelsAPendingExpiry()
	{
		CombatState state = new CombatState();
		state.engage(true);
		state.disengage(T0);
		state.engage(true);

		assertFalse(state.expire(T0.plusSeconds(60), TIMEOUT, false));
		assertTrue(state.showTarget());
	}

	@Test
	public void switchingToAFishingSpotDropsTheOpponentReadout()
	{
		CombatState state = new CombatState();
		state.engage(true);
		state.engage(false);

		assertFalse(state.showTarget());
		assertFalse(state.inCombat(T0.plusSeconds(1), TIMEOUT));
	}

	@Test
	public void clearForgetsEverything()
	{
		CombatState state = new CombatState();
		state.engage(true);
		state.hit(true, T0);
		state.clear();

		assertFalse(state.showTarget());
		assertFalse(state.inCombat(T0, TIMEOUT));
		assertFalse(state.expire(T0.plusSeconds(60), TIMEOUT, false));
	}

	@Test
	public void damageAndBlocksAreCombatEvidence()
	{
		assertTrue("hit on me", CombatState.evidence(true, hitsplat(HitsplatID.DAMAGE_ME)));
		assertTrue("hit on me, typed as others", CombatState.evidence(true, hitsplat(HitsplatID.DAMAGE_OTHER)));
		assertTrue("blocked hit on me", CombatState.evidence(true, hitsplat(HitsplatID.BLOCK_OTHER)));
		assertTrue("max hit on me", CombatState.evidence(true, hitsplat(HitsplatID.DAMAGE_MAX_ME)));
		assertTrue("my hit on anyone", CombatState.evidence(false, hitsplat(HitsplatID.DAMAGE_ME)));
		assertTrue("my splash on anyone", CombatState.evidence(false, hitsplat(HitsplatID.BLOCK_ME)));
	}

	@Test
	public void poisonAndHealingOnMeAreNotCombat()
	{
		// Poison ticking while fishing must not pop the bar.
		assertFalse(CombatState.evidence(true, hitsplat(HitsplatID.POISON)));
		assertFalse(CombatState.evidence(true, hitsplat(HitsplatID.VENOM)));
		assertFalse(CombatState.evidence(true, hitsplat(HitsplatID.DISEASE)));
		assertFalse(CombatState.evidence(true, hitsplat(HitsplatID.HEAL)));
		assertFalse(CombatState.evidence(true, hitsplat(HitsplatID.PRAYER_DRAIN)));
	}

	@Test
	public void bystandersHittingEachOtherAreNotCombat()
	{
		assertFalse(CombatState.evidence(false, hitsplat(HitsplatID.DAMAGE_OTHER)));
		assertFalse(CombatState.evidence(false, hitsplat(HitsplatID.BLOCK_OTHER)));
		assertFalse(CombatState.evidence(false, hitsplat(HitsplatID.POISON)));
	}

	/** Only the type matters; {@code isMine()} and {@code isOthers()} derive from it. */
	private static Hitsplat hitsplat(int type)
	{
		return new Hitsplat()
		{
			@Override
			public int getHitsplatType()
			{
				return type;
			}

			@Override
			public int getAmount()
			{
				return 1;
			}

			@Override
			public int getDisappearsOnGameCycle()
			{
				return 0;
			}
		};
	}
}
