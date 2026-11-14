package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * Every image and local link in the README, SETUP and docs resolves: a file
 * that is there, and a {@code #heading} that is in the page. Web links are
 * not fetched; tests stay offline.
 */
public class DocsLinksTest
{
	/** Markdown {@code [text](target)} and {@code ![alt](target)}, and HTML src/href. */
	private static final Pattern MARKDOWN = Pattern.compile("\\]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
	private static final Pattern HTML = Pattern.compile("(?:src|href)=\"([^\"]+)\"");
	private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.+?)\\s*$", Pattern.MULTILINE);
	private static final Pattern IMAGE_EXT = Pattern.compile("(?i).*\\.(png|jpe?g|gif|webp)$");

	@Test
	public void everyImageAndLocalLinkResolves() throws IOException
	{
		final List<String> broken = new ArrayList<>();
		for (File page : pages())
		{
			broken.addAll(brokenLinks(page));
		}
		assertTrue("broken links:\n" + String.join("\n", broken), broken.isEmpty());
	}

	@Test
	public void theReadmeOnlyUsesImagesUnderDocsImages() throws IOException
	{
		final List<String> outside = new ArrayList<>();
		for (String target : targets(read(new File("README.md"))))
		{
			if (IMAGE_EXT.matcher(path(target)).matches() && !isWeb(target)
				&& !path(target).startsWith("docs/images/"))
			{
				outside.add(target);
			}
		}
		assertTrue("README images outside docs/images: " + outside, outside.isEmpty());
	}

	/** In-game captures are bigger than the generated ones, so they get their own, looser budget. */
	@Test
	public void everyReadmeImageIsWithinBudget() throws IOException
	{
		final long budget = 160 * 1024;
		final List<String> over = new ArrayList<>();
		for (String target : targets(read(new File("README.md"))))
		{
			final File f = new File(path(target));
			if (!isWeb(target) && IMAGE_EXT.matcher(f.getName()).matches() && f.length() > budget)
			{
				over.add(target + " is " + f.length() / 1024 + " KB");
			}
		}
		assertTrue("over " + budget / 1024 + " KB: " + over, over.isEmpty());
	}

	@Test
	public void theCheckerCatchesAMissingFileAndAMissingHeading() throws IOException
	{
		final File dir = Files.createTempDirectory("docs-links").toFile();
		final File page = new File(dir, "page.md");
		Files.write(page.toPath(), ("# Real heading\n\n![x](nope.png) [ok](#real-heading) [bad](#no-such)\n")
			.getBytes(StandardCharsets.UTF_8));
		final List<String> broken = brokenLinks(page);
		assertTrue(broken.toString(), broken.size() == 2);
	}

	static List<File> pages()
	{
		final List<File> pages = new ArrayList<>();
		for (String name : new String[]{"README.md", "SETUP.md", "CHANGELOG.md", "CONTRIBUTING.md"})
		{
			final File f = new File(name);
			if (f.isFile())
			{
				pages.add(f);
			}
		}
		final File[] docs = new File("docs").listFiles((d, n) -> n.endsWith(".md"));
		if (docs != null)
		{
			for (File f : docs)
			{
				pages.add(f);
			}
		}
		return pages;
	}

	static List<String> brokenLinks(File page) throws IOException
	{
		final String text = read(page);
		final Set<String> anchors = anchors(text);
		final List<String> broken = new ArrayList<>();
		for (String target : targets(text))
		{
			if (isWeb(target))
			{
				continue;
			}
			final int hash = target.indexOf('#');
			final String file = path(target);
			final String anchor = hash < 0 ? null : target.substring(hash + 1);
			if (file.isEmpty())
			{
				if (anchor != null && !anchors.contains(anchor))
				{
					broken.add(page + ": no heading #" + anchor);
				}
			}
			else if (!new File(page.getAbsoluteFile().getParentFile(), file).exists())
			{
				broken.add(page + ": missing " + file);
			}
		}
		return broken;
	}

	private static List<String> targets(String text)
	{
		final String withoutCode = text.replaceAll("(?s)```.*?```", "").replaceAll("(?s)<!--.*?-->", "");
		final List<String> targets = new ArrayList<>();
		for (Pattern p : new Pattern[]{MARKDOWN, HTML})
		{
			final Matcher m = p.matcher(withoutCode);
			while (m.find())
			{
				targets.add(m.group(1));
			}
		}
		return targets;
	}

	/** GitHub's heading slugs: lower case, punctuation dropped, spaces to dashes. */
	static Set<String> anchors(String text)
	{
		final Set<String> anchors = new HashSet<>();
		final Matcher m = HEADING.matcher(text.replaceAll("(?s)```.*?```", ""));
		while (m.find())
		{
			anchors.add(m.group(1).toLowerCase(Locale.ROOT)
				.replaceAll("[^\\p{L}\\p{N} _-]", "")
				.replace(' ', '-'));
		}
		return anchors;
	}

	private static boolean isWeb(String target)
	{
		return target.startsWith("http://") || target.startsWith("https://") || target.startsWith("mailto:");
	}

	private static String path(String target)
	{
		final int hash = target.indexOf('#');
		return hash < 0 ? target : target.substring(0, hash);
	}

	private static String read(File f) throws IOException
	{
		return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
	}
}
