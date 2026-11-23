package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * The plugin no longer talks to the Shortest Path plugin (RUNELITE-537): the
 * next spot is shown with the game's hint arrow. Nothing a user or the Hub
 * reads (source, resources, README, SETUP, NOTICE) may mention it again.
 * The CHANGELOG, which records the removal, is left out.
 */
public class NoShortestPathTest
{
	private static final String[] SPELLINGS = {"shortest path", "shortestpath", "shortest-path"};

	@Test
	public void noReferenceToShortestPathRemains() throws IOException
	{
		final List<Path> files = new ArrayList<>();
		for (String root : new String[]{"src/main"})
		{
			try (Stream<Path> walk = Files.walk(Paths.get(root)))
			{
				files.addAll(walk.filter(Files::isRegularFile).filter(NoShortestPathTest::isText).collect(Collectors.toList()));
			}
		}
		for (String doc : new String[]{"README.md", "SETUP.md", "NOTICE"})
		{
			files.add(Paths.get(doc));
		}

		final List<String> found = new ArrayList<>();
		for (Path file : files)
		{
			final String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
			for (String spelling : SPELLINGS)
			{
				if (text.contains(spelling))
				{
					found.add(file + " mentions \"" + spelling + "\"");
				}
			}
		}
		assertEquals(new ArrayList<String>(), found);
	}

	@Test
	public void noMessageGoesToAnotherPlugin() throws IOException
	{
		try (Stream<Path> walk = Files.walk(Paths.get("src/main/java")))
		{
			for (Path file : walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList()))
			{
				final String code = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
				assertEquals(file + " posts a PluginMessage", false, code.contains("PluginMessage"));
			}
		}
	}

	private static boolean isText(Path path)
	{
		final String name = path.getFileName().toString();
		return name.endsWith(".java") || name.endsWith(".tsv") || name.endsWith(".properties") || name.endsWith(".txt");
	}
}
