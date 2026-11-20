package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The Live cards after the work stops (Board, 2026-11-20): the Skilling card stays on its skill, marked Paused, with
 * its timers; the user's Clear empties it; and the all-time numbers keep growing and survive a logout. No Swing here.
 */
public class PausedCardTest
{
	private static final long T0 = 1_000_000_000L;
	private static final Skill[] WOODCUTTING = {Skill.WOODCUTTING};

	/** A session that took a baseline reading, then gained once at {@code T0 + gainAt}. */
	private static SkillingSession gained(Skill skill, long gainAt)
	{
		final SkillingSession s = new SkillingSession();
		s.setIdleMillis(60_000);
		s.xp(skill, 1_000, T0);
		s.xp(skill, 1_100, T0 + gainAt);
		return s;
	}

	// ---- The card stays ----

	@Test
	public void theCardKeepsItsSkillAfterTheWorkStopsUntilTheUserClearsIt()
	{
		final LiveCardHold hold = new LiveCardHold();
		assertNull("nothing trained yet: the empty card", hold.held(s -> true));
		hold.seen(Skill.WOODCUTTING);
		hold.seen(null);
		hold.seen(null);
		assertEquals("the badge went away, the card did not", Skill.WOODCUTTING, hold.held(s -> true));
		hold.clear();
		assertNull("Clear empties it", hold.held(s -> true));
		hold.seen(null);
		assertNull("and it stays empty until something is trained", hold.held(s -> true));
	}

	@Test
	public void aSkillTheBadgeIsNoLongerSetToShowIsNotHeld()
	{
		final LiveCardHold hold = new LiveCardHold();
		hold.seen(Skill.FISHING);
		assertNull(hold.held(s -> s != Skill.FISHING));
		assertEquals(Skill.FISHING, hold.held(s -> true));
	}

	@Test
	public void theCardFollowsTheLatestSkill()
	{
		final LiveCardHold hold = new LiveCardHold();
		hold.seen(Skill.MINING);
		hold.seen(Skill.FISHING);
		assertEquals(Skill.FISHING, hold.held(s -> true));
	}

	// ---- Timers ----

	@Test
	public void whileActiveItSaysHowLongItRunsAndWhenItPauses()
	{
		final SkillingSession s = gained(Skill.WOODCUTTING, 5_000);
		final CardTimers t = CardTimers.of(s, WOODCUTTING, T0 + 5_000 + 18_000, true, 0);
		assertFalse(t.paused);
		assertEquals("Active · running 18s", t.lines.get(0));
		assertEquals("Pauses in 0:42", t.lines.get(1));
		assertEquals(2, t.lines.size());
	}

	@Test
	public void afterTheIdleLimitItIsPausedAndSaysForHowLong()
	{
		final SkillingSession s = gained(Skill.WOODCUTTING, 5_000);
		final long now = T0 + 5_000 + 60_000 + 130_000;
		final CardTimers t = CardTimers.of(s, WOODCUTTING, now, false, 0);
		assertTrue(t.paused);
		assertEquals("Paused · ran 1m", t.lines.get(0));
		assertEquals("Paused for 2:10", t.lines.get(1));
	}

	@Test
	public void workThatStoppedBeforeTheIdleLimitIsPausedAtOnceAndSaysHowLongIdle()
	{
		final SkillingSession s = gained(Skill.WOODCUTTING, 5_000);
		final CardTimers t = CardTimers.of(s, WOODCUTTING, T0 + 5_000 + 20_000, false, 0);
		assertTrue("the badge is idle: the card is Paused even before the clock pauses", t.paused);
		assertEquals("Idle for 0:20", t.lines.get(1));
	}

	@Test
	public void theSessionResetCountdownShowsOnlyWhenTheSettingIsOn()
	{
		final SkillingSession s = gained(Skill.WOODCUTTING, 5_000);
		final long now = T0 + 5_000 + 120_000;
		assertEquals(2, CardTimers.of(s, WOODCUTTING, now, false, 0).lines.size());
		final CardTimers t = CardTimers.of(s, WOODCUTTING, now, false, 15);
		assertEquals("Session resets in 13:00", t.lines.get(t.lines.size() - 1));
	}

	@Test
	public void aHeldCardWithNothingGainedYetIsPausedAndSaysSo()
	{
		final CardTimers t = CardTimers.of(new SkillingSession(), WOODCUTTING, T0, false, 0);
		assertTrue(t.paused);
		assertEquals(1, t.lines.size());
		assertTrue(t.lines.get(0).startsWith("Paused"));
		final CardTimers working = CardTimers.of(new SkillingSession(), WOODCUTTING, T0, true, 0);
		assertFalse(working.paused);
		assertTrue("nothing to time before the first gain", working.lines.isEmpty());
	}

	@Test
	public void withThePauseSwitchedOffThereIsNoCountdown()
	{
		final SkillingSession s = gained(Skill.WOODCUTTING, 5_000);
		s.setIdleMillis(0);
		final CardTimers t = CardTimers.of(s, WOODCUTTING, T0 + 600_000, true, 0);
		assertFalse(t.paused);
		assertEquals(1, t.lines.size());
	}

	@Test
	public void theSessionResetsOnlyOnceItHasBeenIdleLongEnough()
	{
		final SkillingSession s = gained(Skill.WOODCUTTING, 5_000);
		final long gainAt = T0 + 5_000;
		assertFalse(CardTimers.resetDue(s, Skill.values(), gainAt + 14 * 60_000L, 15));
		assertTrue(CardTimers.resetDue(s, Skill.values(), gainAt + 15 * 60_000L, 15));
		assertFalse("0 means never", CardTimers.resetDue(s, Skill.values(), gainAt + 99 * 60_000L, 0));
		s.reset();
		assertFalse("nothing left to reset", CardTimers.resetDue(s, Skill.values(), gainAt + 99 * 60_000L, 15));
	}

	@Test
	public void clocksReadAsMinutesAndSeconds()
	{
		assertEquals("0:42", CardTimers.clock(42_000));
		assertEquals("12:50", CardTimers.clock(770_000));
		assertEquals("1:02:03", CardTimers.clock(3_723_000));
		assertEquals("0:00", CardTimers.clock(-5));
	}

	// ---- All time ----

	@Test
	public void allTimeGrowsOnlyWhileTrainingAndNotWhilePaused()
	{
		final SkillingSession total = gained(Skill.WOODCUTTING, 1_000);
		final AllTime all = new AllTime();
		long now = T0 + 1_000;
		all.tick(now, total);
		for (int i = 0; i < 3; i++)
		{
			now += 5_000;
			all.tick(now, total);
		}
		assertEquals(15_000, all.skill(Skill.WOODCUTTING, total).millis);
		// No gain for over a minute: the clock is paused, so nothing more is added.
		for (int i = 0; i < 20; i++)
		{
			now += 5_000;
			all.tick(now, total);
		}
		final long atPause = all.skill(Skill.WOODCUTTING, total).millis;
		assertTrue("grew until the pause, then stopped: " + atPause, atPause >= 60_000 && atPause <= 70_000);
		now += 5_000;
		all.tick(now, total);
		assertEquals(atPause, all.skill(Skill.WOODCUTTING, total).millis);
	}

	@Test
	public void aLongGapBetweenTicksIsNotCountedAsTraining()
	{
		final SkillingSession total = gained(Skill.WOODCUTTING, 1_000);
		final AllTime all = new AllTime();
		all.tick(T0 + 1_000, total);
		all.tick(T0 + 1_000 + AllTime.MAX_STEP_MILLIS + 1, total);
		assertEquals(0, all.skill(Skill.WOODCUTTING, total).millis);
	}

	@Test
	public void trainingTwoCombatSkillsAtOnceCountsTheCombatMinuteOnce()
	{
		final SkillingSession total = new SkillingSession();
		total.setIdleMillis(60_000);
		total.xp(Skill.ATTACK, 0, T0);
		total.xp(Skill.STRENGTH, 0, T0);
		total.xp(Skill.ATTACK, 40, T0 + 500);
		total.xp(Skill.STRENGTH, 40, T0 + 500);
		final AllTime all = new AllTime();
		all.tick(T0 + 1_000, total);
		all.tick(T0 + 6_000, total);
		assertEquals(5_000, all.skill(Skill.ATTACK, total).millis);
		assertEquals(5_000, all.skill(Skill.STRENGTH, total).millis);
		assertEquals(5_000, all.combat(total).millis);
		assertEquals(5_000, all.overall(total).millis);
		assertEquals(80, all.combat(total).xp);
	}

	@Test
	public void allTimeSurvivesALogoutAndTheNextLogin()
	{
		final SkillingSession total = gained(Skill.WOODCUTTING, 1_000);
		final AllTime all = new AllTime();
		all.tick(T0 + 1_000, total);
		all.tick(T0 + 6_000, total);

		all.commit(total);
		total.clear();
		final String stored = all.save(total);

		final AllTime next = new AllTime();
		next.load(stored);
		final SkillingSession fresh = new SkillingSession();
		final AllTime.Stats s = next.skill(Skill.WOODCUTTING, fresh);
		assertEquals(5_000, s.millis);
		assertEquals(100, s.xp);
		assertEquals(1, s.actions);
		assertEquals(5_000, next.overall(fresh).millis);

		// The next login adds on top.
		final SkillingSession login2 = gained(Skill.WOODCUTTING, 1_000);
		assertEquals(200, next.skill(Skill.WOODCUTTING, login2).xp);
		assertEquals(2, next.skill(Skill.WOODCUTTING, login2).actions);
	}

	@Test
	public void aSaveInTheMiddleOfALoginIsNotCountedTwiceAtLogout()
	{
		final SkillingSession total = gained(Skill.WOODCUTTING, 1_000);
		final AllTime all = new AllTime();
		final String midLogin = all.save(total);
		all.commit(total);
		total.clear();
		assertEquals(all.save(total), midLogin);
		final AllTime next = new AllTime();
		next.load(midLogin);
		assertEquals(100, next.skill(Skill.WOODCUTTING, new SkillingSession()).xp);
	}

	@Test
	public void numbersMadeBeforeTheProfileWasReadAreKept()
	{
		final AllTime all = new AllTime();
		final SkillingSession total = gained(Skill.WOODCUTTING, 1_000);
		all.tick(T0 + 1_000, total);
		all.tick(T0 + 3_000, total);
		all.load("WOODCUTTING=10000/500/7");
		final AllTime.Stats s = all.skill(Skill.WOODCUTTING, total);
		assertEquals(12_000, s.millis);
		assertEquals(600, s.xp);
		assertEquals(8, s.actions);
	}

	@Test
	public void aDamagedStoredTextIsSkippedNotCrashedOn()
	{
		final AllTime all = new AllTime();
		all.load("FOO=1/2/3;MINING=x/y;;=;WOODCUTTING=1000/50/2;garbage");
		all.load(null);
		all.load("");
		final AllTime.Stats s = all.skill(Skill.WOODCUTTING, new SkillingSession());
		assertEquals(1_000, s.millis);
		assertEquals(50, s.xp);
		assertTrue(all.skill(Skill.MINING, new SkillingSession()).isEmpty());
	}

	@Test
	public void clearingForAnotherAccountCarriesNothingOver()
	{
		final AllTime all = new AllTime();
		all.load("WOODCUTTING=1000/50/2;COMBAT=500/0/0;OVERALL=1000/0/0");
		all.clear();
		assertTrue(all.skill(Skill.WOODCUTTING, new SkillingSession()).isEmpty());
		assertEquals("", all.save(new SkillingSession()));
		assertFalse(all.isDirty());
	}

	// ---- The All time row ----

	@Test
	public void theAllTimeRowHasXpTimeAndTheAverageRate()
	{
		final LiveVitals.Period p = LiveVitals.allTime(new AllTime.Stats(3_600_000, 120_000, 400));
		assertEquals("All time", p.label);
		assertEquals("+120k xp", p.xp);
		assertEquals("1h 00m", p.time);
		assertEquals("120k/h", p.rate);
		assertTrue(p.tip, p.tip.contains("400 actions"));
	}

	@Test
	public void theAllTimeRowIsEmptyBeforeAnythingIsTrained()
	{
		final LiveVitals.Period p = LiveVitals.allTime(AllTime.Stats.NONE);
		assertEquals("All time", p.label);
		assertEquals("nothing yet", p.xp);
	}

	@Test
	public void noRateOverTheFirstMinute()
	{
		assertEquals("-", LiveVitals.allTime(new AllTime.Stats(30_000, 500, 3)).rate);
	}
}
