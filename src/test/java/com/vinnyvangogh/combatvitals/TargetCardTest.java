package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.Test;

/** The target readout in the combat card's style, and the bundled wiki stats behind its NPC rows. */
public class TargetCardTest
{
	private static final File OUT = new File("build/target-card");

	// npcId combat hp maxHit style speed stab slash crush magic ranged weakness poison venom name
	private static final String TABLE = "# header\n"
		+ "1\t45\t17\t3\tMelee\t4\t10\t12\t14\t0\t5\tFire 50%\t1\t0\tTest forester\n"
		+ "2\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t-\t0\t0\tBlank\n"
		+ "bad row\n"
		+ "x\t1\t1\t1\tx\t1\t1\t1\t1\t1\t1\t-\t0\t0\tBad id\n";

	private static Map<Integer, NpcStats> table() throws IOException
	{
		return NpcStats.parse(new ByteArrayInputStream(TABLE.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void tableParsesAndSkipsBadRows() throws IOException
	{
		final Map<Integer, NpcStats> t = table();
		assertEquals(2, t.size());
		final NpcStats f = t.get(1);
		assertEquals(45, f.combatLevel);
		assertEquals("3", f.maxHit);
		assertEquals("2.4s", f.speedText());
		assertTrue(f.poisonImmune);
		assertEquals("Poison", CombatVitalsPanelOverlay.immunity(f));
		final NpcStats blank = t.get(2);
		assertNull(blank.maxHit);
		assertNull(blank.speedText());
		assertEquals(-1, blank.stab);
		assertNull(CombatVitalsPanelOverlay.immunity(blank));
	}

	@Test
	public void bundledTableLoadsWithoutNetwork()
	{
		assertNotNull(NpcStats.load());
	}

	@Test
	public void healthRowsAlwaysAndStatsOnlyForNpcsWithThem() throws IOException
	{
		final List<SplashOverlay.Row> plain = CombatVitalsPanelOverlay.rows("Cow", null, "5/8 (63%)", 0.63,
			SplashOverlay.SAFE, null);
		assertEquals(3, plain.size());
		final List<SplashOverlay.Row> full = CombatVitalsPanelOverlay.rows("Forester", "Level 45", "10/17 (60%)", 0.6,
			SplashOverlay.SAFE, table().get(1));
		assertTrue(full.size() > 6);
		final List<SplashOverlay.Row> noBar = CombatVitalsPanelOverlay.rows("Cow", null, "?", -1, SplashOverlay.SAFE,
			table().get(2));
		assertEquals(3, noBar.size());
	}

	/** The card as drawn, written to build/target-card for the PR. */
	@Test
	public void rendersTheCard() throws IOException
	{
		final List<SplashOverlay.Row> rows = CombatVitalsPanelOverlay.rows("Forester", "Level 45", "10/17 (60%)", 0.6,
			new Color(0, 200, 83), table().get(1));
		final BufferedImage image = new BufferedImage(260, 220, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		g.setColor(new Color(40, 40, 40));
		g.fillRect(0, 0, 260, 220);
		final Dimension size = SplashOverlay.card(g, rows, new Color(0, 200, 83));
		g.dispose();
		assertTrue(size.width > 100 && size.height > 80);
		assertTrue(OUT.isDirectory() || OUT.mkdirs());
		assertTrue(ImageIO.write(image, "png", new File(OUT, "target-card.png")));
		assertTrue(image.getRGB(2, 2) != new Color(40, 40, 40).getRGB());
	}
}
