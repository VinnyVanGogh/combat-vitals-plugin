package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import net.runelite.client.game.FishingSpot;
import org.junit.Test;

public class KnownFishingSpotsTest
{
	@Test
	public void parsesRowsAndSkipsCommentsBlanksAndBadLines() throws Exception
	{
		final String tsv = "# comment\n"
			+ "\n"
			+ "LOBSTER\t2599\t3419\t0\tmembers\t68\tFishing Guild\n"
			+ "SHRIMP\t3246\t3155\t0\tfree\t0\tLumbridge Swamp\n"
			+ "NOT_A_SPOT\t1\t2\t0\tfree\t0\tNowhere\n"
			+ "SHRIMP\tx\t3155\t0\tfree\t0\tBroken\n"
			+ "SHRIMP\t1\t2\n";

		final List<NextFishingSpot.Spot> spots = KnownFishingSpots.parse(new BufferedReader(new StringReader(tsv)));

		assertEquals(2, spots.size());
		final NextFishingSpot.Spot guild = spots.get(0);
		assertEquals(FishingSpot.LOBSTER, guild.getType());
		assertEquals(2599, guild.getX());
		assertEquals(3419, guild.getY());
		assertTrue(guild.isMembers());
		assertEquals(68, guild.getMinLevel());
		assertEquals("Fishing Guild", guild.getName());
		assertFalse(spots.get(1).isMembers());
	}

	@Test
	public void bundledListLoadsWithEverySupportedType()
	{
		final List<NextFishingSpot.Spot> spots = KnownFishingSpots.load();

		assertNotNull(spots);
		for (FishingSpot type : new FishingSpot[]{FishingSpot.SHRIMP, FishingSpot.SALMON, FishingSpot.LOBSTER,
			FishingSpot.SHARK, FishingSpot.BARB_FISH})
		{
			assertTrue(type + " has a bundled location", spots.stream().anyMatch(s -> s.getType() == type));
		}
	}

	@Test
	public void bundledListKeepsOutTheWildernessAndInstances()
	{
		for (NextFishingSpot.Spot spot : KnownFishingSpots.load())
		{
			final boolean wilderness = spot.getX() >= 2944 && spot.getX() <= 3392 && spot.getY() >= 3520 && spot.getY() <= 4000;
			assertFalse(spot.getName() + " is in the Wilderness", wilderness);
			assertTrue(spot.getName() + " is not on the surface", spot.getY() < 4000);
		}
	}
}
