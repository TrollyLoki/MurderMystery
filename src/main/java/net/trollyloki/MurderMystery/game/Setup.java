package net.trollyloki.MurderMystery.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import net.trollyloki.MurderMystery.Main;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Setup {
	
	public static String[] autoStart(List<Player> players, String map) {
		
		if (map.equals("")) {
			boolean picking = true;
			while (picking) {
				
				Object[] maps = Main.getPlugin().getConfig().getConfigurationSection("maps").getKeys(false).toArray();
				if (maps.length < 1) return new String[] {null, "invalid-map"};
				int rand = ThreadLocalRandom.current().nextInt(0, maps.length);
				map = (String) maps[rand];
				
				boolean exempt = Main.getPlugin().getConfig().getBoolean("maps." + map + ".shuffle-exempt");
				if (!exempt) picking = false;
			}
		}
		
		return new String[] {map, startGame(map, players)};
		
	}
	
	public static String startGame(String map, List<Player> players) {
		
		if (!Main.getPlugin().getConfig().contains("maps." + map)) {
			return "invalid-map";
		}
		
		// Check if a game is running
		if (Run.gameRunning) {
			return "already-running";
		}
		
		// Check for valid map
		
		// Check for minimum player count
		List<Player> allPlayers = new ArrayList<Player>();
		for (Player nextPlayer : players) {
			allPlayers.add(nextPlayer);
		}
		
			// Insert Code for picking players
		
		if (allPlayers.size() < 3) {
			return "not-enough-players";
		}
		
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		Team team = null;
		String error = "unknown-error";
		try {
			error = "invalid-team";
			team = manager.getMainScoreboard().registerNewTeam(Main.getPlugin().getConfig().getString("timer.team"));
			team.unregister();
			error = "invalid-objective";
			Objective objective = manager.getMainScoreboard().registerNewObjective(Main.getPlugin().getConfig().getString("timer.objective"), "dummy", "timer");
			objective.unregister();
		} catch (IllegalArgumentException e) {
			return error;
		}
		Main.sendDebug("Created scoreboard team");
		
		// Create Required Variables
		Player murderer;
		Player detective;
		Player deputy = null;
		List<Player> bystanders = new ArrayList<Player>();
		
		// Select Players
		murderer = getRandomPlayer(allPlayers);
		allPlayers.remove(murderer);
		detective = getRandomPlayer(allPlayers);
		allPlayers.remove(detective);
		if (Main.getPlugin().getConfig().getBoolean("enable-deputy")) {
			deputy = getRandomPlayer(allPlayers);
			allPlayers.remove(deputy);
		}
		for (Player nextPlayer : allPlayers) {
			bystanders.add(nextPlayer);
		}
		allPlayers.add(murderer);
		allPlayers.add(detective);
		if (deputy != null) allPlayers.add(deputy);
		
		Main.sendDebug("Assigned roles");
		//Main.sendDebug(String.valueOf(allPlayers));
		
		// Prepare map
		Maps.startMap(map);
		
		// Prepare players for start
		for (Player nextPlayer : allPlayers) {
			nextPlayer.getInventory().clear();
			nextPlayer.setGameMode(GameMode.ADVENTURE);
			nextPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 99999, 255, true, false));
			Location loc = getStartLocation(map);
			if (loc == null) {
				return "unknown-error";
			}
			nextPlayer.teleport(loc);
			nextPlayer.setHealth(20);
			nextPlayer.setCustomNameVisible(false);
		}
		
		Main.sendDebug("Prepared players for game");
		
		ItemStack potion = getInvisPotion();
		
		// Give murderer their sword
		ItemStack sword = getMurdererSword();
		
		int swordSlot = 1;
		if (murderer.getInventory().getHeldItemSlot() == 1) swordSlot = 2;
		murderer.getInventory().setItem(swordSlot, sword);
		murderer.getInventory().setItem(swordSlot + 1, potion);
		
		Main.sendDebug("Gave the murderer a sword");
		
		// Give detective their bow
		ItemStack bow = getDetectiveBow();
		
		int bowSlot = 1;
		if (detective.getInventory().getHeldItemSlot() == 1) bowSlot = 2;
		detective.getInventory().setItem(bowSlot, bow);
		
		Main.sendDebug("Gave the detective a bow");
		
		// Give detective an arrow
		detective.getInventory().setItem(17, new ItemStack(Material.ARROW, 1));
		
		Main.sendDebug("Gave the detective an arrow");
		
		// Give deputy their bow
		if (deputy != null) {
			ItemStack dbow = getDeputyBow();
		
			int dbowSlot = 1;
			if (deputy.getInventory().getHeldItemSlot() == 1) dbowSlot = 2;
			deputy.getInventory().setItem(dbowSlot, dbow);
			
			Main.sendDebug("Gave the deputy a bow");
		
			// Give deputy an arrow
			deputy.getInventory().setItem(17, new ItemStack(Material.ARROW, 1));
			
			Main.sendDebug("Gave the deputy an arrow");
		}
		
		// Give bystanders their potion
		if (potion != null) for (Player nextPlayer : bystanders) {
			int potionSlot = 1;
			if (nextPlayer.getInventory().getHeldItemSlot() == 1) potionSlot = 2;
			nextPlayer.getInventory().setItem(potionSlot, potion);
		}
		
		Main.sendDebug("Gave the bystanders potions");
		
		// Tell players their role
		int fadeIn = Main.getPlugin().getConfig().getInt("titles.in");
		int stay = Main.getPlugin().getConfig().getInt("titles.stay");
		int fadeOut = Main.getPlugin().getConfig().getInt("titles.out");
		murderer.sendTitle(Main.getConfigString(false, "titles.murderer.title"),
				Main.getConfigString(false, "titles.murderer.subtitle"), fadeIn, stay, fadeOut);
		detective.sendTitle(Main.getConfigString(false, "titles.detective.title"),
				Main.getConfigString(false, "titles.detective.subtitle"), fadeIn, stay, fadeOut);
		if (deputy != null) deputy.sendTitle(Main.getConfigString(false, "titles.deputy.title"),
				Main.getConfigString(false, "titles.deputy.subtitle"), fadeIn, stay, fadeOut);
		for (Player nextPlayer : bystanders) {
			nextPlayer.sendTitle(Main.getConfigString(false, "titles.bystander.title"),
					Main.getConfigString(false, "titles.bystander.subtitle"), fadeIn, stay, fadeOut);
		}
		
		Main.sendDebug("Sent titles");
		
		// Send variables to Run class
		Run.murderer = murderer;
		Run.detective = detective;
		Run.deputy = deputy;
		Run.bystanders = bystanders;
		Run.allPlayers = allPlayers;
		
		Run.innoKills.clear();
		for (Player nextPlayer : allPlayers) {
			Run.innoKills.put(nextPlayer, 0);
		}
		
		Run.bow = bow;
		Run.sword = sword;
		Run.map = map;
		
		Main.sendDebug("Assigned global variables");
    	
		// Initiate Game
		Main.sendDebug("Starting game...");
		
		Run.startGame();
		
		return "success";
		
	}
	
	public static ItemStack getMurdererSword() {
		ItemStack sword = new ItemStack(Material.valueOf(Main.getConfigString(false, "items.sword.type")), 1);
		ItemMeta swordMeta = sword.getItemMeta();
		swordMeta.setDisplayName(Main.getConfigString(false, "items.sword.name"));
		swordMeta.setLore(Main.getConfigStringList(false, "items.sword.lore"));
		swordMeta.setUnbreakable(true);
		sword.setItemMeta(swordMeta);
		return sword;
	}
	
	public static ItemStack getDetectiveBow() {
		ItemStack bow = new ItemStack(Material.valueOf(Main.getConfigString(false, "items.bow.type")), 1);
		ItemMeta bowMeta = bow.getItemMeta();
		bowMeta.setDisplayName(Main.getConfigString(false, "items.bow.name"));
		bowMeta.setLore(Main.getConfigStringList(false, "items.bow.lore"));
		bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
		bowMeta.setUnbreakable(true);
		bow.setItemMeta(bowMeta);
		return bow;
	}
	
	public static ItemStack getDeputyBow() {
		ItemStack dbow = new ItemStack(Material.valueOf(Main.getConfigString(false, "items.deputy-bow.type")), 1);
		ItemMeta dbowMeta = dbow.getItemMeta();
		dbowMeta.setDisplayName(Main.getConfigString(false, "items.deputy-bow.name"));
		dbowMeta.setLore(Main.getConfigStringList(false, "items.deputy-bow.lore"));
		dbowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
		dbowMeta.setUnbreakable(true);
		dbow.setItemMeta(dbowMeta);
		return dbow;
	}
	
	public static ItemStack getInvisPotion() {
		if (!Main.getPlugin().getConfig().getBoolean("items.potion.enabled")) return null;
		
		ItemStack potion = new ItemStack(Material.valueOf(Main.getConfigString(false, "items.potion.type")), 1);
		if (!(potion.getItemMeta() instanceof PotionMeta)) {
			Main.getPlugin().getLogger().warning("Invalid potion type in config, defaulting to normal potion");
			potion = new ItemStack(Material.POTION, 1);
		}
		
		PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
		potionMeta.setDisplayName(Main.getConfigString(false, "items.potion.name"));
		potionMeta.setLore(Main.getConfigStringList(false, "items.potion.lore"));
		
		String[] color = Main.getConfigString(false, "items.potion.color").split(",");
		if (color.length == 3) {
			try {
				int red = Integer.valueOf(color[0]);
				int green = Integer.valueOf(color[1]);
				int blue = Integer.valueOf(color[2]);
				potionMeta.setColor(Color.fromRGB(red, green, blue));
			} catch (NumberFormatException e) {
				e.printStackTrace();
				Main.getPlugin().getLogger().warning("Invalid potion color in config");
			}
		}
		else {
			Main.getPlugin().getLogger().warning("Invalid potion color in config");
		}
		
		int duration = Main.getPlugin().getConfig().getInt("items.potion.duration") * 20;
		PotionEffect effect = new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, true, true);
		potionMeta.addCustomEffect(effect, true);
		potion.setItemMeta(potionMeta);
		return potion;
	}
	
	
	public static Player getRandomPlayer(Collection<? extends Player> players) {
		int random = new Random().nextInt(players.size());
		int i = 0;
		for (Player player : players) {
			if (i == random) {
				return player;
			}
			i += 1;
		}
		return null;
	}
	
	public static Location getStartLocation(String map) {
		try {
			String[] location = Main.getPlugin().getConfig().getString("maps." + map + ".spawn.location").split(",");
			double x = Double.parseDouble(location[0]);
			double y = Double.parseDouble(location[1]);
			double z = Double.parseDouble(location[2]);
			float yaw = Float.parseFloat(Main.getPlugin().getConfig().getString("maps." + map + ".spawn.yaw"));
			float pitch = Float.parseFloat(Main.getPlugin().getConfig().getString("maps." + map + ".spawn.pitch"));
			World world = Bukkit.getServer().getWorld(Main.getPlugin().getConfig().getString("maps." + map + ".spawn.world"));
			
			return new Location(world, x, y, z, yaw, pitch);
			
		} catch (NumberFormatException | NullPointerException e) {
			Main.getPlugin().getLogger().warning("Invalid coordinates for spawn of map: " + map);
		}
		
		return null;
		
	}
	
}