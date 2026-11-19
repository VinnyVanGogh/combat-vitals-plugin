package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

/** Goal progress, ETA, daily and weekly periods, kill counts, and the saved form. */
public class GoalsTest
{
	private static final class Mem implements GoalBook.Storage
	{
		String text;
		int saves;
		int unsets;

		@Override
		public String load()
		{
			return text;
		}

		@Override
		public void save(String t)
		{
			saves++;
			text = t;
		}

		@Override
		public void unset()
		{
			unsets++;
			text = null;
		}
	}

	@Test
	public void anEmptyLoadFollowedByAnXpChangeWritesNothing()
	{
		final Mem mem = new Mem();
		final GoalBook book = new GoalBook(mem);
		book.load();
		assertTrue(book.goals().isEmpty());
		assertTrue(book.onXp("Mining", 5000, 0).isEmpty());
		assertTrue(book.onXp("Mining", 6000, 10).isEmpty());
		assertEquals(0, mem.saves);
		assertEquals(0, mem.unsets);
	}

	@Test
	public void aLoginReloadsOnlyWhenTheAccountChanges()
	{
		final Mem mem = new Mem();
		mem.text = Goal.encode(java.util.Collections.singletonList(
			new Goal(1, Goal.Type.ITEM, "x", 1, 0, 0, 0, false)));
		final GoalBook book = new GoalBook(mem);
		assertTrue(book.loadFor(42));
		assertEquals(1, book.goals().size());
		// A hop or loading screen fires LOGGED_IN again for the same account: the held goals stay.
		mem.text = null;
		assertFalse(book.loadFor(42));
		assertEquals(1, book.goals().size());
		// Another account loads its own.
		assertTrue(book.loadFor(43));
		assertTrue(book.goals().isEmpty());
		// A logout forgets which account was loaded, so the same account loads again.
		book.clear();
		assertTrue(book.loadFor(43));
	}

	@Test
	public void anXpChangeThatMeetsNothingWritesNothing()
	{
		final Mem mem = new Mem();
		final GoalBook book = new GoalBook(mem);
		book.addCounted(Goal.Type.ITEM, "Dragon pickaxe", 1, 0);
		final int before = mem.saves;
		assertTrue(book.onXp("Mining", 5000, 0).isEmpty());
		assertEquals(before, mem.saves);
	}

	@Test
	public void aMetGoalIsSavedAndOnlyRemovingTheLastGoalUnsetsTheKey()
	{
		final Mem mem = new Mem();
		final GoalBook book = new GoalBook(mem);
		book.onXp("Mining", 1000, 0);
		final Goal a = book.addSkill("Mining", false, 2000, 0);
		final Goal b = book.addCounted(Goal.Type.ITEM, "x", 1, 0);
		final int before = mem.saves;
		assertEquals(1, book.onXp("Mining", 2000, 10).size());
		assertEquals(before + 1, mem.saves);
		book.remove(a.id);
		assertEquals(0, mem.unsets);
		book.remove(b.id);
		assertEquals(1, mem.unsets);
	}

	@Test
	public void fractionRunsFromBaselineToGoal()
	{
		assertEquals(0.5, GoalMath.fraction(1000, 1500, 2000), 1e-9);
		assertEquals(0, GoalMath.fraction(1000, 900, 2000), 1e-9);
		assertEquals(1, GoalMath.fraction(1000, 2500, 2000), 1e-9);
		assertEquals(1, GoalMath.fraction(2000, 2000, 2000), 1e-9);
	}

	@Test
	public void etaFromRate()
	{
		// 5,000 xp in half an hour is 10,000 an hour; 5,000 left takes 30 minutes.
		assertEquals(10_000, GoalMath.perHour(5000, 1800), 1e-9);
		assertEquals(1800, GoalMath.etaSeconds(5000, 10_000));
		assertEquals(-1, GoalMath.etaSeconds(5000, 0));
		assertEquals(0, GoalMath.etaSeconds(0, 0));
		assertEquals(0, GoalMath.perHour(5000, 30), 0);
		assertEquals("30m", GoalMath.etaText(1800));
		assertEquals("2h 05m", GoalMath.etaText(7500));
		assertEquals("45s", GoalMath.etaText(45));
		assertEquals("no rate yet", GoalMath.etaText(-1));
	}

	@Test
	public void actionsLeftRoundsUp()
	{
		assertEquals(4, GoalMath.actionsLeft(1000, 250));
		assertEquals(5, GoalMath.actionsLeft(1001, 250));
		assertEquals(-1, GoalMath.actionsLeft(1000, 0));
		assertEquals(0, GoalMath.actionsLeft(0, 250));
	}

	@Test
	public void dailyAndWeeklyPeriodsStartAtUtcMidnightAndWednesday()
	{
		final long day = GoalMath.DAY_SECONDS;
		assertEquals(3 * day, GoalMath.dayStart(3 * day + 500));
		// Day 6 of the epoch (1970-01-07) was a Wednesday.
		assertEquals(6 * day, GoalMath.weekStart(6 * day + 100));
		assertEquals(6 * day, GoalMath.weekStart(12 * day + 86_399));
		assertEquals(13 * day, GoalMath.weekStart(13 * day));
		assertEquals(-day, GoalMath.weekStart(5 * day));
	}

	@Test
	public void levelGoalIsMetAtTheLevelsExperience()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.onXp("Woodcutting", 1_000_000, 0);
		book.addSkill("Woodcutting", true, 99, 0);
		// Level 99 is 13,034,431 xp.
		assertEquals(13_034_431, book.goals().get(0).goalValue());
		assertTrue(book.onXp("Woodcutting", 13_034_430, 10).isEmpty());
		final List<Goal> met = book.onXp("Woodcutting", 13_034_431, 20);
		assertEquals(1, met.size());
		// Said once: a later gain does not announce it again.
		assertTrue(book.onXp("Woodcutting", 13_100_000, 30).isEmpty());
	}

	@Test
	public void goalAlreadyMetWhenMadeIsNotAnnounced()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.onXp("Mining", 100_000, 0);
		book.addSkill("Mining", false, 50_000, 0);
		assertTrue(book.goals().get(0).reached);
		assertTrue(book.onXp("Mining", 100_500, 10).isEmpty());
	}

	@Test
	public void sessionRateComesFromTheFirstExperienceSeen()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.onXp("Fishing", 50_000, 1000);
		book.onXp("Fishing", 55_000, 2800);
		// 5,000 xp over 1,800 s.
		assertEquals(10_000, book.xpPerHour("Fishing", 2800), 1e-6);
		assertEquals(0, book.xpPerHour("Cooking", 2800), 0);
	}

	@Test
	public void bossKillCountGoalReadsTheChat()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.addCounted(Goal.Type.BOSS_KC, "Zulrah", 100, 0);
		assertEquals("Zulrah", GoalBook.killCountBoss("Your Zulrah kill count is: 99."));
		assertEquals(99, GoalBook.killCountValue("Your Zulrah kill count is: 99."));
		assertEquals(1234, GoalBook.killCountValue("Your Zulrah kill count is: 1,234."));
		assertNull(GoalBook.killCountBoss("You have been defeated."));
		assertEquals(-1, GoalBook.killCountValue("nothing here"));
		assertTrue(book.onKillCount("zulrah", 99).isEmpty());
		assertEquals(1, book.onKillCount("Zulrah", 100).size());
	}

	@Test
	public void countedGoalsAreTickedByHandAndNeverBelowZero()
	{
		final GoalBook book = new GoalBook(new Mem());
		final long id = book.addCounted(Goal.Type.ITEM, "Dragon pickaxe", 1, 0).id;
		book.bump(id, -5);
		assertEquals(0, book.goals().get(0).current);
		assertEquals(1, book.bump(id, 1).size());
	}

	@Test
	public void dailyGoalStartsAgainTheNextDay()
	{
		final GoalBook book = new GoalBook(new Mem());
		final long day = GoalMath.DAY_SECONDS;
		final Goal g = book.addCounted(Goal.Type.DAILY, "Birdhouse run", 1, 5 * day + 100);
		book.bump(g.id, 1);
		assertTrue(book.goals().get(0).reached);
		book.rollover(5 * day + 50_000);
		assertTrue(book.goals().get(0).reached);
		book.rollover(6 * day + 10);
		assertFalse(book.goals().get(0).reached);
		assertEquals(0, book.goals().get(0).current);
		assertEquals(6 * day, book.goals().get(0).periodStart);
	}

	@Test
	public void goalsSurviveSavingAndLoading()
	{
		final Mem mem = new Mem();
		final GoalBook a = new GoalBook(mem);
		a.onXp("Mining", 5000, 0);
		a.addSkill("Mining", true, 70, 0);
		a.addCounted(Goal.Type.ITEM, "Tab\tbed\nname", 3, 0);
		final GoalBook b = new GoalBook(mem);
		b.load();
		assertEquals(2, b.goals().size());
		assertEquals("Tab bed name", b.goals().get(1).label);
		assertEquals(5000, b.goals().get(0).baseline);
		// A new goal after a load does not reuse an id.
		assertEquals(3, b.addCounted(Goal.Type.WEEKLY, "x", 1, 0).id);
	}

	@Test
	public void aBrokenLineIsSkippedNotFatal()
	{
		final List<Goal> goals = Goal.decode("garbage\n1\tNOPE\tx\t1\t0\t0\t0\t0\n2\tITEM\ty\t4\t0\t1\t0\t0\n");
		assertEquals(1, goals.size());
		assertEquals(2, goals.get(0).id);
		assertTrue(Goal.decode(null).isEmpty());
	}
}
