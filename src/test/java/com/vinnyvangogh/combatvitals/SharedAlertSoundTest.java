package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.client.config.Notification;
import org.junit.Test;

/**
 * "Same for every alert": one sound, volume and repeat for all of them, while
 * each alert still turns on and off by itself. Off, each alert's own settings
 * apply, untouched.
 */
public class SharedAlertSoundTest
{
	/** Shared on, Voice line at 40% with repeat; every alert's own left at RuneLite, 70%, no repeat. */
	private static final CombatVitalsConfig SHARED = new CombatVitalsConfig()
	{
		@Override
		public boolean alertSoundShared()
		{
			return true;
		}

		@Override
		public AlertSound alertSoundAll()
		{
			return AlertSound.VOICE;
		}

		@Override
		public int alertVolumeAll()
		{
			return 40;
		}

		@Override
		public boolean alertRepeatAll()
		{
			return true;
		}

		@Override
		public int volumeInventoryFull()
		{
			return 70;
		}

		@Override
		public Notification alertStunned()
		{
			return Notification.OFF;
		}
	};

	@Test
	public void sharedReplacesEveryAlertsOwnSound()
	{
		for (ActivityAlerts.Type type : ActivityAlerts.Type.values())
		{
			final AlertSettings s = AlertSettings.of(SHARED, type);
			assertEquals(type.name(), AlertSound.VOICE, s.sound);
			assertEquals(type.name(), 40, s.volume);
			assertTrue(type.name(), s.repeat);
		}
	}

	@Test
	public void anAlertSwitchedOffStaysOffWhenShared()
	{
		assertFalse(AlertSettings.of(SHARED, ActivityAlerts.Type.STUNNED).enabled());
		assertTrue(AlertSettings.of(SHARED, ActivityAlerts.Type.INVENTORY_FULL).enabled());
	}

	@Test
	public void offByDefaultSoEachAlertKeepsItsOwn()
	{
		final CombatVitalsConfig own = new CombatVitalsConfig()
		{
			@Override
			public int volumeInventoryFull()
			{
				return 70;
			}

			@Override
			public AlertSound alertSoundAll()
			{
				return AlertSound.VOICE;
			}
		};
		final AlertSettings s = AlertSettings.of(own, ActivityAlerts.Type.INVENTORY_FULL);
		assertEquals(70, s.volume);
		assertEquals(AlertSound.RUNELITE, s.sound);
	}
}
