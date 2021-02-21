package net.trollyloki.murdermystery;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class MurderMysteryPlugin extends JavaPlugin {

    private GameListener gameListener;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        gameListener = new GameListener(this);

        getCommand("murdermystery").setExecutor(new MurderMysteryCommand(this));

    }

    public GameListener getGameListener() {
        return gameListener;
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
