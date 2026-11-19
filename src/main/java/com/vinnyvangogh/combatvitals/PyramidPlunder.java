package com.vinnyvangogh.combatvitals;

/**
 * Pyramid Plunder run state for the skilling badge: which room you are in,
 * what it asks of you, how long is left, and what this room has paid so far.
 *
 * Replaces the stock Pyramid Plunder plugin's timer and room readout. The
 * stock plugin is BSD-2; nothing is copied, the thieving levels are the OSRS
 * wiki's table.
 *
 * Pure: values the client reported in, text and decisions out. Client thread
 * only; not thread safe.
 */
final class PyramidPlunder
{
	static final int ROOMS = 8;

	/** Thieving level each room asks for, room 1 first (OSRS wiki, Pyramid Plunder). */
	private static final int[] LEVEL_FOR_ROOM = {21, 31, 41, 51, 61, 71, 81, 91};

	/** Seconds left at or under which the timer is urgent. */
	static final int URGENT_SECONDS = 30;

	private int room;
	private int secondsLeft = -1;
	private int roomXp;
	private long roomLoot;
	private int roomItems;

	/** Thieving level room {@code room} (1..8) asks for, or -1 out of range. */
	static int levelForRoom(int room)
	{
		return room < 1 || room > ROOMS ? -1 : LEVEL_FOR_ROOM[room - 1];
	}

	/**
	 * The room number the game reports. A new room starts that room's tally
	 * from zero; the same room again changes nothing.
	 */
	void setRoom(int room)
	{
		if (room == this.room)
		{
			return;
		}
		this.room = room;
		roomXp = 0;
		roomLoot = 0;
		roomItems = 0;
	}

	/** Seconds the timer shows, or a negative number when it is not showing. */
	void setSecondsLeft(int seconds)
	{
		secondsLeft = seconds;
	}

	void addXp(int xp)
	{
		if (room > 0 && xp > 0)
		{
			roomXp += xp;
		}
	}

	/** An item taken from an urn, chest or sarcophagus, worth {@code value} gp in total. */
	void addLoot(long value, int quantity)
	{
		if (room > 0 && quantity > 0)
		{
			roomLoot += Math.max(0, value);
			roomItems += quantity;
		}
	}

	/** Leaving the pyramid, or logging out. */
	void clear()
	{
		room = 0;
		secondsLeft = -1;
		roomXp = 0;
		roomLoot = 0;
		roomItems = 0;
	}

	boolean active()
	{
		return room > 0;
	}

	int room()
	{
		return room;
	}

	int roomXp()
	{
		return roomXp;
	}

	long roomLoot()
	{
		return roomLoot;
	}

	int roomItems()
	{
		return roomItems;
	}

	boolean urgent()
	{
		return active() && secondsLeft >= 0 && secondsLeft <= URGENT_SECONDS;
	}

	/** Whether the player's Thieving level opens the next room. */
	boolean nextRoomOpen(int thievingLevel)
	{
		return room < ROOMS && thievingLevel >= levelForRoom(room + 1);
	}

	/** {@code 4:32}, or empty while the timer is not showing. */
	String timerText()
	{
		if (!active() || secondsLeft < 0)
		{
			return "";
		}
		return (secondsLeft / 60) + ":" + (secondsLeft % 60 < 10 ? "0" : "") + (secondsLeft % 60);
	}

	/** {@code Room 3/8}, or empty outside a run. */
	String roomText()
	{
		return active() ? "Room " + room + "/" + ROOMS : "";
	}
}
