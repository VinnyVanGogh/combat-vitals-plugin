package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import net.runelite.api.Skill;
import org.junit.Test;

/** Auto-pause when idle, session against total across activity switches, and the goal line on the Combat card. */
public class PauseAndTotalsTest
{
	private static final long SECOND = 1000;

	private static SkillingSession mining(long idleSeconds)
	{
		final SkillingSession s = new SkillingSession();
		s.setIdleMillis(idleSeconds * SECOND);
		s.xp(Skill.MINING, 1000, 0);
		s.xp(Skill.MINING, 1050, 1 * SECOND);
		return s;
	}

	@Test
	public void theClockRunsWhileGainsKeepComing()
	{
		final SkillingSession s = mining(60);
		s.xp(Skill.MINING, 1100, 31 * SECOND);
		assertEquals(30 * SECOND, s.elapsed(Skill.MINING, 31 * SECOND));
		assertFalse(s.paused(Skill.MINING, 31 * SECOND));
	}

	@Test
	public void theClockStopsAtTheIdleLimitAndStaysStopped()
	{
		final SkillingSession s = mining(30);
		// Last gain at 1 s; at 31 s the limit is reached, and the clock reads the same ever after.
		assertEquals(30 * SECOND, s.elapsed(Skill.MINING, 31 * SECOND));
		assertEquals(30 * SECOND, s.elapsed(Skill.MINING, 400 * SECOND));
		assertTrue(s.paused(Skill.MINING, 400 * SECOND));
	}

	@Test
	public void theClockResumesWithTheNextGainAndLeavesThePauseOut()
	{
		final SkillingSession s = mining(30);
		s.xp(Skill.MINING, 1100, 401 * SECOND);
		assertFalse(s.paused(Skill.MINING, 401 * SECOND));
		// 30 s active before the stop, none during the pause.
		assertEquals(30 * SECOND, s.elapsed(Skill.MINING, 401 * SECOND));
		assertEquals(50 * SECOND, s.elapsed(Skill.MINING, 421 * SECOND));
		// Quiet again: it stops 30 s after the last gain, at 60 s of active time.
		assertEquals(60 * SECOND, s.elapsed(Skill.MINING, 461 * SECOND));
		assertEquals(60 * SECOND, s.elapsed(Skill.MINING, 900 * SECOND));
	}

	@Test
	public void ratesLeaveThePausedTimeOut()
	{
		final SkillingSession s = mining(60);
		// Gaps of 120 s and 879 s with a 60 s limit: 60 s and 819 s of them paused, 120 s of active time in all.
		s.xp(Skill.MINING, 1150, 121 * SECOND);
		s.xp(Skill.MINING, 1250, 1_000 * SECOND);
		final long active = s.elapsed(Skill.MINING, 1_000 * SECOND);
		assertEquals(120 * SECOND, active);
		assertEquals((int) (200L * 3_600_000 / active), s.xpPerHour(Skill.MINING, 1_000 * SECOND));
	}

	@Test
	public void aZeroLimitNeverPauses()
	{
		final SkillingSession s = mining(0);
		assertEquals(10_000 * SECOND - SECOND, s.elapsed(Skill.MINING, 10_000 * SECOND));
		assertFalse(s.paused(Skill.MINING, 10_000 * SECOND));
	}

	@Test
	public void aResetStartsTheSessionOverButTheTotalKeepsGoing()
	{
		final SkillingSession session = mining(60);
		final SkillingSession total = mining(60);
		session.xp(Skill.MINING, 1100, 10 * SECOND);
		total.xp(Skill.MINING, 1100, 10 * SECOND);
		// A new activity: the session resets, the total does not.
		session.reset();
		session.xp(Skill.FISHING, 500, 11 * SECOND);
		session.xp(Skill.FISHING, 540, 12 * SECOND);
		total.xp(Skill.FISHING, 500, 11 * SECOND);
		total.xp(Skill.FISHING, 540, 12 * SECOND);
		assertEquals(0, session.xpGained(Skill.MINING));
		assertEquals(100, total.xpGained(Skill.MINING));
		assertEquals(40, session.xpGained(Skill.FISHING));
		assertEquals(40, total.xpGained(Skill.FISHING));
		final List<LiveVitals.Gain> sessionList = LiveVitals.session(session, 12 * SECOND, s -> Color.WHITE);
		final List<LiveVitals.Gain> totalList = LiveVitals.session(total, 12 * SECOND, s -> Color.WHITE);
		assertEquals(1, sessionList.size());
		assertEquals(2, totalList.size());
	}

	@Test
	public void thePeriodLinesShowSessionAndTotalAndSayPaused()
	{
		final SkillingSession session = mining(30);
		final SkillingSession total = mining(30);
		session.reset();
		final List<LiveVitals.Period> lines = LiveVitals.periods(session, total, new Skill[]{Skill.MINING}, 400 * SECOND);
		assertEquals("This session", lines.get(0).label);
		assertEquals("nothing yet", lines.get(0).xp);
		assertEquals("Total", lines.get(1).label);
		assertEquals("+50 xp", lines.get(1).xp);
		assertEquals("30s", lines.get(1).time);
		assertEquals("paused", lines.get(1).rate);
		assertEquals("Total: +50 xp in 30s (paused)", lines.get(1).tip);
	}

	@Test
	public void thePeriodRowsHaveAnXpPerHourColumnOnceThereIsAMinuteToDivideBy()
	{
		final SkillingSession session = new SkillingSession();
		session.setIdleMillis(0);
		// The clock starts at the first gain (1 s), not the login reading, and never pauses with a zero limit.
		session.xp(Skill.MINING, 1_000, 0);
		session.xp(Skill.MINING, 2_000, 1 * SECOND);
		session.xp(Skill.MINING, 4_000, 121 * SECOND);
		final LiveVitals.Period p = LiveVitals.periods(session, session, new Skill[]{Skill.MINING}, 121 * SECOND)
			.get(0);
		assertEquals("+3k xp", p.xp);
		assertEquals("2m", p.time);
		assertEquals("90k/h", p.rate);
		assertTrue(p.tip, p.tip.endsWith(", 90,000 xp/h"));
	}

	private static void labels(Component c, List<String> out)
	{
		if (c instanceof JLabel)
		{
			out.add(((JLabel) c).getText());
		}
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				labels(child, out);
			}
		}
	}

	@Test
	public void aMagicGoalShowsOnTheCombatCard()
	{
		final GoalBook book = new GoalBook(new GoalsPolishTestStore());
		book.onXp("Magic", 100_000, 0);
		book.onXp("Mining", 100_000, 0);
		book.addSkill("Magic", true, 99, 0);
		book.addSkill("Mining", true, 99, 0);
		final List<LiveVitals.GoalLine> lines = book.skillGoals(Arrays.asList("Attack", "Magic"), 10);
		assertEquals(1, lines.size());
		assertEquals("Magic level 99", lines.get(0).title);
		assertTrue(lines.get(0).bar.text, lines.get(0).bar.text.startsWith("Magic 99 · "));
		assertTrue(lines.get(0).tooltip, lines.get(0).tooltip.contains(" xp to go"));

		final LiveVitals.Bar bar = new LiveVitals.Bar(0.5, "Hitpoints 50/99", Color.RED);
		final LiveVitals.Combat combat = new LiveVitals.Combat(bar, bar, null, null, null, null, null)
			.with(lines, Arrays.asList(LiveVitals.Period.nothingYet("This session"),
				LiveVitals.Period.nothingYet("Total")));
		final LiveSection[] section = new LiveSection[1];
		SwingDriver.onEdt(() ->
		{
			section[0] = new LiveSection(PanelFixture.store());
			section[0].update(new LiveVitals(true, combat, null, Collections.emptyList()));
			final List<String> texts = new ArrayList<>();
			labels(section[0], texts);
			assertTrue(texts.toString(), texts.stream().anyMatch(t -> t.startsWith("Magic 99 · ")));
			assertTrue(texts.toString(), texts.stream().anyMatch(t -> t.equals("Total")));
			assertTrue(texts.toString(), texts.stream().anyMatch(t -> t.equals("nothing yet")));
		});
	}

	/** An in-memory goal store. */
	private static final class GoalsPolishTestStore implements GoalBook.Storage
	{
		private String text;

		@Override
		public String load()
		{
			return text;
		}

		@Override
		public void save(String t)
		{
			text = t;
		}

		@Override
		public void unset()
		{
			text = null;
		}
	}
}
