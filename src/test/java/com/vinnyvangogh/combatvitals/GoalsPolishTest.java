package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/** The Goals polish: names and icons, typing, defaults, persistence retry, progress, Skilling tab, Max Hit setup. */
public class GoalsPolishTest
{
	private static class Mem implements GoalBook.Storage
	{
		String text;

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

	private static GoalsPage page(GoalBook book)
	{
		final GoalsPage[] out = new GoalsPage[1];
		SwingDriver.onEdt(() -> out[0] = new GoalsPage(book, s -> new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)));
		return out[0];
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

	private static List<String> texts(Component c)
	{
		final List<String> out = new ArrayList<>();
		labels(c, out);
		return out;
	}

	@Test
	public void theSkillDropdownShowsProperNamesAndIcons()
	{
		final GoalsPage p = page(new GoalBook(new Mem()));
		SwingDriver.onEdt(() ->
		{
			final JList<Skill> list = new JList<>();
			final JLabel row = (JLabel) p.skillBox().getRenderer().getListCellRendererComponent(list, Skill.ATTACK, 0,
				false, false);
			assertEquals("Attack", row.getText());
			assertNotNull(row.getIcon());
			final JLabel def = (JLabel) p.skillBox().getRenderer().getListCellRendererComponent(list, Skill.DEFENCE, 1,
				false, false);
			assertEquals("Defence", def.getText());
		});
	}

	@Test
	public void aTypedTargetIsTakenWithoutLeavingTheBox()
	{
		final Mem mem = new Mem();
		final GoalBook book = new GoalBook(mem);
		final GoalsPage p = page(book);
		SwingDriver.onEdt(() ->
		{
			p.skillBox().setSelectedItem(Skill.MINING);
			final JTextField field = ((javax.swing.JSpinner.DefaultEditor) p.targetBox().getEditor()).getTextField();
			// What a keystroke does: the text changes, nothing else is clicked.
			field.setText("60");
			p.clickAdd();
		});
		final List<Goal> goals = book.goals();
		assertEquals(1, goals.size());
		assertEquals(60, goals.get(0).target);
		assertEquals("Mining", goals.get(0).label);
	}

	@Test
	public void aTargetThatDoesNotParseAddsNothingAndNeverFallsBackTo99()
	{
		final GoalBook book = new GoalBook(new Mem());
		final GoalsPage p = page(book);
		SwingDriver.onEdt(() ->
		{
			final JTextField field = ((javax.swing.JSpinner.DefaultEditor) p.targetBox().getEditor()).getTextField();
			field.setText("six");
			p.clickAdd();
		});
		assertTrue(book.goals().isEmpty());
		assertFalse(p.problemText().trim().isEmpty());
	}

	@Test
	public void theTargetStartsFiveLevelsAboveTheCurrentOneCappedAt99()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.onXp("Mining", Experience.getXpForLevel(50), 0);
		book.onXp("Fishing", Experience.getXpForLevel(97), 0);
		final GoalsPage p = page(book);
		SwingDriver.onEdt(() ->
		{
			p.skillBox().setSelectedItem(Skill.MINING);
			assertEquals(55, ((Number) p.targetBox().getValue()).intValue());
			p.skillBox().setSelectedItem(Skill.FISHING);
			assertEquals(99, ((Number) p.targetBox().getValue()).intValue());
			p.kindBox().setSelectedIndex(1);
			assertEquals(Experience.getXpForLevel(99), ((Number) p.targetBox().getValue()).intValue());
		});
		assertEquals(60, GoalMath.defaultLevel(55, 5));
		assertEquals(99, GoalMath.defaultLevel(99, 5));
	}

	@Test
	public void goalsLoadedAfterTheProfileIsReadyAreNotLost()
	{
		final Mem mem = new Mem();
		final GoalBook book = new GoalBook(mem);
		final Mem later = new Mem();
		later.text = Goal.encode(Collections.singletonList(new Goal(1, Goal.Type.ITEM, "x", 1, 0, 0, 0, false)));
		// LOGGED_IN comes before the profile key: the read is empty and must not count as loaded.
		assertTrue(book.loadFor(7));
		assertTrue(book.needsLoad());
		assertTrue(book.goals().isEmpty());
		mem.text = later.text;
		assertTrue(book.loadFor(7));
		assertFalse(book.needsLoad());
		assertEquals(1, book.goals().size());
		// Once loaded, a repeat for the same account is left alone.
		mem.text = null;
		assertFalse(book.loadFor(7));
		assertEquals(1, book.goals().size());
	}

	@Test
	public void aGoalAddedBeforeTheProfileAnswersSurvivesTheRetry()
	{
		// The profile is not ready: reads come back empty and writes go nowhere.
		final GoalBook book = new GoalBook(new Mem()
		{
			@Override
			public void save(String t)
			{
			}
		});
		book.loadFor(7);
		book.addCounted(Goal.Type.ITEM, "Whip", 1, 0);
		// The retry reads nothing again: the held goal stays.
		assertFalse(book.loadFor(7));
		assertEquals(1, book.goals().size());
	}

	@Test
	public void goalsSurviveARestartThroughTheStorage()
	{
		final Mem mem = new Mem();
		final GoalBook first = new GoalBook(mem);
		first.loadFor(9);
		first.addSkill("Mining", true, 70, 0);
		final GoalBook second = new GoalBook(mem);
		assertTrue(second.loadFor(9));
		assertEquals(1, second.goals().size());
		assertEquals("Mining", second.goals().get(0).label);
		assertEquals(70, second.goals().get(0).target);
	}

	@Test
	public void progressGivesXpLeftPercentRatesAndActions()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.actionXp(skill -> "Mining".equals(skill) ? 50 : 0);
		book.onXp("Mining", 100_000, 0);
		final Goal g = book.addSkill("Mining", false, 200_000, 0);
		book.onXp("Mining", 110_000, 1800);
		final GoalBook.Progress p = book.progress(g, 1800);
		assertEquals(90_000, p.left);
		assertEquals(10, p.percent);
		assertEquals(20_000, p.sessionRate, 1);
		assertEquals(20_000, p.hourRate, 1);
		assertEquals(1800, p.actionsLeft);
		assertEquals(50, p.actionXp, 0.001);
		final String line = GoalMath.summary(p);
		assertTrue(line, line.contains("90,000 xp to go (10%)"));
		assertTrue(line, line.contains("1,800 actions left"));
		assertTrue(line, line.contains("(session)") && line.contains("(last hour)"));
	}

	@Test
	public void theLastHourRateForgetsXpOlderThanAnHour()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.onXp("Mining", 0, 0);
		book.onXp("Mining", 100_000, 10);
		// Two hours of nothing, then a little: the last hour holds only the little.
		book.onXp("Mining", 100_000, 7200);
		book.onXp("Mining", 101_000, 7260);
		final double hour = book.xpLastHour("Mining", 7260);
		assertEquals(1000.0 * 3600 / 3600, hour, 1.0);
		assertTrue(book.xpPerHour("Mining", 7260) > 0);
	}

	@Test
	public void noActionYetMeansNoActionCount()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.onXp("Mining", 100, 0);
		final Goal g = book.addSkill("Mining", true, 50, 0);
		assertEquals(-1, book.progress(g, 10).actionsLeft);
	}

	@Test
	public void theSkillingTabListsTheActiveSkillsUnmetGoalsOnly()
	{
		final GoalBook book = new GoalBook(new Mem());
		book.onXp("Mining", 100_000, 0);
		book.onXp("Fishing", 100, 0);
		book.addSkill("Mining", true, 80, 0);
		book.addSkill("Fishing", true, 50, 0);
		book.addCounted(Goal.Type.ITEM, "Mining thing", 1, 0);
		final List<String> lines = book.skillLines("Mining", 10);
		assertEquals(1, lines.size());
		assertTrue(lines.get(0), lines.get(0).startsWith("Goal Mining level 80: "));
		assertTrue(book.skillLines("Smithing", 10).isEmpty());
	}

	@Test
	public void theLiveSectionShowsTheGoalLines()
	{
		final LiveVitals.Skilling s = new LiveVitals.Skilling("Mining 72", java.awt.Color.GREEN, null, null,
			Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
			Collections.singletonList(new LiveVitals.GoalLine("Mining level 80",
				new LiveVitals.Bar(0.5, "50%", java.awt.Color.ORANGE), "5,000 xp to go · 2h 05m")));
		final LiveSection[] section = new LiveSection[1];
		SwingDriver.onEdt(() ->
		{
			section[0] = new LiveSection(PanelFixture.store());
			section[0].update(new LiveVitals(true, null, s, Collections.emptyList()));
			assertTrue(texts(section[0]).stream().anyMatch(t -> t.equals("Mining level 80")));
			assertTrue(texts(section[0]).stream().anyMatch(t -> t.equals("5,000 xp to go · 2h 05m")));
			assertTrue(texts(section[0]).stream().noneMatch(t -> t.startsWith("Goal ")));
		});
	}

	@Test
	public void maxHitShowsTheWornItemsAndLevelsAndOnlyRefillsOnAChange()
	{
		final MaxHitPage.Snapshot a = new MaxHitPage.Snapshot();
		a.attack = 99;
		a.strength = 118;
		a.itemIds[3] = 4151;
		a.itemNames[3] = "Abyssal whip";
		a.itemIcons[3] = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		final MaxHitPage[] page = new MaxHitPage[1];
		SwingDriver.onEdt(() ->
		{
			page[0] = new MaxHitPage(done -> done.accept(a));
			page[0].fill(a);
			assertEquals(a.setupKey(), page[0].lastSetup());
			final List<String> t = texts(page[0]);
			assertTrue(t.toString(), t.contains("Str 118"));
			assertTrue(t.toString(), t.contains("Att 99"));
		});
		final MaxHitPage.Snapshot b = new MaxHitPage.Snapshot();
		b.attack = 99;
		b.strength = 118;
		b.itemIds[3] = 4151;
		assertEquals(a.setupKey(), b.setupKey());
		b.itemIds[3] = 11802;
		assertFalse(a.setupKey().equals(b.setupKey()));
	}
}
