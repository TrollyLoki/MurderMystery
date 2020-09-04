package net.trollyloki.MurderMystery.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.trollyloki.MurderMystery.Main;
import net.trollyloki.MurderMystery.game.Maps;
import net.trollyloki.MurderMystery.game.Run;
import net.trollyloki.MurderMystery.game.Setup;
import net.trollyloki.MurderMystery.game.Timer;

public class CommandMM implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("mm")) {

			if (args.length == 0) {
				sender.sendMessage(Main.getConfigString(false, "lang.command.usage"));
				return false;
			}

			if (args[0].equalsIgnoreCase("start")) {
				if (!sender.hasPermission("mm.start")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.start.usage"));
					return false;
				}

				if (args.length >= 2) {
					if (Run.autoRestartMap != null && Run.autoRestartMap.equals("")) {
						sender.sendMessage(Main.getConfigString(true, "lang.command.start.shuffle-enabled"));
						return false;
					} else if (Run.autoRestartMap != null) {
						sender.sendMessage(Main.getConfigString(true, "lang.command.start.loop-enabled"));
						return false;
					}

					String result = Setup.startGame(args[1].toLowerCase(), parsePlayers(args, 2));

					String name = args[1].toLowerCase();
					if (result != "invalid-map")
						name = Main.getConfigString(false, "maps." + args[1].toLowerCase() + ".name");

					String msg = Main.getConfigString(true, "lang.command.start." + result).replaceAll("%map%", name);
					if (result.equalsIgnoreCase("unknown-error")) {
						msg = Main.getConfigString(true, "lang.command.unknown-error");
					}

					sender.sendMessage(msg);
					return (result.equals("success"));
				}

			}

			if (args[0].equalsIgnoreCase("shuffle")) {

				if (!sender.hasPermission("mm.start")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				Run.autoRestartMap = "";
				String[] result = Setup.autoStart(parsePlayers(args, 1), Run.autoRestartMap);

				String name = null;
				if (result[1] != "invalid-map")
					name = Main.getConfigString(false, "maps." + result[0].toLowerCase() + ".name");

				sender.sendMessage(
						Main.getConfigString(true, "lang.command.start." + result[1]).replaceAll("%map%", name));
				if (result[1].equals("success")) {
					sender.sendMessage(Main.getConfigString(true, "lang.command.shuffle.enabled"));
				}
				return (result[1].equals("success"));

			}

			if (args[0].equalsIgnoreCase("loop")) {

				if (!sender.hasPermission("mm.start")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.loop.usage"));
					return false;
				}

				Run.autoRestartMap = args[1];
				String[] result = Setup.autoStart(parsePlayers(args, 2), Run.autoRestartMap);

				String name = null;
				if (result[1] != "invalid-map")
					name = Main.getConfigString(false, "maps." + result[0].toLowerCase() + ".name");

				sender.sendMessage(
						Main.getConfigString(true, "lang.command.start." + result[1]).replaceAll("%map%", name));
				if (result[1].equals("success")) {
					sender.sendMessage(Main.getConfigString(true, "lang.command.loop.enabled"));
				}
				return (result[1].equals("success"));

			}

			if (args[0].equalsIgnoreCase("maps")) {
				if (!sender.hasPermission("mm.start")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				sender.sendMessage(Main.getConfigString(true, "lang.command.maps"));
				for (String map : Main.getPlugin().getConfig().getConfigurationSection("maps").getKeys(false)) {

					String name = Main.getConfigString(false, "maps." + map + ".name");
					sender.sendMessage(ChatColor.DARK_GREEN + "- " + name + " (" + map + ")");

				}

				return true;
			}

			if (args[0].equalsIgnoreCase("stop")) {
				if (!sender.hasPermission("mm.stop")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length >= 1) {
					String result = Run.endGame(true);
					if (result != "not-running") {
						result = Main.getConfigString(true, "lang.command.stop.success").replaceAll("%map%", result);
					} else
						result = Main.getConfigString(true, "lang.command.not-running");

					sender.sendMessage(result);
					if (Run.autoRestartMap != null && Run.autoRestartMap.equals("")) {
						sender.sendMessage(Main.getConfigString(true, "lang.command.shuffle.disabled"));
					} else if (Run.autoRestartMap != null) {
						sender.sendMessage(Main.getConfigString(true, "lang.command.loop.disabled"));
					}
					Run.autoRestartMap = null;
					return !Run.gameRunning;
				}

			}

			if (args[0].equalsIgnoreCase("kill")) {
				if (!sender.hasPermission("mm.kill")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.kill.usage"));
					return false;
				}

				if (args.length >= 2) {
					Player toKill = Bukkit.getPlayerExact(args[1]);
					String result = Run.kill(toKill, false);
					sender.sendMessage(
							Main.getConfigString(true, "lang.command." + result).replaceAll("%player%", args[1]));
					return (result == "kill.success");
				}

			}

			if (args[0].equalsIgnoreCase("revive")) {
				if (!sender.hasPermission("mm.revive")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.revive.usage"));
					return false;
				}

				if (args.length >= 2) {
					Player toRevive = Bukkit.getPlayerExact(args[1]);
					String result = Run.revive(toRevive);
					sender.sendMessage(
							Main.getConfigString(true, "lang.command." + result).replaceAll("%player%", args[1]));
					return (result == "revive.success");
				}
			}

			if (args[0].equalsIgnoreCase("role")) {
				if (!sender.hasPermission("mm.role")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.role.usage"));
					return false;
				}

				if (args.length == 2) {
					Player player = Bukkit.getPlayerExact(args[1]);
					if (Run.allPlayers.contains(player)) {
						String role = null;
						if (player == Run.murderer)
							role = Main.getConfigString(false, "titles.murderer.role");
						else if (player == Run.detective)
							role = Main.getConfigString(false, "titles.detective.role");
						else if (player == Run.deputy)
							role = Main.getConfigString(false, "titles.deputy.role");
						else if (Run.bystanders.contains(player))
							role = Main.getConfigString(false, "titles.bystander.role");
						else
							role = Main.getConfigString(false, "titles.dead.role");

						String message = Main.getConfigString(true, "lang.command.role.success")
								.replaceAll("%player%", player.getName()).replaceAll("%role%", role);
						sender.sendMessage(message);
						return true;
					}

					sender.sendMessage(Main.getConfigString(true, "lang.command.not-playing"));
					return false;
				}

			}

			if (args[0].equalsIgnoreCase("changerole")) {
				if (!sender.hasPermission("mm.changerole")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length < 3) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.changerole.usage"));
					return false;
				}

				if (args.length == 3) {
					Player player = Bukkit.getPlayerExact(args[1]);
					if (Run.allPlayers.contains(player)) {
						if (Run.bystanders.contains(player) || Run.murderer == player || Run.detective == player || Run.deputy == player) {
							
							String role = args[2];
							String role2 = "bystander";
							Player player2 = null;
							if (role.equalsIgnoreCase("murderer"))
								player2 = Run.murderer;
							else if (role.equalsIgnoreCase("detective"))
								player2 = Run.detective;
							else if (role.equalsIgnoreCase("deputy")
									&& Main.getPlugin().getConfig().getBoolean("enable-deputy")) {
								player2 = Run.deputy;
							} else if (role.equalsIgnoreCase("bystander")) {
								if (Run.bystanders.isEmpty()) {
									sender.sendMessage(Main.getConfigString(true, "lang.command.changerole.no-bystanders"));
									return false;
								}
								player2 = Setup.getRandomPlayer(Run.bystanders);
							} else {
								sender.sendMessage(Main.getConfigString(true, "land.command.changerole.not-role"));
								return false;
							}
	
							if (player == Run.murderer)
								role2 = "murderer";
							else if (player == Run.detective)
								role2 = "detective";
							else if (player == Run.deputy)
								role2 = "deputy";
	
							if (role.equalsIgnoreCase(role2)) {
								sender.sendMessage(Main.getConfigString(true, "lang.command.changerole.no-change"));
								return false;
							}
	
							for (int i = 1; i <= 2; i++) {
								String r = i == 1 ? role : role2;
								Player p = i == 1 ? player : player2;
	
								if (p == null)
									continue;
								p.getInventory().clear();
	
								if (r.equalsIgnoreCase("murderer")) {
									Run.murderer = p;
									Run.bystanders.remove(p);
									// Give murderer their sword
									ItemStack sword = Setup.getMurdererSword();
	
									int swordSlot = 1;
									if (p.getInventory().getHeldItemSlot() == 1)
										swordSlot = 2;
									p.getInventory().setItem(swordSlot, sword);
								} else if (r.equalsIgnoreCase("detective")) {
									if (Run.stand != null) {
										Run.stand.remove();
										Run.stand = null;
										Main.sendDebug("Removed armorstand");
	
										p.sendMessage(Main.getConfigString(false, "lang.messages.you-got-bow"));
										for (Player nextPlayer : Run.allPlayers) {
											if (nextPlayer != p) {
												nextPlayer.sendMessage(
														Main.getConfigString(false, "lang.messages.player-got-bow"));
											}
										}
									}
	
									Run.detective = p;
									Run.bystanders.remove(p);
									// Give detective their bow
									ItemStack bow = Setup.getDetectiveBow();
	
									int bowSlot = 1;
									if (p.getInventory().getHeldItemSlot() == 1)
										bowSlot = 2;
									p.getInventory().setItem(bowSlot, bow);
	
									Main.sendDebug("Gave the detective a bow");
	
									// Give detective an arrow
									p.getInventory().setItem(17, new ItemStack(Material.ARROW, 1));
								} else if (r.equalsIgnoreCase("deputy")) {
									Run.deputy = p;
									Run.bystanders.remove(p);
									// Give deputy their bpw
									ItemStack dbow = Setup.getDeputyBow();
	
									int dbowSlot = 1;
									if (p.getInventory().getHeldItemSlot() == 1)
										dbowSlot = 2;
									p.getInventory().setItem(dbowSlot, dbow);
	
									Main.sendDebug("Gave the deputy a bow");
	
									// Give deputy an arrow
									p.getInventory().setItem(17, new ItemStack(Material.ARROW, 1));
								} else if (r.equalsIgnoreCase("bystander")) {
									Run.bystanders.add(p);
									// Give bystanders their potion
									ItemStack potion = Setup.getInvisPotion();
	
									if (potion != null) {
										int potionSlot = 1;
										if (p.getInventory().getHeldItemSlot() == 1)
											potionSlot = 2;
										p.getInventory().setItem(potionSlot, potion);
									}
								}
								
							}

							String message = Main.getConfigString(true, "lang.command.changerole.success")
									.replaceAll("%player%", player.getName())
									.replaceAll("%role%", Main.getConfigString(false, "titles." + role + ".role"));
							sender.sendMessage(message);
							if (player2 != null) {
								String message2 = Main.getConfigString(true, "lang.command.changerole.success")
										.replaceAll("%player%", player2.getName())
										.replaceAll("%role%", Main.getConfigString(false, "titles." + role2 + ".role"));
								sender.sendMessage(message2);
							}
									
							return true;
							
						}
						
						sender.sendMessage(Main.getConfigString(true, "lang.command.changerole.dead"));
						return false;
					}

					sender.sendMessage(Main.getConfigString(true, "lang.command.not-playing"));
					return false;
				}

			}

			if (args[0].equalsIgnoreCase("tt")) {
				if (!sender.hasPermission("mm.role")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.tt.usage"));
					return false;
				}

				if (args.length == 2) {
					Player player = Bukkit.getPlayerExact(args[1]);
					if (Run.allPlayers.contains(player)) {
						if (player == Run.murderer) {
							String message = Main.getConfigString(true, "lang.command.role.success");
							sender.sendMessage(message);
							return true;
						}

						else {
							String message = Main.getConfigString(true, "lang.command.role.failure");
							sender.sendMessage(message);
							return false;
						}
					}

					sender.sendMessage(Main.getConfigString(true, "lang.command.not-playing"));
					return false;
				}

			}

			if (args[0].equalsIgnoreCase("time")) {
				if (!sender.hasPermission("mm.time")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length < 3) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.time.usage"));
					return false;
				}

				if (args.length >= 3) {
					int amount = 0;
					try {
						amount = Integer.parseInt(args[2]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Main.getConfigString(false, "lang.command.nan"));
						return false;
					}

					String result = Timer.changeTime(args[1], amount);
					sender.sendMessage(Main.getConfigString(true, "lang.command." + result).replaceAll("%time%",
							String.valueOf(Timer.convertToTime(Timer.getSecondsLeft()))));
					return (result == "time.success");
				}

			}

			if (args[0].equalsIgnoreCase("create")) {
				if (!sender.hasPermission("mm.create")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.create.usage"));
					return false;
				}

				if (!(sender instanceof Player)) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.create.not-player"));
					return false;
				}

				if (args.length >= 2) {

					String name = args[1];
					if (args.length > 2)
						name = args[2];

					Player player = (Player) sender;
					boolean success = Maps.createMap(args[1], name, player.getLocation());
					if (success) {
						sender.sendMessage(
								Main.getConfigString(true, "lang.command.create.success").replaceAll("%map%", args[1]));
						return true;
					} else {
						sender.sendMessage(Main.getConfigString(true, "lang.command.create.already-exists")
								.replaceAll("%map%", args[1]));
						return false;
					}

				}

			}

			if (args[0].equalsIgnoreCase("delete")) {
				if (!sender.hasPermission("mm.delete")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length == 1) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.delete.usage"));
					return false;
				}

				if (args.length >= 2) {

					boolean success = Maps.removeMap(args[1]);
					if (success) {
						sender.sendMessage(
								Main.getConfigString(true, "lang.command.delete.success").replaceAll("%map%", args[1]));
						return true;
					} else {
						sender.sendMessage(Main.getConfigString(true, "lang.command.delete.doesnt-exist")
								.replaceAll("%map%", args[1]));
						return false;
					}

				}

			}

			if (args[0].equalsIgnoreCase("reload")) {
				if (!sender.hasPermission("mm.reload")) {
					sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
					return false;
				}

				if (args.length >= 1) {
					Main.getPlugin().reloadConfig();
					sender.sendMessage(Main.getConfigString(true, "lang.command.reload.success"));
					return true;
				}

			}

			/*
			 * if (args[0].equalsIgnoreCase("fix")) { if
			 * (!sender.hasPermission("mm.reload")) {
			 * sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
			 * return false; }
			 * 
			 * if (args.length >= 1) { for (Player nextPlayer :
			 * Bukkit.getServer().getOnlinePlayers()) {
			 * nextPlayer.setCustomNameVisible(true); } return true; }
			 * 
			 * }
			 */

			sender.sendMessage(Main.getConfigString(false, "lang.command.invalid"));
			return false;

		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("mm")) {

			List<String> list = new ArrayList<String>();
			if (args.length <= 1) {
				String start = "";
				if (args.length > 0)
					start = args[0];
				list.add("maps"); list.add("create"); list.add("delete");
				list.add("start"); list.add("shuffle"); list.add("loop");
				list.add("stop"); list.add("reload"); list.add("kill");
				list.add("revive"); list.add("role"); list.add("changerole");
				list.add("tt"); list.add("time");
				return filter(list, start);
			}

			if (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("loop")) {
				if (!sender.hasPermission("mm.start")) {
					return list;
				}

				if (args.length == 2) {
					list.addAll(Main.getPlugin().getConfig().getConfigurationSection("maps").getKeys(false));
					return filter(list, args[1]);
				}

				else if (args.length == 3) {
					autoCompletePlayerList(list, args[2]);
					return filter(list, args[2]);
				}

			}

			if (args[0].equalsIgnoreCase("shuffle")) {
				if (!sender.hasPermission("mm.start")) {
					return list;
				}

				if (args.length == 2) {
					autoCompletePlayerList(list, args[1]);
					return filter(list, args[1]);
				}

			}

			if (args[0].equalsIgnoreCase("kill")) {
				if (!sender.hasPermission("mm.kill")) {
					return list;
				}

				if (args.length == 2) {
					list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
					return filter(list, args[1]);
				}

			}

			if (args[0].equalsIgnoreCase("revive")) {
				if (!sender.hasPermission("mm.revive")) {
					return list;
				}

				if (args.length == 2) {
					list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
					return filter(list, args[1]);
				}

			}

			if (args[0].equalsIgnoreCase("role") || args[0].equalsIgnoreCase("tt")) {
				if (!sender.hasPermission("mm.role")) {
					return list;
				}

				if (args.length == 2) {
					list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
					return filter(list, args[1]);
				}

			}

			if (args[0].equalsIgnoreCase("changerole")) {
				if (!sender.hasPermission("mm.changerole")) {
					return list;
				}

				if (args.length == 2) {
					list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
					return filter(list, args[1]);
				}

				if (args.length == 3) {
					list.add("murderer");
					list.add("detective");
					if (Main.getPlugin().getConfig().getBoolean("enable-deputy"))
						list.add("deputy");
					list.add("bystander");
					return filter(list, args[2]);
				}

			}

			if (args[0].equalsIgnoreCase("time")) {
				if (!sender.hasPermission("mm.time")) {
					return list;
				}

				if (args.length == 2) {
					list.add("set");
					list.add("add");
					list.add("remove");
					return filter(list, args[1]);
				}

			}

			if (args[0].equalsIgnoreCase("delete")) {
				if (!sender.hasPermission("mm.delete")) {
					return list;
				}

				if (args.length == 2) {
					list.addAll(Main.getPlugin().getConfig().getConfigurationSection("maps").getKeys(false));
					return filter(list, args[1]);
				}

			}

			return list;

		}
		return null;
	}

	
	private static List<String> filter(List<String> list, String start) {
		if (start.equals(""))
			return list;
		List<String> filtered = new ArrayList<String>();
		for (String string : list) {
			if (string.toLowerCase().startsWith(start.toLowerCase())) {
				filtered.add(string);
			}
		}
		return filtered;
	}
	
	private static void autoCompletePlayerList(List<String> list, String arg) {
		String[] a = arg.split(",");
		if (a.length < 1) return;
		String currentName = a[a.length - 1];
		boolean endsWithComma = arg.endsWith(",");
		
		List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
		String withoutName = "";
		if (!currentName.equals("")) withoutName = endsWithComma ? arg : arg.replaceAll("[" + currentName.replaceAll("\\\\", "\\\\\\\\") + "]", "");
		boolean nameMatches = false;
		for (String name : players) {
			if (!nameMatches && currentName.equals(name)) nameMatches = true;
			if (!arg.contains(name + ",") && !arg.endsWith(name))
				list.add(withoutName + name);
		}
		
		if (!endsWithComma && nameMatches && !list.isEmpty()) list.add(arg + ",");
	}
	
	private static List<Player> parsePlayers(String[] args, int index) {
		List<Player> players = new ArrayList<Player>();
		if (args.length > index) {
			for (String name : args[index].split(",")) {
				Player p = Bukkit.getPlayerExact(name);
				if (p != null)
					players.add(p);
			}
		} else {
			List<Player> temp = new ArrayList<Player>();
			temp.addAll(Bukkit.getOnlinePlayers());
			players.addAll(temp);
		}
		return players;
	}

}
