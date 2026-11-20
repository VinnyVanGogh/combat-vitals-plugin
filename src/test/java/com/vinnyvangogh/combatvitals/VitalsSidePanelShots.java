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
 * its guide open as a first-time user sees it, then each tab with the guides
 * hidden. {@code ./gradlew test} renders them into build/screenshots,
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
			for (VitalsSidePanel.Main m : VitalsSidePanel.Main.values())
			{
				panel.display(m);
				ui.layout(WIDTH);
				shot(ui, "panel-main-" + m.name().toLowerCase() + ".png");
			}
			// The Live tab as played, one shot per card: goals as bars, session and total as aligned columns.
			panel.display(VitalsSidePanel.Main.LIVE);
			panel.live(liveWithRows());
			final LiveSection live = ui.find(LiveSection.class, s -> true, "the Live section");
			for (LiveSection.View v : LiveSection.View.values())
			{
				live.applyView(v);
				ui.layout(WIDTH);
				shot(ui, "panel-live-" + v.name().toLowerCase() + ".png");
			}
			panel.display(VitalsSidePanel.Main.PRESETS);
			ui.layout(WIDTH);
			ui.click("Skiller");
			shot(ui, "panel-presets.png");
			ui.click("Cancel");
			for (PanelLayout.Tab tab : PanelLayout.Tab.values())
			{
				panel.show(tab);
				ui.layout(WIDTH);
				shot(ui, "panel-" + tab.name().toLowerCase() + ".png");
			}
			// The same tabs with the guides hidden: what a returning user sees.
			ui.click(VitalsSidePanel.HIDE_GUIDES);
			for (PanelLayout.Tab tab : PanelLayout.Tab.values())
			{
				panel.show(tab);
				ui.layout(WIDTH);
				shot(ui, "panel-" + tab.name().toLowerCase() + "-no-guides.png");
			}
		});
		assertTrue("rendering wrote settings: " + store.writes,
			store.writes.equals(java.util.Collections.singletonList(VitalsSidePanel.GUIDES_HIDDEN_KEY)));
	}

	/** Live data for the shots: goals and session/total rows on the Combat and Skilling cards, a long session list. */
	private static LiveVitals liveWithRows()
	{
		final java.awt.Color accent = PanelStyle.ACCENT;
		final java.util.List<LiveVitals.GoalLine> goals = java.util.Arrays.asList(
			new LiveVitals.GoalLine("Magic level 55", new LiveVitals.Bar(0.76, "Magic 55 · 76% · ~54m", accent),
				"Magic level 55: 17,324 xp to go (76%), 931 actions left, 54m at 19,022 xp/hr (session), "
					+ "54m at 19,022 xp/hr (last hour)"),
			new LiveVitals.GoalLine("Ranged level 99", new LiveVitals.Bar(0.31, "Ranged 99 · 31% · ~12h 05m", accent),
				"Ranged level 99: 8,100,000 xp to go (31%), 62,000 actions left, 12h 05m at 670,000 xp/hr (session), "
					+ "no rate yet (last hour)"));
		final java.util.List<LiveVitals.Period> periods = java.util.Arrays.asList(
			new LiveVitals.Period("This session", "+412.3k xp", "2h 10m", "190.2k/h",
				"This session: +412.3k xp in 2h 10m, 190,200 xp/h"),
			new LiveVitals.Period("Total", "+1.24m xp", "12h 05m", "102.7k/h",
				"Total: +1.24m xp in 12h 05m, 102,700 xp/h"));
		final LiveVitals.Skilling chop = LiveSectionTest.chopping();
		return new LiveVitals(true, LiveSectionTest.fighting().with(goals, periods),
			new LiveVitals.Skilling(chop.header, chop.colour, chop.icon, chop.xp, chop.stats, chop.chips, chop.lines,
				goals).withPeriods(periods),
			LiveSectionTest.LONG_SESSION, LiveSectionTest.LONG_SESSION);
	}

	private static void shot(SwingDriver ui, String name)
	{
		final BufferedImage image = ui.render(WIDTH, 0, SCALE);
		assertTrue(name + " is empty", image.getHeight() > 100);
		SwingDriver.writePng(image, new File(dir(), name));
	}
}
