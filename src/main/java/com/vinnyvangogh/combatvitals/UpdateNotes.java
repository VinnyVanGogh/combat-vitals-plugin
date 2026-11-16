package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The game-chat lines posted on the first login after the plugin updates.
 *
 * <p>1.0.0 never stored a version, so "nothing stored" is either a Plugin Hub
 * user coming from 1.0.0 or a brand-new install. The Hub updates plugins while
 * the client starts, so an update starts the plugin on the login screen; an
 * install from the Plugin Hub panel starts it in a running, usually logged-in
 * client. Nothing stored and started logged in is therefore read as a fresh
 * install and gets one welcome line instead of the whole list.
 */
final class UpdateNotes
{
	/** Must match {@code version} in runelite-plugin.properties (UpdateNotesTest). */
	static final String VERSION = "1.2.0";

	/** Says the old name too, so a player who knew "Combat Vitals" knows what this is. */
	static final String TITLE = PluginName.FORMER_NAME + " is now " + PluginName.NAME
		+ ", updated to " + VERSION + ". What's new:";

	/** Where the skilling settings are, as the settings panel titles it. */
	static final String CONFIGURE = "Configure " + PluginName.NAME + " > Skilling badge";

	/** What 1.2.0 adds over 1.1.0, written for the players updating from it. */
	static final List<String> NOTES = Collections.unmodifiableList(Arrays.asList(
		"New sidebar panel: presets, a tab per area with a short guide, and your live vitals.",
		"The skilling badge now covers every skill, plus the Blast Furnace pump with its own checks.",
		"More alerts: bird nests, clues, pets, level-ups, Forestry and random events.",
		"Each alert can play its own sound or a voice line; every sound starts as RuneLite's own.",
		"Log basket, fish barrel and other containers count toward your inventory.",
		"Set it up in the new sidebar panel, or in " + CONFIGURE + "."
	));

	static final String WELCOME = PluginName.NAME + " " + VERSION
		+ " is on. Skilling badge, alerts and more: " + CONFIGURE + ".";

	private UpdateNotes()
	{
	}

	/**
	 * What to post on this login, the title first; empty when nothing.
	 *
	 * @param lastSeen         the stored last-seen version, "" when none
	 * @param enabled          the "Show update notes in chat" setting
	 * @param startedLoggedIn  whether the plugin started while already logged in
	 */
	static List<String> toShow(String lastSeen, boolean enabled, boolean startedLoggedIn)
	{
		if (!enabled || !needsRecord(lastSeen))
		{
			return Collections.emptyList();
		}
		if (isNullOrEmpty(lastSeen) && startedLoggedIn)
		{
			return Collections.singletonList(WELCOME);
		}
		final List<String> lines = new ArrayList<>(NOTES.size() + 1);
		lines.add(TITLE);
		lines.addAll(NOTES);
		return lines;
	}

	/** Whether this version still has to be stored as seen; true with the setting off too. */
	static boolean needsRecord(String lastSeen)
	{
		return !VERSION.equals(lastSeen);
	}

	private static boolean isNullOrEmpty(String s)
	{
		return s == null || s.isEmpty();
	}
}
