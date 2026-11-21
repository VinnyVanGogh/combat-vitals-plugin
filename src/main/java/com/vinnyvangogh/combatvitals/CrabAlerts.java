package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.List;
import net.runelite.client.config.Notification;

/**
 * Which Gemstone Crab alerts are due and how they sound, from the config
 * alone. The crab has no sound setting of its own, so it follows the
 * "Same for every alert" group when that is on; otherwise it plays the soft
 * chime at the default volume. A voice line, a file or RuneLite's own sound
 * cannot say anything about the crab, so under those the soft chime plays.
 *
 * <p>The three alerts are on by default (a new key is the default for an
 * existing profile) and are not behind any other alert's switch: only the
 * Gemstone Crab mode itself, the sidebar mute and Mute alert sounds silence
 * them.
 */
final class CrabAlerts
{
	/** The chime's volume when "Same for every alert" is off; the shared group's own default. */
	static final int OWN_VOLUME = 40;

	private CrabAlerts()
	{
	}

	/** One alert to raise. */
	static final class Due
	{
		final Notification notification;
		final String message;

		Due(Notification notification, String message)
		{
			this.notification = notification;
			this.message = message;
		}
	}

	/** The alerts to raise this tick, in the order they matter, leaving out any the player turned off. */
	static List<Due> due(GemstoneCrab.Alerts alerts, CombatVitalsConfig config)
	{
		final List<Due> out = new ArrayList<>();
		if (alerts == null || !config.gemstoneCrab())
		{
			return out;
		}
		if (alerts.spawned)
		{
			add(out, config.gemstoneCrabAlertSpawn(), "A Gemstone Crab has appeared.");
		}
		if (alerts.aboutToBurrow)
		{
			add(out, config.gemstoneCrabAlertBurrow(), "The Gemstone Crab is about to burrow.");
		}
		if (alerts.goNow)
		{
			add(out, config.gemstoneCrabAlertGo(), "The Gemstone Crab has burrowed: crawl through the cave now.");
		}
		return out;
	}

	private static void add(List<Due> out, Notification notification, String message)
	{
		if (notification != null && notification.isEnabled())
		{
			out.add(new Due(notification, message));
		}
	}

	/** The sound: the shared one when "Same for every alert" is on and it can play for the crab, else the soft chime. */
	static AlertSound sound(CombatVitalsConfig config)
	{
		if (!config.alertSoundShared())
		{
			return AlertSound.SOFT_CHIME;
		}
		final AlertSound shared = config.alertSoundAll();
		return shared == null || shared == AlertSound.RUNELITE || shared == AlertSound.VOICE
			|| shared == AlertSound.MY_SOUNDS ? AlertSound.SOFT_CHIME : shared;
	}

	static int volume(CombatVitalsConfig config)
	{
		return config.alertSoundShared() ? config.alertVolumeAll() : OWN_VOLUME;
	}
}
