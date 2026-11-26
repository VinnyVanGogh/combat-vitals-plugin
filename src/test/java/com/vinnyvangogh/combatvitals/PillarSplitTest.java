package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.Test;

/** Both split layouts: where the parts go, and the pixels each part paints. */
public class PillarSplitTest
{
	private static final Rectangle BOX = new Rectangle(4, 4, SidePillars.WIDTH, SidePillars.HEIGHT);

	@Test
	public void offIsTheWholePillar()
	{
		final Rectangle[] parts = PillarSplit.OFF.split(BOX);
		assertEquals(1, parts.length);
		assertEquals(BOX, parts[0]);
		assertEquals(1, PillarSplit.OFF.segments());
	}

	@Test
	public void sideBySideIsTwoLanesAtFullHeightAndTheGapCoversTheRest()
	{
		final Rectangle[] parts = PillarSplit.SIDE_BY_SIDE.split(BOX);
		assertEquals(2, parts.length);
		assertEquals(BOX.height, parts[0].height);
		assertEquals(BOX.height, parts[1].height);
		assertEquals(BOX.y, parts[0].y);
		assertEquals(BOX.x, parts[0].x);
		assertEquals(parts[0].x + parts[0].width + PillarSplit.GAP, parts[1].x);
		assertEquals(BOX.x + BOX.width, parts[1].x + parts[1].width);
		assertFalse(parts[0].intersects(parts[1]));
		assertEquals(BOX.width, parts[0].width + PillarSplit.GAP + parts[1].width);
	}

	@Test
	public void topAndBottomAreTwoHalvesAtFullWidth()
	{
		final Rectangle[] parts = PillarSplit.TOP_BOTTOM.split(BOX);
		assertEquals(2, parts.length);
		assertEquals(BOX.width, parts[0].width);
		assertEquals(BOX.width, parts[1].width);
		assertEquals(BOX.y, parts[0].y);
		assertEquals(parts[0].y + parts[0].height + PillarSplit.GAP, parts[1].y);
		assertEquals(BOX.y + BOX.height, parts[1].y + parts[1].height);
		assertFalse(parts[0].intersects(parts[1]));
		assertEquals(BOX.height, parts[0].height + PillarSplit.GAP + parts[1].height);
	}

	@Test
	public void anOddSizeLosesNothing()
	{
		final Rectangle odd = new Rectangle(0, 0, 21, 253);
		final Rectangle[] lanes = PillarSplit.SIDE_BY_SIDE.split(odd);
		assertEquals(21, lanes[0].width + PillarSplit.GAP + lanes[1].width);
		final Rectangle[] halves = PillarSplit.TOP_BOTTOM.split(odd);
		assertEquals(253, halves[0].height + PillarSplit.GAP + halves[1].height);
	}

	@Test
	public void aSecondPartCannotBeTheDefaultPillar()
	{
		final PillarSetup setup = new PillarSetup(PillarSplit.TOP_BOTTOM, PillarSource.AMMO, PillarSource.DEFAULT);
		assertEquals(PillarSource.NONE, setup.source(1));
		assertEquals(PillarSetup.DEFAULT, new PillarSetup(null, null, null));
		assertEquals(PillarSource.DEFAULT, new PillarSetup(null, null, null).source(0));
	}

	@Test
	public void onlyACustomSourceDrawsWithoutTheModeOn()
	{
		assertFalse(PillarSetup.DEFAULT.anyCustom());
		assertTrue(new PillarSetup(PillarSplit.OFF, PillarSource.AMMO, PillarSource.NONE).anyCustom());
		// With no split, the second source is not shown, so it does not count.
		assertFalse(new PillarSetup(PillarSplit.OFF, PillarSource.DEFAULT, PillarSource.AMMO).anyCustom());
		assertTrue(new PillarSetup(PillarSplit.SIDE_BY_SIDE, PillarSource.DEFAULT, PillarSource.AMMO).anyCustom());
	}

	private static BufferedImage paint(PillarSplit split, Color first, Color second)
	{
		final BufferedImage image = new BufferedImage(BOX.x * 2 + BOX.width, BOX.y * 2 + BOX.height,
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			final Rectangle[] parts = split.split(BOX);
			final Color[] colours = {first, second};
			for (int i = 0; i < parts.length; i++)
			{
				final Pillar pillar = Pillar.builder().fraction(1).colour(colours[i]).build();
				PillarPainter.paint(g, parts[i], pillar, PillarStyle.FLAT, PillarLabels.NONE, Color.BLACK);
			}
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	private static boolean close(int argb, Color colour)
	{
		final Color c = new Color(argb, true);
		return Math.abs(c.getRed() - colour.getRed()) < 12 && Math.abs(c.getGreen() - colour.getGreen()) < 12
			&& Math.abs(c.getBlue() - colour.getBlue()) < 12;
	}

	@Test
	public void sideBySidePaintsEachSourceInItsOwnLaneTheWholeHeight()
	{
		final Color a = new Color(200, 30, 30);
		final Color b = new Color(30, 30, 200);
		final BufferedImage image = paint(PillarSplit.SIDE_BY_SIDE, a, b);
		final Rectangle[] parts = PillarSplit.SIDE_BY_SIDE.split(BOX);
		for (int y : new int[] {BOX.y + 3, BOX.y + BOX.height / 2, BOX.y + BOX.height - 3})
		{
			assertTrue(close(image.getRGB(parts[0].x + parts[0].width / 2, y), a));
			assertTrue(close(image.getRGB(parts[1].x + parts[1].width / 2, y), b));
		}
		save(image, "split-side-by-side");
	}

	@Test
	public void topAndBottomPaintsEachSourceInItsOwnHalf()
	{
		final Color a = new Color(200, 30, 30);
		final Color b = new Color(30, 30, 200);
		final BufferedImage image = paint(PillarSplit.TOP_BOTTOM, a, b);
		final Rectangle[] parts = PillarSplit.TOP_BOTTOM.split(BOX);
		final int x = BOX.x + BOX.width / 2;
		assertTrue(close(image.getRGB(x, parts[0].y + parts[0].height / 2), a));
		assertTrue(close(image.getRGB(x, parts[1].y + parts[1].height / 2), b));
		save(image, "split-top-bottom");
	}

	private static void save(BufferedImage image, String name)
	{
		final File out = new File("build/pillar-renders");
		try
		{
			out.mkdirs();
			ImageIO.write(image, "png", new File(out, name + ".png"));
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}
	}
}
