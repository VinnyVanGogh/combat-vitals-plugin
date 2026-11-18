package com.vinnyvangogh.combatvitals;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Skill;

/**
 * Tells when the skilling activity changed (yew logs to maple logs, one skill
 * to another) so the session can reset on its own, and keeps the finished
 * session's summary for a short while so it can still be read.
 *
 * Pure: the skill and resource in, whether to reset out.
 */
final class SessionActivity
{
	/** How long the previous session's summary stays visible: two minutes. */
	static final long SUMMARY_MILLIS = 120_000;

	private static final Pattern LOGS = Pattern.compile("^You get some ([\\w' -]*logs)[.!]?$");
	private static final Pattern ORE = Pattern.compile("^You manage to mine some ([\\w' -]+?)\\.?$");
	private static final Pattern CATCH = Pattern.compile("^You catch (?:an? |some )?([\\w' -]+?)[.!]?$");

	private Skill skill;
	private String resource;

	/** The skill a gathering chat line is about ("You get some yew logs."), or null. */
	static Skill skillOf(String message)
	{
		if (message == null)
		{
			return null;
		}
		if (LOGS.matcher(message).matches())
		{
			return Skill.WOODCUTTING;
		}
		if (ORE.matcher(message).matches())
		{
			return Skill.MINING;
		}
		return CATCH.matcher(message).matches() ? Skill.FISHING : null;
	}

	/** What a gathering chat line gathered, lower case ("yew logs", "iron ore"), or null. */
	static String resourceOf(String message)
	{
		if (message == null)
		{
			return null;
		}
		for (Pattern p : new Pattern[]{LOGS, ORE, CATCH})
		{
			final Matcher m = p.matcher(message);
			if (m.matches())
			{
				return m.group(1).toLowerCase(Locale.ROOT);
			}
		}
		return null;
	}
	private String summary;
	private long summaryAt;

	/**
	 * @param resource what is gathered or made, or null when not known (never a change by itself)
	 * @param autoReset the "Auto-reset session on a new activity" option
	 * @return true when the activity changed and the option is on
	 */
	boolean observe(Skill skill, String resource, boolean autoReset)
	{
		if (skill == null)
		{
			return false;
		}
		if (this.skill == null)
		{
			this.skill = skill;
			this.resource = resource;
			return false;
		}
		final boolean otherSkill = this.skill != skill;
		final boolean otherResource = resource != null && this.resource != null && !Objects.equals(this.resource, resource);
		if (!otherSkill && !otherResource)
		{
			if (this.resource == null)
			{
				this.resource = resource;
			}
			return false;
		}
		this.skill = skill;
		this.resource = resource;
		return autoReset;
	}

	Skill skill()
	{
		return skill;
	}

	String resource()
	{
		return resource;
	}

	void keepSummary(String text, long nowMillis)
	{
		summary = text;
		summaryAt = nowMillis;
	}

	/** The previous session's summary while it is fresh, else null. */
	String previous(long nowMillis)
	{
		return summary != null && nowMillis - summaryAt < SUMMARY_MILLIS ? summary : null;
	}

	void clear()
	{
		skill = null;
		resource = null;
		summary = null;
	}
}
