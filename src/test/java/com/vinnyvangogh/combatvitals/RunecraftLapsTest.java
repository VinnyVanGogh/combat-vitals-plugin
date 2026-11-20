package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

public class RunecraftLapsTest
{
	private static final Instant T0 = Instant.parse("2026-11-20T12:00:00Z");

	@Test
	public void emptyTrackerReportsZeroes()
	{
		RunecraftLaps l = new RunecraftLaps();
		assertEquals(0, l.laps());
		assertEquals(0, l.runesPerHour());
		assertEquals(Duration.ZERO, l.averageLap());
	}

	@Test
	public void zeroOrNegativeCraftIsIgnored()
	{
		RunecraftLaps l = new RunecraftLaps();
		l.crafted(T0, 0, 100);
		l.crafted(T0, -5, 100);
		assertEquals(0, l.laps());
		assertEquals(0, l.xp());
	}

	@Test
	public void ratesUseTimedLapsOnly()
	{
		RunecraftLaps l = new RunecraftLaps();
		l.start(T0);
		l.crafted(T0.plusSeconds(60), 100, 500);
		l.crafted(T0.plusSeconds(150), 100, 500);
		assertEquals(2, l.laps());
		assertEquals(200, l.runes());
		assertEquals(Duration.ofSeconds(90), l.lastLap());
		assertEquals(Duration.ofSeconds(60), l.bestLap());
		assertEquals(Duration.ofSeconds(75), l.averageLap());
		// 200 runes over 150s of lapping
		assertEquals(4800, l.runesPerHour());
		assertEquals(24000, l.xpPerHour());
	}

	@Test
	public void breakIsNotCountedAsLapTime()
	{
		RunecraftLaps l = new RunecraftLaps();
		l.start(T0);
		l.crafted(T0.plusSeconds(60), 100, 0);
		l.crafted(T0.plus(Duration.ofMinutes(30)), 100, 0);
		assertEquals(2, l.laps());
		assertEquals(Duration.ofSeconds(60), l.averageLap());
		assertTrue(l.runesPerHour() > 0);
	}

	@Test
	public void firstCraftWithoutStartIsUntimed()
	{
		RunecraftLaps l = new RunecraftLaps();
		l.crafted(T0, 50, 10);
		assertEquals(1, l.laps());
		assertEquals(0, l.runesPerHour());
	}

	@Test
	public void resetClearsEverything()
	{
		RunecraftLaps l = new RunecraftLaps();
		l.start(T0);
		l.crafted(T0.plusSeconds(30), 10, 10);
		l.reset();
		assertEquals(0, l.laps());
		assertEquals(0, l.runes());
		assertEquals(Duration.ZERO, l.bestLap());
	}
}
