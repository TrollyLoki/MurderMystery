package net.trollyloki.murdermystery.game;

import net.trollyloki.murdermystery.Utils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

public class Map {

    private final boolean active;
    private final String name;
    private final Location location;

    /**
     * Constructs a new map
     *
     * @param name Name
     * @param location Starting location
     */
    public Map(boolean active, String name, Location location) {
        this.active = active;
        this.name = name;
        this.location = location;
    }

    /**
     * Gets if this map is active
     *
     * @return {@code true} if this map is active, otherwise {@code false}
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Gets the name of this map
     *
     * @return Name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the starting location of this map
     *
     * @return Starting location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Loads a map from a configuration section
     *
     * @param config Configuration section
     * @return Map
     */
    public static Map load(ConfigurationSection config) {
        return new Map(config.getBoolean("active"),
                config.getString("name"), Utils.loadLocation(config.getConfigurationSection("location")));
    }

    /**
     * Loads a random map from a configuration section of maps
     *
     * @param mapConfig Configuration section
     * @return Random map
     */
    public static Map loadRandom(ConfigurationSection mapConfig) {
        ArrayList<String> maps = new ArrayList<>(mapConfig.getKeys(false));

        Map map;
        do {
            String mapKey = maps.get((int) (Math.random() * maps.size()));
            map = load(mapConfig.getConfigurationSection(mapKey));
        } while (!map.isActive());

        return map;
    }

}
