package net.trollyloki.MurderMystery;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.trollyloki.MurderMystery.commands.CommandMM;
import net.trollyloki.MurderMystery.game.Run;

public class Main extends JavaPlugin implements Listener {
	
	public static JavaPlugin plugin;
	
    @Override
    public void onEnable() {
    	plugin = this;
    	registerEvents(this, new Run(), new UpdateChecker());
    	getCommand("mm").setExecutor(new CommandMM());
    	
    	getConfig().options().copyDefaults(true);
    	saveConfig();
    	
    	UpdateChecker.run();
    
    }
    
    @Override
    public void onDisable() {
    	sendDebug("Attemping to end game");
    	Run.endGame(true);
    	
    	plugin = null;
    }
    
    public static void registerEvents(Plugin plugin, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
    
    public static JavaPlugin getPlugin() {
        return plugin;
    }
    
    public static String getConfigString(Boolean prefix, String path) {
    	if (plugin.getConfig().getString(path) != null) {
    		String string = plugin.getConfig().getString(path);
    		if (prefix) string = plugin.getConfig().getString("lang.prefix") + string;
    		string = ChatColor.translateAlternateColorCodes('&', string);
    		return string;
    	}
    	
    	else {
    		return path;	
    	}
    }
    
    public static List<String> getConfigStringList(Boolean prefix, String path) {
    	if (plugin.getConfig().getString(path) != null) {
    		List<String> rawStrings = plugin.getConfig().getStringList(path);
    		List<String> strings = new ArrayList<String>();
    		for (String nextString : rawStrings) {
    			if (prefix) nextString = plugin.getConfig().getString("lang.prefix") + nextString;
    			strings.add(ChatColor.translateAlternateColorCodes('&', nextString));
    			
    		}
    		return strings;
    	}
    	
    	else {
    		List<String> paths = new ArrayList<String>();
    		paths.add(path);
    		return paths;
    	}
    }
    
    public static void sendDebug(String message) {
    	message = ChatColor.translateAlternateColorCodes('&', message);
    	if (plugin.getConfig().getBoolean("debug")) plugin.getLogger().info(message);
    }
    
    public static void colorConsole(String message) {
    	ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
    	console.sendMessage(message);
    }
    
    public static void broadcastMessages(String... messages) {
    	for (String message : messages) {
    		colorConsole(message);
    		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
    			player.sendMessage(message);
    		}
    	}
    }
}
