package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.lang.reflect.Proxy;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RuneLiteConfig;
import org.junit.Test;

/**
 * A new install sounds exactly as before: RuneLite's sound, no repeat. And
 * when the plugin plays its own sound, RuneLite's is off for that alert while
 * everything else the user set stays.
 */
public class AlertSettingsTest
{
	private static final CombatVitalsConfig NEW_PROFILE = new CombatVitalsConfig()
	{
	};

	/**
	 * RuneLite's settings as ConfigManager serves them, through a proxy:
	 * every boolean on (so tray and focus-sending show up copied), every int
	 * 4321, every enum its first value.
	 */
	private static final RuneLiteConfig RUNELITE = (RuneLiteConfig) Proxy.newProxyInstance(
		RuneLiteConfig.class.getClassLoader(), new Class<?>[]{RuneLiteConfig.class},
		(proxy, method, args) ->
		{
			final Class<?> type = method.getReturnType();
			if (type == boolean.class)
			{
				return true;
			}
			if (type == int.class)
			{
				return 4321;
			}
			if (type.isEnum())
			{
				return type.getEnumConstants()[0];
			}
			if (type == Color.class)
			{
				return Color.RED;
			}
			return null;
		});

	@Test
	public void newInstallsKeepRuneLitesSoundAndNeverRepeat()
	{
		for (ActivityAlerts.Type type : ActivityAlerts.Type.values())
		{
			final AlertSettings settings = AlertSettings.of(NEW_PROFILE, type);
			assertEquals(type.name(), AlertSound.RUNELITE, settings.sound);
			assertFalse(type + " repeats by default", settings.repeat);
			assertEquals(type.name(), 40, settings.volume);
		}
		assertEquals(15, NEW_PROFILE.repeatEverySeconds());
		assertEquals(10, NEW_PROFILE.repeatMaxMinutes());
	}

	@Test
	public void anAlertThatIsOffIsOffForSound()
	{
		assertFalse(new AlertSettings(Notification.OFF, AlertSound.VOICE, 40, true).enabled());
		assertTrue(new AlertSettings(Notification.ON, AlertSound.VOICE, 40, true).enabled());
	}

	@Test
	public void quietCopiesRuneLitesSettingsWithTheSoundOff()
	{
		final Notification quiet = AlertSettings.quiet(Notification.ON, RUNELITE);
		assertTrue(quiet.isEnabled());
		assertTrue("an override, so Notifier reads its fields", quiet.isOverride());
		assertTrue(quiet.isInitialized());
		assertEquals(NotificationSound.OFF, quiet.getSound());
		assertTrue("tray kept", quiet.isTray());
		assertTrue("focus setting kept", quiet.isSendWhenFocused());
		assertEquals(4321, quiet.getTimeout());
		assertEquals(Color.RED, quiet.getFlashColor());
	}

	@Test
	public void quietKeepsAnOverridesOwnSettings()
	{
		final Notification own = Notification.ON.withOverride(true).withInitialized(true).withTray(false)
			.withSound(NotificationSound.CUSTOM).withTimeout(1234);
		final Notification quiet = AlertSettings.quiet(own, RUNELITE);
		assertEquals(NotificationSound.OFF, quiet.getSound());
		assertFalse("the alert's own tray choice, not RuneLite's", quiet.isTray());
		assertEquals(1234, quiet.getTimeout());
	}
}
