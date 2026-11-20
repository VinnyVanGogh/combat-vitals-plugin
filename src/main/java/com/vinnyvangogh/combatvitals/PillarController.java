package com.vinnyvangogh.combatvitals;

import com.google.gson.Gson;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import lombok.extern.slf4j.Slf4j;

/**
 * The side pillars' sources, the client side: once a game tick it reads the player's own hitpoints,
 * prayer, run energy, special attack, ammo, runes, food and potions, and the health bar of the target
 * being fought, into a {@link PillarSnapshot} that {@link PillarValues} turns into pillars. Also keeps
 * what chat says about weapon charges, and runs the preset controls: Apply preset, Save, Load, Rename,
 * Delete and Auto-switch. Reads only; it never changes a menu, clicks or sends input.
 *
 * Charges are read from the game's chat lines because the client exposes no varbit this plugin
 * relies on for them, so a weapon shows nothing until its charges have been checked once.
 */
@Slf4j
@Singleton
class PillarController
{
	private static final Skill[] BOOSTED = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final SkillIconManager skillIconManager;
	private final CombatVitalsConfig config;
	private final ConfigManager configManager;
	private final ScheduledExecutorService executor;
	private final SplashWatcher splashWatcher;
	private final Gson gson;

	private final Map<Integer, ItemInfo> items = new HashMap<>();
	private final Map<String, Integer> charges = new HashMap<>();
	private final Map<String, Integer> chargeMax = new HashMap<>();
	private final int[] boostPeak = new int[Skill.values().length];
	private int blowpipeDarts = -1;
	private int blowpipeScales = -1;
	private String lastAuto;

	private volatile PillarSnapshot snapshot = PillarSnapshot.builder().build();

	@Inject
	PillarController(Client client, ClientThread clientThread, ItemManager itemManager,
		SkillIconManager skillIconManager, CombatVitalsConfig config, ConfigManager configManager,
		ScheduledExecutorService executor, SplashWatcher splashWatcher, Gson gson)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.skillIconManager = skillIconManager;
		this.config = config;
		this.configManager = configManager;
		this.executor = executor;
		this.splashWatcher = splashWatcher;
		this.gson = gson;
	}

	/** The latest tick's reading; the overlay reads it every frame. */
	PillarSnapshot snapshot()
	{
		return snapshot;
	}

	/** One side's setup as the settings say. */
	PillarSetup setup(PillarSide side)
	{
		return side == PillarSide.LEFT
			? new PillarSetup(config.pillarLeftSplit(), config.pillarLeftSource(), config.pillarLeftSource2())
			: new PillarSetup(config.pillarRightSplit(), config.pillarRightSource(), config.pillarRightSource2());
	}

	/** Forgets what was read; for logging out or shutting down. */
	void reset()
	{
		snapshot = PillarSnapshot.builder().build();
		items.clear();
		lastAuto = null;
	}

	/**
	 * Once a game tick, on the client thread.
	 *
	 * @param target   the readout of the opponent being fought, or null
	 * @param skilling a skilling session is running
	 */
	void tick(CombatVitalsPlugin.Readout target, boolean skilling)
	{
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		autoSwitch(skilling);
		if (!setup(PillarSide.LEFT).anyCustom() && !setup(PillarSide.RIGHT).anyCustom())
		{
			return;
		}
		snapshot = read(target);
	}

	private PillarSnapshot read(CombatVitalsPlugin.Readout target)
	{
		final PillarSnapshot.PillarSnapshotBuilder s = PillarSnapshot.builder()
			.hp(client.getBoostedSkillLevel(Skill.HITPOINTS))
			.maxHp(client.getRealSkillLevel(Skill.HITPOINTS))
			.prayer(client.getBoostedSkillLevel(Skill.PRAYER))
			.maxPrayer(client.getRealSkillLevel(Skill.PRAYER))
			.runEnergy(client.getEnergy() / 100)
			.specPercent(client.getVarpValue(VarPlayerID.SA_ENERGY) / 10);

		equipment(s);
		inventory(s);
		runesAndCasts(s);
		boost(s);
		if (target != null)
		{
			final double fraction = HealthReadout.fraction(target.getRatio(), target.getScale(),
				target.getExactHealth(), target.getMaxHealth());
			s.targetName(target.getName()).targetFraction(fraction).targetMax(target.getMaxHealth())
				.targetHp(PillarText.targetHp(target.getExactHealth(), target.getRange(), target.getMaxHealth()));
		}
		return s.build();
	}

	/** Ammo and charges: the weapon, and what it fires. */
	private void equipment(PillarSnapshot.PillarSnapshotBuilder s)
	{
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		final Item weapon = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		final String weaponName = weapon == null || weapon.getId() <= 0 ? null : info(weapon.getId()).name;
		if (weaponName != null)
		{
			chargesOf(s, weaponName);
		}

		if (PillarSupplies.blowpipe(weaponName))
		{
			s.ammoName("Toxic blowpipe darts").ammoCount(blowpipeDarts).ammoExtra(blowpipeScales);
		}
		else if (PillarSupplies.thrown(weaponName))
		{
			s.ammoName(weaponName).ammoCount(weapon.getQuantity());
		}
		else
		{
			final Item ammo = worn == null ? null : worn.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
			if (ammo != null && ammo.getId() > 0)
			{
				s.ammoName(info(ammo.getId()).name).ammoCount(ammo.getQuantity());
			}
		}
	}

	private void chargesOf(PillarSnapshot.PillarSnapshotBuilder s, String weaponName)
	{
		final String wanted = weaponName.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Integer> e : charges.entrySet())
		{
			if (wanted.contains(e.getKey()) || e.getKey().contains(wanted))
			{
				s.charges(e.getValue()).chargesName(weaponName).chargesMax(chargeMax.getOrDefault(e.getKey(), -1));
				return;
			}
		}
	}

	/** Cannonballs, food and potion doses in the inventory. */
	private void inventory(PillarSnapshot.PillarSnapshotBuilder s)
	{
		final ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return;
		}
		int cannonballs = 0;
		int food = 0;
		int heal = 0;
		int unknown = 0;
		int prayer = 0;
		int stamina = 0;
		for (Item item : inventory.getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}
			final ItemInfo info = info(item.getId());
			if (info.lower.contains("cannonball"))
			{
				cannonballs += item.getQuantity();
			}
			if (info.food)
			{
				food += item.getQuantity();
				final int each = PillarSupplies.heal(info.name);
				if (each < 0)
				{
					unknown += item.getQuantity();
				}
				else
				{
					heal += each * item.getQuantity();
				}
			}
			prayer += PillarSupplies.prayerDoses(info.name) * item.getQuantity();
			stamina += PillarSupplies.staminaDoses(info.name) * item.getQuantity();
		}
		s.cannonballs(cannonballs).foodCount(food).foodHeal(heal).foodUnknown(unknown).prayerDoses(prayer)
			.staminaDoses(stamina);
	}

	private void runesAndCasts(PillarSnapshot.PillarSnapshotBuilder s)
	{
		final SpellCasts.Supply supply = splashWatcher.supplyNow();
		s.runeCounts(supply.runes.clone());
		final CastsReadout casts = splashWatcher.castsForPillar();
		if (casts != null)
		{
			s.castsLeft(casts.casts).castsSpell(casts.spell);
		}
	}

	/** The strongest combat boost still running, against the highest it reached. */
	private void boost(PillarSnapshot.PillarSnapshotBuilder s)
	{
		Skill best = null;
		int bestBoost = 0;
		for (Skill skill : BOOSTED)
		{
			final int boost = client.getBoostedSkillLevel(skill) - client.getRealSkillLevel(skill);
			final int i = skill.ordinal();
			boostPeak[i] = boost <= 0 ? 0 : Math.max(boostPeak[i], boost);
			if (boost > bestBoost)
			{
				bestBoost = boost;
				best = skill;
			}
		}
		if (best != null)
		{
			s.boostLeft(bestBoost).boostPeak(boostPeak[best.ordinal()]).boostSkill(best.getName());
		}
	}

	/** A game message: a weapon's charges, or the blowpipe's darts and scales. */
	void chat(String message)
	{
		final PillarSupplies.Charge charge = PillarSupplies.charge(message);
		if (charge != null)
		{
			charges.put(charge.item, charge.charges);
			chargeMax.put(charge.item, charge.max);
		}
		final int[] blowpipe = PillarSupplies.blowpipeCheck(message);
		if (blowpipe != null)
		{
			blowpipeDarts = blowpipe[0];
			blowpipeScales = blowpipe[1];
		}
	}

	private ItemInfo info(int id)
	{
		return items.computeIfAbsent(id, key ->
		{
			final ItemComposition c = itemManager.getItemComposition(key);
			return new ItemInfo(c.getName(), PillarSupplies.isFood(c.getInventoryActions()));
		});
	}

	private static final class ItemInfo
	{
		final String name;
		final String lower;
		final boolean food;

		ItemInfo(String name, boolean food)
		{
			this.name = name;
			this.lower = name.toLowerCase(Locale.ROOT);
			this.food = food;
		}
	}

	// ---- presets ----

	/** Switches presets by weapon and activity, once per change, when the player has it on. */
	private void autoSwitch(boolean skilling)
	{
		if (!config.pillarAutoSwitch())
		{
			lastAuto = null;
			return;
		}
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		final Item weapon = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		final String weaponName = weapon == null || weapon.getId() <= 0 ? null : info(weapon.getId()).name;
		final String kind = PillarPresets.autoChoice(weaponName, skilling);
		if (kind.equals(lastAuto))
		{
			return;
		}
		lastAuto = kind;
		final PillarPresets.Builtin builtin = PillarPresets.builtin(kind);
		final String name = builtin == PillarPresets.Builtin.SKILLING ? config.pillarAutoSkilling()
			: builtin == PillarPresets.Builtin.RANGED ? config.pillarAutoRanged()
			: builtin == PillarPresets.Builtin.MAGIC ? config.pillarAutoMagic() : config.pillarAutoMelee();
		final Map<String, String> preset = PillarPresets.find(name, saved());
		if (preset != null)
		{
			PillarPresets.apply(preset, store());
		}
	}

	private Map<String, Map<String, String>> saved()
	{
		return PillarPresets.parse(gson, config.pillarUserPresets());
	}

	private ConfigStore store()
	{
		return ConfigStore.of(configManager, executor);
	}

	/**
	 * A preset control was used. May run on any thread: the work and the chat reply go to the client
	 * thread. The control's box is cleared so it can be used again.
	 */
	void configChanged(String key)
	{
		switch (key)
		{
			case "pillarPreset":
				final PillarPresetChoice choice = config.pillarPreset();
				if (choice != PillarPresetChoice.KEEP)
				{
					clientThread.invoke(() -> applyChoice(choice.name()));
				}
				break;
			case "pillarSaveAs":
				command(config.pillarSaveAs(), this::save);
				break;
			case "pillarLoad":
				command(config.pillarLoad(), this::load);
				break;
			case "pillarRename":
				command(config.pillarRename(), this::rename);
				break;
			case "pillarDelete":
				command(config.pillarDelete(), this::delete);
				break;
			default:
				break;
		}
	}

	/** Runs a typed command on the client thread; a cleared box is not one. */
	private void command(String text, java.util.function.Consumer<String> run)
	{
		if (text != null && !text.trim().isEmpty())
		{
			clientThread.invoke(() -> run.accept(text));
		}
	}

	private void applyChoice(String value)
	{
		configManager.unsetConfiguration(CombatVitalsConfig.GROUP, "pillarPreset");
		try
		{
			final PillarPresets.Builtin builtin = PillarPresetChoice.valueOf(value).builtin;
			if (builtin != null)
			{
				PillarPresets.apply(builtin.settings(), store());
				say("Pillars set to " + builtin.label + ".");
			}
		}
		catch (IllegalArgumentException e)
		{
			log.debug("Unknown pillar preset choice {}", value);
		}
	}

	private void save(String name)
	{
		configManager.unsetConfiguration(CombatVitalsConfig.GROUP, "pillarSaveAs");
		final Map<String, Map<String, String>> saved = saved();
		if (!PillarPresets.save(saved, name, PillarPresets.current(store())))
		{
			say("Pillar preset not saved: use a name of 1 to " + PillarPresets.MAX_NAME
				+ " characters that is not a built-in preset's name.");
			return;
		}
		write(saved);
		say("Saved pillar preset " + name.trim() + ". Presets: " + PillarPresets.names(saved) + ".");
	}

	private void load(String name)
	{
		configManager.unsetConfiguration(CombatVitalsConfig.GROUP, "pillarLoad");
		final Map<String, Map<String, String>> saved = saved();
		final Map<String, String> preset = PillarPresets.find(name, saved);
		if (preset == null)
		{
			say("No pillar preset named " + name.trim() + ". Presets: " + PillarPresets.names(saved) + ".");
			return;
		}
		PillarPresets.apply(preset, store());
		say("Pillars set to " + name.trim() + ".");
	}

	private void rename(String value)
	{
		configManager.unsetConfiguration(CombatVitalsConfig.GROUP, "pillarRename");
		final int arrow = value.indexOf('>');
		final Map<String, Map<String, String>> saved = saved();
		if (arrow < 0 || !PillarPresets.rename(saved, value.substring(0, arrow), value.substring(arrow + 1)))
		{
			say("Pillar preset not renamed: type \"old name > new name\" for one of your saved presets, with a new "
				+ "name that is free and not a built-in's.");
			return;
		}
		write(saved);
		say("Renamed to " + value.substring(arrow + 1).trim() + ".");
	}

	private void delete(String name)
	{
		configManager.unsetConfiguration(CombatVitalsConfig.GROUP, "pillarDelete");
		final Map<String, Map<String, String>> saved = saved();
		if (!PillarPresets.delete(saved, name))
		{
			say("No saved pillar preset named " + name.trim() + " (built-ins cannot be deleted).");
			return;
		}
		write(saved);
		say("Deleted " + name.trim() + ".");
	}

	private void write(Map<String, Map<String, String>> saved)
	{
		configManager.setConfiguration(CombatVitalsConfig.GROUP, "pillarUserPresets", PillarPresets.toJson(gson, saved));
	}

	private void say(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}

	// ---- looks ----

	/** How the player's settings dress the sources. */
	PillarValues.Settings settings()
	{
		return new PillarValues.Settings()
		{
			@Override
			public Color colour(PillarSource source)
			{
				switch (source)
				{
					case HITPOINTS:
						return config.pillarColourHitpoints();
					case PRAYER:
						return config.pillarColourPrayer();
					case RUN_ENERGY:
						return config.pillarColourRun();
					case SPEC:
						return config.pillarColourSpec();
					case AMMO:
						return config.pillarColourAmmo();
					case RUNES:
						return config.pillarColourRunes();
					case CASTS:
						return config.pillarColourCasts();
					case CHARGES:
						return config.pillarColourCharges();
					case CANNONBALLS:
						return config.pillarColourCannonballs();
					case FOOD:
						return config.pillarColourFood();
					case PRAYER_DOSES:
						return config.pillarColourDoses();
					case STAMINA:
						return config.pillarColourStamina();
					case BOOST:
						return config.pillarColourBoost();
					default:
						return source.getDefaultColour();
				}
			}

			@Override
			public Color targetColour(double fraction)
			{
				return HealthReadout.colour(fraction, config.healthFullColour(), config.healthMidColour(),
					config.healthLowColour());
			}

			@Override
			public Color lowColour()
			{
				return config.pillarColourLow();
			}

			@Override
			public BufferedImage icon(PillarSource source)
			{
				switch (source)
				{
					case HITPOINTS:
					case TARGET_HP:
						return skillIconManager.getSkillImage(Skill.HITPOINTS, true);
					case PRAYER:
					case PRAYER_DOSES:
						return skillIconManager.getSkillImage(Skill.PRAYER, true);
					case RUN_ENERGY:
					case STAMINA:
						return skillIconManager.getSkillImage(Skill.AGILITY, true);
					case AMMO:
					case SPEC:
						return skillIconManager.getSkillImage(Skill.RANGED, true);
					case RUNES:
					case CASTS:
						return skillIconManager.getSkillImage(Skill.MAGIC, true);
					case CHARGES:
						return skillIconManager.getSkillImage(Skill.ATTACK, true);
					case CANNONBALLS:
						return skillIconManager.getSkillImage(Skill.SMITHING, true);
					case FOOD:
						return skillIconManager.getSkillImage(Skill.COOKING, true);
					case BOOST:
						return skillIconManager.getSkillImage(Skill.STRENGTH, true);
					default:
						return null;
				}
			}

			@Override
			public int ammoFull()
			{
				return config.pillarAmmoFull();
			}

			@Override
			public int ammoLow()
			{
				return config.pillarAmmoLow();
			}

			@Override
			public int runeFull()
			{
				return config.pillarRuneFull();
			}

			@Override
			public PillarRune rune()
			{
				return config.pillarRune();
			}

			@Override
			public int castsLow()
			{
				return config.castsWarnAt();
			}
		};
	}
}
