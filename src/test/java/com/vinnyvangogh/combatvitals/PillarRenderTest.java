package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import org.junit.Test;

/**
 * Paints each pillar state headless and checks the pixels: labels on and
 * off, the idle dim, the Status Bars shine and the slot ticks. The images
 * go to build/pillar-renders for the PR and a look by eye.
 */
public class PillarRenderTest
{
	private static final File OUT = new File("build/pillar-renders");
	private static final Rectangle BOX = new Rectangle(4, 4, SidePillars.WIDTH, SidePillars.HEIGHT);
	private static final Color FISHING = SkillColours.of(Skill.FISHING);
	private static final SkillIconManager ICONS = new SkillIconManager();

	private static Pillar xp(double fraction, boolean idle)
	{
		final XpProgress p = XpProgress.of(Experience.getXpForLevel(16)
			+ (int) ((Experience.getXpForLevel(17) - Experience.getXpForLevel(16)) * fraction));
		return Pillar.builder()
			.fraction(p.getFraction())
			.colour(FISHING)
			.icon(ICONS.getSkillImage(Skill.FISHING, true))
			.number(PillarText.xpNumber(p, XpDisplay.PERCENT))
			.bottom(PillarText.xpBottom(p))
			.divisions(4)
			.idle(idle)
			.build();
	}

	private static Pillar inventory(int empty, boolean idle)
	{
		final double fill = InventoryFill.fraction(InventoryFill.SLOTS - empty, empty);
		return Pillar.builder()
			.fraction(fill)
			.colour(InventoryFill.colour(fill, Color.GREEN, Color.YELLOW, Color.RED))
			.icon(PillarIcons.backpack())
			.number(PillarText.inventoryNumber(empty, InventoryCount.FREE))
			.divisions(InventoryFill.SLOTS)
			.idle(idle)
			.build();
	}

	private static Pillar target(int hp, int max)
	{
		final double f = (double) hp / max;
		return Pillar.builder()
			.fraction(f)
			.colour(HealthReadout.colour(f, Color.GREEN, Color.YELLOW, Color.RED))
			.icon(ICONS.getSkillImage(Skill.HITPOINTS, true))
			.number(PillarText.targetNumber(hp, f))
			.divisions(1)
			.build();
	}

	private static BufferedImage render(Pillar pillar, PillarStyle style, PillarLabels labels)
	{
		final BufferedImage image = new BufferedImage(BOX.width + 8, BOX.height + 8, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		try
		{
			PillarPainter.paint(g, BOX, pillar, style, labels, new Color(0, 0, 0, 150));
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	@Test
	public void writesEveryStateForThePr() throws IOException
	{
		assertTrue(OUT.isDirectory() || OUT.mkdirs());
		final Object[][] states = {
			{"xp-82", xp(0.82, false), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER},
			{"xp-idle", xp(0.82, true), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER},
			{"xp-icon-only", xp(0.40, false), PillarStyle.STATUS_BARS, PillarLabels.ICON_ONLY},
			{"xp-no-labels", xp(0.40, false), PillarStyle.STATUS_BARS, PillarLabels.NONE},
			{"xp-flat", xp(0.82, false), PillarStyle.FLAT, PillarLabels.ICON_AND_NUMBER},
			{"inv-18-free", inventory(18, false), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER},
			{"inv-3-free", inventory(3, false), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER},
			{"inv-idle", inventory(18, true), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER},
			{"target-12-of-50", target(12, 50), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER},
		};

		final BufferedImage sheet = new BufferedImage(states.length * 40 + 8, BOX.height + 16,
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = sheet.createGraphics();
		g.setColor(new Color(62, 53, 41));
		g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
		for (int i = 0; i < states.length; i++)
		{
			final BufferedImage one = render((Pillar) states[i][1], (PillarStyle) states[i][2],
				(PillarLabels) states[i][3]);
			ImageIO.write(one, "png", new File(OUT, states[i][0] + ".png"));
			g.drawImage(one, 8 + i * 40, 4, null);
		}
		g.dispose();
		ImageIO.write(scaled(sheet, 2), "png", new File(OUT, "pillars-sheet.png"));
		assertTrue(new File(OUT, "pillars-sheet.png").isFile());
	}

	@Test
	public void labelsDrawTheIconAndTheNumberAtTheTop()
	{
		final BufferedImage on = render(xp(0, false), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER);
		assertTrue("white number under the icon", bright(on, BOX.y + 22, BOX.y + 40));
		assertTrue("icon at the top", drawn(on, BOX.y + 2, BOX.y + 20));
		assertTrue("level at the foot", bright(on, BOX.y + BOX.height - 14, BOX.y + BOX.height - 2));
	}

	@Test
	public void numbersStayInsideThePillar()
	{
		final Pillar[] widest = {
			xp(0.82, false),
			xp(0.82, false).toBuilder().number("100%").build(),
			xp(0.82, false).toBuilder().number("Max").build(),
			xp(0.82, false).toBuilder().number("999k").build(),
			target(12, 50).toBuilder().number("255").build(),
		};
		for (Pillar pillar : widest)
		{
			final BufferedImage image = render(pillar, PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER);
			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					final boolean outside = x < BOX.x || x >= BOX.x + BOX.width;
					assertFalse(pillar.getNumber() + " spills out at " + x + "," + y,
						outside && new Color(image.getRGB(x, y), true).getAlpha() > 0);
				}
			}
		}
	}

	/** Dropping the sign of a percent too wide for the bar made "82%" paint exactly as "82". */
	@Test
	public void aWidePercentKeepsItsSignWhenDrawn()
	{
		final Pillar withSign = xp(0, false).toBuilder().number("82%").build();
		final Pillar withoutSign = xp(0, false).toBuilder().number("82").build();
		assertFalse(same(render(withSign, PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER),
			render(withoutSign, PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER)));
	}

	@Test
	public void onlyALabelTooWideForTheBarIsCondensed()
	{
		assertEquals(1.0, PillarPainter.labelScale(15, 18), 0);
		assertEquals(1.0, PillarPainter.labelScale(18, 18), 0);
		assertEquals(0.75, PillarPainter.labelScale(24, 18), 1e-9);
	}

	@Test
	public void theTargetCarriesTheHeart()
	{
		final BufferedImage on = render(target(12, 50), PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER);
		assertTrue("red heart at the top", coloured(on, BOX.y + 2, BOX.y + 20));
		assertTrue("hitpoints under it", bright(on, BOX.y + 22, BOX.y + 40));
	}

	@Test
	public void iconOnlyHasNoNumber()
	{
		final BufferedImage icon = render(xp(0, false), PillarStyle.STATUS_BARS, PillarLabels.ICON_ONLY);
		assertTrue(drawn(icon, BOX.y + 2, BOX.y + 20));
		assertFalse(bright(icon, BOX.y + 24, BOX.y + BOX.height - 2));
	}

	@Test
	public void labelsOffDrawsNothingButTheBar()
	{
		final BufferedImage off = render(xp(0, false), PillarStyle.STATUS_BARS, PillarLabels.NONE);
		assertFalse(bright(off, BOX.y + 2, BOX.y + BOX.height - 2));
		assertFalse(drawn(off, BOX.y + 2, BOX.y + BOX.height - 2));
	}

	@Test
	public void idleDimsThePillar()
	{
		final int y = BOX.y + BOX.height - 20;
		final int x = BOX.x + BOX.width / 2;
		final int active = new Color(render(xp(0.82, false), PillarStyle.STATUS_BARS, PillarLabels.NONE)
			.getRGB(x, y), true).getAlpha();
		final int idle = new Color(render(xp(0.82, true), PillarStyle.STATUS_BARS, PillarLabels.NONE)
			.getRGB(x, y), true).getAlpha();
		assertTrue("idle " + idle + " < active " + active, idle < active * 0.7);
	}

	@Test
	public void statusBarsStyleShinesOnTheLeft()
	{
		final BufferedImage image = render(xp(0.82, false), PillarStyle.STATUS_BARS, PillarLabels.NONE);
		final int y = BOX.y + BOX.height - 30;
		assertTrue(luma(image.getRGB(BOX.x + 3, y)) > luma(image.getRGB(BOX.x + BOX.width - 3, y)));
	}

	@Test
	public void flatStyleIsOneColour()
	{
		final BufferedImage image = render(xp(0.82, false), PillarStyle.FLAT, PillarLabels.NONE);
		final int y = BOX.y + BOX.height - 30;
		assertTrue(image.getRGB(BOX.x + 3, y) == image.getRGB(BOX.x + BOX.width - 3, y));
	}

	@Test
	public void inventoryShowsSlotTicks()
	{
		final BufferedImage image = render(inventory(0, false), PillarStyle.STATUS_BARS, PillarLabels.NONE);
		final int[] ticks = PillarText.ticks(BOX.y + 1, BOX.height - 2, InventoryFill.SLOTS);
		final int x = BOX.x + BOX.width / 2;
		final int tick = luma(image.getRGB(x, ticks[20]));
		final int between = luma(image.getRGB(x, ticks[20] + 4));
		assertTrue("tick " + tick + " darker than slot " + between, tick < between);
	}

	@Test
	public void theBackpackIconIsDrawn()
	{
		final BufferedImage bag = PillarIcons.backpack();
		assertNotNull(bag);
		assertTrue(bag.getWidth() <= SidePillars.WIDTH);
		assertTrue(coloured(bag, 0, bag.getHeight()));
	}

	private static boolean same(BufferedImage a, BufferedImage b)
	{
		for (int y = 0; y < a.getHeight(); y++)
		{
			for (int x = 0; x < a.getWidth(); x++)
			{
				if (a.getRGB(x, y) != b.getRGB(x, y))
				{
					return false;
				}
			}
		}
		return true;
	}

	/** Any near-white pixel in these rows. */
	private static boolean bright(BufferedImage image, int fromY, int toY)
	{
		return any(image, fromY, toY, c -> c.getAlpha() > 100 && c.getRed() > 200 && c.getGreen() > 200
			&& c.getBlue() > 200);
	}

	/**
	 * Any opaque pixel lighter than the black frame: an icon. The small
	 * fishing icon is grey, so colour alone would miss it.
	 */
	private static boolean drawn(BufferedImage image, int fromY, int toY)
	{
		return any(image, fromY, toY, c -> c.getAlpha() > 200
			&& Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue())) > 60);
	}

	/** Any opaque pixel that is neither grey nor black. */
	private static boolean coloured(BufferedImage image, int fromY, int toY)
	{
		return any(image, fromY, toY, c -> c.getAlpha() > 150
			&& Math.max(c.getRed(), Math.max(c.getGreen(), c.getBlue()))
			- Math.min(c.getRed(), Math.min(c.getGreen(), c.getBlue())) > 60);
	}

	private static boolean any(BufferedImage image, int fromY, int toY, java.util.function.Predicate<Color> test)
	{
		for (int y = Math.max(0, fromY); y < Math.min(image.getHeight(), toY); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if (test.test(new Color(image.getRGB(x, y), true)))
				{
					return true;
				}
			}
		}
		return false;
	}

	private static int luma(int argb)
	{
		final Color c = new Color(argb, true);
		return c.getRed() * 3 + c.getGreen() * 6 + c.getBlue();
	}

	private static BufferedImage scaled(BufferedImage image, int factor)
	{
		final BufferedImage out = new BufferedImage(image.getWidth() * factor, image.getHeight() * factor,
			BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(image, 0, 0, out.getWidth(), out.getHeight(), null);
		g.dispose();
		return out;
	}
}
