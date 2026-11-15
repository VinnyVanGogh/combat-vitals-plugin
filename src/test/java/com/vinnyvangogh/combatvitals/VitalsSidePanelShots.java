package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertTrue;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.awt.image.BufferedImage;
import java.io.File;
import net.runelite.client.ui.PluginPanel;
import org.junit.Test;

/**
 * Renders the README's sidebar panel screenshots from the real panel over
 * default settings: presets with Skiller's preview open, then each tab with
 * its guide open. {@code ./gradlew test} renders them into build/screenshots,
 * so a broken shot fails the build; {@code ./gradlew screenshots} writes them
 * into docs/images (GIM Utilities' ReadmeShots, same driver).
 */
public class VitalsSidePanelShots
{
	static final int WIDTH = PluginPanel.PANEL_WIDTH;
	private static final double SCALE = 2.0;

	private static File dir()
	{
		return new File(System.getProperty("screenshots.dir", "build/screenshots"));
	}

	@Test
	public void rendersThePanelShots()
	{
		final PanelFixture.Store store = PanelFixture.store();
		SwingDriver.onEdt(() ->
		{
			RuneLiteLook.install();
			final VitalsSidePanel panel = new VitalsSidePanel(store, PanelFixture.descriptors(), null, t ->
			{
			});
			final SwingDriver ui = new SwingDriver(panel);
			ui.layout(WIDTH);
			ui.click("Skiller");
			shot(ui, "panel-presets.png");
			ui.click("Cancel");
			for (PanelLayout.Tab tab : PanelLayout.Tab.values())
			{
				panel.show(tab);
				ui.layout(WIDTH);
				ui.click("+ Guide: " + tab.label);
				shot(ui, "panel-" + tab.name().toLowerCase() + ".png");
			}
		});
		assertTrue("rendering wrote settings: " + store.writes, store.writes.isEmpty());
	}

	private static void shot(SwingDriver ui, String name)
	{
		final BufferedImage image = ui.render(WIDTH, 0, SCALE);
		assertTrue(name + " is empty", image.getHeight() > 100);
		SwingDriver.writePng(image, new File(dir(), name));
	}
}
