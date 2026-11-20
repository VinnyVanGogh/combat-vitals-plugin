package com.vinnyvangogh.combatvitals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.runelite.api.Skill;

/**
 * What the sidebar's Live section shows for one tick: your vitals, your
 * target, the skill being trained and this session's gains, every line's text
 * already built. The plugin builds it on the client thread from the readouts
 * the overlays draw ({@link CombatVitalsPlugin.Readout}, {@link SkillingReadout})
 * and {@link SkillingSession}, so there is one data path; the panel only lays
 * it out on the Swing thread ({@link LiveSection}).
 *
 * <p>A half switched off is null here, and the panel says so; an empty state
 * ("Not in combat", "Not skilling") is a null target or header.
 */
final class LiveVitals
{
	/** The combat skills whose XP says what a fight is training; Hitpoints rides along with all of them. */
	static final Skill[] COMBAT_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};

	static final LiveVitals LOGGED_OUT = new LiveVitals(false, null, null, Collections.emptyList());

	final boolean loggedIn;
	/** Null while Combat vitals is off. */
	final Combat combat;
	/** Null while Skilling vitals is off. */
	final Skilling skilling;
	/** "Fishing: +12.3k xp, +1 lvl in 23m", one per skill gained in this session, the most XP first. */
	final List<Gain> session;

	/** The same skills over the whole login, which an activity change does not reset; empty until a gain. */
	final List<Gain> totals;

	LiveVitals(boolean loggedIn, Combat combat, Skilling skilling, List<Gain> session)
	{
		this(loggedIn, combat, skilling, session, Collections.emptyList());
	}

	LiveVitals(boolean loggedIn, Combat combat, Skilling skilling, List<Gain> session, List<Gain> totals)
	{
		this.loggedIn = loggedIn;
		this.combat = combat;
		this.skilling = skilling;
		this.session = session;
		this.totals = totals;
	}

	/**
	 * "This session: +12.3k xp in 23m" and "Total: +40.1k xp in 1h 05m" for the skills of one card; the session
	 * resets on a new activity, the total only at logout. Time is active time, paused stretches left out.
	 */
	static List<Period> periods(SkillingSession session, SkillingSession total, Skill[] skills, long nowMillis)
	{
		return Arrays.asList(period("This session", session, skills, nowMillis),
			period("Total", total, skills, nowMillis));
	}

	/** Every skill's three rows, for the Overall card: the all-time row sits with the other totals. */
	List<Period> overall = Collections.emptyList();

	LiveVitals withOverall(List<Period> overall)
	{
		this.overall = overall;
		return this;
	}

	/**
	 * The row after "This session" and "Total": all time, kept across logins: xp, time trained and the average
	 * xp/h over it. The tooltip adds the actions.
	 */
	static Period allTime(AllTime.Stats s)
	{
		if (s.isEmpty())
		{
			return Period.nothingYet("All time");
		}
		final String gained = "+" + SessionRow.shortNumber(s.xp) + " xp";
		final String time = SessionRow.duration(s.millis);
		final long perHour = s.millis >= Period.MIN_RATE_MILLIS ? s.xp * 3_600_000L / s.millis : -1;
		final String tip = "All time: " + GoalMath.number(s.xp) + " xp, " + GoalMath.number(s.actions)
			+ " actions in " + time + (perHour < 0 ? "" : ", " + GoalMath.number(perHour) + " xp/h")
			+ ". Kept across sessions.";
		return new Period("All time", gained, time, perHour < 0 ? "-" : SessionRow.shortNumber(perHour) + "/h", tip);
	}

	/**
	 * One compact row in three aligned columns after its label: xp, time, xp/h. Empty cells are "" (nothing yet) or
	 * "-" (no rate yet); {@link #tip} says it all in words, for the row's tooltip.
	 */
	static final class Period
	{
		/** The first minute has no rate to show; a rate over a shorter stretch would be noise. */
		static final long MIN_RATE_MILLIS = 60_000;

		final String label;
		/** "+12.3k xp", or "nothing yet". */
		final String xp;
		/** "23m", or "". */
		final String time;
		/** "45.2k/h", "-" before a rate exists, "paused" while paused, or "". */
		final String rate;
		final String tip;

		Period(String label, String xp, String time, String rate, String tip)
		{
			this.label = label;
			this.xp = xp;
			this.time = time;
			this.rate = rate;
			this.tip = tip;
		}

		/** A row with nothing in it yet. */
		static Period nothingYet(String label)
		{
			return new Period(label, "nothing yet", "", "", label + ": nothing yet");
		}
	}

	/** One unmet skill goal: a single bar with its words inside ({@code bar.text}), and every detail in a tooltip. */
	static final class GoalLine
	{
		/** The goal in full, "Magic level 55". */
		final String title;
		/** Progress toward the goal; its text is "Magic 55 · 76% · ~54m". */
		final Bar bar;
		/** xp to go, actions left and the session and last-hour rates ({@link GoalMath#summary}). */
		final String tooltip;

		GoalLine(String title, Bar bar, String tooltip)
		{
			this.title = title;
			this.bar = bar;
			this.tooltip = tooltip;
		}
	}

	private static Period period(String label, SkillingSession s, Skill[] skills, long nowMillis)
	{
		long xp = 0;
		long millis = -1;
		boolean paused = false;
		for (Skill skill : skills)
		{
			if (s.actions(skill) == 0)
			{
				continue;
			}
			xp += s.xpGained(skill);
			millis = Math.max(millis, s.elapsed(skill, nowMillis));
			paused |= s.paused(skill, nowMillis);
		}
		if (millis < 0)
		{
			return Period.nothingYet(label);
		}
		final String gained = "+" + SessionRow.shortNumber((int) Math.min(Integer.MAX_VALUE, xp)) + " xp";
		final String time = SessionRow.duration(millis);
		final long perHour = millis >= Period.MIN_RATE_MILLIS ? xp * 3_600_000L / millis : -1;
		final String rate = perHour < 0 ? "-" : SessionRow.shortNumber(perHour) + "/h";
		final String tip = label + ": " + gained + " in " + time + (paused ? " (paused)" : "")
			+ (perHour < 0 ? "" : ", " + GoalMath.number(perHour) + " xp/h");
		return new Period(label, gained, time, paused ? "paused" : rate, tip);
	}

	/** A bar with its words: fraction 0 to 1, or -1 while unknown (drawn empty). */
	static final class Bar
	{
		final double fraction;
		final String text;
		final Color colour;

		Bar(double fraction, String text, Color colour)
		{
			this.fraction = fraction;
			this.text = text;
			this.colour = colour;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Bar))
			{
				return false;
			}
			final Bar b = (Bar) o;
			return Double.compare(fraction, b.fraction) == 0 && Objects.equals(text, b.text)
				&& Objects.equals(colour, b.colour);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(fraction, text, colour);
		}
	}

	static final class Combat
	{
		final Bar hitpoints;
		final Bar prayer;
		/** "Run 64% | Spec 100%". */
		final String energy;
		/** The target's name, or null: "Not in combat". */
		final String targetName;
		final Bar target;
		/** The combat skill gained in last, its level progress; null before any combat XP. */
		final Bar training;
		/** "45.2k xp/h | lvl 71 in 1h 05m", or null before a rate exists. */
		final String trainingRate;

		Combat(Bar hitpoints, Bar prayer, String energy, String targetName, Bar target, Bar training,
			String trainingRate)
		{
			this.hitpoints = hitpoints;
			this.prayer = prayer;
			this.energy = energy;
			this.targetName = targetName;
			this.target = target;
			this.training = training;
			this.trainingRate = trainingRate;
		}

		/** Unmet goals in the skills this card tracks, one line each, as the Skilling card has them. */
		List<GoalLine> goals = Collections.emptyList();
		/** The "This session" and "Total" rows, see {@link LiveVitals#periods}. */
		List<Period> periods = Collections.emptyList();

		/** "Active · running 14m", "Pauses in 0:42": the session's timers, see {@link CardTimers}. */
		List<String> timers = Collections.emptyList();

		Combat with(List<GoalLine> goals, List<Period> periods)
		{
			this.goals = goals;
			this.periods = periods;
			return this;
		}

		Combat withTimers(CardTimers timers)
		{
			this.timers = timers.lines;
			return this;
		}
	}

	static final class Skilling
	{
		/** The badge's header ("Fishing 72 (+3) 41s", "Not fishing"), or null: "Not skilling". */
		final String header;
		final Color colour;
		/** The skill's small icon, or null (then a dot in the skill's colour). Drawn only. */
		final BufferedImage icon;
		/** The XP row, or null with it off. */
		final Bar xp;
		/** The session row's stats by kind ("60.1k xp/h"), the ones the badge has now. */
		final Map<SessionRow.Kind, String> stats;
		/** A training method's timers, "Active" and "Pump stops in 97:30", each its own chip. */
		final List<String> chips;
		/** Gear warning, the inventory line and each container's, as the badge has them. */
		final List<String> lines;

		/** The unmet goals in this skill, one line each ("Goal Mining level 90: 1,200 xp to go ..."); may be empty. */
		final List<GoalLine> goals;

		Skilling(String header, Color colour, BufferedImage icon, Bar xp, Map<SessionRow.Kind, String> stats,
			List<String> chips, List<String> lines)
		{
			this(header, colour, icon, xp, stats, chips, lines, Collections.emptyList());
		}

		Skilling(String header, Color colour, BufferedImage icon, Bar xp, Map<SessionRow.Kind, String> stats,
			List<String> chips, List<String> lines, List<GoalLine> goals)
		{
			this.goals = goals;
			this.header = header;
			this.colour = colour;
			this.icon = icon;
			this.xp = xp;
			this.stats = stats;
			this.chips = chips;
			this.lines = lines;
		}

		/** "This session: ..." and "Total: ..." lines for the active skill, see {@link LiveVitals#periods}. */
		List<Period> periods = Collections.emptyList();

		Skilling withPeriods(List<Period> periods)
		{
			this.periods = periods;
			return this;
		}

		/** "Woodcutting 62", the skill's name and level: what the card's title says once it is Paused. */
		String title;
		/** The work stopped or the clock paused: the card stays, marked Paused, until the user clears it. */
		boolean paused;
		/** "Paused · ran 14m", "Paused for 2:10", "Session resets in 12:50": see {@link CardTimers}. */
		List<String> timers = Collections.emptyList();

		Skilling withTimers(CardTimers timers)
		{
			this.paused = timers.paused;
			this.timers = timers.lines;
			return this;
		}
	}

	/** One skill gained in this session: its line (the words inside its bar), its XP for ranking, and its colour. */
	static final class Gain
	{
		final String text;
		final int xp;
		final Color colour;
		/** The row's tooltip: the line, and the rate when there is one. */
		final String tip;

		Gain(String text, int xp, Color colour)
		{
			this(text, xp, colour, text);
		}

		Gain(String text, int xp, Color colour, String tip)
		{
			this.text = text;
			this.xp = xp;
			this.colour = colour;
			this.tip = tip;
		}
	}

	// ---- Building, from what the plugin already has ----

	static final Color HITPOINTS = new Color(200, 50, 50);
	static final Color PRAYER = new Color(80, 160, 230);
	static final Color TARGET = new Color(60, 180, 60);

	/** "Hitpoints 72/99". */
	static Bar level(String name, int current, int max, Color colour)
	{
		return new Bar(max <= 0 ? -1 : Math.max(0, Math.min(1.0, (double) current / max)),
			name + " " + current + "/" + max, colour);
	}

	/** "Run 64% on | Spec 100%": the run toggle, on or off, is said beside the run energy. */
	static String energy(int runHundredths, int specTenths, boolean running)
	{
		return "Run " + runHundredths / 100 + "% " + (running ? "on" : "off") + " | Spec " + specTenths / 10 + "%";
	}

	/** "Run 64% | Spec 100%"; run energy in hundredths and spec in tenths of a percent, as the client gives them. */
	static String energy(int runHundredths, int specTenths)
	{
		return "Run " + runHundredths / 100 + "% | Spec " + specTenths / 10 + "%";
	}

	/** The target's bar, the same text and fraction the overhead readout draws; null without a readout. */
	static Bar target(CombatVitalsPlugin.Readout readout, boolean decimals)
	{
		if (readout == null)
		{
			return null;
		}
		final String text = readout.text(decimals);
		final double fraction = HealthReadout.fraction(readout.getRatio(), readout.getScale(),
			readout.getExactHealth(), readout.getMaxHealth());
		return new Bar(fraction, text == null ? "Health unknown" : text, TARGET);
	}

	/** The combat skill trained last: "Attack: Lv 70 -> 71: 45% (12,345 xp)". */
	static Bar training(Skill skill, XpProgress xp, Color colour)
	{
		return new Bar(xp.getFraction(), skill.getName() + ": " + xp.text(), colour);
	}

	/** "45.2k xp/h | lvl 71 in 1h 05m", or null for the first minute. */
	static String trainingRate(XpProgress xp, int xpPerHour)
	{
		final String rate = SessionRow.xpRate(xpPerHour);
		if (rate == null)
		{
			return null;
		}
		final String toLevel = SessionRow.timeToLevel(xp, xpPerHour);
		return toLevel == null ? rate : rate + " | " + toLevel;
	}

	/** The skilling half from the badge's own readout; a null readout is "Not skilling". */
	static Skilling skilling(SkillingReadout r)
	{
		return skilling(r, null);
	}

	/** The same, with the skill's icon for the card's title. */
	static Skilling skilling(SkillingReadout r, BufferedImage icon)
	{
		return skilling(r, icon, Collections.emptyList());
	}

	/** The same, with the active skill's goal lines under the readout. */
	static Skilling skilling(SkillingReadout r, BufferedImage icon, List<GoalLine> goals)
	{
		if (r == null)
		{
			return new Skilling(null, SkillColours.DEFAULT, null, null, Collections.emptyMap(),
				Collections.emptyList(), Collections.emptyList());
		}
		final Bar xp = r.getXpText() == null || r.getXp() == null ? null
			: new Bar(r.getXp().getFraction(), r.getXpText(), r.getColour());
		final Map<SessionRow.Kind, String> stats = new EnumMap<>(SessionRow.Kind.class);
		for (SessionRow.Cell c : r.getSessionCells())
		{
			stats.put(c.getKind(), c.getText());
		}
		// A training method puts its timers where the inventory line goes ("Active | Pump stops in 97:30").
		final boolean timers = r.getTrainingMethod() != null && r.getInventoryLabel() != null;
		final List<String> chips = timers ? Arrays.asList(r.getInventoryLabel().split(" \\| "))
			: Collections.emptyList();
		final Skilling out = new Skilling(SkillingBadgeRenderer.headerText(r), r.getColour(), icon, xp, stats, chips,
			lines(r, !timers), goals);
		out.title = r.getSkill().getName() + " " + r.getReal();
		return out;
	}

	/** The badge's lines under its XP and session rows: gear warning, inventory, containers. */
	private static List<String> lines(SkillingReadout r, boolean inventory)
	{
		final List<String> lines = new ArrayList<>();
		if (r.getGearText() != null)
		{
			lines.add(r.getGearText());
		}
		if (inventory && r.getInventoryLabel() != null)
		{
			lines.add(r.getInventoryLabel());
		}
		for (ContainerRow row : r.getContainers())
		{
			lines.add(row.label(r.getContainerCount()));
		}
		return lines;
	}

	/** Each skill gained in this session, the most XP first; colours from the Look tab's skill colours. */
	static List<Gain> session(SkillingSession s, long nowMillis, Function<Skill, Color> colours)
	{
		final List<Gain> out = new ArrayList<>();
		for (Skill skill : Skill.values())
		{
			final int gained = s.xpGained(skill);
			// Only gains: the login reading is a baseline, never shown as XP made.
			if (s.actions(skill) == 0 || gained <= 0)
			{
				continue;
			}
			final StringBuilder line = new StringBuilder(skill.getName()).append(": +")
				.append(SessionRow.shortNumber(gained)).append(" xp");
			final int levels = s.levelsGained(skill);
			if (levels > 0)
			{
				line.append(", +").append(levels).append(levels == 1 ? " lvl" : " lvls");
			}
			final long elapsed = s.elapsed(skill, nowMillis);
			line.append(" in ").append(SessionRow.duration(elapsed));
			final String text = line.toString();
			final String tip = elapsed >= Period.MIN_RATE_MILLIS
				? text + ", " + GoalMath.number(gained * 3_600_000L / elapsed) + " xp/h" : text;
			out.add(new Gain(text, gained, colours.apply(skill), tip));
		}
		out.sort(Comparator.comparingInt((Gain g) -> g.xp).reversed());
		return out;
	}
}
