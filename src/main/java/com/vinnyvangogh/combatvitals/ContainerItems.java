package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a log basket or forestry kit holds, item by item ("14 Yew logs"),
 * from a Check dialog's text and the gathering chat lines. {@link
 * ContainerContents} keeps only the total; this keeps the kinds.
 *
 * Unverified in game: the forestry kit's leaf line ("You collect some maple
 * leaves.") is an assumption; an unmatched line is simply ignored and the
 * kit's contents stay unknown until checked.
 *
 * Pure: strings in, strings and maps out.
 */
final class ContainerItems
{
	private static final Pattern ENTRY = Pattern.compile("(\\d+) x ([^,]+)");
	private static final Pattern LOGS = Pattern.compile("^You get some ([\\w' -]*logs)[.!]?$");
	private static final Pattern LEAVES = Pattern.compile("^You collect some ([\\w' -]*leaves)[.!]?$");

	private ContainerItems()
	{
	}

	/** "14 x Yew logs, 3 x Oak logs" to {Yew logs=14, Oak logs=3}, in order. */
	static Map<String, Integer> parse(String text)
	{
		final Map<String, Integer> items = new LinkedHashMap<>();
		if (text == null)
		{
			return items;
		}
		final Matcher m = ENTRY.matcher(text);
		while (m.find())
		{
			items.merge(m.group(2).trim(), Integer.parseInt(m.group(1)), Integer::sum);
		}
		return items;
	}

	static int total(Map<String, Integer> items)
	{
		int total = 0;
		for (int n : items.values())
		{
			total += n;
		}
		return total;
	}

	static void add(Map<String, Integer> items, String name)
	{
		items.merge(name, 1, Integer::sum);
	}

	/** "14 Yew logs, 3 Oak logs, +1 more": biggest first, at most {@code max} kinds. */
	static String label(Map<String, Integer> items, int max)
	{
		final List<Map.Entry<String, Integer>> sorted = new ArrayList<>(items.entrySet());
		sorted.sort((a, b) -> b.getValue() - a.getValue());
		final StringBuilder out = new StringBuilder();
		for (int i = 0; i < sorted.size() && i < max; i++)
		{
			out.append(i == 0 ? "" : ", ").append(sorted.get(i).getValue()).append(' ').append(sorted.get(i).getKey());
		}
		if (sorted.size() > max)
		{
			out.append(", +").append(sorted.size() - max).append(" more");
		}
		return out.toString();
	}

	/** "You get some yew logs." to "Yew logs", or null. */
	static String gathered(String message)
	{
		return capture(LOGS, message);
	}

	/** "You collect some maple leaves." to "Maple leaves", or null. */
	static String leaf(String message)
	{
		return capture(LEAVES, message);
	}

	private static String capture(Pattern pattern, String message)
	{
		if (message == null)
		{
			return null;
		}
		final Matcher m = pattern.matcher(message);
		if (!m.matches())
		{
			return null;
		}
		final String name = m.group(1).trim().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
