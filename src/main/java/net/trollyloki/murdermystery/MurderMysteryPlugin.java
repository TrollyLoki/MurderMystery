package net.trollyloki.murdermystery;

import net.trollyloki.minigames.library.MiniGameLibraryPlugin;
import net.trollyloki.minigames.library.managers.MiniGameManager;
import net.trollyloki.minigames.library.utils.MiniGameUtils;
import net.trollyloki.murdermystery.game.Map;
import net.trollyloki.murdermystery.game.MurderMysteryGame;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MurderMysteryPlugin extends JavaPlugin {

    private MiniGameManager manager;
    private File mapsFile, databaseFile;
    private FileConfiguration maps, database;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        saveResource("maps.yml", false);
        mapsFile = new File(getDataFolder(), "maps.yml");
        maps = YamlConfiguration.loadConfiguration(mapsFile);
        saveResource("database.yml", false);
        databaseFile = new File(getDataFolder(), "database.yml");
        database = YamlConfiguration.loadConfiguration(databaseFile);

        getServer().getMessenger().registerOutgoingPluginChannel(this, MurderMysteryGame.MUTE_CHANNEL);

        Plugin miniGameLibrary = getServer().getPluginManager().getPlugin("MiniGameLibrary");
        if (miniGameLibrary == null)
            throw new IllegalStateException("MiniGameLibrary not found");
        this.manager = ((MiniGameLibraryPlugin) miniGameLibrary).getMiniGameManager();

        //noinspection ConstantConditions
        getCommand("murdermystery").setExecutor(new MurderMysteryCommand(this));

    }

    @Override
    public void onDisable() {

        saveDatabase();
        saveMaps();

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

    public Set<String> listMaps() {
        return maps.getKeys(false);
    }

    /**
     * Loads a map from a configuration section
     *
     * @return Map
     */
    public Map loadMap(String map) {
        if (maps.isConfigurationSection(map)) {
            ConfigurationSection config = maps.getConfigurationSection(map);
            return new Map(config.getBoolean("active"),
                    config.getString("name"), MiniGameUtils.loadLocation(config.getConfigurationSection("location")));
        }
        return null;
    }

    /**
     * Loads a random map
     *
     * @return Random map
     */
    public Map loadRandomMap() {
        ArrayList<String> maps = new ArrayList<>(this.maps.getKeys(false));

        Map map;
        do {
            String mapKey = maps.get((int) (Math.random() * maps.size()));
            map = loadMap(mapKey);
        } while (!map.isActive());

        return map;
    }

    public void saveMap(String key, Map map) {
        ConfigurationSection config = maps.createSection(key);
        config.set("active", map.isActive());
        config.set("name", map.getName());
        ConfigurationSection locationConfig = config.createSection("location");
        Location location = map.getLocation();
        locationConfig.set("world", location.getWorld().getName());
        locationConfig.set("x", location.getX());
        locationConfig.set("y", location.getY());
        locationConfig.set("z", location.getZ());
        locationConfig.set("yaw", location.getYaw());
        locationConfig.set("pitch", location.getPitch());
    }

    public void removeMap(String key) {
        maps.set(key, null);
    }

    public boolean saveMaps() {
        try {
            maps.save(mapsFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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
