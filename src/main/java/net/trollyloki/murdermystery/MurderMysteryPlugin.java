package net.trollyloki.murdermystery;

import net.trollyloki.minigames.library.MiniGameLibraryPlugin;
import net.trollyloki.minigames.library.managers.MiniGameManager;
import net.trollyloki.murdermystery.game.MurderMysteryGame;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MurderMysteryPlugin extends JavaPlugin {

    private MiniGameManager manager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        getServer().getMessenger().registerOutgoingPluginChannel(this, MurderMysteryGame.MUTE_CHANNEL);

        Plugin miniGameLibrary = getServer().getPluginManager().getPlugin("MiniGameLibrary");
        this.manager = ((MiniGameLibraryPlugin) miniGameLibrary).getMiniGameManager();

        getCommand("murdermystery").setExecutor(new MurderMysteryCommand(this));

    }

    public MiniGameManager getManager() {
        return manager;
    }

    /**
     * Gets a string from the config, converting formatting codes
     *
     * @param path Path of the string
     */
    public String getConfigString(String path) {
        String string = getConfig().getString(path);
        if (string == null)
            return null;
        return ChatColor.translateAlternateColorCodes('&', string);
    }

}
