package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
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

	/**
	 * RuneLite's ConfigManager serves settings through a java.lang.reflect.Proxy
	 * of this interface, generated outside the plugin's package. A setting whose
	 * type is package-private cannot be returned from that proxy: reading it
	 * throws IllegalAccessError on the client thread, which black-screens the
	 * game (FishingSpotPath, Board's in-game test 2026-11-04). Same proxy, same
	 * call, every setting.
	 */
	@Test
	public void everySettingCanBeReadThroughAConfigProxy()
	{
		final CombatVitalsConfig proxy = (CombatVitalsConfig) Proxy.newProxyInstance(
			CombatVitalsConfig.class.getClassLoader(),
			new Class<?>[]{CombatVitalsConfig.class},
			ConfigKeysTest::storedValue);
		for (Method method : items())
		{
			try
			{
				method.invoke(proxy);
			}
			catch (ReflectiveOperationException | LinkageError e)
			{
				final Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
				throw new AssertionError(method.getName() + " cannot be read through the config proxy: " + cause, cause);
			}
		}
	}

	/** A value of the setting's own type, as ConfigManager hands back a stored one. */
	private static Object storedValue(Object proxy, Method method, Object[] args)
	{
		final Class<?> type = method.getReturnType();
		if (type.isEnum())
		{
			return type.getEnumConstants()[0];
		}
		if (type == boolean.class)
		{
			return false;
		}
		if (type == int.class)
		{
			return 0;
		}
		if (type == double.class)
		{
			return 0d;
		}
		if (type == Color.class)
		{
			return Color.WHITE;
		}
		return null;
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
			assertTrue(method.getName() + " is never read", isRead(code, method.getName()));
		}
	}

	/**
	 * A setting is read by a call, config.key(), or by a method reference,
	 * config::key, handed on to be called later (SpotPathing's
	 * fishingSpotShortestPath). The reference must end at the key's last
	 * letter, so config::keyLonger does not count as reading key.
	 */
	static boolean isRead(String code, String key)
	{
		return code.contains("config." + key + "()")
			|| Pattern.compile("config::" + Pattern.quote(key) + "(?![\\p{javaJavaIdentifierPart}])").matcher(code).find();
	}

	@Test
	public void aMethodReferenceCountsAsARead()
	{
		assertTrue(isRead("new SpotPathing(config::fishingSpotPath, router(bridge, config::fishingSpotShortestPath));",
			"fishingSpotShortestPath"));
		assertTrue(isRead("if (config.fishingSpotPath() == ON)", "fishingSpotPath"));
		assertFalse("a longer key's reference is not this key's read",
			isRead("router(bridge, config::fishingSpotShortestPath)", "fishingSpot"));
		assertFalse("named but never read", isRead("// fishingSpotShortestPath is optional", "fishingSpotShortestPath"));
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
			NEW_PROFILE.skillingShowHeader(), NEW_PROFILE.skillingShowXp(), NEW_PROFILE.skillingGearWarning(),
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
	public void pillarLooksDefaultToNativeAndLabelled()
	{
		assertEquals(PillarLabels.ICON_AND_NUMBER, NEW_PROFILE.pillarLabels());
		assertEquals(PillarStyle.STATUS_BARS, NEW_PROFILE.pillarStyle());
		assertTrue("time to level rides along with the rates", NEW_PROFILE.skillingTimeToLevel());
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
