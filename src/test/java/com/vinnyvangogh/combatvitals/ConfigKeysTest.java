package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/**
 * Every setting has a default a new profile gets, and every setting is read
 * somewhere outside the config interface: a key nothing reads is a switch
 * that does nothing. Also pins the round 3 defaults that keep today's look.
 */
public class ConfigKeysTest
{
	private static final CombatVitalsConfig NEW_PROFILE = new CombatVitalsConfig()
	{
	};

	private static final Path SOURCES = Paths.get("src/main/java/com/vinnyvangogh/combatvitals");

	@Test
	public void everyKeyHasADefault() throws Exception
	{
		for (Method method : items())
		{
			assertNotNull(method.getName() + " has no default", method.invoke(NEW_PROFILE));
		}
	}

	@Test
	public void everyKeyIsUniqueAndNamedForItsMethod()
	{
		final Set<String> keys = new HashSet<>();
		for (Method method : items())
		{
			final String key = method.getAnnotation(ConfigItem.class).keyName();
			assertEquals(method.getName(), key);
			assertTrue("duplicate key " + key, keys.add(key));
		}
	}

	@Test
	public void everyKeyIsRead() throws IOException
	{
		final String code;
		try (Stream<Path> files = Files.list(SOURCES))
		{
			code = files
				.filter(p -> !p.getFileName().toString().equals("CombatVitalsConfig.java"))
				.map(ConfigKeysTest::read)
				.collect(Collectors.joining("\n"));
		}

		for (Method method : items())
		{
			assertTrue(method.getName() + " is never read", code.contains("config." + method.getName() + "()"));
		}
	}

	@Test
	public void idleBadgeDefaultsToSixtySeconds()
	{
		assertEquals(IdleBadgeMode.TIMED, NEW_PROFILE.skillingIdleMode());
		assertEquals(60, NEW_PROFILE.skillingIdleSeconds());
	}

	@Test
	public void everySkillAndRowIsOnByDefault()
	{
		final boolean[] on = {
			NEW_PROFILE.skillingFishing(), NEW_PROFILE.skillingWoodcutting(), NEW_PROFILE.skillingMining(),
			NEW_PROFILE.skillingThieving(), NEW_PROFILE.skillingRunecraft(), NEW_PROFILE.skillingHunter(),
			NEW_PROFILE.skillingShowHeader(), NEW_PROFILE.skillingShowXp(),
		};
		for (boolean b : on)
		{
			assertTrue(b);
		}
	}

	@Test
	public void newRowsAndHidingAreOffByDefault()
	{
		final boolean[] off = {
			NEW_PROFILE.skillingSessionCount(), NEW_PROFILE.skillingRates(),
			NEW_PROFILE.skillingSessionReset(), NEW_PROFILE.skillingHideInCombat(),
		};
		for (boolean b : off)
		{
			assertEquals(false, b);
		}
	}

	@Test
	public void round3DefaultsKeepTodaysLook()
	{
		assertEquals(XpDisplay.BOTH, NEW_PROFILE.skillingXpDisplay());
		assertEquals(InventoryCount.FREE, NEW_PROFILE.skillingInventoryCount());
		assertEquals(BadgePosition.OVER_HEAD, NEW_PROFILE.skillingPosition());
		assertEquals(0, NEW_PROFILE.skillingOffset());
		assertEquals(100, NEW_PROFILE.skillingScale());
		assertEquals(LockedFish.DIM, NEW_PROFILE.skillingLockedFish());
		assertEquals(PillarMode.OFF, NEW_PROFILE.pillarMode());
	}

	@Test
	public void skillColoursDefaultToThePalette()
	{
		final Color[] colours = {
			NEW_PROFILE.skillingFishingColour(), NEW_PROFILE.skillingWoodcuttingColour(),
			NEW_PROFILE.skillingMiningColour(), NEW_PROFILE.skillingThievingColour(),
			NEW_PROFILE.skillingRunecraftColour(), NEW_PROFILE.skillingHunterColour(),
		};
		final Skill[] skills = {
			Skill.FISHING, Skill.WOODCUTTING, Skill.MINING, Skill.THIEVING, Skill.RUNECRAFT, Skill.HUNTER,
		};
		for (int i = 0; i < skills.length; i++)
		{
			assertEquals(skills[i].getName(), SkillColours.of(skills[i]), colours[i]);
		}
	}

	private static Set<Method> items()
	{
		final Set<Method> out = new HashSet<>();
		for (Method method : CombatVitalsConfig.class.getMethods())
		{
			if (method.isAnnotationPresent(ConfigItem.class))
			{
				out.add(method);
			}
		}
		return out;
	}

	private static String read(Path path)
	{
		try
		{
			return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
