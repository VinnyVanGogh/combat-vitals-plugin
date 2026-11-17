package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class EventLogTest
{
	private EventLog log;

	@Before
	public void setUp()
	{
		log = new EventLog();
	}

	@Test
	public void nothingOpenAtFirst()
	{
		assertFalse(log.isOpen());
		assertNull(log.finish());
	}

	@Test
	public void tracksStartContributionAndRewards()
	{
		log.started("Pheasant Control", 100);
		assertTrue(log.isOpen());
		log.xp(120);
		log.xp(80);
		log.reward("Anima-infused bark");
		log.reward("Anima-infused bark");
		assertEquals("Pheasant Control: 200 xp, 2 x Anima-infused bark", log.finish());
		assertFalse(log.isOpen());
	}

	@Test
	public void noContributionSaysSo()
	{
		log.started("Rising Roots", 5);
		assertEquals("Rising Roots: no contribution", log.finish());
	}

	@Test
	public void xpOutsideAnEventIsNotCounted()
	{
		log.xp(50);
		log.started("Struggling Sapling", 5);
		assertEquals("Struggling Sapling: no contribution", log.finish());
	}

	@Test
	public void aNewEventClosesTheOldOne()
	{
		log.started("Rising Roots", 5);
		log.xp(10);
		assertEquals("Rising Roots: 10 xp", log.started("Pheasant Control", 9));
		log.xp(7);
		assertEquals("Pheasant Control: 7 xp", log.finish());
	}

	@Test
	public void onlyKnownRewardsCount()
	{
		assertTrue(EventLog.isReward("Anima-infused bark"));
		assertTrue(EventLog.isReward("Sturdy beehive parts"));
		assertFalse(EventLog.isReward("Yew logs"));
		assertFalse(EventLog.isReward(null));
	}

	@Test
	public void rewardsOutsideAnEventAreIgnored()
	{
		log.reward("Anima-infused bark");
		log.started("Friendly Ent", 1);
		assertEquals("Friendly Ent: no contribution", log.finish());
	}

	@Test
	public void clearDropsTheOpenEvent()
	{
		log.started("Friendly Ent", 1);
		log.clear();
		assertFalse(log.isOpen());
	}
}
