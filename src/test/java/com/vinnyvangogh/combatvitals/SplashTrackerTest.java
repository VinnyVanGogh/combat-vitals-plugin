package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SplashTrackerTest
{
	private static final Map<Integer, MonsterMagic> MONSTERS = new HashMap<>();

	static
	{
		MONSTERS.put(1, new MonsterMagic(1, 0, "Rat"));
	}

	private static SplashTracker.Input input(int spell, int gear, int index, int npcId, int bonus)
	{
		return new SplashTracker.Input(spell, gear, index, npcId, "Rat", bonus, 99, 1.0);
	}

	@Test
	public void aBonusOfMinus64OrLowerIsSafeAgainstAnything()
	{
		final SplashTracker tracker = new SplashTracker(MONSTERS);
		// An NPC with no wiki data at all: the bonus alone decides.
		final SplashTracker.Result r = tracker.step(input(1, 1, 5, 99999, -80), 0);
		assertEquals(SplashTracker.Status.SAFE, r.status);
		assertEquals("Magic bonus: -80", r.bonusLine);
	}

	@Test
	public void aHighBonusAboveTheThresholdWarns()
	{
		final SplashTracker tracker = new SplashTracker(MONSTERS);
		final SplashTracker.Result r = tracker.step(input(1, 1, 5, 1, 0), 5);
		assertEquals(SplashTracker.Status.WARN, r.status);
		assertEquals("Magic bonus: 0 (need -64 or lower)", r.bonusLine);
		assertEquals("Hit chance on Rat: 95.4%", r.chanceLine);
	}

	@Test
	public void aSmallChanceAtOrBelowTheThresholdIsOnlyLow()
	{
		final SplashTracker tracker = new SplashTracker(MONSTERS);
		// -63 against a Rat is 8.4%.
		assertEquals(SplashTracker.Status.LOW, tracker.step(input(1, 1, 5, 1, -63), 10).status);
		assertEquals(SplashTracker.Status.WARN, tracker.step(input(1, 1, 5, 1, -63), 8).status);
	}

	@Test
	public void noWikiDataAndAHighBonusIsUnknownNotGuessed()
	{
		final SplashTracker tracker = new SplashTracker(MONSTERS);
		final SplashTracker.Result r = tracker.step(input(1, 1, 5, 99999, 20), 0);
		assertEquals(SplashTracker.Status.UNKNOWN, r.status);
		assertEquals("Hit chance: no wiki data for Rat", r.chanceLine);
	}

	@Test
	public void theFirstCastNeverAlerts()
	{
		assertNull(new SplashTracker(MONSTERS).step(input(1, 1, 5, 1, -80), 0).changed);
	}

	@Test
	public void changingGearSpellOrTargetAlertsAndNamesWhatChanged()
	{
		final SplashTracker tracker = new SplashTracker(MONSTERS);
		tracker.step(input(1, 1, 5, 1, -80), 0);
		assertNull(tracker.step(input(1, 1, 5, 1, -80), 0).changed);
		assertEquals("Splash setup changed: gear changed.", tracker.step(input(1, 2, 5, 1, -80), 0).changed);
		assertEquals("Splash setup changed: spell changed.", tracker.step(input(3, 2, 5, 1, -80), 0).changed);
		assertEquals("Splash setup changed: target changed.", tracker.step(input(3, 2, 6, 1, -80), 0).changed);
		assertEquals("Splash setup changed: gear changed, spell changed, target changed.",
			tracker.step(input(1, 1, 7, 1, -80), 0).changed);
	}

	@Test
	public void aBriefGapKeepsTheBaselineAndALongOneForgetsIt()
	{
		final SplashTracker tracker = new SplashTracker(MONSTERS);
		tracker.step(input(1, 1, 5, 1, -80), 0);
		for (int i = 0; i < SplashTracker.FORGET_TICKS - 1; i++)
		{
			tracker.idle();
		}
		assertNotNull(tracker.step(input(1, 2, 5, 1, -80), 0).changed);

		for (int i = 0; i < SplashTracker.FORGET_TICKS; i++)
		{
			tracker.idle();
		}
		assertNull(tracker.step(input(1, 3, 6, 1, -80), 0).changed);
	}

	@Test
	public void percentIsHonestAboutTinyChances()
	{
		assertEquals("0.0%", SplashTracker.percent(0));
		assertEquals("<0.1%", SplashTracker.percent(0.0004));
		assertEquals("50.0%", SplashTracker.percent(0.5));
	}

	@Test
	public void theTableParsesAndSkipsBadRows() throws IOException
	{
		final String tsv = "# header\n1\t1\t0\tRat\nbad row\nx\t1\t1\tName\n2919\t168\t30\tMithril dragon\n";
		final Map<Integer, MonsterMagic> table =
			MonsterMagic.parse(new ByteArrayInputStream(tsv.getBytes(StandardCharsets.UTF_8)));
		assertEquals(2, table.size());
		assertEquals(168, table.get(2919).level);
		assertEquals(30, table.get(2919).defence);
	}

	@Test
	public void theBundledTableHasTheWikiRows()
	{
		final Map<Integer, MonsterMagic> table = MonsterMagic.load();
		assertTrue("table has " + table.size() + " rows", table.size() > 3000);
		// Wiki, Mithril dragon: Magic level 168, magic defence +30.
		assertEquals(168, table.get(2919).level);
		assertEquals(30, table.get(2919).defence);
	}
}
