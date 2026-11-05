package com.vinnyvangogh.combatvitals;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.events.PluginMessage;

/**
 * Hands a tile to the Shortest Path plugin (Skretzo/shortest-path) as a
 * target, and clears it again.
 *
 * Copied from QuestPath's {@code ShortestPathBridge}
 * (VinnyVanGogh/questpath-plugin, main), which verified every constant and
 * the message shape with {@code javap -c} against the installed Shortest Path
 * jar; that class's doc has the full record. In short:
 *
 * <ul>
 * <li>Nothing here reflects into Shortest Path. It listens for RuneLite's own
 * {@link PluginMessage} (shipped in {@code runelite-client}), namespace
 * {@code "shortestpath"}: {@code "path"} with a {@code "target"} entry that
 * may be a {@code WorldPoint}, a packed int or a {@code Set} of either, and
 * {@code "clear"} to clear. An empty target set does NOT clear; it re-sends
 * the previous targets. Re-checked against Skretzo/shortest-path master
 * 4c6595e (2026-11-04): {@code api/PluginMessageHandler#onPluginMessage}
 * (L90-103) routes {@code "path"} to {@code onPathMessage} (L107-138), which
 * reads {@code "target"} and hands it to {@code PluginMessageTargets#decode}
 * (L49-68): a {@code Collection} is decoded element by element, and a
 * {@code WorldPoint} element is accepted.</li>
 * <li>Its {@code onPluginMessage} asserts it is on the client thread, and
 * {@code EventBus#post} calls subscribers on the posting thread, so the
 * poster must land the post there: build it with
 * {@link #clientThreadEventPoster}. The assertion is an {@code Error}, which
 * is why the catches below take {@code AssertionError} too.</li>
 * <li>Posting is fire-and-forget: {@link RouteOutcome#SENT} means the event
 * went out, never that a path is drawn. Seeing the line needs a human at a
 * client.</li>
 * <li>Shortest Path clears its own path when the player gets within its
 * "reached distance" of the target.</li>
 * </ul>
 */
@Slf4j
class ShortestPathBridge
{
	static final String PLUGIN_CLASS_NAME = "shortestpath.ShortestPathPlugin";
	static final String CONFIG_GROUP = "shortestpath";
	static final String MESSAGE_NAME = "path";
	static final String CLEAR_MESSAGE_NAME = "clear";
	static final String TARGET_KEY = "target";

	/** Whether a path can be handed to Shortest Path right now, and why not when it cannot. */
	enum Availability
	{
		AVAILABLE,
		NOT_INSTALLED,
		NOT_ENABLED,
	}

	/** What became of a {@link #route} call. */
	enum RouteOutcome
	{
		UNAVAILABLE,
		/** The poster threw; logged at warn. */
		CALL_FAILED,
		/** Handed to the poster without throwing. Not proof a path is drawn. */
		SENT,
	}

	private final Supplier<Collection<?>> plugins;
	private final Predicate<Object> pluginEnabled;
	private final String pluginClassName;
	private final Consumer<Object> eventPoster;

	/**
	 * @param plugins       every plugin RuneLite knows about, e.g. {@code pluginManager::getPlugins}
	 * @param pluginEnabled whether a plugin instance is switched on
	 * @param eventPoster   posts on the client thread; build with {@link #clientThreadEventPoster}
	 */
	ShortestPathBridge(Supplier<Collection<?>> plugins, Predicate<Object> pluginEnabled, Consumer<Object> eventPoster)
	{
		this(plugins, pluginEnabled, PLUGIN_CLASS_NAME, eventPoster);
	}

	/** Test seam: the plugin class name is overridable so a test can stand in a fixture. */
	ShortestPathBridge(Supplier<Collection<?>> plugins, Predicate<Object> pluginEnabled, String pluginClassName,
		Consumer<Object> eventPoster)
	{
		this.plugins = plugins;
		this.pluginEnabled = pluginEnabled;
		this.pluginClassName = pluginClassName;
		this.eventPoster = eventPoster;
	}

	/**
	 * A poster that always lands {@code post} on the client thread, whichever
	 * thread calls it.
	 *
	 * @param onClientThread production passes {@code clientThread::invoke}
	 * @param post           production passes {@code eventBus::post}
	 */
	static Consumer<Object> clientThreadEventPoster(Consumer<Runnable> onClientThread, Consumer<Object> post)
	{
		return object -> onClientThread.accept(() -> post.accept(object));
	}

	Availability availability()
	{
		final Object plugin = findPlugin();
		if (plugin == null)
		{
			return Availability.NOT_INSTALLED;
		}
		return isEnabledSafely(plugin) ? Availability.AVAILABLE : Availability.NOT_ENABLED;
	}

	/** Sends one tile as Shortest Path's target, replacing whatever path it had. */
	RouteOutcome route(WorldPoint target)
	{
		final Availability availability = availability();
		if (availability != Availability.AVAILABLE)
		{
			log.debug("Not sending a fishing spot to Shortest Path -- {}", availability);
			return RouteOutcome.UNAVAILABLE;
		}
		if (!post(MESSAGE_NAME, Map.of(TARGET_KEY, Set.of(target))))
		{
			return RouteOutcome.CALL_FAILED;
		}
		log.debug("Sent fishing spot {} to Shortest Path", target);
		return RouteOutcome.SENT;
	}

	/** Clears Shortest Path's path. @return whether the message went out */
	boolean clear()
	{
		return availability() == Availability.AVAILABLE && post(CLEAR_MESSAGE_NAME, Map.of());
	}

	private boolean post(String name, Map<String, Object> data)
	{
		try
		{
			eventPoster.accept(new PluginMessage(CONFIG_GROUP, name, data));
			return true;
		}
		// Not Throwable: an OutOfMemoryError this deep is not "Shortest Path
		// didn't get the message" and must not be reported as one.
		catch (RuntimeException | AssertionError e)
		{
			log.warn("Posting PluginMessage(\"{}\", \"{}\") failed -- Shortest Path may be out of date",
				CONFIG_GROUP, name, e);
			return false;
		}
	}

	private boolean isEnabledSafely(Object plugin)
	{
		try
		{
			return pluginEnabled.test(plugin);
		}
		catch (RuntimeException e)
		{
			log.warn("Could not read Shortest Path's enabled state", e);
			return false;
		}
	}

	private Object findPlugin()
	{
		final Collection<?> loaded;
		try
		{
			loaded = plugins.get();
		}
		catch (RuntimeException e)
		{
			log.warn("Could not list loaded plugins while looking for Shortest Path", e);
			return null;
		}
		if (loaded == null)
		{
			return null;
		}
		for (Object candidate : loaded)
		{
			if (candidate != null && pluginClassName.equals(candidate.getClass().getName()))
			{
				return candidate;
			}
		}
		return null;
	}
}
