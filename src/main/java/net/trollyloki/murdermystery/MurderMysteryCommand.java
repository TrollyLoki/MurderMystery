package net.trollyloki.murdermystery;

import net.trollyloki.minigames.library.managers.Game;
import net.trollyloki.minigames.library.managers.Party;
import net.trollyloki.murdermystery.game.Map;
import net.trollyloki.murdermystery.game.MurderMysteryGame;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MurderMysteryCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERM = "murdermystery.admin";

    private final MurderMysteryPlugin plugin;

    public MurderMysteryCommand(MurderMysteryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission(ADMIN_PERM)) {

                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "The configuration has been reloaded");
                return true;

            }

            else if (args[0].equalsIgnoreCase("maps") && sender.hasPermission(ADMIN_PERM)) {

                if (args.length > 1) {

                    if (args[1].equalsIgnoreCase("list")) {

                        Set<String> maps = plugin.listMaps();
                        sender.sendMessage(ChatColor.GREEN + "Current Maps: (" + maps.size() + ")");
                        for (String key : maps) {
                            Map map = plugin.loadMap(key);
                            String string = " - " + map.getName() + " (" + key + ") - ";
                            string += map.isActive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Not active";
                            sender.sendMessage(string);
                        }
                        return true;

                    }

                    if (args[1].equalsIgnoreCase("add")) {

                        if (!(sender instanceof Player)) {
                            sender.sendMessage(ChatColor.RED + "Only players can use this command");
                            return false;
                        }

                        if (args.length > 3) {

                            args[2] = args[2].toLowerCase();
                            if (plugin.loadMap(args[2]) != null) {
                                sender.sendMessage(ChatColor.RED + "A map with that key already exists");
                                return false;
                            }

                            String displayName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                            Location location = ((Player) sender).getLocation();
                            location.setX(location.getBlockX() + 0.5);
                            location.setY(location.getBlockY());
                            location.setZ(location.getBlockZ() + 0.5);
                            location.setYaw(90 * (Math.round(location.getYaw() / 90)));
                            location.setPitch(90 * (Math.round(location.getPitch() / 90)));

                            Map map = new Map(false, displayName, location);
                            plugin.saveMap(args[2], map);
                            sender.sendMessage(ChatColor.GREEN + map.getName() + " created" +
                                    ". Use \"/" + label + " maps setactive " + args[2] + " true\" to put it in rotation.");
                            return true;

                        }

                        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " maps add <map> <displayname>");
                        return false;

                    } else if (args[1].equalsIgnoreCase("remove")) {

                        if (args.length > 2) {

                            args[2] = args[2].toLowerCase();
                            Map map = plugin.loadMap(args[2]);
                            if (map == null) {
                                sender.sendMessage(ChatColor.RED + "Unknown map '" + args[2] + "'");
                                return false;
                            }

                            plugin.removeMap(args[2]);
                            sender.sendMessage(ChatColor.GREEN + "Removed " + map.getName());
                            return true;

                        }

                        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " maps remove <map>");
                        return false;

                    } else if (args[1].equalsIgnoreCase("setactive")) {

                        if (args.length > 3) {

                            Boolean active = null;
                            if (args[3].equalsIgnoreCase("true"))
                                active = true;
                            else if (args[3].equalsIgnoreCase("false"))
                                active = false;

                            if (active != null) {

                                args[2] = args[2].toLowerCase();
                                Map map = plugin.loadMap(args[2]);
                                if (map == null) {
                                    sender.sendMessage(ChatColor.RED + "Unknown map '" + args[2] + "'");
                                    return false;
                                }

                                map.setActive(active);
                                plugin.saveMap(args[2], map);
                                sender.sendMessage(ChatColor.GREEN + map.getName() + " is "
                                        + (active ? "now active" : "no longer active"));
                                return true;

                            }

                        }

                        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " maps setactive <map> <true|false>");
                        return false;

                    }

                }

                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " maps <list|add|remove|setactive>");
                return false;

            }

            else if (args[0].equalsIgnoreCase("start")) {

                Party party = getModeratingParty(sender);
                if (party == null)
                    return false;

                ConfigurationSection mapConfig = plugin.getConfig().getConfigurationSection("maps");
                Map map;
                if (args.length > 1) {
                    map = plugin.loadMap(args[1]);
                    if (map == null) {
                        sender.sendMessage(ChatColor.RED + "Unknown map '" + args[1] + "'");
                        return false;
                    }
                } else {
                    map = plugin.loadRandomMap();
                }

                int potatoChance = plugin.getConfig().getInt("chance.hotpotato");
                int invisChance = plugin.getConfig().getInt("chance.invis");
                int speedChance = plugin.getConfig().getInt("chance.speed");
                for (int i = 2; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("potato"))
                        potatoChance = 100;
                    else if (args[i].equalsIgnoreCase("invis"))
                        invisChance = 100;
                    else if (args[i].equalsIgnoreCase("speed"))
                        speedChance = 100;
                }

                sender.sendMessage(ChatColor.GREEN + "Starting game on " + map.getName());
                MurderMysteryGame game = new MurderMysteryGame(plugin.getManager(), plugin);
                try {
                    game.addAll(party);
                    game.start(map, potatoChance, invisChance, speedChance);
                } catch (IllegalStateException e) {
                    sender.sendMessage(ChatColor.RED + "Failed to start game: " + e.getMessage());
                    game.close();
                    return false;
                }
                return true;

            }

            else if (args[0].equalsIgnoreCase("stop")) {

                MurderMysteryGame game = getMurderMysteryGame(sender);
                if (game == null)
                    return false;

                if (!game.isRunning()) {
                    sender.sendMessage(ChatColor.RED + "Your game has not been started");
                    return false;
                }

                game.stop();
                sender.sendMessage(ChatColor.GREEN + "Stopped game");
                return true;

            }

        }

        String help = "start|stop";
        if (sender.hasPermission(ADMIN_PERM))
            help += "|reload|maps";
        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <" + help + ">");
        return false;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    public Party getModeratingParty(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command");
            return null;
        }

        Party party = plugin.getManager().getParty(player.getUniqueId());
        if (party == null || !party.isModerator(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You must be a party moderator to start a murder mystery game");
            return null;
        }

        return party;

    }

    public MurderMysteryGame getMurderMysteryGame(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command");
            return null;
        }

        Game game = plugin.getManager().getGame(player.getUniqueId());
        if (!(game instanceof MurderMysteryGame)) {
            sender.sendMessage(ChatColor.RED + "You are not in a murder mystery game");
            return null;
        }

        return (MurderMysteryGame) game;

    }

}
