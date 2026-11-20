package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StarSessionTest
{
	@Test
	public void tierFollowsObjectId()
	{
		assertEquals(9, StarSession.tierOfObject(41020));
		assertEquals(8, StarSession.tierOfObject(41021));
		assertEquals(7, StarSession.tierOfObject(41223));
		assertEquals(4, StarSession.tierOfObject(41226));
		assertEquals(1, StarSession.tierOfObject(41229));
		assertEquals(0, StarSession.tierOfObject(41230));
		assertEquals(0, StarSession.tierOfObject(1));
	}

	@Test
	public void firstReadingIsBaselineAndFallsAreNotLosses()
	{
		StarSession s = new StarSession();
		s.dustHeld(40);
		assertEquals(0, s.earned());
		s.dustHeld(55);
		assertEquals(15, s.earned());
		s.dustHeld(0);
		assertEquals(15, s.earned());
		s.dustHeld(10);
		assertEquals(25, s.earned());
	}

	@Test
	public void ratePerHourUsesMinedTime()
	{
		StarSession s = new StarSession();
		s.dustHeld(0);
		s.dustHeld(100);
		assertEquals(0, s.perHour());
		for (int i = 0; i < 100; i++)
		{
			s.tick(true);
			s.tick(false);
		}
		// 100 dust in 60 s mined = 6000/h
		assertEquals(6000, s.perHour());
	}

	@Test
	public void labelShowsTierDustAndMax()
	{
		StarSession s = new StarSession();
		s.dustHeld(0);
		s.dustHeld(1240);
		assertTrue(s.label(7, 85).startsWith("Star T7 | 1,240 dust"));
		assertTrue(s.label(7, 85).endsWith("max T8"));
		assertTrue(s.label(9, 70).contains("(needs 90)"));
	}

	@Test
	public void motherlodeLabel()
	{
		MotherlodeSack sack = new MotherlodeSack();
		sack.sackChanged(30);
		sack.sackChanged(60);
		assertEquals("Sack 60/81 | carrying 12 pay-dirt | 1 deposit | 2 nuggets", sack.label(12, false, 2));
		assertTrue(sack.label(30, false, 0).contains("(full)"));
		assertTrue(sack.label(30, true, 0).startsWith("Sack 60/162 |"));
	}
}
