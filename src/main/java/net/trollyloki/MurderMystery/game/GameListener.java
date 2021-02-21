package net.trollyloki.murdermystery.game;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;

public class GameListener implements Listener {

    private final HashMap<UUID, Game> games;

    public GameListener(Plugin plugin) {
        this.games = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Registers a game
     *
     * @param game Game
     */
    public void registerGame(Game game) {
        for (UUID uuid : game.getPlayers())
            games.put(uuid, game);
    }

    /**
     * Unregisters a game
     *
     * @param game Game
     */
    public void unregisterGame(Game game) {
        games.values().removeIf(g -> g == game);
    }

    /**
     * Gets the game that the given player is in
     *
     * @param player Player
     * @return Game
     */
    public Game getGame(Player player) {
        return games.get(player.getUniqueId());
    }

}
