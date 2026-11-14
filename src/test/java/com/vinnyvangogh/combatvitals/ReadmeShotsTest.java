package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.FontManager;
import org.junit.Test;

/**
 * The README's overlay pictures, drawn headless by the plugin's own painters
 * from made-up scenes, so they never show an account name and a reshoot is
 * one command:
 *
 * <pre>./gradlew readmeShots</pre>
 *
 * That task points {@code readme.shots.dir} at {@code docs/images/generated}.
 * A plain {@code ./gradlew test} writes the same images to
 * {@code build/readme-shots} and only checks they are drawn and in budget,
 * so CI never rewrites the committed ones.
 *
 * Backdrop: a flat neutral gradient, not a game capture (no players, no
 * names). Shots that need the real game are in docs/shot-list.md.
 */
public class ReadmeShotsTest
{
	/** Size budget per committed image; the README loads several. */
	static final long BUDGET_BYTES = 40 * 1024;

	private static final File OUT = new File(System.getProperty("readme.shots.dir", "build/readme-shots"));
	private static final SkillIconManager ICONS = new SkillIconManager();
	private static final int SCALE = 2;
	private static final int MARGIN = 10;

	/** Settings as a new profile has them, with the session row on so the badge shows it. */
	private static final CombatVitalsConfig CONFIG = new CombatVitalsConfig()
	{
	};

	private static final SkillingBadgeRenderer BADGE =
		new SkillingBadgeRenderer(null, CONFIG, ICONS, null, null);

	@Test
	public void woodcuttingWithALogBasket() throws IOException
	{
		final List<ContainerRow> basket = Collections.singletonList(
			new ContainerRow(SkillingContainer.LOG_BASKET, 16, 28));
		final XpProgress xp = progress(72, 0.64);
		final SkillingReadout readout = base(Skill.WOODCUTTING, 72, 72, xp)
			.sessionCells(SessionRow.cells(Skill.WOODCUTTING, 112, 14 * 60_000L, 38_400, 480, xp,
				true, true, true))
			.inventoryFill(InventoryFill.fraction(19, 8))
			.inventoryLabel(ContainerRow.inventoryLabel(8, InventoryCount.FREE, basket, false))
			.inventoryFree(8)
			.containers(basket)
			.build();
		write("badge-woodcutting-basket", badge(readout));
	}

	@Test
	public void fishingWithABoostTimer() throws IOException
	{
		final XpProgress xp = progress(79, 0.31);
		final SkillingReadout readout = base(Skill.FISHING, 79, 82, xp)
			.ticksUntilDrop(75)
			.cycleLength(100)
			.timerConfirmed(true)
			.source("Admiral pie")
			.sessionCells(SessionRow.cells(Skill.FISHING, 240, 22 * 60_000L, 41_200, 655, xp,
				true, true, true))
			.inventoryFill(InventoryFill.fraction(14, 13))
			.inventoryLabel(InventoryFill.label(13, InventoryCount.FREE))
			.inventoryFree(13)
			.build();
		write("badge-fishing-boost", badge(readout));
	}

	@Test
	public void idleAfterStopping() throws IOException
	{
		final XpProgress xp = progress(55, 0.12);
		final SkillingReadout readout = base(Skill.MINING, 55, 55, xp)
			.idle(true)
			.inventoryFill(InventoryFill.fraction(27, 0))
			.inventoryLabel(InventoryFill.label(0, InventoryCount.FREE))
			.inventoryFree(0)
			.build();
		write("badge-idle-full", badge(readout));
	}

	@Test
	public void sidePillars() throws IOException
	{
		final Pillar[] pillars = {
			PillarRenderTest.xp(0.82, false),
			PillarRenderTest.inventory(10, false),
			PillarRenderTest.inventory(0, false),
			PillarRenderTest.target(12, 50),
		};
		final BufferedImage first = pillar(pillars[0]);
		final int gap = 14;
		final BufferedImage sheet = backdrop(pillars.length * (first.getWidth() + gap) - gap + MARGIN * 2,
			first.getHeight() + MARGIN * 2);
		final Graphics2D g = sheet.createGraphics();
		for (int i = 0; i < pillars.length; i++)
		{
			g.drawImage(pillar(pillars[i]), MARGIN + i * (first.getWidth() + gap), MARGIN, null);
		}
		g.dispose();
		write("side-pillars-sheet", sheet);
	}

	private static BufferedImage pillar(Pillar p)
	{
		return PillarRenderTest.render(p, PillarStyle.STATUS_BARS, PillarLabels.ICON_AND_NUMBER);
	}

	private static XpProgress progress(int level, double through)
	{
		final int from = Experience.getXpForLevel(level);
		return XpProgress.of(from + (int) ((Experience.getXpForLevel(level + 1) - from) * through));
	}

	private static SkillingReadout.SkillingReadoutBuilder base(Skill skill, int real, int boosted, XpProgress xp)
	{
		return SkillingReadout.builder()
			.skill(skill)
			.real(real)
			.boosted(boosted)
			.colour(SkillColours.of(skill))
			.header(true)
			.xp(xp)
			.xpText(xp.text(XpDisplay.BOTH))
			.tickMillis(System.currentTimeMillis());
	}

	/** The badge as the fixed-on-screen overlay draws it, on the backdrop. */
	private static BufferedImage badge(SkillingReadout readout)
	{
		final BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D pg = probe.createGraphics();
		pg.setFont(FontManager.getRunescapeFont());
		final Dimension size = BADGE.drawFromCorner(pg, readout);
		pg.dispose();
		assertNotNull("every row is off", size);

		final BufferedImage image = backdrop(size.width + MARGIN * 2, size.height + MARGIN * 2);
		final Graphics2D g = image.createGraphics();
		g.setFont(FontManager.getRunescapeFont());
		g.translate(MARGIN, MARGIN);
		BADGE.drawFromCorner(g, readout);
		g.dispose();
		return image;
	}

	/** A neutral ground-coloured gradient: reads like the game without being a capture of it. */
	private static BufferedImage backdrop(int width, int height)
	{
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setPaint(new GradientPaint(0, 0, new Color(74, 82, 52), 0, height, new Color(52, 46, 34)));
		g.fillRect(0, 0, width, height);
		g.dispose();
		return image;
	}

	private static void write(String name, BufferedImage image) throws IOException
	{
		assertTrue(OUT.isDirectory() || OUT.mkdirs());
		final File file = new File(OUT, name + ".png");
		ImageIO.write(PillarRenderTest.scaled(image, SCALE), "png", file);
		assertTrue(name + " was not written", file.isFile());
		assertTrue(name + " is " + file.length() + " bytes, over the " + BUDGET_BYTES + " budget",
			file.length() <= BUDGET_BYTES);
	}
}
