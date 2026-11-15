package com.vinnyvangogh.combatvitals;

/**
 * The name players see: the plugin list, the settings panel title, the
 * overlay menu, chat lines. Held equal to {@code displayName} in
 * runelite-plugin.properties by PluginNameTest.
 *
 * <p>Only the name changed from "Combat Vitals". The Hub id
 * {@code combat-vitals}, the class, the config group and every keyName stay
 * as published, so nothing a player saved is lost.
 */
final class PluginName
{
	static final String NAME = "Combat & Skilling Vitals";

	/** The name before 2026-11-14, kept for the one update note that says it changed. */
	static final String FORMER_NAME = "Combat Vitals";

	private PluginName()
	{
	}
}
