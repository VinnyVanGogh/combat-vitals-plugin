package com.vinnyvangogh.combatvitals;

import net.runelite.api.Skill;

/**
 * Decides when the player is working a skilling object: a tree, a rock, a
 * stall.
 *
 * Objects are never interacted with the way NPCs are; no
 * {@code InteractingChanged} fires for a tree. Two things together are the
 * evidence instead: the player clicked a skilling option on the object
 * ("Chop down", "Mine"), and is now animating within reach of it. Either one
 * alone is not enough. A click is followed by a walk that may never arrive,
 * and an animation could be a potion being drunk on the way.
 *
 * Walking out of reach clears the badge at once. Stopping in place (the tree
 * fell, the rock emptied) lets it linger for the timeout like any other
 * engagement, so a respawn a few seconds later does not flicker it.
 *
 * Pure: ticks and tile coordinates in, steps out, no client. Not thread
 * safe; the plugin touches it from the client thread only.
 */
final class ObjectSkilling
{
	/** How long after the click the swing may start: about 18 s of walking. */
	static final int START_TICKS = 30;

	/**
	 * Tiles beyond the object's footprint that still count as at it. The
	 * player works from an adjacent tile; one more covers a rotated
	 * footprint, which the menu does not tell us about.
	 */
	static final int REACH = 2;

	enum Step
	{
		NONE,
		/** The swing started, or started again: raise the badge, cancel any linger. */
		ENGAGE,
		/** The swing stopped in place: let the badge linger. */
		DISENGAGE,
		/** The player left the object: drop the badge now. */
		RELEASE
	}

	private Skill skill;
	private int plane;
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	private int clickTick = -1;

	/** The player has been seen working it since the click. */
	private boolean started;

	/** Animating at it on the last tick. */
	private boolean animating;

	/**
	 * The player clicked an option on an object.
	 *
	 * @param skill the skill the option trains, or null to forget any object
	 * @param x     the object's south-west world tile
	 * @param sizeX its footprint, at least 1
	 */
	void clicked(Skill skill, int x, int y, int plane, int sizeX, int sizeY, int tick)
	{
		forget();
		if (skill == null)
		{
			return;
		}

		this.skill = skill;
		this.plane = plane;
		minX = x;
		minY = y;
		maxX = x + Math.max(1, sizeX) - 1;
		maxY = y + Math.max(1, sizeY) - 1;
		clickTick = tick;
	}

	/** Drop the object: another click, combat, or the badge expired. */
	void forget()
	{
		skill = null;
		clickTick = -1;
		started = false;
		animating = false;
	}

	/** An object is clicked and not yet forgotten. */
	boolean held()
	{
		return skill != null;
	}

	/** The badge is up for this object, lingering or not. */
	boolean started()
	{
		return skill != null && started;
	}

	/** Still swinging at it: holds the badge against the linger timeout. */
	boolean working()
	{
		return started() && animating;
	}

	Skill skill()
	{
		return skill;
	}

	/**
	 * One game tick.
	 *
	 * @param animating the local player has a non-idle animation
	 * @return what the badge should do
	 */
	Step tick(int tick, int playerX, int playerY, int playerPlane, boolean animating)
	{
		if (skill == null)
		{
			return Step.NONE;
		}

		final boolean near = near(playerX, playerY, playerPlane);

		if (!started)
		{
			if (animating && near)
			{
				started = true;
				this.animating = true;
				return Step.ENGAGE;
			}
			if (tick - clickTick > START_TICKS)
			{
				forget();
			}
			return Step.NONE;
		}

		if (!near)
		{
			forget();
			return Step.RELEASE;
		}

		if (animating != this.animating)
		{
			this.animating = animating;
			return animating ? Step.ENGAGE : Step.DISENGAGE;
		}
		return Step.NONE;
	}

	private boolean near(int x, int y, int playerPlane)
	{
		if (playerPlane != plane)
		{
			return false;
		}

		final int dx = Math.max(0, Math.max(minX - x, x - maxX));
		final int dy = Math.max(0, Math.max(minY - y, y - maxY));
		return Math.max(dx, dy) <= REACH;
	}
}
