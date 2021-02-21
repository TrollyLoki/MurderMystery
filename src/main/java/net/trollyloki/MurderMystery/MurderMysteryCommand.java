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

import java.util.List;

public class MurderMysteryCommand implements CommandExecutor, TabCompleter {

    private final MurderMysteryPlugin plugin;

    public MurderMysteryCommand(MurderMysteryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("start")) {

                if (args.length > 1) {

                    ConfigurationSection config = plugin.getConfig().getConfigurationSection("maps." + args[1]);
                    if (config == null) {
                        sender.sendMessage(ChatColor.RED + "Unknown map '" + args[1] + "'");
                        return false;
                    }
                    Map map = Map.load(config);

                    try {
                        Game game = new Game(plugin);
                        sender.sendMessage(ChatColor.GREEN + "Starting game on " + map.getName() + "...");
                        game.start(map);
                        return true;
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(ChatColor.RED + e.getMessage());
                        return false;
                    }

                }

                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " start <map>");
                return false;

            }

            else if (args[0].equalsIgnoreCase("stop")) {

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command");
                    return false;
                }

                Game game = plugin.getGameListener().getGame((Player) sender);
                if (game == null) {
                    sender.sendMessage(ChatColor.RED + "You are not in a game");
                    return false;
                }
                game.release();
                sender.sendMessage(ChatColor.GREEN + "Stopped game");
                return true;

            }

            else if (args[0].equalsIgnoreCase("reload")) {

                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "The configuration has been reloaded");
                return true;

            }

        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <start|stop|reload>");
        return false;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

}
