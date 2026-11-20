package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class ShootingStarsTest
{
	private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

	private static NextStars.Star star(int world, String region, int tier, int x, int y)
	{
		return new NextStars.Star(world, region, tier, x, y, 0, NOW.plusSeconds(600));
	}

	@Test
	public void maxTierFollowsMiningLevel()
	{
		assertEquals(0, ShootingStars.maxTier(9));
		assertEquals(1, ShootingStars.maxTier(10));
		assertEquals(5, ShootingStars.maxTier(59));
		assertEquals(9, ShootingStars.maxTier(99));
		assertEquals(70, ShootingStars.levelFor(7));
	}

	@Test
	public void layerPercentIsClamped()
	{
		assertEquals(0, ShootingStars.layerPercentLeft(5, 0));
		assertEquals(50, ShootingStars.layerPercentLeft(50, 100));
		assertEquals(100, ShootingStars.layerPercentLeft(500, 100));
		assertEquals(0, ShootingStars.layerPercentLeft(-5, 100));
	}

	@Test
	public void perHourAndTimeToNextLayer()
	{
		assertEquals(0, ShootingStars.perHour(10, Duration.ZERO));
		assertEquals(600, ShootingStars.perHour(10, Duration.ofMinutes(1)));
		assertNull(ShootingStars.timeToNextLayer(50, 0, Duration.ofMinutes(1)));
		assertNull(ShootingStars.timeToNextLayer(0, 10, Duration.ofMinutes(1)));
		assertEquals(Duration.ofMinutes(5), ShootingStars.timeToNextLayer(50, 10, Duration.ofMinutes(1)));
	}

	@Test
	public void filterKeepsSelectedRegionsAndTierWindow()
	{
		List<NextStars.Star> stars = Arrays.asList(
			star(301, "Varrock", 6, 3200, 3400),
			star(302, "Varrock", 5, 3200, 3400),
			star(303, "Varrock", 4, 3200, 3400),
			star(304, "Falador", 6, 3000, 3300),
			star(305, "Varrock", 9, 3200, 3400));
		List<NextStars.Star> out = NextStars.pick(stars, new HashSet<>(Collections.singletonList("varrock")),
			6, 3200, 3400, 0, NOW);
		assertEquals(2, out.size());
		assertTrue(out.get(0).world == 301 && out.get(1).world == 302);
	}

	@Test
	public void sortsByDistanceThenTierThenWorldAndCapsAtThree()
	{
		List<NextStars.Star> stars = Arrays.asList(
			star(310, "A", 5, 100, 100),
			star(311, "A", 5, 10, 10),
			star(312, "A", 6, 10, 10),
			star(313, "A", 5, 50, 50),
			star(314, "A", 5, 10, 10));
		List<NextStars.Star> out = NextStars.pick(stars, new HashSet<>(Collections.singletonList("A")),
			6, 0, 0, 0, NOW);
		assertEquals(3, out.size());
		assertEquals(312, out.get(0).world);
		assertEquals(311, out.get(1).world);
		assertEquals(314, out.get(2).world);
	}

	@Test
	public void dropsExpiredOtherPlaneAndNoSelection()
	{
		NextStars.Star expired = new NextStars.Star(320, "A", 5, 0, 0, 0, NOW.minusSeconds(1));
		NextStars.Star upstairs = new NextStars.Star(321, "A", 5, 0, 0, 1, NOW.plusSeconds(60));
		List<NextStars.Star> stars = Arrays.asList(expired, upstairs);
		assertTrue(NextStars.pick(stars, new HashSet<>(Collections.singletonList("A")), 5, 0, 0, 0, NOW).isEmpty());
		assertTrue(NextStars.pick(Collections.singletonList(star(322, "A", 5, 0, 0)),
			Collections.<String>emptySet(), 5, 0, 0, 0, NOW).isEmpty());
		assertTrue(NextStars.pick(Collections.singletonList(star(323, "A", 5, 0, 0)),
			new HashSet<>(Collections.singletonList("A")), 0, 0, 0, 0, NOW).isEmpty());
	}

	@Test
	public void motherlodeSackCountsDepositsOnRises()
	{
		MotherlodeSack s = new MotherlodeSack();
		s.sackChanged(10);
		assertEquals(0, s.deposits());
		s.sackChanged(35);
		assertEquals(1, s.deposits());
		assertEquals(25, s.paydirtDeposited());
		s.sackChanged(0);
		assertEquals(1, s.deposits());
		assertEquals(0, s.sack());
		assertEquals(81, MotherlodeSack.capacity(false));
		assertEquals(162, MotherlodeSack.capacity(true));
		assertTrue(MotherlodeSack.wouldOverflow(60, 28, false));
		assertTrue(!MotherlodeSack.wouldOverflow(60, 28, true));
		assertEquals(50, MotherlodeSack.percentFull(81, true));
	}
}
