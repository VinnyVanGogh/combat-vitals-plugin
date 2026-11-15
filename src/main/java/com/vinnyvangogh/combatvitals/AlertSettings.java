package com.vinnyvangogh.combatvitals;

import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RuneLiteConfig;

/** One activity alert's settings: its notification, sound, volume and repeat. */
final class AlertSettings
{
	final Notification notification;
	final AlertSound sound;
	final int volume;
	final boolean repeat;

	AlertSettings(Notification notification, AlertSound sound, int volume, boolean repeat)
	{
		this.notification = notification;
		this.sound = sound;
		this.volume = volume;
		this.repeat = repeat;
	}

	/**
	 * With "Same for every alert" on, the shared sound, volume and repeat
	 * replace the alert's own; whether the alert is on at all stays its own.
	 */
	static AlertSettings of(CombatVitalsConfig config, ActivityAlerts.Type type)
	{
		final AlertSettings own = own(config, type);
		if (!config.alertSoundShared())
		{
			return own;
		}
		return new AlertSettings(own.notification, config.alertSoundAll(), config.alertVolumeAll(),
			config.alertRepeatAll());
	}

	private static AlertSettings own(CombatVitalsConfig config, ActivityAlerts.Type type)
	{
		switch (type)
		{
			case INVENTORY_FULL:
				return new AlertSettings(config.alertInventoryFull(), config.soundInventoryFull(),
					config.volumeInventoryFull(), config.repeatInventoryFull());
			case GATHERING_STOPPED:
				return new AlertSettings(config.alertGatheringStopped(), config.soundGatheringStopped(),
					config.volumeGatheringStopped(), config.repeatGatheringStopped());
			case THIEVING_STOPPED:
				return new AlertSettings(config.alertThievingStopped(), config.soundThievingStopped(),
					config.volumeThievingStopped(), config.repeatThievingStopped());
			case PRODUCTION_FINISHED:
				return new AlertSettings(config.alertMakeFinished(), config.soundMakeFinished(),
					config.volumeMakeFinished(), config.repeatMakeFinished());
			case SPOT_MOVED:
				return new AlertSettings(config.alertSpotMoved(), config.soundSpotMoved(),
					config.volumeSpotMoved(), config.repeatSpotMoved());
			case STUNNED:
				return new AlertSettings(config.alertStunned(), config.soundStunned(),
					config.volumeStunned(), config.repeatStunned());
			case COIN_POUCH_CAP:
				return new AlertSettings(config.alertCoinPouch(), config.soundCoinPouch(),
					config.volumeCoinPouch(), config.repeatCoinPouch());
			case NECKLACE_BROKE:
				return new AlertSettings(config.alertNecklaceBroke(), config.soundNecklaceBroke(),
					config.volumeNecklaceBroke(), config.repeatNecklaceBroke());
			case LOW_RUN_ENERGY:
				return new AlertSettings(config.alertLowRunEnergy(), config.soundLowRunEnergy(),
					config.volumeLowRunEnergy(), config.repeatLowRunEnergy());
			default:
				return ofMethod(config, type);
		}
	}

	/** The training method alerts: the Blast Furnace pump's stopped, idle logout and auto stop. */
	private static AlertSettings ofMethod(CombatVitalsConfig config, ActivityAlerts.Type type)
	{
		switch (type)
		{
			case RECLICK_SOON:
				return new AlertSettings(config.alertReclickSoon(), config.soundReclickSoon(),
					config.volumeReclickSoon(), config.repeatReclickSoon());
			case AUTO_STOP_SOON:
				return new AlertSettings(config.alertAutoStopSoon(), config.soundAutoStopSoon(),
					config.volumeAutoStopSoon(), config.repeatAutoStopSoon());
			case METHOD_STOPPED:
			default:
				return new AlertSettings(config.alertMethodStopped(), config.soundMethodStopped(),
					config.volumeMethodStopped(), config.repeatMethodStopped());
		}
	}

	/**
	 * The same notification with RuneLite's sound off, for when the plugin
	 * plays its own: tray, flash, focus and the rest stay as the user set
	 * them. A notification that does not override RuneLite's settings gets
	 * them copied in, the way {@code Notifier} would apply them, since
	 * {@code Notifier} ignores the fields of one that is not an override.
	 */
	static Notification quiet(Notification notification, RuneLiteConfig defaults)
	{
		if (notification.isOverride() && notification.isInitialized())
		{
			return notification.withSound(NotificationSound.OFF);
		}
		return notification
			.withOverride(true)
			.withInitialized(true)
			.withTray(defaults.enableTrayNotifications())
			.withRequestFocus(defaults.notificationRequestFocus())
			.withSound(NotificationSound.OFF)
			.withVolume(defaults.notificationVolume())
			.withTimeout(defaults.notificationTimeout())
			.withGameMessage(defaults.enableGameMessageNotification())
			.withFlash(defaults.flashNotification())
			.withFlashColor(defaults.notificationFlashColor())
			.withSendWhenFocused(defaults.sendNotificationsWhenFocused());
	}

	/** The alert is on at all; off, it neither notifies nor sounds nor repeats. */
	boolean enabled()
	{
		return notification != null && notification.isEnabled();
	}
}
