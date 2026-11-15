package com.vinnyvangogh.combatvitals;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.plugins.PluginDescriptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * One name everywhere a player sees it: the Plugin Hub (displayName) and the
 * client's plugin list and settings title (the descriptor name) agree, while
 * everything RuneLite stores state under stays as published.
 */
public class PluginNameTest
{
	@Test
	public void theDescriptorNameIsTheHubDisplayName() throws IOException
	{
		final PluginDescriptor descriptor = CombatVitalsPlugin.class.getAnnotation(PluginDescriptor.class);
		assertEquals(properties().getProperty("displayName"), descriptor.name());
		assertEquals("Combat & Skilling Vitals", descriptor.name());
	}

	/**
	 * RuneLite keys the on/off state by configName, or the class's simple name
	 * when that is empty (PluginManager#isPluginEnabled). Setting configName
	 * or renaming the class would turn the plugin off for everyone.
	 */
	@Test
	public void theEnabledStateKeyIsUnchanged()
	{
		final PluginDescriptor descriptor = CombatVitalsPlugin.class.getAnnotation(PluginDescriptor.class);
		assertEquals("", descriptor.configName());
		assertEquals("CombatVitalsPlugin", CombatVitalsPlugin.class.getSimpleName());
	}

	@Test
	public void theConfigGroupIsUnchanged()
	{
		assertEquals("combat-vitals", CombatVitalsConfig.class.getAnnotation(ConfigGroup.class).value());
	}

	@Test
	public void theDescriptorNameHasNoCommaForThePinList()
	{
		// PluginListPanel stores pinned plugins as a comma-separated list of names.
		assertTrue(!PluginName.NAME.contains(","));
	}

	private static Properties properties() throws IOException
	{
		final Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(Paths.get("runelite-plugin.properties")))
		{
			properties.load(reader);
		}
		return properties;
	}
}
