package net.trollyloki.murdermystery.game;

import net.trollyloki.minigames.library.utils.MiniGameUtils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;

public class Map {

    private boolean active;
    private String name;
    private Location location;

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

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

}
