package com.vinnyvangogh.combatvitals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.util.Text;

/**
 * The plugin's side of the Goals and Max Hit pages: the {@link GoalBook}
 * kept in the account's own config profile, the events that move it, and the
 * snapshot of levels and gear the Max Hit page fills itself from.
 *
 * <p>The book's storage is {@code ConfigManager}'s RuneScape profile
 * configuration, so goals belong to the account and not the install. Reads
 * and writes of it happen off the client thread's hot path: a save goes to
 * the executor.
 */
@Singleton
final class GoalsHub
{
	/** The hidden, per-account key the goals are saved under. */
	static final String KEY = "goals";
	/** The book is checked for a new day or week this often, in game ticks. */
	private static final int ROLLOVER_TICKS = 100;

	private final Client client;
	private final ClientThread clientThread;
	private final ItemManager itemManager;
	private final ConfigManager configManager;
	private final ScheduledExecutorService executor;
	private final GoalBook book;
	private Map<Integer, MonsterMagic> monsters;

	@Inject
	GoalsHub(Client client, ClientThread clientThread, ItemManager itemManager, ConfigManager configManager,
		ScheduledExecutorService executor)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.itemManager = itemManager;
		this.configManager = configManager;
		this.executor = executor;
		this.book = new GoalBook(new GoalBook.Storage()
		{
			@Override
			public String load()
			{
				return configManager.getRSProfileConfiguration(CombatVitalsConfig.GROUP, KEY);
			}

			@Override
			public void save(String text)
			{
				executor.execute(() -> configManager.setRSProfileConfiguration(CombatVitalsConfig.GROUP, KEY, text));
			}

			@Override
			public void unset()
			{
				executor.execute(() -> configManager.unsetRSProfileConfiguration(CombatVitalsConfig.GROUP, KEY));
			}
		});
	}

	GoalBook book()
	{
		return book;
	}

	/**
	 * Logged in: the account's goals replace what was held, but only when the account is not the one already
	 * loaded (see {@link GoalBook#loadFor}). Client thread.
	 */
	void loggedIn()
	{
		book.loadFor(client.getAccountHash());
		book.rollover(now());
	}

	/** Logged out: nothing of one account stays for the next. */
	void loggedOut()
	{
		book.clear();
	}

	List<Goal> onStat(Skill skill, int xp)
	{
		return book.onXp(skill.getName(), xp, now());
	}

	/** A game message; the goals it moves, if it is a kill count line. */
	List<Goal> onChat(String message)
	{
		final String boss = GoalBook.killCountBoss(message);
		final long count = boss == null ? -1 : GoalBook.killCountValue(message);
		return count < 0 ? new ArrayList<>() : book.onKillCount(boss, count);
	}

	void tick(int tickCount)
	{
		if (tickCount % ROLLOVER_TICKS == 0)
		{
			book.rollover(now());
		}
	}

	/** The sentence a reached goal is announced with. */
	static String message(Goal g)
	{
		switch (g.type)
		{
			case SKILL_LEVEL:
				return "Goal reached: " + g.label + " level " + g.target;
			case SKILL_XP:
				return "Goal reached: " + g.label + " " + g.target + " xp";
			default:
				return "Goal reached: " + g.label;
		}
	}

	/** Max Hit's "use what I wear": read on the client thread, handed back on the Swing thread. */
	MaxHitPage.Source source()
	{
		return done -> clientThread.invoke(() ->
		{
			final MaxHitPage.Snapshot s = snapshot();
			SwingUtilities.invokeLater(() -> done.accept(s));
		});
	}

	private MaxHitPage.Snapshot snapshot()
	{
		final MaxHitPage.Snapshot s = new MaxHitPage.Snapshot();
		s.attack = client.getBoostedSkillLevel(Skill.ATTACK);
		s.strength = client.getBoostedSkillLevel(Skill.STRENGTH);
		s.ranged = client.getBoostedSkillLevel(Skill.RANGED);
		s.magic = client.getBoostedSkillLevel(Skill.MAGIC);
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		// Stab, slash and crush attack summed over everything worn; the page takes the best, as the game's
		// weapon style would pick one.
		final int[] melee = new int[3];
		if (worn != null)
		{
			for (Item item : worn.getItems())
			{
				addGear(s, melee, item.getId());
			}
		}
		s.attackBonus = Math.max(melee[0], Math.max(melee[1], melee[2]));
		final Actor target = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getInteracting();
		if (target instanceof NPC)
		{
			final NPC npc = (NPC) target;
			s.target = npc.getName() == null ? "" : Text.removeTags(npc.getName());
			final MonsterMagic m = monsters().get(npc.getId());
			if (m != null)
			{
				s.targetMagicLevel = m.level;
				s.targetMagicDefence = m.defence;
			}
		}
		return s;
	}

	private void addGear(MaxHitPage.Snapshot s, int[] melee, int id)
	{
		if (id <= 0)
		{
			return;
		}
		final ItemStats stats = itemManager.getItemStats(id);
		final ItemEquipmentStats e = stats == null ? null : stats.getEquipment();
		if (e == null)
		{
			return;
		}
		melee[0] += e.getAstab();
		melee[1] += e.getAslash();
		melee[2] += e.getAcrush();
		s.strengthBonus += e.getStr();
		s.rangedAttack += e.getArange();
		s.rangedStrength += e.getRstr();
		s.magicAttack += e.getAmagic();
		s.magicDamagePercent += e.getMdmg();
	}

	private Map<Integer, MonsterMagic> monsters()
	{
		if (monsters == null)
		{
			monsters = MonsterMagic.load();
		}
		return monsters;
	}

	private static long now()
	{
		return System.currentTimeMillis() / 1000;
	}
}
