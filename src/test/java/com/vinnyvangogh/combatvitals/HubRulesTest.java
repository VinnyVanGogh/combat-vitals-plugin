package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * The Plugin Hub's own source checks, run here first so a flagged line never
 * reaches a Hub PR. Each rule is something the Hub's bot has refused.
 */
public class HubRulesTest
{
	private static final Path MAIN = Paths.get("src/main/java");

	/** "Use of javax.sound, use net.runelite.client.audio.AudioPlayer instead" (plugin-hub #20795). */
	@Test
	public void noJavaxSoundInThePlugin() throws IOException
	{
		assertEquals("lines naming javax.sound", new ArrayList<String>(), linesContaining("javax.sound"));
	}

	private static List<String> linesContaining(String needle) throws IOException
	{
		assertTrue("run from the project root: " + MAIN.toAbsolutePath(), Files.isDirectory(MAIN));
		final List<Path> sources;
		try (Stream<Path> walk = Files.walk(MAIN))
		{
			sources = walk.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
		}
		assertTrue("no sources found", sources.size() > 10);
		final List<String> hits = new ArrayList<>();
		for (Path source : sources)
		{
			final List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
			for (int i = 0; i < lines.size(); i++)
			{
				if (lines.get(i).contains(needle))
				{
					hits.add(MAIN.relativize(source) + ":" + (i + 1) + ": " + lines.get(i).trim());
				}
			}
		}
		return hits;
	}
}
