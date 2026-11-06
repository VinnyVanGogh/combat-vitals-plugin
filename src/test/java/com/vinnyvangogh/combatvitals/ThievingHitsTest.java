package com.vinnyvangogh.combatvitals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * Board's in-game test, 2026-11-06: a stun or a hit while thieving counted
 * as combat and hid the skilling badge ("Hide during combat").
 */
public class ThievingHitsTest
{
	/** A scene: who each actor is interacting with, and which can fight. */
	private static final class Scene
	{
		final Map<Object, Object> interacting = new IdentityHashMap<>();
		final Set<Object> fighters = Collections.newSetFromMap(new IdentityHashMap<>());

		Object actor(boolean canFight, Object target)
		{
			final Object actor = new Object();
			interacting.put(actor, target);
			if (canFight)
			{
				fighters.add(actor);
			}
			return actor;
		}

		boolean otherAttacker(List<Object> actors, Object self, Object target)
		{
			return ThievingHits.otherAttacker(actors, self, target, fighters::contains, interacting::get);
		}
	}

	private final Object self = new Object();

	@Test
	public void aStunWhilePickpocketingIsNotAFight()
	{
		assertFalse(ThievingHits.isFight(true, true, false));
	}

	@Test
	public void aHitWhileNotThievingIsAFight()
	{
		assertTrue(ThievingHits.isFight(true, false, false));
	}

	@Test
	public void yourOwnHitIsAlwaysAFight()
	{
		assertTrue(ThievingHits.isFight(false, true, false));
	}

	@Test
	public void someoneElseOnYouWhileThievingIsAFight()
	{
		assertTrue(ThievingHits.isFight(true, true, true));
	}

	@Test
	public void theTargetTurningToYouAfterAStunIsNotAnotherAttacker()
	{
		final Scene scene = new Scene();
		final Object man = scene.actor(true, self);

		assertFalse(scene.otherAttacker(Arrays.asList(self, man), self, man));
	}

	@Test
	public void aGuardOnYouAtAStallIsAnotherAttacker()
	{
		final Scene scene = new Scene();
		final Object guard = scene.actor(true, self);

		assertTrue(scene.otherAttacker(Arrays.asList(self, guard), self, null));
	}

	@Test
	public void aPetFollowingYouIsNot()
	{
		final Scene scene = new Scene();
		final Object pet = scene.actor(false, self);

		assertFalse(scene.otherAttacker(Arrays.asList(self, pet), self, null));
	}

	@Test
	public void aCrowdFightingEachOtherIsNotOnYou()
	{
		final Scene scene = new Scene();
		final Object man = scene.actor(true, null);
		final Set<Object> crowd = new HashSet<>();
		Object previous = scene.actor(true, null);
		crowd.add(previous);
		for (int i = 0; i < 20; i++)
		{
			final Object next = scene.actor(true, previous);
			crowd.add(next);
			previous = next;
		}
		final List<Object> actors = new java.util.ArrayList<>(crowd);
		actors.add(self);
		actors.add(man);

		assertFalse(scene.otherAttacker(actors, self, man));

		actors.add(scene.actor(true, self));
		assertTrue("one of them turning on you is", scene.otherAttacker(actors, self, man));
	}
}
