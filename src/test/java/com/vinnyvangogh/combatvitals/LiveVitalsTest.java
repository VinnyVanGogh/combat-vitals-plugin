package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
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
		assertEquals(Arrays.asList("24 chopped in 3m | 60.1k xp/h", "Inv 20/28 free", "Log basket 16/28 free"),
			s.lines);
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
		assertEquals(Collections.singletonList("Fishing: +100 xp, +1 lvl in 1m"), LiveVitals.session(s, 61_000));

		s.reset();
		assertEquals(0, s.xpGained(Skill.FISHING));
		s.xp(Skill.FISHING, start + 130, 70_000);
		assertEquals("counted from the reset's total", 30, s.xpGained(Skill.FISHING));
		assertEquals(0, s.levelsGained(Skill.FISHING));
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
