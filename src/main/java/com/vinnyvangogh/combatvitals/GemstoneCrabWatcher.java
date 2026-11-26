package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

/**
 * Gemstone Crab mode, the client side: once a tick finds the crab and its
 * shell among the NPCs, reads the crab's health bar, and hands them to
 * {@link GemstoneCrab}. Also keeps the caves seen, so the overlay can point
 * at the one to crawl through. Information only: it reads state and shows
 * text and an arrow, and never clicks, walks or changes a menu.
 */
@Singleton
class GemstoneCrabWatcher
{
	private final Client client;
	private final CombatVitalsConfig config;
	private final List<GameObject> caves = new ArrayList<>();
	private final CaveArrow arrow = new CaveArrow();

	/** Where the player stood when the crab or its shell was last in view. */
	private WorldPoint anchor;
	/** Where the crab or its shell was last seen; only a cave near it is worth an arrow. */
	private WorldPoint crabAt;
	private GemstoneCrab crab;
	private int crabWarn = -1;
	private volatile GemstoneCrab.Reading reading;

	@Inject
	GemstoneCrabWatcher(Client client, CombatVitalsConfig config)
	{
		this.client = client;
		this.config = config;
	}

	/** The latest read-out, or null while the mode is off or no crab is about. */
	GemstoneCrab.Reading reading()
	{
		final GemstoneCrab.Reading r = reading;
		return config.gemstoneCrab() && r != null && r.phase != GemstoneCrab.Phase.NONE ? r : null;
	}

	/** Everything, caves included: logout, hop, or the mode switched off. */
	void reset()
	{
		leave();
		clearCaves();
	}

	/**
	 * The scene is reloading: the caves seen belong to the old one. Caves of the
	 * new area are added after this, as it loads, so they must not be wiped by
	 * the leave reset a tick later.
	 */
	void clearCaves()
	{
		caves.clear();
	}

	/** The player left the crab area: the crab's card, timers and arrow go; the caves stay. */
	private void leave()
	{
		reading = null;
		crab = null;
		anchor = null;
		crabAt = null;
		arrow.reset();
	}

	void caveSpawned(GameObject object)
	{
		if (object.getId() == GemstoneCrab.CAVE_OBJECT_ID && !caves.contains(object))
		{
			caves.add(object);
		}
	}

	void caveDespawned(GameObject object)
	{
		caves.remove(object);
	}

	/**
	 * The cave nearest the player, when the arrow is on and it is time to
	 * crawl through (the burrow warning has fired, or the crab has burrowed).
	 */
	GameObject arrowTarget()
	{
		if (!config.gemstoneCrab() || !config.gemstoneCrabArrow() || !arrow.shown())
		{
			return null;
		}
		final Player self = client.getLocalPlayer();
		final WorldPoint from = self == null ? null : self.getWorldLocation();
		final WorldPoint crabSpot = crabAt;
		if (crabSpot == null)
		{
			return null;
		}
		GameObject best = null;
		int bestDistance = Integer.MAX_VALUE;
		for (GameObject cave : caves)
		{
			final WorldPoint at = cave.getWorldLocation();
			if (at == null || !GemstoneCrab.nearCrab(crabSpot.getX(), crabSpot.getY(), crabSpot.getPlane(),
				at.getX(), at.getY(), at.getPlane()))
			{
				continue;
			}
			final int distance = from == null ? 0 : from.distanceTo2D(at);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				best = cave;
			}
		}
		return best;
	}

	/** True only once the crab has actually burrowed; never while it is still up. */
	boolean due(GemstoneCrab.Reading r)
	{
		return r.phase == GemstoneCrab.Phase.SHELL;
	}

	/** Reads this tick; the alerts due, or null when there are none. */
	GemstoneCrab.Alerts tick()
	{
		if (!config.gemstoneCrab())
		{
			reset();
			return null;
		}
		final WorldView view = client.getTopLevelWorldView();
		if (view == null)
		{
			return null;
		}
		boolean crabSeen = false;
		boolean shellSeen = false;
		double fraction = -1;
		WorldPoint spot = null;
		for (NPC npc : view.npcs())
		{
			if (npc.getId() == GemstoneCrab.CRAB_ID)
			{
				crabSeen = true;
				fraction = fraction(npc);
				spot = npc.getWorldLocation();
			}
			else if (npc.getId() == GemstoneCrab.SHELL_ID)
			{
				shellSeen = true;
				spot = npc.getWorldLocation();
			}
		}
		final Player self = client.getLocalPlayer();
		return observe(crabSeen, shellSeen, fraction, spot, self == null ? null : self.getWorldLocation(),
			System.currentTimeMillis());
	}

	/** One tick's reading, with the client already read; the alerts due, or null when the player has left. */
	GemstoneCrab.Alerts observe(boolean crabSeen, boolean shellSeen, double fraction, WorldPoint spot,
		WorldPoint here, long now)
	{
		if (crab == null || crabWarn != warnSeconds())
		{
			crabWarn = warnSeconds();
			crab = new GemstoneCrab(crabWarn);
		}
		if (crabSeen || shellSeen)
		{
			anchor = here;
			crabAt = spot;
		}
		else if (anchor != null && here != null
			&& GemstoneCrab.left(anchor.getX(), anchor.getY(), anchor.getPlane(), here.getX(), here.getY(), here.getPlane()))
		{
			// Teleported or walked away: the card, arrow and outline go with the crab.
			leave();
			return null;
		}
		if (here != null)
		{
			arrow.update(crabSeen, shellSeen, here.getX(), here.getY(), here.getPlane());
		}
		final GemstoneCrab.Alerts alerts = new GemstoneCrab.Alerts();
		reading = crab.step(crabSeen, shellSeen, fraction, now, alerts);
		return alerts;
	}

	private int warnSeconds()
	{
		return config == null ? 0 : config.gemstoneCrabWarnSeconds();
	}

	/** The raw read-out, whatever the config says; for tests. */
	GemstoneCrab.Reading latest()
	{
		return reading;
	}

	/** Whether the cave arrow is up, whatever the config says; for tests. */
	boolean arrowUp()
	{
		return arrow.shown();
	}

	/** The health bar as 0..1, or -1 while the client is not showing one. */
	private static double fraction(NPC npc)
	{
		final int scale = npc.getHealthScale();
		final int ratio = npc.getHealthRatio();
		return scale <= 0 || ratio < 0 ? -1 : Math.min(1.0, (double) ratio / scale);
	}
}
