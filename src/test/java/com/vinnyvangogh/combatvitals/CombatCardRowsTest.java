package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** The combat card's layout: the crab is a section of the one card, never a second box. */
public class CombatCardRowsTest
{
	private static final long NOW = 1_000_000L;

	private static SplashReadout casting()
	{
		return new SplashReadout(null, "Fire Strike", "142 casts left", 142, false, new int[]{1}, new int[]{1},
			0, SplashTimer.STOP_MS, 60_000, NOW);
	}

	private static GemstoneCrab.Reading crab()
	{
		return new GemstoneCrab.Reading(GemstoneCrab.Phase.CRAB, 0.87, 4 * 60_000L, GemstoneCrab.Source.BAR, -1);
	}

	private static List<String> texts(List<SplashOverlay.Row> rows)
	{
		final List<String> out = new ArrayList<>();
		for (SplashOverlay.Row r : rows)
		{
			if (r.text != null)
			{
				out.add(r.text);
			}
		}
		return out;
	}

	private static List<SplashOverlay.Row> rows(SplashReadout r, GemstoneCrab.Reading crab, boolean splashing)
	{
		return SplashOverlay.rows(r, crab, false, splashing, NOW, Color.GREEN, 600_000L, id -> null);
	}

	@Test
	public void atTheCrabOneCardCarriesCrabAndCasts()
	{
		final List<String> t = texts(rows(casting(), crab(), false));
		assertEquals("Gemstone Crab", t.get(0));
		assertTrue(t.contains("Health 87%"));
		assertTrue(t.contains("Burrows in ~4:00"));
		assertFalse(t.contains("(health bar)"));
		assertTrue(t.contains("Fire Strike"));
		assertTrue(t.contains("142 casts left"));
		// The casting clocks stay on the one card.
		assertTrue(t.stream().anyMatch(s -> s.contains("Casting 1:00")));
	}

	@Test
	public void leavingTheCrabLeavesTheCastsCardAlone()
	{
		final List<String> t = texts(rows(casting(), null, false));
		assertEquals("Fire Strike", t.get(0));
		assertFalse(t.stream().anyMatch(s -> s.contains("Health") || s.contains("Burrow")));
		assertTrue(t.contains("142 casts left"));
	}

	@Test
	public void theHeaderNamesTheActiveMode()
	{
		assertEquals("Casting", rows(casting(), null, false).get(0).tag);
		assertEquals("Splashing mode", rows(casting(), null, true).get(0).tag);
		assertNull(rows(null, crab(), false).get(0).tag);
	}

	@Test
	public void aCrabOnlyCardShowsTheLogoutClock()
	{
		final List<String> t = texts(rows(null, crab(), false));
		assertTrue(t.contains("Logout in 10:00"));
	}

	@Test
	public void aBurrowedCrabSaysToCrawlThrough()
	{
		final GemstoneCrab.Reading shell = new GemstoneCrab.Reading(GemstoneCrab.Phase.SHELL, -1, 0,
			GemstoneCrab.Source.CLOCK, 90_000L);
		final List<String> t = texts(rows(casting(), shell, false));
		assertTrue(t.contains("Burrowed: crawl through the cave"));
		assertTrue(t.contains("Shell gone in 1:30"));
	}

	@Test
	public void aStoppedCastCardKeepsItsAfterimageLine()
	{
		final SplashReadout stopped = casting().stopped(NOW - 12_000);
		final List<String> t = texts(rows(stopped, null, false));
		assertTrue(t.contains("Stopped 12s ago"));
	}

	@Test
	public void theStandaloneCastsReadoutBecomesTheSameCardWithNoClocks()
	{
		final CastsReadout c = new CastsReadout("Fire Strike", "142 casts left", 142, false, new int[]{1}, new int[]{1});
		final SplashReadout card = SplashReadout.castsOnly(c, NOW);
		assertNull(card.splash);
		assertNull(card.logoutText(NOW));
		assertEquals(-1, card.session(NOW));
		assertTrue(card.logoutFraction(NOW) < 0);
		final List<SplashOverlay.Row> rows = rows(card, null, false);
		final List<String> t = texts(rows);
		assertEquals("Fire Strike", t.get(0));
		assertEquals("Casting", rows.get(0).tag);
		assertTrue(t.contains("142 casts left"));
		assertFalse(t.stream().anyMatch(s -> s.contains("Logout") || s.contains("Casting ")));
	}

	@Test
	public void lingerFollowsTheSameSettingInBothModes()
	{
		// The card's linger is decided by SplashTimer alone, whatever the mode.
		assertTrue(SplashTimer.lingers(29_000, 30, false));
		assertFalse(SplashTimer.lingers(31_000, 30, false));
		assertTrue(SplashTimer.lingers(10 * 60_000L, 30, true));
	}
}
