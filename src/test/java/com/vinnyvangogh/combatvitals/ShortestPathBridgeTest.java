package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;

public class ShortestPathBridgeTest
{
	/** Stands in for Shortest Path's plugin class, matched by name. */
	private static final class FakeShortestPath
	{
	}

	private final List<Object> posted = new ArrayList<>();

	private ShortestPathBridge bridge(List<Object> plugins, boolean enabled)
	{
		return new ShortestPathBridge(() -> plugins, p -> enabled, FakeShortestPath.class.getName(), posted::add);
	}

	@Test
	public void notInstalled()
	{
		final ShortestPathBridge bridge = bridge(Collections.singletonList(new Object()), true);

		assertEquals(ShortestPathBridge.Availability.NOT_INSTALLED, bridge.availability());
		assertEquals(ShortestPathBridge.RouteOutcome.UNAVAILABLE, bridge.route(new WorldPoint(1, 2, 0)));
		assertFalse(bridge.clear());
		assertTrue(posted.isEmpty());
	}

	@Test
	public void installedButOff()
	{
		final ShortestPathBridge bridge = bridge(Collections.singletonList(new FakeShortestPath()), false);

		assertEquals(ShortestPathBridge.Availability.NOT_ENABLED, bridge.availability());
		assertEquals(ShortestPathBridge.RouteOutcome.UNAVAILABLE, bridge.route(new WorldPoint(1, 2, 0)));
		assertTrue(posted.isEmpty());
	}

	@Test
	public void routeSendsTheVerifiedMessageShape()
	{
		final ShortestPathBridge bridge = bridge(Collections.singletonList(new FakeShortestPath()), true);
		final WorldPoint target = new WorldPoint(2844, 3429, 0);

		assertEquals(ShortestPathBridge.RouteOutcome.SENT, bridge.route(target));

		assertEquals(1, posted.size());
		final PluginMessage message = (PluginMessage) posted.get(0);
		assertEquals("shortestpath", message.getNamespace());
		assertEquals("path", message.getName());
		assertEquals(Set.of(target), message.getData().get("target"));
	}

	@Test
	public void clearSendsTheClearMessage()
	{
		final ShortestPathBridge bridge = bridge(Collections.singletonList(new FakeShortestPath()), true);

		assertTrue(bridge.clear());

		final PluginMessage message = (PluginMessage) posted.get(0);
		assertEquals("shortestpath", message.getNamespace());
		assertEquals("clear", message.getName());
	}

	@Test
	public void aThrowingPostIsReportedNotPropagated()
	{
		final ShortestPathBridge bridge = new ShortestPathBridge(() -> Collections.singletonList(new FakeShortestPath()),
			p -> true, FakeShortestPath.class.getName(), m ->
			{
				throw new AssertionError("not on the client thread");
			});

		assertEquals(ShortestPathBridge.RouteOutcome.CALL_FAILED, bridge.route(new WorldPoint(1, 2, 0)));
		assertFalse(bridge.clear());
	}

	@Test
	public void aFailingPluginListReadsAsNotInstalled()
	{
		final ShortestPathBridge bridge = new ShortestPathBridge(() ->
		{
			throw new IllegalStateException("boom");
		}, p -> true, FakeShortestPath.class.getName(), posted::add);

		assertEquals(ShortestPathBridge.Availability.NOT_INSTALLED, bridge.availability());
	}

	@Test
	public void clientThreadPosterDefersThePost()
	{
		final List<Runnable> queued = new ArrayList<>();
		ShortestPathBridge.clientThreadEventPoster(queued::add, posted::add).accept("event");

		assertTrue(posted.isEmpty());
		queued.get(0).run();
		assertEquals(Collections.singletonList("event"), posted);
	}
}
