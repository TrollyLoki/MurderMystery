package net.trollyloki.MurderMystery.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import net.trollyloki.MurderMystery.Main;

public class Run implements Listener {
	
	public static boolean gameRunning = false;
	public static String autoRestartMap = null;
	public static Boolean grace;
	public static BukkitTask compassTask;
	
	public static Player murderer;
	public static Player detective;
	public static Player deputy;
	public static List<Player> bystanders = new ArrayList<Player>();
	public static List<Player> allPlayers = new ArrayList<Player>();
	public static Map<Player, Integer> innoKills = new HashMap<Player, Integer>();
	public static ItemStack bow;
	public static ItemStack sword;
	public static String map;
	public static ArmorStand stand = null;
	
	public static void startGame() {
		
		JavaPlugin plugin = Main.getPlugin();
		
		Main.sendDebug(String.valueOf(allPlayers));
		
		Main.sendDebug("Attempting to start timer...");
		
		Timer.CountdownTimer(plugin, plugin.getConfig().getInt("timer.time"), plugin.getConfig().getInt("timer.grace-period"), plugin.getConfig().getInt("timer.murder-compass"));
		gameRunning = true;
		grace = true;
		
		compassTask = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
		
		
			double minDistance = Double.MAX_VALUE;
			Player closestPlayer = null;
			for (Player p : allPlayers) {
				if (p == murderer || !(p == detective || p == deputy || bystanders.contains(p)))
					continue;
				if (p.hasPotionEffect(PotionEffectType.INVISIBILITY))
					continue;
				
				double distance = murderer.getLocation().distanceSquared(p.getLocation());
				if (distance < minDistance) {
					minDistance = distance;
					closestPlayer = p;
				}
			}
			
			if (closestPlayer != null)
				murderer.setCompassTarget(closestPlayer.getLocation());
		

		}, 0, 0);
		
		Main.sendDebug("Set gameRunning and grace");
		
	}
	
	@EventHandler
	public void onPaintingDestroyed(HangingBreakByEntityEvent event) {
		if (gameRunning) {
			Player player = null;
			if (event.getRemover() instanceof Player) {
				
				player = (Player) event.getRemover();
				
			}
			
			if (event.getRemover() instanceof Projectile) {
				
				Projectile projectile = (Projectile) event.getRemover();
				if (projectile.getShooter() instanceof Player) {
					player = (Player) projectile.getShooter();
				}
				
			}
			
			if (player != null && allPlayers.contains(player))
				event.setCancelled(true);
			
		}
		
	}
	
	@EventHandler
	public void onItemFrameHit(EntityDamageByEntityEvent event) {
		if (gameRunning) {
			if (event.getEntity().getType() != EntityType.ITEM_FRAME) return;
			
			Player player = null;
			if (event.getDamager() instanceof Player) {
				
				player = (Player) event.getDamager();
				
			}
			
			if (event.getDamager() instanceof Projectile) {
				
				Projectile projectile = (Projectile) event.getDamager();
				if (projectile.getShooter() instanceof Player) {
					player = (Player) projectile.getShooter();
				}
				
			}
			
			if (player != null && allPlayers.contains(player))
				event.setCancelled(true);
			
		}
		
	}
	
	@EventHandler
	public void onArrowHit(ProjectileHitEvent event) {
		if (gameRunning) {
			if (event.getEntity().getType() == EntityType.ARROW) {
				
				Arrow arrow = (Arrow) event.getEntity();
				if (arrow.getShooter() instanceof Player) {
					
					Player player = (Player) arrow.getShooter();
					if (allPlayers.contains(player)) {
						Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {
	
							@Override
							public void run() {
								if (event.getEntity() != null) {
									event.getEntity().remove();
								}
							}}, 10L);
						event.getEntity().remove();
					}
					
				}
				
			}
		}
		
	}
	
	
	@EventHandler
	public void onEntityDamageEvent(EntityDamageEvent event) {
		if (gameRunning) {
			if (event.getEntity().getType() == EntityType.PLAYER) {
				Player player = (Player) event.getEntity();
				if (allPlayers.contains(player)) {
					Main.sendDebug("Player Damaged");
					
					if (grace) {
						event.setCancelled(true);
						return;
					}
					
					else {
					
						if (event.getCause() != DamageCause.PROJECTILE && event.getCause() != DamageCause.ENTITY_ATTACK) {
							event.setCancelled(true);
							return;
						}
						
						if (event.getCause() == DamageCause.PROJECTILE && !player.hasPermission("mm.immune")) {
							
							if (player == murderer) {
								kill(player, false);
							}
							
							else {
								kill(player, false);
								killedinno(detective);
							}
							
							return;
							
						}
						
					}
					event.setCancelled(true);
					
				}
				
			}
		}
	}
	
	@EventHandler
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (gameRunning) {
			if (event.getEntity().getType() == EntityType.PLAYER && event.getDamager().getType() == EntityType.PLAYER) {
				Player player = (Player) event.getEntity();
				Player attacker = (Player) event.getDamager();
				if (allPlayers.contains(player)) {
					Main.sendDebug("Player Attacked");
					
					if (grace) {
						event.setCancelled(true);
						return;
					}
					
					else {
						if (event.getCause() != DamageCause.PROJECTILE && event.getCause() != DamageCause.ENTITY_ATTACK) {
							event.setCancelled(true);
							return;
						}
						
						if (event.getCause() == DamageCause.ENTITY_ATTACK) {
							
							if (attacker == murderer && murderer.getPotionEffect(PotionEffectType.INVISIBILITY) == null && murderer.getInventory().getItemInMainHand().isSimilar(sword)
									&& !player.hasPermission("mm.immune")) {
								kill(player, false);
							}
							
							else {
								event.setCancelled(true);
							}
							
						}
						
						if (event.getCause() == DamageCause.PROJECTILE && !player.hasPermission("mm.immune")) {
							
							if (player == murderer) {
								kill(player, false);
							}
							
							else {
								kill(player, false);
								killedinno(attacker);
							}
							
							return;
							
						}
					}
					event.setCancelled(true);
					
				}
				
			}
		}
	}
	
	private void killedinno(Player player) {
		player.sendMessage(Main.getConfigString(false, "lang.messages.killed-inno"));
		int kills = innoKills.get(player) + 1;
		innoKills.put(player, kills);
		
		int innocentKillsBeforeDetectiveKill = Main.getPlugin().getConfig().getInt("innocent-kills-before-detective-kill");
		if (innocentKillsBeforeDetectiveKill > 0 && kills >= innocentKillsBeforeDetectiveKill) {
			
			kill(player, false);
			
		}
		else {
			
			PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 300, 255, true, false);
			player.addPotionEffect(effect);
			effect = new PotionEffect(PotionEffectType.BLINDNESS, 300, 255, true, false);
			player.addPotionEffect(effect);
			
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (gameRunning && allPlayers.contains(player)) {
			kill(player, false);
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (gameRunning && stand != null && bystanders.contains(event.getPlayer())) {
			Location loc = event.getTo();
			Location standLoc = stand.getLocation().add(0, 1.6875, 0);
			//Main.sendDebug("Bystander moved. Distance to bow: " + standLoc);
			if (loc.distance(standLoc) < 2) {
				Main.sendDebug("Player moved to bow");
				stand.remove();
				stand = null;
				Main.sendDebug("Removed armorstand");
				Player player = event.getPlayer();
				
				int bowSlot = 1;
				if (player.getInventory().getHeldItemSlot() == 1) bowSlot = 2;
				player.getInventory().setItem(bowSlot, bow);
				
				player.getInventory().setItem(17, new ItemStack(Material.ARROW, 1));
				detective = player;
				Main.sendDebug(player.getName() + " is now the detective");
				
				player.sendMessage(Main.getConfigString(false, "lang.messages.you-got-bow"));
				for (Player nextPlayer : allPlayers) {
					if (nextPlayer != player) {
						nextPlayer.sendMessage(Main.getConfigString(false, "lang.messages.player-got-bow"));
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		if (gameRunning && allPlayers.contains(event.getPlayer())) {
			Main.sendDebug("Player Changed Gamemode");
			
			Player player = event.getPlayer();
			if (player.hasPermission("mm.gamemode")) return;
			
			if (player == murderer || player == detective || bystanders.contains(player)) {
				if (event.getNewGameMode() != GameMode.ADVENTURE) {
					player.sendMessage(Main.getConfigString(false, "lang.messages.gamemode"));
					event.setCancelled(true);
				}
			}
			
			else {
				if (event.getNewGameMode() != GameMode.SPECTATOR) {
					player.sendMessage(Main.getConfigString(false, "lang.messages.gamemode"));
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onItemDropped(PlayerDropItemEvent event) {
		if (gameRunning && allPlayers.contains(event.getPlayer())) {
			Main.sendDebug("Player dropped item");
			
			Player player = event.getPlayer();
			if (player.hasPermission("mm.drop")) return;
			
			player.sendMessage(Main.getConfigString(false, "lang.messages.drop"));
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onItemPickedUp(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (gameRunning && allPlayers.contains(player)) {
				ItemStack stack = event.getItem().getItemStack();
				if (stack.getType() == Material.GOLD_INGOT) {
					Main.sendDebug("Player picked up gold");
					event.setCancelled(true);
					event.getItem().remove();
					
					int amount = stack.getAmount();
					int current = 0;
					if (player.getInventory().getItem(8) != null && player.getInventory().getItem(8).getType() == Material.GOLD_INGOT) {
						current = player.getInventory().getItem(8).getAmount();
					}
					
					player.getInventory().setItem(8, new ItemStack(Material.GOLD_INGOT, current + amount));
					player.sendMessage(Main.getConfigString(false, "lang.messages.gold"));
				}
			}
		}
	}
	
	@EventHandler
	public void onDisconnect(PlayerQuitEvent event) {
		if (gameRunning && allPlayers.contains(event.getPlayer())) {
			kill(event.getPlayer(), true);
			allPlayers.remove(event.getPlayer());
		}
	}

	public static String kill(Player player, Boolean quit) {
		if (allPlayers.contains(player)) {
			if (player == murderer || player == detective || player == deputy || bystanders.contains(player)) {
				Main.sendDebug(player.getName() + " died/quit");
				
				if (bystanders.contains(player)) bystanders.remove(player);
				if (player == murderer) murdererKilled();
				if (player == detective) detectiveKilled();
				if (player == deputy) deputy = null;
				
				if (!quit) {
					player.setGameMode(GameMode.SPECTATOR);
					player.getInventory().clear();
					player.sendMessage(Main.getConfigString(false, "lang.messages.you-died"));
					
					for (Player nextPlayer : allPlayers) {
						if (nextPlayer != player) {
							nextPlayer.sendMessage(Main.getConfigString(false, "lang.messages.player-died"));
						}
						String strSound = Main.getPlugin().getConfig().getString("lang.messages.died-sound");
						if (!strSound.equalsIgnoreCase("none")) {
							Sound sound = Sound.valueOf(strSound);
							nextPlayer.playSound(nextPlayer.getLocation(), sound, 1, 1);
						}
					}
					
				}
				
				else {
					
					for (Player nextPlayer : allPlayers) {
						if (nextPlayer != player) {
							nextPlayer.sendMessage(Main.getConfigString(true, "lang.messages.player-quit").replaceAll("%player%", player.getName()));
						}
						String strSound = Main.getPlugin().getConfig().getString("lang.messages.died-sound");
						if (!strSound.equalsIgnoreCase("none")) {
							Sound sound = Sound.valueOf(strSound);
							nextPlayer.playSound(nextPlayer.getLocation(), sound, 1, 1);
						}
					}
					
				}
				
				Main.sendDebug("Removed player's role");

				if (detective == null && deputy == null && bystanders.size() == 0) {
					Main.sendDebug("All players are dead");
					for (Player nextPlayer : allPlayers) {
						nextPlayer.sendMessage(Main.getConfigString(false, "lang.messages.all-dead"));
					}
					
					Main.sendDebug("Attemping to end game...");

					endGame(false);
				}
				Timer.forceUpdate();
				
				return "kill.success";
			}

			return "kill.already-dead";
		}
		
		return "not-playing";
	}
	
	public static String revive(Player player) {
		if (allPlayers.contains(player)) {
			if (!(player == murderer || player == detective || player == deputy || bystanders.contains(player))) {
				Main.sendDebug(player.getName() + " was revived");
				
				bystanders.add(player);
				
				player.getInventory().clear();
				player.setGameMode(GameMode.ADVENTURE);
				player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 99999, 255, true, false));
				Location loc = Setup.getStartLocation(map);
				if (loc == null) {
					return "unknown-error";
				}
				player.teleport(loc);
				player.setHealth(20);
				
				int potionSlot = 1;
				if (player.getInventory().getHeldItemSlot() == 1) potionSlot = 2;
				player.getInventory().setItem(potionSlot, Setup.getInvisPotion());
				player.sendMessage(Main.getConfigString(false, "lang.messages.you-revived"));
				
				for (Player nextPlayer : allPlayers) {
					if (nextPlayer != player) {
						nextPlayer.sendMessage(Main.getConfigString(true, "lang.messages.player-revived").replaceAll("%player%", player.getName()));
					}
				}
				
				Main.sendDebug("Re-setup player");
				
				Timer.forceUpdate();
				
				return "revive.success";
			}

			return "revive.not-dead";
		}
		
		return "not-playing";
	}

	private static void detectiveKilled() {
		@SuppressWarnings("deprecation")
		boolean noGrav = detective.isOnGround();
		Main.sendDebug("Detective died");
		stand = detective.getLocation().getWorld().spawn(detective.getLocation().add(0, -1.6875, 0), ArmorStand.class);
		stand.setGravity(false);
		stand.setVisible(false);
		stand.setRemoveWhenFarAway(false);
		stand.getEquipment().setHelmet(bow);
		stand.setInvulnerable(true);
		stand.setHeadPose(new EulerAngle(1.5708, 0.0, 0.0));
		if (!noGrav)
			applyGravity(detective.getLocation());
		Main.sendDebug("Created Armorstand");
		detective = null;
		for (Player nextPlayer : allPlayers) {
			nextPlayer.sendMessage(Main.getConfigString(false, "lang.messages.detective-dead"));
		}
	}
	
	private static void applyGravity(Location loc) {
		
		final ArmorStand checker = loc.getWorld().spawn(loc, ArmorStand.class);
		checker.setVisible(false);
		//checker.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 5, 128, false, false));
		Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {

			@Override
			public void run() {
				if (stand != null)
					stand.teleport(checker.getLocation().add(0, -1.6875, 0));
				checker.remove();
			}}, 20L);
		
	}
	
	
	@EventHandler
	public void onArmorstandInteract(PlayerInteractAtEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		if (allPlayers.contains(player) && entity.getType() == EntityType.ARMOR_STAND) {
			event.setCancelled(true);
		}
	}

	private static void murdererKilled() {
		Main.sendDebug("Murderer died");
		murderer = null;
		for (Player nextPlayer : allPlayers) {
			nextPlayer.sendMessage(Main.getConfigString(false, "lang.messages.murderer-dead"));
		}
		
		endGame(false);
	}

	public static void timeExpired() {
		Main.sendDebug("Time expired");
		for (Player nextPlayer : allPlayers) {
			nextPlayer.sendMessage(Main.getConfigString(false, "lang.messages.time-up"));
		}
		
		endGame(false);
	}
	
	public static String endGame(Boolean force) {
		
		if (gameRunning) {
			Main.sendDebug("Ending Game...");
			gameRunning = false;
			if (!force) {
				String winner = Main.getConfigString(false, "titles.game-over.roles.players");	
				if (detective == null && deputy == null && bystanders.size() == 0) winner = Main.getConfigString(false, "titles.game-over.roles.murderer");
				int fadeIn = Main.getPlugin().getConfig().getInt("titles.in");
				int stay = Main.getPlugin().getConfig().getInt("titles.stay");
				int fadeOut = Main.getPlugin().getConfig().getInt("titles.out");
				String title = Main.getConfigString(false, "titles.game-over.title").replaceAll("%winner%", winner);
				String subtitle = Main.getConfigString(false, "titles.game-over.subtitle").replaceAll("%winner%", winner);
				
				for (Player nextPlayer : allPlayers) {
					nextPlayer.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
				}
				
				Main.sendDebug("Sent ending titles");
			}
			else {
				autoRestartMap = null;
			}
			
			Main.sendDebug("Attemping to end timer...");
			Timer.endTimer();
			compassTask.cancel();
			
			if (stand != null) {
				stand.remove();
				stand = null;
				Main.sendDebug("Removed Armorstand");
			}
			
			for (Player nextPlayer : allPlayers) {
				nextPlayer.getInventory().clear();
				for (PotionEffect effect : nextPlayer.getActivePotionEffects()) {
					PotionEffectType effectType = effect.getType();
					nextPlayer.removePotionEffect(effectType);
				}
			}
			
			Main.sendDebug("Cleared inventories");
			
			Maps.endMap(map);
			
			
			
			if (autoRestartMap != null) {
				
				final int delay = Main.getPlugin().getConfig().getInt("timer.restart-delay");
				for (Player nextPlayer : allPlayers) {
					nextPlayer.sendMessage(Main.getConfigString(true, "lang.messages.auto-restart").replaceAll("%time%", String.valueOf(delay)));
				}
				
				final List<Player> oldList = new ArrayList<Player>();
				for (Player player : allPlayers) {
					if (player.isOnline()) oldList.add(player);
				}
				
				
				for (int i = 1; i < delay; i++) {
					
					final int j = i;
					Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {

						@Override
						public void run() {
							for (Player nextPlayer : oldList) {
								nextPlayer.sendMessage(Main.getConfigString(false, "lang.messages.auto-restart").replaceAll("%time%", String.valueOf(delay - j)));
							}
						}}, i * 20);
					
				}
				
				
				Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), new Runnable() {

					@Override
					public void run() {
						Setup.autoStart(oldList, autoRestartMap);
					}}, delay * 20);
				
			}
			
			
			
			String mapName = Main.getConfigString(false, "maps." + map + ".name");
			murderer = null;
			detective = null;
			deputy = null;
			bystanders.clear();
			allPlayers.clear();
			innoKills.clear();
			bow = null;
			sword = null;
			map = null;
			stand = null;
			grace = null;
			
			Main.sendDebug("Cleared global variables");
			
			return mapName;
		}
		
		else {
			return "not-running";
		}
		
	}
	
	
	public static int getPlayersRemaining() {
		int playerCount = 0;
		playerCount += bystanders.size();
		if (detective != null) playerCount += 1;
		if (deputy != null) playerCount += 1;
		if (murderer != null) playerCount += 1;
		return playerCount;
	}
	
}
