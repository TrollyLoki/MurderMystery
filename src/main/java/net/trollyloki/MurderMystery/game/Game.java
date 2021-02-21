package net.trollyloki.murdermystery.game;

import net.trollyloki.murdermystery.MurderMysteryPlugin;
import net.trollyloki.murdermystery.Utils;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.*;

public class Game extends BukkitRunnable {

    private final MurderMysteryPlugin plugin;
    private final ArrayList<UUID> players;
    private final HashMap<UUID, Integer> scores;
    private final GameScoreboard scoreboard;

    /**
     * Constructs a new game including the given players
     *
     * @param plugin Plugin running this game
     * @throws IllegalArgumentException If less than 2 players are provided
     */
    public Game(MurderMysteryPlugin plugin, ArrayList<Player> players) {
        if (players.size() < 2)
            throw new IllegalArgumentException("A game must have at least 2 players");

        this.plugin = plugin;
        this.players = new ArrayList<>();
        this.scores = new HashMap<>();
        for (Player player : players)
            this.players.add(player.getUniqueId());

        this.scoreboard = new GameScoreboard(this);
        plugin.getGameListener().registerGame(this);
        runTaskTimer(plugin, 0, 20);

    }

    /**
     * Gets the plugin running this game
     *
     * @return Plugin
     */
    public MurderMysteryPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the players in this game
     *
     * @return List of players
     */
    public List<UUID> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /**
     * Gets the score of a player in this game
     *
     * @param player Player
     * @return Score
     */
    public int getScore(UUID player) {
        return scores.getOrDefault(player, 0);
    }

    /**
     * Increments the score of a player in this game
     *
     * @param player Player
     * @param amount Amount to increase the score by
     * @return New score
     */
    public int increaseScore(UUID player, int amount) {
        int score = getScore(player) + amount;
        scores.put(player, score);
        return score;
    }

    /**
     * Releases this game
     */
    public void release() {
        if (isRunning())
            stop();

        cancel();
        plugin.getGameListener().unregisterGame(this);
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                player.setScoreboard(player.getServer().getScoreboardManager().getMainScoreboard());

            }
        }

    }



    private boolean running = false;
    private Map map = null;
    private HashMap<UUID, Role> roles = null;
    private int time = 0, graceTime = 0;
    private String formattedTime = "0:00";

    private ItemStack sword, bow;
    private ArmorStand droppedBow;

    /**
     * Checks if this game is currently running
     *
     * @return {@code true} if running, otherwise {@code false}
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Gets the map that this game is on
     *
     * @return Map
     */
    public Map getMap() {
        return map;
    }

    /**
     * Gets the role of the given player in this game
     *
     * @param player Player
     * @return Role
     */
    public Role getRole(Player player) {
        if (roles == null)
            return null;
        return roles.getOrDefault(player.getUniqueId(), Role.DEAD);
    }

    /**
     * Gets the players in this game that are alive
     *
     * @return Set of players
     */
    public Set<UUID> getAlivePlayers() {
        if (roles == null)
            return null;
        else
            return Collections.unmodifiableSet(roles.keySet());
    }

    /**
     * Gets the amount of players in this game that are alive
     *
     * @return Number of players
     */
    public int getAlivePlayerCount() {
        if (roles == null)
            return 0;
        else
            return roles.size();
    }

    /**
     * Gets the time remaining
     *
     * @return Number of seconds remaining
     */
    public int getTimeLeft() {
        return time;
    }

    /**
     * Gets the time remaining as a formatted string
     *
     * @return Formatted time
     */
    public String getFormattedTimeLeft() {
        return formattedTime;
    }

    /**
     * Checks if it is the grace period
     *
     * @return {@code true} if is the grace period, otherwise {@code false}
     */
    public boolean isGracePeriod() {
        return graceTime >= 0;
    }

    /**
     * Starts this game on the given map
     *
     * @param map Map
     * @throws IllegalStateException If this game is already running
     * @see #isRunning()
     */
    public void start(Map map) {
        if (isRunning())
            throw new IllegalStateException("Game is already running");

        this.running = true;
        this.map = map;
        this.time = plugin.getConfig().getInt("time.total");
        this.graceTime = plugin.getConfig().getInt("time.grace");

        // Assign Roles
        this.roles = new HashMap<>();
        ArrayList<UUID> options = new ArrayList<>(players);
        this.roles = new HashMap<>();
        UUID murderer = Utils.removeRandomElement(options);
        this.roles.put(murderer, Role.MURDERER);
        UUID detective = Utils.removeRandomElement(options);
        this.roles.put(detective, Role.DETECTIVE);
        for (UUID bystander : options)
            this.roles.put(bystander, Role.BYSTANDER);

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                // Initial setup
                player.teleport(map.getLocation());
                player.getInventory().clear();
                Utils.clearPotionEffects(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, time * 20, 255,
                        true, false));
                player.setGameMode(GameMode.ADVENTURE);

                Role role = getRole(player);

                // Give role items
                int slot = 1;
                if (player.getInventory().getHeldItemSlot() == slot)
                    slot++;
                if (role == Role.MURDERER) {

                    sword = new ItemStack(Material.IRON_SWORD);
                    ItemMeta meta = sword.getItemMeta();
                    meta.setDisplayName(plugin.getConfigString("items.murderer.sword_name"));
                    meta.setUnbreakable(true);
                    sword.setItemMeta(meta);
                    player.getInventory().setItem(slot, sword);

                } else if (role == Role.DETECTIVE) {

                    bow = new ItemStack(Material.BOW);
                    ItemMeta meta = bow.getItemMeta();
                    meta.setDisplayName(plugin.getConfigString("items.detective.bow_name"));
                    meta.setUnbreakable(true);
                    meta.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
                    bow.setItemMeta(meta);
                    player.getInventory().setItem(slot, bow);

                }
                player.getInventory().setItem(9, new ItemStack(Material.ARROW));

                // Show titles
                player.sendTitle(convertPlaceholders(plugin.getConfigString("titles.start.title"), player),
                        convertPlaceholders(plugin.getConfigString("titles.start.subtitle"), player),
                        5, 100, 10);

            }
        }

    }

    /**
     * Stops this game
     *
     * @throws IllegalStateException If this game is not running
     * @see #isRunning()
     */
    public void stop() {
        if (!isRunning())
            throw new IllegalStateException("Game is not running");

        if (this.droppedBow != null)
            droppedBow.remove();
        this.running = false;

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                player.getInventory().clear();
                player.removePotionEffect(PotionEffectType.SATURATION);

            }
        }

    }

    /**
     * Converts placeholders in the given string for the given player
     *
     * @param string String
     * @param player Player
     * @return Converted string
     */
    public String convertPlaceholders(String string, Player player) {
        Map map = getMap();
        Role role = getRole(player);
        int alive = getAlivePlayerCount();
        String time = getFormattedTimeLeft();
        int score = getScore(player.getUniqueId());

        return string
                .replaceAll("%map%", map != null ? map.getName() : "None")
                .replaceAll("%role%", role != null ?
                        plugin.getConfigString("roles." + role.name().toLowerCase() + ".name") : "None")
                .replaceAll("%goal%", role != null ?
                        plugin.getConfigString("roles." + role.name().toLowerCase() + ".goal") : "None")
                .replaceAll("%alive%", String.valueOf(getAlivePlayerCount()))
                .replaceAll("%time%", time)
                .replaceAll("%score%", String.valueOf(score));
    }

    /**
     * Converts placeholders in the given string list for the given player
     *
     * @param strings String list
     * @param player Player
     * @return New converted string list
     */
    public ArrayList<String> convertPlaceholders(List<String> strings, Player player) {
        ArrayList<String> list = new ArrayList<>();
        for (String string : strings)
            list.add(convertPlaceholders(string, player));
        return list;
    }

    /**
     * Kills the given player
     *
     * @param player Player
     * @return Role of the killed player, or {@code null} if they were not killed
     * @throws IllegalStateException If this game is not running
     */
    public Role kill(Player player) {
        if (!isRunning())
            throw new IllegalStateException("Game is not running");

        Role role = roles.remove(player.getUniqueId());
        if (role != null) {

            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();

            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {

                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1, 1);

                }
            }

            if (role == Role.MURDERER)
                end(EndReason.MURDERER_KILLED);
            else if (role == Role.DETECTIVE)
                dropBow(player.getLocation());

            if (getAlivePlayerCount() <= 1)
                end(EndReason.ALL_KILLED);

        }

        return role;
    }

    /**
     * Drops the bow at the given location
     *
     * @param location Location
     */
    public void dropBow(Location location) {

        if (droppedBow != null)
            droppedBow.remove();

        droppedBow = location.getWorld().spawn(location, ArmorStand.class);
        droppedBow.setInvisible(true);
        droppedBow.setRightArmPose(new EulerAngle(-1.48353, 0, -0.174533));
        droppedBow.getEquipment().setItemInMainHand(bow, true);

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                player.sendMessage(plugin.getConfigString("items.detective.bow_dropped"));

            }
        }

    }

    /**
     * Gives the dropped bow to the given player
     *
     * @param player Player
     */
    public void pickupBow(Player player) {

        if (droppedBow != null) {
            droppedBow.remove();
            droppedBow = null;
        }

        roles.put(player.getUniqueId(), Role.DETECTIVE);
        player.getInventory().setItem(1, bow);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {

                p.sendMessage(plugin.getConfigString("items.detective.bow_pickedup"));

            }
        }

    }

    /**
     * Ends this game
     *
     * @param reason Reason
     * @throws IllegalStateException If this game is not running
     */
    public void end(EndReason reason) {

        stop();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                // Show titles
                player.sendTitle(plugin.getConfigString("titles.end.title"),
                        plugin.getConfigString("titles.end.subtitles." + reason.name().toLowerCase()),
                        5, 100, 10);

            }
        }

        if (reason == EndReason.ALL_KILLED) {
            for (UUID player : roles.keySet())
                increaseScore(player, 2);
        } else {
            for (java.util.Map.Entry<UUID, Role> entry : roles.entrySet()) {
                if (entry.getValue() != Role.MURDERER)
                    increaseScore(entry.getKey(), 1);
            }
        }

    }

    @Override
    public void run() {

        this.formattedTime = Utils.formatTime(this.time);
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        ListIterator<String> iter = lines.listIterator();
        while (iter.hasNext())
            iter.set(ChatColor.translateAlternateColorCodes('&', iter.next()));

        String graceMessage = null;
        if (isRunning() && this.graceTime >= 0 && this.graceTime <= 5) {
            if (this.graceTime == 0)
                graceMessage = plugin.getConfigString("time.grace_ended");
            else
                graceMessage = String.format(plugin.getConfigString("time.grace_warning"), this.graceTime);
        }

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                scoreboard.update(player, convertPlaceholders(lines, player));

                if (graceMessage != null)
                    player.sendMessage(graceMessage);

            }
        }

        if (isRunning()) {
            if (this.graceTime >= 0)
                this.graceTime--;

            if (this.time <= 0)
                end(EndReason.TIME_EXPIRED);
            else
                this.time--;
        }

    }

    public void onEntityDamage(EntityDamageEvent event) {
        event.setCancelled(true);
        if (!isRunning())
            return;

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            kill((Player) event.getEntity());
    }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        event.setCancelled(true);
        if (!isRunning())
            return;

        if (!isGracePeriod()) {

            Entity damager = event.getDamager();
            if (damager instanceof Projectile)
                damager = (Entity) ((Projectile) damager).getShooter(); // get projectile shooter as damager

            if (damager instanceof Player) {

                Player player = (Player) damager;
                if (player.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD
                        || event.getDamager() instanceof Projectile) { // damage method is valid for kill

                    Role role = kill((Player) event.getEntity());
                    if (role != Role.MURDERER && getRole(player) != Role.MURDERER) // kill player if they killed an innocent
                        kill(player);

                }

            }

        }

    }

    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isRunning())
            return;

        if (droppedBow != null && getRole(event.getPlayer()) == Role.BYSTANDER
                && event.getPlayer().getLocation().distanceSquared(droppedBow.getLocation()) <= 1)
            pickupBow(event.getPlayer());
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!isRunning())
            return;

        kill(event.getPlayer());
        for (UUID player : players) {
            if (plugin.getServer().getPlayer(player) != null)
                return;
        }

        plugin.getLogger().warning("This game is being released because all players disconnected");
        release();
    }

}
