package com.vinnyvangogh.combatvitals;

import java.util.regex.Pattern;

/**
 * Agility's line on the badge: laps this session, the last lap's time, and
 * marks of grace picked up. "4 laps, last 58s, 3 marks".
 *
 * A lap is the game's own "Your Canifis Rooftop lap count is: 6." message,
 * the same message the base client's Chat Commands plugin reads for its
 * lap counts ({@code KILLCOUNT_PATTERN}). Marks are counted from the
 * inventory's mark of grace stack going up; the first count seen is a
 * baseline, and spending them is not a pickup.
 *
 * Pure: messages, counts and wall-clock milliseconds in, text out.
 */
final class AgilityLaps
{
	private static final Pattern LAP = Pattern.compile("^Your .+ lap count is: [\\d,]+\\.?$");

	private int laps;
	private long lastLapAt = -1;
	private long lastLapMillis = -1;
	private int marks;
	private int marksHeld = -1;

	/** A game message, tags removed. @return true when it was a lap */
	boolean message(String message, long nowMillis)
	{
		if (message == null || !LAP.matcher(message).matches())
		{
			return false;
		}
		laps++;
		if (lastLapAt >= 0)
		{
			lastLapMillis = nowMillis - lastLapAt;
		}
		lastLapAt = nowMillis;
		return true;
	}

	/** Marks of grace in the inventory now. */
	void marksHeld(int held)
	{
		if (marksHeld >= 0 && held > marksHeld)
		{
			marks += held - marksHeld;
		}
		marksHeld = held;
	}

	/** "2 laps, last 58s, 3 marks", or null with nothing to say yet. */
	String label()
	{
		final StringBuilder text = new StringBuilder();
		if (laps > 0)
		{
			text.append(laps).append(laps == 1 ? " lap" : " laps");
			if (lastLapMillis >= 0)
			{
				text.append(", last ").append(SessionRow.duration(lastLapMillis));
			}
		}
		if (marks > 0)
		{
			text.append(text.length() == 0 ? "" : ", ").append(marks).append(marks == 1 ? " mark" : " marks");
		}
		return text.length() == 0 ? null : text.toString();
	}

	void clear()
	{
		laps = 0;
		lastLapAt = -1;
		lastLapMillis = -1;
		marks = 0;
		marksHeld = -1;
	}
}
