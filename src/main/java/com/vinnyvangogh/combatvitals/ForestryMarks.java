package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.runelite.api.coords.WorldPoint;

/**
 * The parts of Forestry events that are on the scene right now, kept so the
 * highlight overlay can outline them, and which of them is the one to use.
 * Observation only: the same spawn ids and rules the base RuneLite
 * Woodcutting plugin (BSD 2-Clause) reads, which {@link SkillingEvents}
 * already carries for the alerts. From that plugin (runelite-parent 1.13.6,
 * disassembled 2026-11-20):
 *
 * <ul>
 * <li>Enchantment Ritual: five circle NPCs, ids {@link #RITUAL_FIRST} to
 * {@link #RITUAL_LAST}, a 4 shape by 4 colour grid. The circle that pairs
 * with no other on shape or colour is the one to stand on (an XOR of
 * shape and colour bits, {@code solveCircles()}).</li>
 * <li>Flowering Bush: the two bushes a player was last seen tending, on the
 * pick-up animation, are the pair; the base plugin forgets them on its three
 * "no flowers" lines.</li>
 * <li>Struggling Sapling: the game says "The sapling seems to love X as the
 * first/second/third ingredient"; the ingredient object named X is that
 * slot's.</li>
 * </ul>
 *
 * Added from the OSRS Wiki (read 2026-11-20): only the special Rising Root is
 * worth the extra experience; Pheasant Control wants an egg from an empty
 * nest; the entlings' overhead request names the prune. Those three are
 * unverified in game.
 *
 * Unlike the alerts, nothing here waits for you to be chopping or near: a
 * mark is there while the thing is.
 *
 * Pure: whatever the client spawned in, whatever is wanted out. Client thread only.
 */
final class ForestryMarks
{
	static final int ROOT_SPECIAL = 47483;
	static final int INGREDIENT_FIRST = 47493;
	static final int INGREDIENT_LAST = 47499;
	static final int RAINBOW = 49934;
	static final int NEST_1 = 49936;
	static final int NEST_2 = 49937;
	static final int RITUAL_FIRST = 12520;
	static final int RITUAL_LAST = 12535;
	static final int FORESTER = 12536;
	static final int PHEASANT = 12537;
	static final int TRAP = 12541;
	static final int BEEBOX_FIRST = 12515;
	static final int BEEBOX_LAST = 12518;
	static final int ENTLING = 12543;
	static final int ENTLING_PRUNED = 12544;

	/** A pheasant this many tiles from a nest or less is in it. */
	static final int PHEASANT_REACH = 1;
	private static final String[] ORDINALS = {"1st", "2nd", "3rd"};

	/** What the overlay needs from the client for a part; kept out of here so this stays pure. */
	interface View
	{
		/** The part's id right now: an entling changes id when pruned. */
		int idOf(Object thing);

		WorldPoint tile(Object thing);
	}

	/** One part to outline. {@code correct} is the one to use; the rest only show on "all". */
	static final class Target
	{
		final Object thing;
		final String label;
		final boolean correct;

		Target(Object thing, String label, boolean correct)
		{
			this.thing = thing;
			this.label = label;
			this.correct = correct;
		}
	}

	private final Map<SkillingEvents.Forestry, Set<Object>> marks = new EnumMap<>(SkillingEvents.Forestry.class);
	private final List<Object> activeFlowers = new ArrayList<>(2);
	private final Object[] saplingOrder = new Object[3];
	private final Map<Object, String> entlingLabels = new IdentityHashMap<>();
	private Object lastFlower;

	/** The kind of Forestry event a spawned object belongs to, or null. */
	static SkillingEvents.Forestry kindOfObject(int id)
	{
		if (id == 47482 || id == ROOT_SPECIAL)
		{
			return SkillingEvents.Forestry.RISING_ROOTS;
		}
		if (id == 47484 || id == 47485 || id == 47487 || id == 47488 || id == 47490 || id == 47491
			|| id == 50699 || id == 50700 || (id >= INGREDIENT_FIRST && id <= INGREDIENT_LAST))
		{
			return SkillingEvents.Forestry.STRUGGLING_SAPLING;
		}
		if (id == RAINBOW)
		{
			return SkillingEvents.Forestry.LEPRECHAUN;
		}
		if (id == NEST_1 || id == NEST_2)
		{
			return SkillingEvents.Forestry.PHEASANT_CONTROL;
		}
		return null;
	}

	/** The kind of Forestry event a spawned NPC belongs to, or null. */
	static SkillingEvents.Forestry kindOfNpc(int id)
	{
		if (isBush(id))
		{
			return SkillingEvents.Forestry.FLOWERING_BUSH;
		}
		if (id == 12163)
		{
			return SkillingEvents.Forestry.LEPRECHAUN;
		}
		if (id == TRAP || id == 12559 || id == 12560)
		{
			return SkillingEvents.Forestry.POACHERS;
		}
		if (id == FORESTER || id == PHEASANT)
		{
			return SkillingEvents.Forestry.PHEASANT_CONTROL;
		}
		if (id >= BEEBOX_FIRST && id <= BEEBOX_LAST)
		{
			return SkillingEvents.Forestry.BEE_HIVE;
		}
		if (id == 12519 || (id >= RITUAL_FIRST && id <= RITUAL_LAST))
		{
			return SkillingEvents.Forestry.ENCHANTMENT_RITUAL;
		}
		if (id == ENTLING || id == ENTLING_PRUNED)
		{
			return SkillingEvents.Forestry.FRIENDLY_ENT;
		}
		return null;
	}

	private static boolean isBush(int id)
	{
		return id == 12120 || id == 12134 || id == 12141 || id == 12143 || id == 12145 || id == 12147
			|| id == 12149 || id == 12151;
	}

	void objectSpawned(Object thing, int id)
	{
		spawned(kindOfObject(id), thing);
	}

	void npcSpawned(Object thing, int id)
	{
		spawned(kindOfNpc(id), thing);
	}

	void spawned(SkillingEvents.Forestry kind, Object thing)
	{
		if (kind != null && thing != null)
		{
			marks.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(thing);
		}
	}

	void despawned(SkillingEvents.Forestry kind, Object thing)
	{
		final Set<Object> set = marks.get(kind);
		if (set != null)
		{
			set.remove(thing);
		}
	}

	/** A part gone from the scene, whatever it was; its solver state goes with it. */
	void despawned(Object thing)
	{
		for (Set<Object> set : marks.values())
		{
			set.remove(thing);
		}
		entlingLabels.remove(thing);
		if (activeFlowers.contains(thing))
		{
			activeFlowers.clear();
		}
		if (thing == lastFlower)
		{
			lastFlower = null;
		}
		for (int i = 0; i < saplingOrder.length; i++)
		{
			if (saplingOrder[i] == thing)
			{
				saplingOrder[i] = null;
			}
		}
	}

	void clear()
	{
		marks.clear();
		activeFlowers.clear();
		entlingLabels.clear();
		lastFlower = null;
		for (int i = 0; i < saplingOrder.length; i++)
		{
			saplingOrder[i] = null;
		}
	}

	boolean isEmpty()
	{
		for (Set<Object> set : marks.values())
		{
			if (!set.isEmpty())
			{
				return false;
			}
		}
		return true;
	}

	/** The things to outline, for the event kinds the player has switched on. */
	List<Object> wanted(Predicate<SkillingEvents.Forestry> enabled)
	{
		if (isEmpty())
		{
			return Collections.emptyList();
		}
		final List<Object> out = new ArrayList<>();
		for (Map.Entry<SkillingEvents.Forestry, Set<Object>> entry : marks.entrySet())
		{
			if (enabled.test(entry.getKey()))
			{
				out.addAll(entry.getValue());
			}
		}
		return out;
	}

	// ---- what the game tells us: the player tends a bush, the chat, an entling speaks ----

	/** Someone on the pick-up animation, beside a bush: it is one of the pair. */
	void flowerTended(Object bush)
	{
		if (!tracked(SkillingEvents.Forestry.FLOWERING_BUSH, bush) || activeFlowers.contains(bush))
		{
			return;
		}
		if (activeFlowers.size() >= 2)
		{
			activeFlowers.clear();
		}
		activeFlowers.add(bush);
	}

	/** The local player began talking to a bush; remembered for the "no flowers" lines. */
	void flowerClicked(Object bush)
	{
		if (tracked(SkillingEvents.Forestry.FLOWERING_BUSH, bush))
		{
			lastFlower = bush;
		}
	}

	/**
	 * A game message. The sapling's "loves X as the first ingredient" names a
	 * slot; the bush lines that say it has nothing to give forget the pair.
	 *
	 * @param objectName an ingredient object's name, lower case or not
	 */
	void chat(String message, Function<Object, String> objectName)
	{
		if (message == null)
		{
			return;
		}
		if (message.startsWith("The sapling seems to love"))
		{
			final int slot = ordinal(message);
			if (slot < 0)
			{
				return;
			}
			for (Object thing : set(SkillingEvents.Forestry.STRUGGLING_SAPLING))
			{
				final String name = objectName.apply(thing);
				if (name != null && !name.isEmpty()
					&& message.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
				{
					saplingOrder[slot] = thing;
					return;
				}
			}
		}
		else if (isNoFlowersLine(message) && lastFlower != null && activeFlowers.contains(lastFlower))
		{
			activeFlowers.clear();
		}
	}

	/** 0, 1 or 2 for first, second, third; -1 if the line says none. */
	static int ordinal(String message)
	{
		if (message.contains("first"))
		{
			return 0;
		}
		if (message.contains("second"))
		{
			return 1;
		}
		return message.contains("third") ? 2 : -1;
	}

	private static boolean isNoFlowersLine(String message)
	{
		return message.equals("There are no open, unpollinated flowers on this bush yet.")
			|| message.equals("The flowers on this bush have not yet opened enough to harvest pollen.")
			|| message.equals("The bush is already fruiting and won't benefit from any more pollen.");
	}

	/** An entling said something: the prune its request calls for. */
	void entlingSaid(Object entling, String text)
	{
		if (!tracked(SkillingEvents.Forestry.FRIENDLY_ENT, entling) || text == null)
		{
			return;
		}
		final String prune = entlingPrune(text);
		if (prune != null)
		{
			entlingLabels.put(entling, prune);
		}
	}

	/** The OSRS Wiki's table of entling requests (Friendly Ent, read 2026-11-20); null if not one. */
	static String entlingPrune(String text)
	{
		final String said = text.toLowerCase(Locale.ROOT);
		if (said.contains("breezy at the back"))
		{
			return "Prune back";
		}
		if (said.contains("leafy mullet"))
		{
			return "Prune top + sides";
		}
		if (said.contains("short back and sides"))
		{
			return "Prune back + sides";
		}
		if (said.contains("short on top"))
		{
			return "Prune top";
		}
		return null;
	}

	/**
	 * The circle to stand on: of five, the one that pairs with no other on
	 * shape or colour, by the base plugin's XOR. -1 unless exactly five.
	 *
	 * @param ids the five circles' NPC ids, {@link #RITUAL_FIRST} to {@link #RITUAL_LAST}
	 */
	static int solveCircles(int[] ids)
	{
		if (ids.length != 5)
		{
			return -1;
		}
		int s = 0;
		for (int id : ids)
		{
			s ^= circleBit(id);
		}
		for (int i = 0; i < ids.length; i++)
		{
			final int bit = circleBit(ids[i]);
			if ((bit & s) == bit)
			{
				return i;
			}
		}
		return -1;
	}

	private static int circleBit(int id)
	{
		final int offset = id - RITUAL_FIRST;
		return (16 << (offset / 4)) | (1 << (offset % 4));
	}

	// ---- what to draw ----

	/**
	 * What to outline now, for the event kinds the player has on. The correct
	 * parts come with a label; the rest only when {@code all} is set, the
	 * fallback of outlining every part like the base plugin does.
	 */
	List<Target> targets(Predicate<SkillingEvents.Forestry> enabled, boolean all, View view)
	{
		if (isEmpty())
		{
			return Collections.emptyList();
		}
		final List<Target> out = new ArrayList<>();
		for (Map.Entry<SkillingEvents.Forestry, Set<Object>> entry : marks.entrySet())
		{
			if (!enabled.test(entry.getKey()) || entry.getValue().isEmpty())
			{
				continue;
			}
			final List<Target> kind = new ArrayList<>();
			switch (entry.getKey())
			{
				case RISING_ROOTS:
					roots(entry.getValue(), view, kind);
					break;
				case STRUGGLING_SAPLING:
					sapling(entry.getValue(), view, kind);
					break;
				case FLOWERING_BUSH:
					for (Object bush : entry.getValue())
					{
						final boolean pair = activeFlowers.contains(bush);
						kind.add(new Target(bush, pair ? "Pair" : null, pair));
					}
					break;
				case LEPRECHAUN:
					for (Object thing : entry.getValue())
					{
						final boolean rainbow = view.idOf(thing) == RAINBOW;
						kind.add(new Target(thing, rainbow ? "Stand under" : null, rainbow));
					}
					break;
				case POACHERS:
					for (Object thing : entry.getValue())
					{
						final boolean trap = view.idOf(thing) == TRAP;
						kind.add(new Target(thing, trap ? "Disarm" : null, trap));
					}
					break;
				case PHEASANT_CONTROL:
					pheasants(entry.getValue(), view, kind);
					break;
				case BEE_HIVE:
					for (Object thing : entry.getValue())
					{
						final int id = view.idOf(thing);
						final boolean box = id >= BEEBOX_FIRST && id <= BEEBOX_LAST;
						kind.add(new Target(thing, box ? "Add logs" : null, box));
					}
					break;
				case ENCHANTMENT_RITUAL:
					circles(entry.getValue(), view, kind);
					break;
				case FRIENDLY_ENT:
					for (Object thing : entry.getValue())
					{
						final int id = view.idOf(thing);
						if (id == ENTLING_PRUNED)
						{
							continue;
						}
						final String label = entlingLabels.get(thing);
						kind.add(new Target(thing, label != null ? label : "Prune", id == ENTLING));
					}
					break;
				default:
					break;
			}
			for (Target target : kind)
			{
				if (target.correct || all)
				{
					out.add(target);
				}
			}
		}
		return out;
	}

	private void roots(Set<Object> things, View view, List<Target> out)
	{
		for (Object thing : things)
		{
			final boolean special = view.idOf(thing) == ROOT_SPECIAL;
			out.add(new Target(thing, special ? "Anima root" : null, special));
		}
	}

	private void sapling(Set<Object> things, View view, List<Target> out)
	{
		for (Object thing : things)
		{
			final int id = view.idOf(thing);
			String label = null;
			if (id >= INGREDIENT_FIRST && id <= INGREDIENT_LAST)
			{
				for (int slot = 0; slot < saplingOrder.length; slot++)
				{
					if (saplingOrder[slot] == thing)
					{
						label = ORDINALS[slot];
						break;
					}
				}
			}
			out.add(new Target(thing, label, label != null));
		}
	}

	private void pheasants(Set<Object> things, View view, List<Target> out)
	{
		final List<WorldPoint> birds = new ArrayList<>();
		for (Object thing : things)
		{
			if (view.idOf(thing) == PHEASANT)
			{
				birds.add(view.tile(thing));
			}
		}
		for (Object thing : things)
		{
			final int id = view.idOf(thing);
			if (id == FORESTER)
			{
				out.add(new Target(thing, "Eggs here", true));
			}
			else if (id == NEST_1 || id == NEST_2)
			{
				final boolean empty = !occupied(view.tile(thing), birds);
				out.add(new Target(thing, empty ? "Empty nest" : null, empty));
			}
			else
			{
				out.add(new Target(thing, null, false));
			}
		}
	}

	private static boolean occupied(WorldPoint nest, List<WorldPoint> birds)
	{
		if (nest == null)
		{
			return false;
		}
		for (WorldPoint bird : birds)
		{
			if (bird != null && bird.getPlane() == nest.getPlane()
				&& Math.max(Math.abs(bird.getX() - nest.getX()), Math.abs(bird.getY() - nest.getY())) <= PHEASANT_REACH)
			{
				return true;
			}
		}
		return false;
	}

	private void circles(Set<Object> things, View view, List<Target> out)
	{
		final List<Object> circles = new ArrayList<>();
		final List<Integer> ids = new ArrayList<>();
		for (Object thing : things)
		{
			final int id = view.idOf(thing);
			if (id >= RITUAL_FIRST && id <= RITUAL_LAST)
			{
				circles.add(thing);
				ids.add(id);
			}
			else
			{
				out.add(new Target(thing, null, false));
			}
		}
		final int[] array = new int[ids.size()];
		for (int i = 0; i < array.length; i++)
		{
			array[i] = ids.get(i);
		}
		final int solved = solveCircles(array);
		for (int i = 0; i < circles.size(); i++)
		{
			out.add(new Target(circles.get(i), i == solved ? "Step here" : null, i == solved));
		}
	}

	private boolean tracked(SkillingEvents.Forestry kind, Object thing)
	{
		return thing != null && set(kind).contains(thing);
	}

	private Set<Object> set(SkillingEvents.Forestry kind)
	{
		final Set<Object> set = marks.get(kind);
		return set != null ? set : Collections.emptySet();
	}
}
