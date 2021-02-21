package net.trollyloki.murdermystery;

import net.trollyloki.murdermystery.game.Game;
import net.trollyloki.murdermystery.game.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MurderMysteryCommand implements CommandExecutor, TabCompleter {

    private final MurderMysteryPlugin plugin;

    public MurderMysteryCommand(MurderMysteryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("reload")) {

                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "The configuration has been reloaded");
                return true;

            }

            else if (args[0].equalsIgnoreCase("create")) {

                ArrayList<Player> players = new ArrayList<>();
                if (args.length > 1) {
                    for (int i = 1; i < args.length; i++) {
                        Player player = plugin.getServer().getPlayerExact(args[i]);
                        if (player == null) {
                            sender.sendMessage(ChatColor.RED + args[i] + " is not online");
                            return false;
                        }
                        players.add(player);
                    }
                } else {
                    for (Player player : plugin.getServer().getOnlinePlayers())
                        players.add(player);
                }

                for (Player player : players) {
                    if (plugin.getGameListener().getGame(player) != null) {
                        sender.sendMessage(ChatColor.RED + player.getName() + " is already in a game");
                        return false;
                    }
                }

                if (players.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "A game must have at least 2 players");
                    return false;
                }

                sender.sendMessage(ChatColor.GREEN + "Creating game with " + players.size() + " players");
                new Game(plugin, players);
                return true;

            }

            else if (args[0].equalsIgnoreCase("release")) {

                Game game = getGame(sender);
                if (game == null)
                    return false;

                game.release();
                sender.sendMessage(ChatColor.GREEN + "Your game has been released");
                return true;

            }

            else if (args[0].equalsIgnoreCase("start")) {

                Game game = getGame(sender);
                if (game == null)
                    return false;

                if (game.isRunning()) {
                    sender.sendMessage(ChatColor.RED + "Your game has already been started");
                    return false;
                }

                ConfigurationSection mapConfig = plugin.getConfig().getConfigurationSection("maps");
                Map map;
                if (args.length > 1) {
                    if (!mapConfig.isConfigurationSection(args[1])) {
                        sender.sendMessage(ChatColor.RED + "Unknown map '" + args[1] + "'");
                        return false;
                    }
                    map = Map.load(mapConfig.getConfigurationSection(args[1]));
                } else {
                    map = Map.loadRandom(mapConfig);
                }

                sender.sendMessage(ChatColor.GREEN + "Starting game on " + map.getName());
                game.start(map);
                return true;

            }

            else if (args[0].equalsIgnoreCase("stop")) {

                Game game = getGame(sender);
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

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <reload|create|release|start|stop>");
        return false;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    public Game getGame(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command");
            return null;
        }

        Player player = (Player) sender;
        Game game = plugin.getGameListener().getGame(player);
        if (game == null) {
            sender.sendMessage(ChatColor.RED + "You are not in a game");
            return null;
        }

        return game;

    }

}
