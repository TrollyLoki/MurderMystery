package net.trollyloki.murdermystery;

import net.trollyloki.minigames.library.MiniGameLibraryPlugin;
import net.trollyloki.minigames.library.managers.MiniGameManager;
import net.trollyloki.murdermystery.game.MurderMysteryGame;
import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class MurderMysteryPlugin extends JavaPlugin {

    private MiniGameManager manager;
    private File databaseFile;
    private FileConfiguration database;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        saveResource("database.yml", false);
        databaseFile = new File(getDataFolder(), "database.yml");
        database = YamlConfiguration.loadConfiguration(databaseFile);

        getServer().getMessenger().registerOutgoingPluginChannel(this, MurderMysteryGame.MUTE_CHANNEL);

        Plugin miniGameLibrary = getServer().getPluginManager().getPlugin("MiniGameLibrary");
        this.manager = ((MiniGameLibraryPlugin) miniGameLibrary).getMiniGameManager();

        getCommand("murdermystery").setExecutor(new MurderMysteryCommand(this));

    }

    @Override
    public void onDisable() {

        saveDatabase();

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

    /**
     * Saves the database to disk
     *
     * @return {@code true} if the database was saved
     */
    public boolean saveDatabase() {
        try {
            database.save(databaseFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the score of a player from the database
     *
     * @param player Player
     * @return Score
     */
    public int getScore(UUID player) {
        return database.getInt("scores." + player.toString());
    }

    /**
     * Sets the score of a player in the database
     *
     * @param player Player
     * @param score Score
     */
    public void setScore(UUID player, int score) {
        database.set("scores." + player.toString(), score);
    }

}
