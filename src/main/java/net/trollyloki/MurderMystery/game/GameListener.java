package net.trollyloki.murdermystery.game;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

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

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (getGame(event.getPlayer()) != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
        if (getGame(event.getPlayer()) != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        ProjectileSource source = event.getEntity().getShooter();
        if (source instanceof Player) {
            if (getGame((Player) source) != null)
                event.getEntity().remove();
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Game game = getGame((Player) event.getEntity());
            if (game != null)
                game.onEntityDamage(event);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Game game = getGame((Player) event.getEntity());
            if (game != null)
                game.onEntityDamageByEntity(event);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Game game = getGame(event.getPlayer());
        if (game != null)
            game.onPlayerMove(event);
    }

}
