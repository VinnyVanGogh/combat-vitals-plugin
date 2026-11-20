package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;

/**
 * The Live section's text, built from the same readouts the overlays draw and
 * the session's counts: nothing here reads the client.
 */
public class LiveVitalsTest
{
	@Test
	public void yourVitalsReadAsTheOrbsDo()
	{
		final LiveVitals.Bar hp = LiveVitals.level("Hitpoints", 72, 99, LiveVitals.HITPOINTS);
		assertEquals("Hitpoints 72/99", hp.text);
		assertEquals(72 / 99.0, hp.fraction, 1e-9);
		// Boosted above the real level fills the bar, never past it.
		assertEquals(1.0, LiveVitals.level("Prayer", 120, 99, LiveVitals.PRAYER).fraction, 1e-9);
		assertEquals(-1, LiveVitals.level("Prayer", 0, 0, LiveVitals.PRAYER).fraction, 1e-9);
		// Run energy in hundredths, spec in tenths, as the client gives them.
		assertEquals("Run 64% | Spec 100%", LiveVitals.energy(6_480, 1_000));
		// The run toggle (OPTION_RUN) is said beside the energy, on or off.
		assertEquals("Run 64% on | Spec 100%", LiveVitals.energy(6_480, 1_000, true));
		assertEquals("Run 64% off | Spec 100%", LiveVitals.energy(6_480, 1_000, false));
	}

	@Test
	public void theTargetIsTheOverheadReadoutsTextAndFraction()
	{
		final CombatVitalsPlugin.Readout r = new CombatVitalsPlugin.Readout(null, "Goblin", 15, 30, 5, null, true,
			-1, null, DisplayMode.PERCENTAGE);
		final LiveVitals.Bar bar = LiveVitals.target(r, false);
		assertEquals(r.text(false), bar.text);
		assertEquals(0.5, bar.fraction, 1e-9);
		assertNull(LiveVitals.target(null, false));
	}

	@Test
	public void trainingShowsProgressThenTheRateOnceItExists()
	{
		final XpProgress xp = XpProgress.of(Experience.getXpForLevel(70) + 100);
		final LiveVitals.Bar bar = LiveVitals.training(Skill.ATTACK, xp, Color.RED);
		assertTrue(bar.text, bar.text.startsWith("Attack: Lv 70 -> 71"));
		assertNull("no rate in the first minute", LiveVitals.trainingRate(xp, -1));
		assertTrue(LiveVitals.trainingRate(xp, 45_200).startsWith("45.2k xp/h | lvl 71 in "));
	}

	@Test
	public void skillingIsTheBadgesOwnLines()
	{
		final SkillingReadout r = SkillingReadout.builder()
			.skill(Skill.WOODCUTTING).real(70).boosted(70).colour(Color.GREEN).header(true)
			.xp(XpProgress.of(Experience.getXpForLevel(70))).xpText("Lv 70 -> 71: 0%")
			.sessionCells(Arrays.asList(new SessionRow.Cell(SessionRow.Kind.COUNT, "24 chopped in 3m"),
				new SessionRow.Cell(SessionRow.Kind.XP_RATE, "60.1k xp/h")))
			.inventoryLabel("Inv 20/28 free")
			.containers(Collections.singletonList(new ContainerRow(SkillingContainer.LOG_BASKET, 12, 28)))
			.tickMillis(System.currentTimeMillis())
			.build();
		final LiveVitals.Skilling s = LiveVitals.skilling(r);
		assertEquals(SkillingBadgeRenderer.headerText(r), s.header);
		assertEquals("Lv 70 -> 71: 0%", s.xp.text);
		assertEquals("24 chopped in 3m", s.stats.get(SessionRow.Kind.COUNT));
		assertEquals("60.1k xp/h", s.stats.get(SessionRow.Kind.XP_RATE));
		assertNull("no rate yet, no tile", s.stats.get(SessionRow.Kind.ACTION_RATE));
		assertTrue("no method, no timers", s.chips.isEmpty());
		assertEquals(Arrays.asList("Inv 20/28 free", "Log basket 16/28 free"), s.lines);
	}

	@Test
	public void aMethodsTimersAreChipsNotTheInventoryLine()
	{
		final SkillingReadout r = SkillingReadout.builder()
			.skill(Skill.STRENGTH).real(85).boosted(85).colour(Color.RED).header(true)
			.trainingMethod(TrainingMethod.values()[0])
			.inventoryLabel("Active | Pump stops in 97:30")
			.tickMillis(System.currentTimeMillis())
			.build();
		final LiveVitals.Skilling s = LiveVitals.skilling(r);
		assertEquals(Arrays.asList("Active", "Pump stops in 97:30"), s.chips);
		assertTrue(s.lines.isEmpty());
	}

	@Test
	public void noBadgeIsNotSkilling()
	{
		final LiveVitals.Skilling s = LiveVitals.skilling(null);
		assertNull(s.header);
		assertNull(s.xp);
		assertTrue(s.lines.isEmpty());
	}

	@Test
	public void theSessionCountsXpAndLevelsFromTheFirstReading()
	{
		final SkillingSession s = new SkillingSession();
		final int start = Experience.getXpForLevel(10) - 50;
		s.xp(Skill.FISHING, start, 0);
		s.xp(Skill.FISHING, start + 40, 1_000);
		s.xp(Skill.FISHING, start + 100, 61_000);
		s.xp(Skill.ATTACK, 1_000, 0);
		assertEquals(100, s.xpGained(Skill.FISHING));
		assertEquals(1, s.levelsGained(Skill.FISHING));
		assertEquals("baseline only, no gain", 0, s.xpGained(Skill.ATTACK));
		final List<LiveVitals.Gain> gains = LiveVitals.session(s, 61_000, SkillColours::of);
		assertEquals("Attack's reading is a baseline, not a gain", 1, gains.size());
		assertEquals("Fishing: +100 xp, +1 lvl in 1m", gains.get(0).text);
		assertEquals(100, gains.get(0).xp);
		assertEquals(SkillColours.of(Skill.FISHING), gains.get(0).colour);

		s.reset();
		assertEquals(0, s.xpGained(Skill.FISHING));
		s.xp(Skill.FISHING, start + 130, 70_000);
		assertEquals("counted from the reset's total", 30, s.xpGained(Skill.FISHING));
		assertEquals(0, s.levelsGained(Skill.FISHING));
	}

	@Test
	public void theSessionRanksTheMostXpFirst()
	{
		final SkillingSession s = new SkillingSession();
		s.xp(Skill.FISHING, 1_000, 0);
		s.xp(Skill.FISHING, 1_050, 1_000);
		s.xp(Skill.ATTACK, 1_000, 0);
		s.xp(Skill.ATTACK, 1_900, 1_000);
		final List<LiveVitals.Gain> gains = LiveVitals.session(s, 2_000, SkillColours::of);
		assertEquals(900, gains.get(0).xp);
		assertTrue(gains.get(0).text.startsWith("Attack"));
		assertEquals(50, gains.get(1).xp);
	}

	@Test
	public void theCombatSkillIsTheOneGainedInLast()
	{
		final SkillingSession s = new SkillingSession();
		assertNull(s.latest(LiveVitals.COMBAT_SKILLS));
		for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.HITPOINTS})
		{
			s.xp(skill, 1_000, 0);
		}
		s.xp(Skill.ATTACK, 1_100, 1_000);
		s.xp(Skill.STRENGTH, 1_100, 2_000);
		s.xp(Skill.HITPOINTS, 1_100, 3_000);
		assertEquals("Hitpoints is never the style", Skill.STRENGTH, s.latest(LiveVitals.COMBAT_SKILLS));
		s.xp(Skill.ATTACK, 1_200, 4_000);
		assertEquals(Skill.ATTACK, s.latest(LiveVitals.COMBAT_SKILLS));
	}
}
