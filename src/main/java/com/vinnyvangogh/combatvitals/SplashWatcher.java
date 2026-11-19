package com.vinnyvangogh.combatvitals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.util.Text;

/**
 * Splashing mode and the casts-left read-out, the client side: reads the
 * player's own Magic attack bonus, Magic level and prayer, the runes they
 * carry, and the NPC the player is casting at, once a tick, and hands them to
 * {@link SplashTracker} and {@link SpellCasts}. Information only: it reads
 * state and shows text, and never casts, clicks or changes a menu.
 *
 * "Casting" is autocast on (varbit AUTOCAST_SET), or a spell the player
 * clicked on that NPC in the last {@link #MANUAL_TICKS} ticks, seen by
 * reading, never consuming, the {@code MenuOptionClicked} event.
 */
@Singleton
class SplashWatcher
{
	/** How long a hand-cast spell counts as casting at that NPC. */
	static final int MANUAL_TICKS = 8;

	/** COMBAT_WEAPON_CATEGORY values of weapons that can autocast: Staff and Bladed staff. */
	private static final int WEAPON_STAFF = 18;
	private static final int WEAPON_BLADED_STAFF = 21;

	private static final Prayer[] MAGIC_PRAYERS = {
		Prayer.AUGURY, Prayer.MYSTIC_VIGOUR, Prayer.MYSTIC_MIGHT, Prayer.MYSTIC_LORE, Prayer.MYSTIC_WILL};
	/** The wiki's prayer multipliers, in {@link #MAGIC_PRAYERS} order. */
	private static final double[] PRAYER_BONUS = {1.25, 1.18, 1.15, 1.10, 1.05};

	private static final int[] POUCH_TYPES = {VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
		VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4};
	private static final int[] POUCH_QUANTITIES = {VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4};

	/** The item shown for each rune in a spell's cost, by {@link SpellCasts.Rune} ordinal. */
	private static final int[] RUNE_ITEMS = {ItemID.AIRRUNE, ItemID.WATERRUNE, ItemID.EARTHRUNE, ItemID.FIRERUNE,
		ItemID.MINDRUNE, ItemID.CHAOSRUNE, ItemID.DEATHRUNE, ItemID.BLOODRUNE, ItemID.SOULRUNE, ItemID.WRATHRUNE};

	private final Client client;
	private final ItemManager itemManager;
	private final CombatVitalsConfig config;
	private final SplashTracker tracker = new SplashTracker(MonsterMagic.load());
	private final SplashTimer timer = new SplashTimer();
	private final Map<Integer, String> names = new HashMap<>();

	private final SplashAuto auto = new SplashAuto();
	private int autoGearHash;
	private int autoBonus;
	private Boolean autoSwitch;

	private volatile SplashReadout readout;
	private int manualNpcIndex = -1;
	/** The NPC the player last clicked Attack on; autocast only counts as casting at that NPC. */
	private int attackNpcIndex = -1;
	private int manualSpell;
	private String manualSpellName;
	private int manualTicks;
	private String spellName;
	private boolean lowArmed = true;
	private String lowAlert;
	private String stopSoonAlert;
	private String stoppedAlert;

	@Inject
	SplashWatcher(Client client, ItemManager itemManager, CombatVitalsConfig config)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.config = config;
	}

	/**
	 * Runs the first-cast work once at start-up (class loading, the spell tables, the regexes, String.format)
	 * so the first attack does it off the hot path instead of on the client thread. Touches no client state.
	 */
	void warm()
	{
		final SplashTracker.Input in = new SplashTracker.Input(1, 1, 1, 1, "warm", 0, 1, 1.0);
		new SplashTracker(Collections.emptyMap()).step(in, 0);
		final int[] cost = SpellCasts.cost("Fire Strike");
		if (cost != null)
		{
			final SpellCasts.Supply supply = new SpellCasts.Supply();
			supply.item("staff of fire");
			SplashReadout.castsText(SpellCasts.casts(cost, supply));
		}
		SplashTracker.percent(0.5);
		SplashTimer.clock(1000);
	}

	/** The latest read-out for the overlay, or null when nothing is being cast at an NPC. */
	SplashReadout readout()
	{
		final SplashReadout r = readout;
		if (!on() || r == null)
		{
			return null;
		}
		if (r.isStopped() && !SplashTimer.lingers(System.currentTimeMillis() - r.stoppedAt,
			config.splashLingerSeconds(), config.splashLingerForever()))
		{
			return null;
		}
		return r;
	}

	private boolean on()
	{
		return config.splashMode() || config.castsLeft();
	}

	void reset()
	{
		readout = null;
		manualNpcIndex = -1;
		attackNpcIndex = -1;
		lowArmed = true;
		lowAlert = null;
		stopSoonAlert = null;
		stoppedAlert = null;
		tracker.reset();
		timer.reset();
	}

	/** Logout or shutdown: forget what auto-detect saw. */
	void resetAuto()
	{
		auto.reset();
		autoGearHash = 0;
		autoSwitch = null;
	}

	/** The low-casts alert text due this tick, once, or null. */
	String takeLowCastsAlert()
	{
		final String text = lowAlert;
		lowAlert = null;
		return text;
	}

	void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!on())
		{
			return;
		}
		if (isNpcOption(event.getMenuAction()))
		{
			final NPC clicked = event.getMenuEntry().getNpc();
			if (clicked != null)
			{
				attackNpcIndex = attackClick(event.getMenuOption(), clicked.getIndex(), attackNpcIndex);
			}
			return;
		}
		if (event.getMenuAction() != MenuAction.WIDGET_TARGET_ON_NPC)
		{
			return;
		}
		final NPC npc = event.getMenuEntry().getNpc();
		if (npc == null)
		{
			return;
		}
		final Widget spell = client.getSelectedWidget();
		manualNpcIndex = npc.getIndex();
		manualSpell = spell == null ? 0 : spell.getId();
		manualSpellName = spell == null || spell.getName() == null ? null : Text.removeTags(spell.getName());
		manualTicks = MANUAL_TICKS;
	}

	private static boolean isNpcOption(MenuAction action)
	{
		switch (action)
		{
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
				return true;
			default:
				return false;
		}
	}

	/**
	 * The attack target after a click on an NPC option: that NPC for Attack; none if it was the target and
	 * the click was Talk-to, Pickpocket, Trade or anything else; otherwise unchanged.
	 */
	static int attackClick(String option, int npcIndex, int current)
	{
		if ("Attack".equalsIgnoreCase(option))
		{
			return npcIndex;
		}
		return current == npcIndex ? -1 : current;
	}

	/**
	 * Reads this tick. Returns the setup-changed alert text, or null; the
	 * plugin sends it, so the sidebar mute and "Mute alert sounds" apply.
	 */
	String tick()
	{
		autoDetect();
		if (!on())
		{
			reset();
			return null;
		}
		if (manualTicks > 0)
		{
			manualTicks--;
		}
		final SplashTracker.Input in = read();
		final long now = System.currentTimeMillis();
		final SplashReadout shown = readout;
		if (in != null || shown != null && !shown.isStopped())
		{
			afk(idleMillis());
		}
		if (in == null)
		{
			if (shown != null && !shown.isStopped())
			{
				readout = shown.stopped(now);
			}
			tracker.idle();
			timer.idle();
			return null;
		}
		timer.cast(now);
		final SplashTracker.Result splash = tracker.step(in, config.splashMaxChance());
		final boolean splashing = config.splashMode();
		readout = build(splashing ? splash : null, now);
		return splashing ? splash.changed : null;
	}

	/** Reads the worn Magic bonus and queues a Splashing mode switch when auto-detect wants one. */
	private void autoDetect()
	{
		if (!config.autoSplash() || client.getLocalPlayer() == null || client.getItemContainer(InventoryID.WORN) == null)
		{
			return;
		}
		final int[] gear = worn();
		final int hash = Arrays.hashCode(gear);
		if (hash != autoGearHash)
		{
			autoGearHash = hash;
			autoBonus = magicBonus(gear);
		}
		autoSwitch = auto.step(autoBonus, config.autoSplashThreshold(), config.splashMode());
	}

	/** The Splashing mode auto-detect wants set now (once), or null. */
	Boolean takeAutoSwitch()
	{
		final Boolean sw = autoSwitch;
		autoSwitch = null;
		return sw;
	}

	/** The player flipped Splashing mode by hand: auto-detect leaves it until the gear crosses the threshold. */
	void manualSwitch()
	{
		auto.manualSwitch();
	}

	private SplashReadout build(SplashTracker.Result splash, long now)
	{
		String castsText = null;
		int casts = -2;
		boolean low = false;
		int[] items = new int[0];
		int[] costs = new int[0];
		final int[] cost = SpellCasts.cost(spellName);
		if (config.castsLeft() && cost != null)
		{
			final SpellCasts.Result r = SpellCasts.casts(cost, supply());
			casts = r.casts;
			castsText = SplashReadout.castsText(r);
			final int warnAt = config.castsWarnAt();
			low = warnAt > 0 && casts != SpellCasts.INFINITE && casts <= warnAt;
			pace(low, casts);
			items = new int[cost.length];
			costs = new int[cost.length];
			int n = 0;
			for (int i = 0; i < cost.length; i++)
			{
				if (cost[i] > 0)
				{
					items[n] = RUNE_ITEMS[i];
					costs[n++] = cost[i];
				}
			}
			items = Arrays.copyOf(items, n);
			costs = Arrays.copyOf(costs, n);
		}
		return new SplashReadout(splash, spellName, castsText, casts, low, items, costs, idleMillis(),
			SplashTimer.STOP_MS, timer.elapsed(now), now);
	}

	/** Milliseconds since the last mouse or keyboard input the client saw. */
	private long idleMillis()
	{
		return Math.min(client.getKeyboardIdleTicks(), client.getMouseIdleTicks())
			* (long) Constants.CLIENT_TICK_LENGTH;
	}

	private void afk(long idleMillis)
	{
		final int due = timer.afk(idleMillis, config.splashWarnSeconds() * 1000L);
		if (due == SplashTimer.STOPS_SOON)
		{
			stopSoonAlert = "Splashing stops by itself in "
				+ SplashTimer.clock(SplashTimer.logoutLeft(idleMillis, SplashTimer.STOP_MS))
				+ ": click or press a key to keep it going.";
		}
		else if (due == SplashTimer.STOPPED)
		{
			stoppedAlert = "Splashing has stopped: no input for 20 minutes.";
		}
	}

	/** The stops-soon alert text due this tick, once, or null. */
	String takeStopSoonAlert()
	{
		final String text = stopSoonAlert;
		stopSoonAlert = null;
		return text;
	}

	/** The stopped alert text due this tick, once, or null. */
	String takeStoppedAlert()
	{
		final String text = stoppedAlert;
		stoppedAlert = null;
		return text;
	}

	/** Fires the low-casts alert once as the count drops to the warn level, and again after a refill. */
	private void pace(boolean low, int casts)
	{
		if (!low)
		{
			lowArmed = true;
			return;
		}
		if (lowArmed)
		{
			lowArmed = false;
			lowAlert = "Low on casts: " + spellName + " has " + casts + (casts == 1 ? " cast" : " casts")
				+ " left.";
		}
	}

	private SplashTracker.Input read()
	{
		if (client.getLocalPlayer() == null)
		{
			return null;
		}
		final Actor target = client.getLocalPlayer().getInteracting();
		if (!(target instanceof NPC))
		{
			return null;
		}
		final NPC npc = (NPC) target;
		// The autocast flag can stay set while a melee weapon is wielded: only a staff makes it a cast.
		final int weaponCategory = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
		final boolean auto = client.getVarbitValue(VarbitID.AUTOCAST_SET) == 1
			&& (weaponCategory == WEAPON_STAFF || weaponCategory == WEAPON_BLADED_STAFF)
			// Talking to or pickpocketing an NPC also sets the interacting target: only an attacked one counts.
			&& attackNpcIndex == npc.getIndex();
		final boolean manual = manualTicks > 0 && manualNpcIndex == npc.getIndex();
		if (!auto && !manual)
		{
			return null;
		}
		final int autoSpell = client.getVarbitValue(VarbitID.AUTOCAST_SPELL);
		final int spell = manual && !auto ? 100_000 + manualSpell : autoSpell;
		spellName = manual && !auto ? manualSpellName : SpellCasts.autocastName(autoSpell);
		final String name = npc.getName() == null ? "this NPC" : Text.removeTags(npc.getName());
		final int[] gear = worn();
		return new SplashTracker.Input(spell, Arrays.hashCode(gear), npc.getIndex(), npc.getId(), name,
			magicBonus(gear), client.getBoostedSkillLevel(Skill.MAGIC), prayerBonus());
	}

	/** The item ids worn, in slot order, -1 for an empty slot. */
	private int[] worn()
	{
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return new int[0];
		}
		final Item[] items = worn.getItems();
		final int[] ids = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			ids[i] = items[i].getId();
		}
		return ids;
	}

	/** The Equipment stats window's Magic attack: the sum over everything worn. */
	private int magicBonus(int[] gear)
	{
		int total = 0;
		for (int id : gear)
		{
			if (id <= 0)
			{
				continue;
			}
			final ItemStats stats = itemManager.getItemStats(id);
			final ItemEquipmentStats equipment = stats == null ? null : stats.getEquipment();
			if (equipment != null)
			{
				total += equipment.getAmagic();
			}
		}
		return total;
	}

	private double prayerBonus()
	{
		for (int i = 0; i < MAGIC_PRAYERS.length; i++)
		{
			if (client.isPrayerActive(MAGIC_PRAYERS[i]))
			{
				return PRAYER_BONUS[i];
			}
		}
		return 1.0;
	}

	/** Runes in the inventory and rune pouch, and the staff and tome worn. */
	private SpellCasts.Supply supply()
	{
		final SpellCasts.Supply supply = new SpellCasts.Supply();
		boolean pouch = false;
		final ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item.getId() <= 0)
				{
					continue;
				}
				if (!addRune(supply, item.getId(), item.getQuantity()))
				{
					pouch |= name(item.getId()).contains("rune pouch");
				}
			}
		}
		if (pouch)
		{
			addPouch(supply);
		}
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn != null)
		{
			for (Item item : worn.getItems())
			{
				if (item.getId() > 0)
				{
					supply.item(name(item.getId()));
				}
			}
		}
		return supply;
	}

	private void addPouch(SpellCasts.Supply supply)
	{
		final EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		if (runes == null)
		{
			return;
		}
		for (int slot = 0; slot < POUCH_TYPES.length; slot++)
		{
			final int type = client.getVarbitValue(POUCH_TYPES[slot]);
			final int quantity = client.getVarbitValue(POUCH_QUANTITIES[slot]);
			if (type > 0 && quantity > 0)
			{
				addRune(supply, runes.getIntValue(type), quantity);
			}
		}
	}

	/** Adds a rune item to the supply; false when the item is not one the spells use. */
	private static boolean addRune(SpellCasts.Supply supply, int itemId, long count)
	{
		switch (itemId)
		{
			case ItemID.AIRRUNE:
				supply.rune(SpellCasts.Rune.AIR, count);
				return true;
			case ItemID.WATERRUNE:
				supply.rune(SpellCasts.Rune.WATER, count);
				return true;
			case ItemID.EARTHRUNE:
				supply.rune(SpellCasts.Rune.EARTH, count);
				return true;
			case ItemID.FIRERUNE:
			case ItemID.SUNFIRERUNE:
				supply.rune(SpellCasts.Rune.FIRE, count);
				return true;
			case ItemID.MINDRUNE:
				supply.rune(SpellCasts.Rune.MIND, count);
				return true;
			case ItemID.CHAOSRUNE:
				supply.rune(SpellCasts.Rune.CHAOS, count);
				return true;
			case ItemID.DEATHRUNE:
				supply.rune(SpellCasts.Rune.DEATH, count);
				return true;
			case ItemID.BLOODRUNE:
				supply.rune(SpellCasts.Rune.BLOOD, count);
				return true;
			case ItemID.SOULRUNE:
				supply.rune(SpellCasts.Rune.SOUL, count);
				return true;
			case ItemID.WRATHRUNE:
				supply.rune(SpellCasts.Rune.WRATH, count);
				return true;
			case ItemID.DUSTRUNE:
				supply.combo(SpellCasts.Combo.DUST, count);
				return true;
			case ItemID.MISTRUNE:
				supply.combo(SpellCasts.Combo.MIST, count);
				return true;
			case ItemID.MUDRUNE:
				supply.combo(SpellCasts.Combo.MUD, count);
				return true;
			case ItemID.SMOKERUNE:
				supply.combo(SpellCasts.Combo.SMOKE, count);
				return true;
			case ItemID.STEAMRUNE:
				supply.combo(SpellCasts.Combo.STEAM, count);
				return true;
			case ItemID.LAVARUNE:
				supply.combo(SpellCasts.Combo.LAVA, count);
				return true;
			default:
				return false;
		}
	}

	private String name(int itemId)
	{
		return names.computeIfAbsent(itemId, id -> itemManager.getItemComposition(id).getName().toLowerCase(Locale.ROOT));
	}
}
