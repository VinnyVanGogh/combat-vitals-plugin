package com.vinnyvangogh.combatvitals;

/**
 * When nearby NPCs stop being aggressive, after the game's tolerance rule:
 * about ten minutes in an area, around 21x21 tiles, then they ignore you
 * until you leave it. The base client's "NPC Aggression Timer" plugin is the
 * precedent (it draws the same idea); this is our own reimplementation, with
 * no dependency on that plugin.
 *
 * Unverified in game: the timer starts when an NPC first targets you with no
 * timer running, and the area is centred on where you stood then. Leaving it
 * resets, as does a world hop or a plane change.
 *
 * Pure: tiles, world and game ticks in, a state and text out.
 */
final class AggressionTimer
{
	enum State
	{
		IDLE, RUNNING, CALM
	}

	/** Ten minutes of 0.6 s ticks. */
	static final int TIMER_TICKS = 1000;
	/** Tiles from the centre to the edge: 21x21. */
	static final int RADIUS = 10;

	private boolean started;
	private int centreX;
	private int centreY;
	private int plane;
	private int world = -1;
	private int startTick;

	/** An NPC targeted the player; starts the countdown if none is running. */
	void aggressed(int x, int y, int plane, int world, int tick)
	{
		if (started)
		{
			return;
		}
		started = true;
		centreX = x;
		centreY = y;
		this.plane = plane;
		this.world = world;
		startTick = tick;
	}

	/** Once per game tick: leaving the area, changing plane or world resets. */
	void tick(int x, int y, int plane, int world)
	{
		if (started && (this.world != world || this.plane != plane || !inArea(x, y)))
		{
			clear();
		}
	}

	void clear()
	{
		started = false;
		world = -1;
	}

	State state(int tick)
	{
		if (!started)
		{
			return State.IDLE;
		}
		return remainingTicks(tick) > 0 ? State.RUNNING : State.CALM;
	}

	int remainingTicks(int tick)
	{
		return started ? Math.max(0, TIMER_TICKS - (tick - startTick)) : 0;
	}

	boolean inArea(int x, int y)
	{
		return started && Math.abs(x - centreX) <= RADIUS && Math.abs(y - centreY) <= RADIUS;
	}

	int minX()
	{
		return centreX - RADIUS;
	}

	int maxX()
	{
		return centreX + RADIUS;
	}

	int minY()
	{
		return centreY - RADIUS;
	}

	int maxY()
	{
		return centreY + RADIUS;
	}

	int plane()
	{
		return plane;
	}

	/** "Aggressive for 9:54", "NPCs are no longer aggressive here", or null while idle. */
	String text(int tick)
	{
		final State state = state(tick);
		if (state == State.RUNNING)
		{
			final int seconds = (int) Math.ceil(remainingTicks(tick) * 0.6);
			return String.format("Aggressive for %d:%02d", seconds / 60, seconds % 60);
		}
		return state == State.CALM ? "NPCs are no longer aggressive here" : null;
	}
}
