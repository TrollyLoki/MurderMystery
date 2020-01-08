package net.trollyloki.MurderMystery.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import net.trollyloki.MurderMystery.Main;
import net.trollyloki.MurderMystery.game.Maps;
import net.trollyloki.MurderMystery.game.Run;
import net.trollyloki.MurderMystery.game.Setup;
import net.trollyloki.MurderMystery.game.Timer;

public class CommandMM implements CommandExecutor {
	
	@EventHandler
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {   
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
        			if (Run.autoRestart) {
        				sender.sendMessage(Main.getConfigString(true, "lang.command.start.shuffle-enabled"));
        				return false;
        			}
        			
        			List<Player> players = new ArrayList<Player>();
        			if (args.length > 2) {
        				for (String name : args[2].split(",")) {
        					Player p = Bukkit.getPlayerExact(name);
        					if (p != null) players.add(p);
        				}
        			}
        			else {
        				players.addAll(Bukkit.getOnlinePlayers());
        			}
        			
        			String result = Setup.startGame(args[1].toLowerCase(), players);
        			
        			String name = args[1].toLowerCase();
        			if (result != "invalid-map") name = Main.getConfigString(false, "maps." + args[1].toLowerCase() + ".name");
        			
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
        		
        		List<Player> players = new ArrayList<Player>();
    			if (args.length > 1) {
    				for (String name : args[1].split(",")) {
    					Player p = Bukkit.getPlayerExact(name);
    					if (p != null) players.add(p);
    				}
    			}
    			else {
    				players.addAll(Bukkit.getOnlinePlayers());
    			}
        		
        		Run.autoRestart = true;
        		String[] result = Setup.startRandom(players);
        		
        		String name = null;
        		if (result[1] != "invalid-map") name = Main.getConfigString(false, "maps." + result[0].toLowerCase() + ".name");
    			
    			sender.sendMessage(Main.getConfigString(true, "lang.command.start." + result[1]).replaceAll("%map%", name));
    			if (result[1].equals("success")) {
    				Run.autoRestart = true;
    				sender.sendMessage(Main.getConfigString(true, "lang.command.shuffle.enabled"));
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
        			}
        			else result = Main.getConfigString(true, "lang.command.not-running");
        			
        			sender.sendMessage(result);
        			if (Run.autoRestart) {
        				Run.autoRestart = false;
        				sender.sendMessage(Main.getConfigString(true, "lang.command.shuffle.disabled"));
        			}
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
					sender.sendMessage(Main.getConfigString(true, "lang.command." + result)
							.replaceAll("%player%", args[1]));
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
					sender.sendMessage(Main.getConfigString(true, "lang.command." + result)
							.replaceAll("%player%", args[1]));
	        		return (result == "kill.success");
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
        				if (player == Run.murderer) role = Main.getConfigString(false, "titles.murderer.role");
        				else if (player == Run.detective) role = Main.getConfigString(false, "titles.detective.role");
        				else if (player == Run.deputy) role = Main.getConfigString(false, "titles.deputy.role");
        				else if (Run.bystanders.contains(player)) role = Main.getConfigString(false, "titles.bystander.role");
        				else role = Main.getConfigString(false, "titles.dead.role");
        				
        				String message = Main.getConfigString(true, "lang.command.role.success")
        						.replaceAll("%player%", player.getName())
        						.replaceAll("%role%", role);
        				sender.sendMessage(message);
        				return true;
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
        			sender.sendMessage(Main.getConfigString(true, "lang.command." + result)
        					.replaceAll("%time%", String.valueOf(Timer.convertToTime(Timer.getSecondsLeft()))));
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
        			if (args.length > 2) name = args[2];
        			
        			Player player = (Player) sender;
        			boolean success = Maps.createMap(args[1], name, player.getLocation());
        			if (success) {
        				sender.sendMessage(Main.getConfigString(true, "lang.command.create.success").replaceAll("%map%", args[1]));
        				return true;
        			}
        			else {
        				sender.sendMessage(Main.getConfigString(true, "lang.command.create.already-exists").replaceAll("%map%", args[1]));
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
        				sender.sendMessage(Main.getConfigString(true, "lang.command.delete.success").replaceAll("%map%", args[1]));
        				return true;
        			}
        			else {
        				sender.sendMessage(Main.getConfigString(true, "lang.command.delete.doesnt-exist").replaceAll("%map%", args[1]));
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
        	
        	/*if (args[0].equalsIgnoreCase("fix")) {
        		if (!sender.hasPermission("mm.reload")) {
        			sender.sendMessage(Main.getConfigString(false, "lang.command.no-perm"));
        			return false;
        		}
        		
        		if (args.length >= 1) {
        			for (Player nextPlayer : Bukkit.getServer().getOnlinePlayers()) {
        				nextPlayer.setCustomNameVisible(true);
        			}
        			return true;
        		}
        		
        	}*/
        	
        	sender.sendMessage(Main.getConfigString(false, "lang.command.invalid"));
        	return false;
        	
        }
		return false;   
    }

}
