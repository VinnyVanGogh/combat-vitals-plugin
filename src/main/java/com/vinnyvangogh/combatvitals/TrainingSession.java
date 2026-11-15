package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Skill;

/**
 * One stretch at a {@link TrainingMethod}: the Blast Furnace pump. Decides
 * its three alerts and the two countdowns the badge shows.
 *
 * <h3>Stopped</h3>
 * Started when the badge comes up for the method. Any animation or a gain in
 * the method's skill keeps it alive; after the stop delay with neither,
 * "You've stopped pumping" fires once and the session waits for the next
 * start. Walking off, a fight, another badge or logging out end it without
 * an alert ({@link #end}).
 *
 * Only pumping in this login session counts (Board, 2026-11-14): the start
 * itself (the Operate click and the animation at the pump), the animation or
 * a Strength gain since. A reconnect is a new login without a logout
 * ({@link #reconnected}), so a session from before it ends quietly unless
 * the player is seen pumping again.
 *
 * <h3>Re-click</h3>
 * The client logs you out once neither mouse nor keyboard has been touched
 * for its idle timeout: 5 minutes by default, up to 25 in the game's
 * settings. RuneLite's Idle Notifier reads it the same way:
 * {@code min(getKeyboardIdleTicks(), getMouseIdleTicks())} against
 * {@code getIdleTimeout()}, all in client ticks of 20 ms. The countdown is
 * what is left of it, and the warning fires once when it drops under the
 * lead, again only after you have touched the client.
 *
 * <h3>Stops</h3>
 * The game stops the method by itself after {@link TrainingMethod#autoStopTicks}
 * from the click (the pump: 100 minutes). The warning fires once per click.
 *
 * Pure: ticks and milliseconds in, alerts out, no client. Not thread safe;
 * the plugin touches it from the client thread only.
 */
final class TrainingSession
{
	private final int[] lastXp = new int[Skill.values().length];

	/** The method of the last start, kept after it ends for the idle badge's "Not pumping". */
	private TrainingMethod method;
	private boolean running;
	private int startTick = -1;
	private int lastActiveTick = -1;
	private boolean gained;
	/** Seen pumping in this login session: without it, stopping is not news. */
	private boolean worked;
	/** Started since the last tick: back at it, so a repeating alert can stop. */
	private boolean started;
	private boolean resumed;
	private boolean reclickWarned;
	private boolean stopWarned;
	private long reclickMillis = -1;
	/** Mouse or keyboard idle time as of the last tick, -1 when unknown. */
	private long idleMillis = -1;
	private int stopTicksLeft = -1;

	/** Input this recent reads as "Active" instead of a logout countdown (Board, 2026-11-14). */
	static final long ACTIVE_MS = 5_000;

	TrainingSession()
	{
		clear();
	}

	/** A skill's experience, from {@code StatChanged}. The first value per skill is a baseline. */
	void xp(Skill skill, int xp)
	{
		final int i = skill.ordinal();
		final int before = lastXp[i];
		lastXp[i] = xp;
		if (before >= 0 && xp > before && running && skill == method.skill())
		{
			gained = true;
		}
	}

	/**
	 * The badge came up for a method.
	 *
	 * @param clickTick the tick the method's object was clicked: the game's own stop counts from it
	 */
	void start(TrainingMethod method, int clickTick, int tick)
	{
		if (this.method != method || startTick != clickTick)
		{
			stopWarned = false;
		}
		this.method = method;
		started = true;
		running = true;
		worked = true;
		startTick = clickTick;
		lastActiveTick = tick;
	}

	/**
	 * The connection came back without a logout: to the player, logging in
	 * again. The pumping before it no longer counts, and the tick count may
	 * have started over, so the stop delay starts from the next tick.
	 */
	void reconnected()
	{
		worked = false;
		lastActiveTick = -1;
	}

	/** Stopped on purpose or by something else taking over: no alert, countdowns gone. */
	void end()
	{
		running = false;
		gained = false;
		reclickMillis = -1;
		stopTicksLeft = -1;
	}

	/** Logged out or hopped: everything is a baseline again. */
	void clear()
	{
		Arrays.fill(lastXp, -1);
		method = null;
		startTick = -1;
		lastActiveTick = -1;
		started = false;
		worked = false;
		resumed = false;
		reclickWarned = false;
		stopWarned = false;
		end();
	}

	/**
	 * Once per game tick, after the tick's experience has been reported.
	 *
	 * @param animating   the local player has an animation playing
	 * @param idleMillis  how long since the mouse or keyboard was last touched, or -1 when unknown
	 * @param logoutMillis the client's idle timeout, or 0 or less when unknown
	 * @param stopTicks   ticks without animation or experience before "stopped"
	 * @param warnMillis  how long before the logout or the game's stop to warn
	 * @return the alerts to send; empty most ticks
	 */
	List<ActivityAlerts.Alert> tick(int tick, boolean animating, long idleMillis, long logoutMillis, int stopTicks,
		long warnMillis)
	{
		resumed = started;
		started = false;
		final boolean active = gained || animating;
		gained = false;
		if (!running)
		{
			return Collections.emptyList();
		}

		if (active || lastActiveTick < 0)
		{
			lastActiveTick = tick;
		}
		worked |= active;
		if (tick - lastActiveTick >= stopTicks)
		{
			final boolean news = worked;
			end();
			return news ? Collections.singletonList(alert(ActivityAlerts.Type.METHOD_STOPPED)) : Collections.emptyList();
		}

		List<ActivityAlerts.Alert> out = null;
		this.idleMillis = idleMillis;
		reclickMillis = idleMillis < 0 || logoutMillis <= 0 ? -1 : Math.max(0, logoutMillis - idleMillis);
		if (reclickMillis >= 0 && reclickMillis <= warnMillis)
		{
			if (!reclickWarned)
			{
				reclickWarned = true;
				out = add(out, alert(ActivityAlerts.Type.RECLICK_SOON));
			}
		}
		else
		{
			reclickWarned = false;
		}

		final int autoStop = method.autoStopTicks();
		stopTicksLeft = autoStop < 0 ? -1 : Math.max(0, autoStop - (tick - startTick));
		if (stopTicksLeft >= 0 && stopTicksLeft * 600L <= warnMillis && !stopWarned)
		{
			stopWarned = true;
			out = add(out, alert(ActivityAlerts.Type.AUTO_STOP_SOON));
		}
		return out == null ? Collections.emptyList() : out;
	}

	/** The method of the last start, working or not, or null. */
	TrainingMethod method()
	{
		return method;
	}

	/** Started and not yet stopped or ended. */
	boolean running()
	{
		return running;
	}

	/** The method started again before the last tick: a repeating alert can stop. */
	boolean resumed()
	{
		return resumed;
	}

	/** Milliseconds until the idle logout as of the last tick, or -1 when not running or unknown. */
	long reclickMillis()
	{
		return reclickMillis;
	}

	/** The mouse or keyboard was used within {@link #ACTIVE_MS} as of the last tick. */
	boolean active()
	{
		return idleMillis >= 0 && idleMillis < ACTIVE_MS;
	}

	/** Ticks until the game stops the method as of the last tick, or -1 when not running or it never does. */
	int stopTicksLeft()
	{
		return stopTicksLeft;
	}

	private ActivityAlerts.Alert alert(ActivityAlerts.Type type)
	{
		return new ActivityAlerts.Alert(type, method.skill(), method);
	}

	private static List<ActivityAlerts.Alert> add(List<ActivityAlerts.Alert> out, ActivityAlerts.Alert alert)
	{
		final List<ActivityAlerts.Alert> list = out == null ? new ArrayList<>() : out;
		list.add(alert);
		return list;
	}
}
