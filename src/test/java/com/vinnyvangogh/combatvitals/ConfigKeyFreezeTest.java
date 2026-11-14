package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/**
 * A saved setting is stored under its config group and keyName. Renaming
 * either, removing a key, or changing the type its value is read back as,
 * silently resets that setting for every user. This pins the group, every
 * key in {@code published-config-keys.txt} and its type, and makes a new key
 * get appended to the list so it is frozen from then on.
 *
 * The display name may change (the Hub shows {@code displayName} from
 * runelite-plugin.properties); the plugin's class, its Hub file name
 * {@code combat-vitals} and the config group may not.
 */
public class ConfigKeyFreezeTest
{
	@Test
	public void theConfigGroupNeverChanges()
	{
		assertEquals("combat-vitals", CombatVitalsConfig.GROUP);
		assertEquals("combat-vitals", CombatVitalsConfig.class.getAnnotation(ConfigGroup.class).value());
	}

	@Test
	public void everyPublishedKeyIsStillThereWithItsType() throws IOException
	{
		final Map<String, String> current = currentKeys();
		final StringBuilder broken = new StringBuilder();
		for (Map.Entry<String, String> published : publishedKeys().entrySet())
		{
			final String type = current.get(published.getKey());
			if (type == null)
			{
				broken.append("\n").append(published.getKey()).append(" was removed or renamed");
			}
			else if (!type.equals(published.getValue()))
			{
				broken.append("\n").append(published.getKey()).append(" changed from ")
					.append(published.getValue()).append(" to ").append(type);
			}
		}
		assertTrue(broken.toString(), broken.length() == 0);
	}

	@Test
	public void everyKeyIsOnThePublishedList() throws IOException
	{
		final Map<String, String> published = publishedKeys();
		for (String key : currentKeys().keySet())
		{
			assertTrue(key + " is new: append it to published-config-keys.txt", published.containsKey(key));
		}
	}

	@Test
	public void theDisplayNameChangesButThePluginStaysThePlugin() throws IOException
	{
		final Properties properties = pluginProperties();
		assertEquals("Combat & Skilling Vitals", properties.getProperty("displayName"));
		assertEquals("com.vinnyvangogh.combatvitals.CombatVitalsPlugin", properties.getProperty("plugins"));
	}

	/**
	 * The plugin list row holds the name beside the pin, settings cog and
	 * on/off toggle, so it has less room than a setting's row
	 * ({@link ConfigNamesFitTest#BUDGET}): "Combat & Skilling Vitals" is 24.
	 */
	@Test
	public void theDisplayNameFitsThePluginList() throws IOException
	{
		final String name = pluginProperties().getProperty("displayName");
		assertTrue(name + " is " + name.length() + " characters", name.length() <= 24);
	}

	@Test
	public void theSearchTagsReachSkillers() throws IOException
	{
		final String tags = pluginProperties().getProperty("tags");
		for (String tag : new String[]{"combat", "skilling", "woodcutting", "fishing", "mining", "inventory", "idle"})
		{
			assertTrue("missing tag " + tag, (',' + tags + ',').contains(',' + tag + ','));
		}
	}

	private static Properties pluginProperties() throws IOException
	{
		final Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(Paths.get("runelite-plugin.properties")))
		{
			properties.load(in);
		}
		return properties;
	}

	private static Map<String, String> currentKeys()
	{
		final Map<String, String> keys = new LinkedHashMap<>();
		for (Method method : CombatVitalsConfig.class.getMethods())
		{
			final ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				keys.put(item.keyName(), method.getReturnType().getSimpleName());
			}
		}
		return keys;
	}

	private static Map<String, String> publishedKeys() throws IOException
	{
		final Map<String, String> keys = new LinkedHashMap<>();
		try (InputStream in = ConfigKeyFreezeTest.class.getResourceAsStream("/published-config-keys.txt"))
		{
			assertTrue("published-config-keys.txt is missing", in != null);
			final String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			for (String line : text.split("\n"))
			{
				final String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
				{
					continue;
				}
				final String[] parts = trimmed.split("\\s+");
				assertEquals("bad line: " + trimmed, 2, parts.length);
				assertTrue("listed twice: " + parts[0], keys.put(parts[0], parts[1]) == null);
			}
		}
		assertTrue("the list is suspiciously short", keys.size() > 100);
		return keys;
	}
}
