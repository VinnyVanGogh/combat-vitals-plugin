package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import net.runelite.client.config.Notification;
import org.junit.Test;

/**
 * The crab alerts for a player who already had the plugin with every alert on:
 * the three new keys take their defaults, so they fire with nothing to open.
 */
public class CrabAlertsTest
{
	private static GemstoneCrab.Alerts all()
	{
		final GemstoneCrab.Alerts a = new GemstoneCrab.Alerts();
		a.spawned = true;
		a.aboutToBurrow = true;
		a.goNow = true;
		return a;
	}

	/** Existing profile: every older alert on, shared sound off, crab keys left at their defaults. */
	private static final CombatVitalsConfig EXISTING = new CombatVitalsConfig()
	{
		@Override
		public Notification alertStunned()
		{
			return Notification.ON;
		}

		@Override
		public Notification alertInventoryFull()
		{
			return Notification.ON;
		}
	};

	@Test
	public void anExistingProfileWithAlertsOnGetsAllThreeCrabAlerts()
	{
		final List<CrabAlerts.Due> due = CrabAlerts.due(all(), EXISTING);
		assertEquals(3, due.size());
		assertEquals("A Gemstone Crab has appeared.", due.get(0).message);
		assertEquals("The Gemstone Crab is about to burrow.", due.get(1).message);
		assertEquals("The Gemstone Crab has burrowed: crawl through the cave now.", due.get(2).message);
	}

	@Test
	public void aTurnedOffAlertIsLeftOutAndTheRestStay()
	{
		final CombatVitalsConfig config = new CombatVitalsConfig()
		{
			@Override
			public Notification gemstoneCrabAlertBurrow()
			{
				return Notification.OFF;
			}
		};
		final List<CrabAlerts.Due> due = CrabAlerts.due(all(), config);
		assertEquals(2, due.size());
		assertEquals("A Gemstone Crab has appeared.", due.get(0).message);
		assertEquals("The Gemstone Crab has burrowed: crawl through the cave now.", due.get(1).message);
	}

	@Test
	public void nothingFiresWithCrabModeOffOrNothingDue()
	{
		final CombatVitalsConfig off = new CombatVitalsConfig()
		{
			@Override
			public boolean gemstoneCrab()
			{
				return false;
			}
		};
		assertTrue(CrabAlerts.due(all(), off).isEmpty());
		assertTrue(CrabAlerts.due(new GemstoneCrab.Alerts(), EXISTING).isEmpty());
		assertTrue(CrabAlerts.due(null, EXISTING).isEmpty());
	}

	@Test
	public void theCombatHalfBeingOffDoesNotSilenceThem()
	{
		final CombatVitalsConfig config = new CombatVitalsConfig()
		{
			@Override
			public boolean combatVitals()
			{
				return false;
			}
		};
		assertEquals(3, CrabAlerts.due(all(), config).size());
	}

	@Test
	public void sharedOffPlaysTheSoftChimeAtTheDefaultVolume()
	{
		assertEquals(AlertSound.SOFT_CHIME, CrabAlerts.sound(EXISTING));
		assertEquals(CrabAlerts.OWN_VOLUME, CrabAlerts.volume(EXISTING));
	}

	@Test
	public void sharedOnUsesTheSharedSoundAndVolume()
	{
		final CombatVitalsConfig config = new CombatVitalsConfig()
		{
			@Override
			public boolean alertSoundShared()
			{
				return true;
			}

			@Override
			public AlertSound alertSoundAll()
			{
				return AlertSound.SOFT_CHIME;
			}

			@Override
			public int alertVolumeAll()
			{
				return 70;
			}
		};
		assertEquals(AlertSound.SOFT_CHIME, CrabAlerts.sound(config));
		assertEquals(70, CrabAlerts.volume(config));
	}

	@Test
	public void aSharedSoundThatCannotNameTheCrabFallsBackToTheChime()
	{
		for (AlertSound shared : new AlertSound[] {AlertSound.RUNELITE, AlertSound.VOICE, AlertSound.MY_SOUNDS})
		{
			final CombatVitalsConfig config = new CombatVitalsConfig()
			{
				@Override
				public boolean alertSoundShared()
				{
					return true;
				}

				@Override
				public AlertSound alertSoundAll()
				{
					return shared;
				}
			};
			assertEquals(shared.name(), AlertSound.SOFT_CHIME, CrabAlerts.sound(config));
		}
	}
}
