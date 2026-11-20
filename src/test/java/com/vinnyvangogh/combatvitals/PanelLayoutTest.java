package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.junit.Test;

/**
 * RuneLite's settings list and the sidebar panel show the same settings in
 * the same places: each tab is one config section holding exactly the tab's
 * settings, in the tab's order. Every setting is somewhere, once.
 */
public class PanelLayoutTest
{
	private static final Set<String> OUTSIDE_SECTIONS = new HashSet<>(Arrays.asList(
		"combatVitals", "skillingVitals", CombatVitalsConfig.LAST_SEEN_VERSION, LiveSection.OPEN_KEY,
		"pillarUserPresets", VitalsSidePanel.GUIDES_CLOSED_KEY, VitalsSidePanel.GUIDES_HIDDEN_KEY,
			VitalsSidePanel.TAB_KEY, LiveSection.VIEW_KEY));

	@Test
	public void everySettingIsInExactlyOnePlace()
	{
		final List<String> placed = new ArrayList<>(OUTSIDE_SECTIONS);
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			placed.addAll(PanelLayout.listOrder(tab));
		}
		placed.addAll(PanelLayout.ADVANCED);
		assertEquals("a setting is placed twice", new HashSet<>(placed).size(), placed.size());
		assertEquals(PanelFixture.methods().keySet(), new HashSet<>(placed));
	}

	@Test
	public void eachTabIsItsConfigSectionInTheSameOrder()
	{
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			assertEquals(tab.label, PanelLayout.listOrder(tab), listed(tab.section));
		}
		assertEquals("Advanced", PanelLayout.ADVANCED, listed(CombatVitalsConfig.advancedSection));
	}

	/** The section's settings as RuneLite's list orders them: by position, then name. */
	private static List<String> listed(String section)
	{
		return PanelFixture.methods().values().stream()
			.map(m -> m.getAnnotation(ConfigItem.class))
			.filter(i -> i.section().equals(section))
			.sorted(Comparator.comparingInt(ConfigItem::position).thenComparing(ConfigItem::name))
			.map(ConfigItem::keyName)
			.collect(Collectors.toList());
	}

	@Test
	public void theMasterSwitchesSitAboveEverySection()
	{
		for (String key : new String[]{"combatVitals", "skillingVitals"})
		{
			final ConfigItem item = PanelFixture.methods().get(key).getAnnotation(ConfigItem.class);
			assertEquals(key, "", item.section());
		}
	}

	@Test
	public void sixSectionsAllCollapsedByDefault() throws IllegalAccessException
	{
		final Set<String> keys = new HashSet<>();
		for (Field f : CombatVitalsConfig.class.getFields())
		{
			final ConfigSection s = f.getAnnotation(ConfigSection.class);
			if (s != null)
			{
				assertTrue(s.name() + " opens by default", s.closedByDefault());
				keys.add((String) f.get(null));
			}
		}
		final Set<String> expected = new HashSet<>();
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			expected.add(tab.section);
		}
		expected.add(CombatVitalsConfig.advancedSection);
		assertEquals(expected, keys);
	}

	@Test
	public void everySettingInASectionPointsAtADeclaredOne() throws IllegalAccessException
	{
		final Set<String> declared = new HashSet<>();
		for (Field f : CombatVitalsConfig.class.getFields())
		{
			if (f.getAnnotation(ConfigSection.class) != null)
			{
				declared.add((String) f.get(null));
			}
		}
		for (Method m : PanelFixture.methods().values())
		{
			final String section = m.getAnnotation(ConfigItem.class).section();
			assertTrue(m.getName() + " is in undeclared section " + section,
				section.isEmpty() || declared.contains(section));
		}
	}

	@Test
	public void everyAlertHasARowWithItsOwnKeys()
	{
		final Set<ActivityAlerts.Type> rows = EnumSet.noneOf(ActivityAlerts.Type.class);
		final Map<String, Method> methods = PanelFixture.methods();
		for (PanelLayout.AlertRow r : PanelLayout.ALERTS)
		{
			rows.add(r.type);
			for (String key : new String[]{r.toggle, r.sound, r.volume, r.repeat})
			{
				assertTrue(r.label + ": no key " + key, methods.containsKey(key));
			}
		}
		assertEquals(EnumSet.allOf(ActivityAlerts.Type.class), rows);
	}

	@Test
	public void everyTabHasAGuideWithAWalkthrough()
	{
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			final PanelGuide.Page page = PanelGuide.of(tab);
			assertTrue(tab.label, !page.what.isEmpty() && !page.how.isEmpty());
			assertTrue(tab.label, page.walkthrough.size() >= 2);
		}
	}

	/** README's "Sidebar panel" carries each tab's walkthrough word for word, so the two cannot drift. */
	@Test
	public void theReadmeHasEachTabsWalkthrough() throws IOException
	{
		final String readme = new String(Files.readAllBytes(Paths.get("README.md")), StandardCharsets.UTF_8);
		for (PanelLayout.Tab tab : PanelLayout.Tab.values())
		{
			final List<String> steps = PanelGuide.of(tab).walkthrough;
			for (int i = 0; i < steps.size(); i++)
			{
				final String line = (i + 1) + ". " + steps.get(i);
				assertTrue(tab.label + " step missing from README: " + line, readme.contains(line));
			}
		}
	}
}
