package com.vinnyvangogh.combatvitals;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The "what's new" chat lines: once per version, never with the toggle off,
 * and a single welcome line, not the whole list, for a fresh install.
 */
public class UpdateNotesTest
{
	private static final String NEVER_SEEN = "";

	@Test
	public void versionIsThePluginPropertiesVersion() throws Exception
	{
		final Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(Paths.get("runelite-plugin.properties")))
		{
			properties.load(reader);
		}
		assertEquals(properties.getProperty("version"), UpdateNotes.VERSION);
	}

	@Test
	public void hubUpgradeFrom100ShowsTheTitleThenTheNotes()
	{
		// 1.0.0 never stored a version, and an update lands while the client
		// restarts, so the plugin starts on the login screen.
		final List<String> lines = UpdateNotes.toShow(NEVER_SEEN, true, false);

		assertEquals(UpdateNotes.TITLE, lines.get(0));
		assertTrue("title plus 4-6 lines, got " + lines.size(), lines.size() >= 5 && lines.size() <= 7);
		assertEquals(UpdateNotes.NOTES, lines.subList(1, lines.size()));
	}

	@Test
	public void aStoredOlderVersionShowsTheNotesWhereverThePluginStarted()
	{
		assertEquals(UpdateNotes.TITLE, UpdateNotes.toShow("1.0.9", true, false).get(0));
		assertEquals(UpdateNotes.TITLE, UpdateNotes.toShow("1.0.9", true, true).get(0));
	}

	@Test
	public void theSeenVersionShowsNothing()
	{
		assertTrue(UpdateNotes.toShow(UpdateNotes.VERSION, true, false).isEmpty());
		assertTrue(UpdateNotes.toShow(UpdateNotes.VERSION, true, true).isEmpty());
	}

	@Test
	public void theToggleOffShowsNothing()
	{
		assertTrue(UpdateNotes.toShow(NEVER_SEEN, false, false).isEmpty());
		assertTrue(UpdateNotes.toShow(NEVER_SEEN, false, true).isEmpty());
		assertTrue(UpdateNotes.toShow("1.0.9", false, false).isEmpty());
	}

	@Test
	public void aFreshInstallGetsOneWelcomeLine()
	{
		// Installed from the Plugin Hub panel while logged in: no stored version.
		final List<String> lines = UpdateNotes.toShow(NEVER_SEEN, true, true);

		assertEquals(1, lines.size());
		assertEquals(UpdateNotes.WELCOME, lines.get(0));
	}

	@Test
	public void showsOnceAcrossLoginsAndHops()
	{
		final List<String> posted = new ArrayList<>();
		String stored = NEVER_SEEN;
		for (int login = 0; login < 4; login++)
		{
			posted.addAll(UpdateNotes.toShow(stored, true, false));
			if (UpdateNotes.needsRecord(stored))
			{
				stored = UpdateNotes.VERSION;
			}
		}

		assertEquals(UpdateNotes.toShow(NEVER_SEEN, true, false), posted);
	}

	@Test
	public void theToggleOffStillRecordsTheVersion()
	{
		// Turned off now, back on later: the old notes must not appear then.
		assertTrue(UpdateNotes.needsRecord(NEVER_SEEN));
		assertTrue(UpdateNotes.needsRecord("1.0.9"));
		assertFalse(UpdateNotes.needsRecord(UpdateNotes.VERSION));
	}

	@Test
	public void notesAreFeaturesShortAndEndWithWhereToTurnThemOn()
	{
		for (String line : UpdateNotes.NOTES)
		{
			assertTrue("too long for a chat line: " + line, line.length() <= 110);
			assertFalse("1.0.0 users never saw a bug, so nothing is a fix: " + line,
				line.toLowerCase(Locale.ROOT).contains("fix"));
		}
		assertTrue(UpdateNotes.NOTES.get(UpdateNotes.NOTES.size() - 1)
			.contains("Configure Combat Vitals > Skilling badge"));
		assertTrue(UpdateNotes.TITLE.contains(UpdateNotes.VERSION));
		assertTrue(UpdateNotes.WELCOME.contains(UpdateNotes.VERSION));
	}
}
