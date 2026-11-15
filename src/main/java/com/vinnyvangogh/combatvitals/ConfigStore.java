package com.vinnyvangogh.combatvitals;

import java.util.concurrent.Executor;
import net.runelite.client.config.ConfigManager;

/**
 * The plugin's settings as the sidebar panel reads and writes them: the same
 * keys in the same config group as RuneLite's settings list, so the two
 * always show the same values. Reads are immediate; writes go through
 * {@code ConfigManager} off the Swing thread, and the panel redraws from the
 * {@code ConfigChanged} that follows, the way the list does.
 */
interface ConfigStore
{
	/** The stored text of a key, or null while it is unset (its default). */
	String raw(String key);

	/** Stores a key's text; null unsets it, back to its default. */
	void setRaw(String key, String value);

	/** A key's value as {@code type}, or null while it is unset. */
	<T> T get(String key, Class<T> type);

	/** Stores a value of the key's own type. */
	void set(String key, Object value);

	/** The real one: {@code ConfigManager}, group {@link CombatVitalsConfig#GROUP}, writes on {@code writer}. */
	static ConfigStore of(ConfigManager configManager, Executor writer)
	{
		return new ConfigStore()
		{
			@Override
			public String raw(String key)
			{
				return configManager.getConfiguration(CombatVitalsConfig.GROUP, key);
			}

			@Override
			public void setRaw(String key, String value)
			{
				writer.execute(() ->
				{
					if (value == null)
					{
						configManager.unsetConfiguration(CombatVitalsConfig.GROUP, key);
					}
					else
					{
						configManager.setConfiguration(CombatVitalsConfig.GROUP, key, value);
					}
				});
			}

			@Override
			public <T> T get(String key, Class<T> type)
			{
				return configManager.getConfiguration(CombatVitalsConfig.GROUP, key, type);
			}

			@Override
			public void set(String key, Object value)
			{
				writer.execute(() -> configManager.setConfiguration(CombatVitalsConfig.GROUP, key, value));
			}
		};
	}
}
