package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.vinnyvangogh.testkit.ui.RuneLiteLook;
import com.vinnyvangogh.testkit.ui.SwingDriver;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/** Building the sidebar panel, and building it again, never writes Follow game volume. */
public class FollowGameVolumePanelTest
{
	private static void build(PanelFixture.Store store)
	{
		final AtomicReference<VitalsSidePanel> panel = new AtomicReference<>();
		SwingDriver.onEdt(() ->
		{
			RuneLiteLook.install();
			panel.set(new VitalsSidePanel(store, PanelFixture.descriptors(), null, t ->
			{
			}, new AlertMute(System::currentTimeMillis, ZoneOffset.UTC)));
			new SwingDriver(panel.get()).layout(VitalsSidePanelShots.WIDTH);
		});
	}

	@Test
	public void panelBuiltTwiceLeavesAnUntickedSettingUnticked()
	{
		final PanelFixture.Store store = PanelFixture.store();
		store.set(AlertSounds.FOLLOW_KEY, false);
		store.writes.clear();
		build(store);
		build(store);
		assertFalse(store.writes.contains(AlertSounds.FOLLOW_KEY));
		assertEquals("false", store.raw(AlertSounds.FOLLOW_KEY));
	}

	@Test
	public void panelBuiltAgainKeepsATickedSettingTicked()
	{
		final PanelFixture.Store store = PanelFixture.store();
		store.set(AlertSounds.FOLLOW_KEY, true);
		store.writes.clear();
		build(store);
		build(store);
		assertFalse(store.writes.contains(AlertSounds.FOLLOW_KEY));
		assertEquals("true", store.raw(AlertSounds.FOLLOW_KEY));
	}
}
