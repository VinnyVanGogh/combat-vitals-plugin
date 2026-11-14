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

	static AlertSettings of(CombatVitalsConfig config, ActivityAlerts.Type type)
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
			default:
				return new AlertSettings(config.alertLowRunEnergy(), config.soundLowRunEnergy(),
					config.volumeLowRunEnergy(), config.repeatLowRunEnergy());
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
