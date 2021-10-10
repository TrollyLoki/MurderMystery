package net.trollyloki.murdermystery;

import net.trollyloki.minigames.library.managers.Game;
import net.trollyloki.minigames.library.managers.Party;
import net.trollyloki.murdermystery.game.Map;
import net.trollyloki.murdermystery.game.MurderMysteryGame;
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

            if (args[0].equalsIgnoreCase("reload")) {

                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "The configuration has been reloaded");
                return true;

            }

            else if (args[0].equalsIgnoreCase("start")) {

                Party party = getModeratingParty(sender);
                if (party == null)
                    return false;

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

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <reload|start|stop>");
        return false;

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    public Party getModeratingParty(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command");
            return null;
        }

        Player player = (Player) sender;
        Party party = plugin.getManager().getParty(player.getUniqueId());
        if (party == null || !party.isModerator(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You must be a party moderator to start a murder mystery game");
            return null;
        }

        return party;

    }

    public MurderMysteryGame getMurderMysteryGame(CommandSender sender) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command");
            return null;
        }

        Player player = (Player) sender;
        Game game = plugin.getManager().getGame(player.getUniqueId());
        if (!(game instanceof MurderMysteryGame)) {
            sender.sendMessage(ChatColor.RED + "You are not in a murder mystery game");
            return null;
        }

        return (MurderMysteryGame) game;

    }

}
