package net.trollyloki.murdermystery.game;

import net.trollyloki.murdermystery.Utils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class Map {

    private final String name;
    private final Location location;

    /**
     * Constructs a new map
     *
     * @param name Name
     * @param location Starting location
     */
    public Map(String name, Location location) {
        this.name = name;
        this.location = location;
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
        return new Map(config.getString("name"), Utils.loadLocation(config.getConfigurationSection("location")));
    }

}
