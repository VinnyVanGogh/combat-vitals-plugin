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
		if (SkillingEvents.isEvent(type))
		{
			return ofEvent(config, type);
		}
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

	/** The skilling events' settings ({@link SkillingEvents#TYPES}). */
	private static AlertSettings ofEvent(CombatVitalsConfig config, ActivityAlerts.Type type)
	{
		switch (type)
		{
			case BIRD_NEST:
				return new AlertSettings(config.alertBirdNest(), config.soundBirdNest(),
					config.volumeBirdNest(), config.repeatBirdNest());
			case CLUE_DROP:
				return new AlertSettings(config.alertClueDrop(), config.soundClueDrop(),
					config.volumeClueDrop(), config.repeatClueDrop());
			case FORESTRY_EVENT:
				return new AlertSettings(config.alertForestryEvent(), config.soundForestryEvent(),
					config.volumeForestryEvent(), config.repeatForestryEvent());
			case PET:
				return new AlertSettings(config.alertPet(), config.soundPet(),
					config.volumePet(), config.repeatPet());
			case LEVEL_UP:
				return new AlertSettings(config.alertLevelUp(), config.soundLevelUp(),
					config.volumeLevelUp(), config.repeatLevelUp());
			case RANDOM_EVENT:
			default:
				return new AlertSettings(config.alertRandomEvent(), config.soundRandomEvent(),
					config.volumeRandomEvent(), config.repeatRandomEvent());
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
