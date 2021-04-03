package net.trollyloki.murdermystery.game;

import net.trollyloki.minigames.library.managers.Game;
import net.trollyloki.minigames.library.managers.MiniGameManager;
import net.trollyloki.minigames.library.managers.Party;
import net.trollyloki.minigames.library.utils.MiniGameUtils;
import net.trollyloki.murdermystery.MurderMysteryPlugin;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.*;

public class MurderMysteryGame extends Game {

    public static final String MUTE_CHANNEL = "murdermystery:mute";

    private final MurderMysteryPlugin plugin;
    private boolean running = false;
    private Map map = null;
    private HashMap<UUID, Role> roles = null;
    private int time = 0, graceTime = 0, potatoTime = 0;
    private String formattedTime = "0:00";
    // Boolean that decides whether hotpotato mode is on or off
    private boolean hotPotatoMode = false;
    private ItemStack sword, bow, potato;
    // Stores the UUID of the player that the potato will kill eventually - this will change several times!
    private UUID potatoVictim = null;
    private ArmorStand droppedBow;
    private boolean potatoCooldown = false;
    private HashMap<UUID, Integer> arrowTimes;

    /**
     * Constructs a new murder mystery game
     *
     * @param manager Mini game manager
     * @param plugin Plugin running this game
     */
    public MurderMysteryGame(MiniGameManager manager, MurderMysteryPlugin plugin) {
        super(manager);
        this.plugin = plugin;
    }

    /**
     * Gets the plugin running this game
     *
     * @return Plugin
     */
    public MurderMysteryPlugin getPlugin() {
        return plugin;
    }

    @Override
    public int addAll(Party party) {
        Set<Player> players = party.getOnlinePlayers();
        String title = plugin.getConfigString("scoreboard.title");
        for (Player player : players) {
            add(player.getUniqueId());
            getScoreboard().add(player.getUniqueId(), player.getName());
            getScoreboard().getPlayerScoreboard(player.getUniqueId()).setTitle(title);
        }
        return players.size();
    }

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
     * Sets the role of the given player to the given role
     *
     * @param player Player
     * @param role Role
     * @return Previous role of the player
     */
    public Role setRole(Player player, Role role) {
        if (roles == null)
            return null;

        Role previous;
        if (role != Role.DEAD) {
            previous = roles.put(player.getUniqueId(), role);
            player.addScoreboardTag(role.name().toLowerCase());
        } else
            previous = roles.remove(player.getUniqueId());

        if (previous != null)
            player.removeScoreboardTag(previous.name().toLowerCase());
        return previous;
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

        Set<Player> players = getOnlinePlayers();
        if (players.size() < 2)
            throw new IllegalStateException("Less than 2 players are online");

        this.running = true;
        this.map = map;
        this.time = plugin.getConfig().getInt("time.total");
        this.graceTime = plugin.getConfig().getInt("time.grace");
        this.arrowTimes = new HashMap<>();
        // Stores the time until the potato kills its host. Acts like the grace period timer.
        this.potatoTime = plugin.getConfig().getInt("time.potato");
        // Be sure to reset the hotpotato value between games!
        this.hotPotatoMode = false;
        // This is probably a bad way to do randomness, but I'm a Valve developer so who cares
        if (Math.random() < plugin.getConfig().getInt("chance.hotpotato") / 100.0) {
            hotPotatoMode = true;
        }
        // Assign Roles
        this.roles = new HashMap<>();
        ArrayList<UUID> options = new ArrayList<>(getPlayers());
        options.removeIf(uuid -> plugin.getServer().getPlayer(uuid) == null);
        if (hotPotatoMode) {
            this.potatoVictim = MiniGameUtils.getRandomElement(options);
        }
        UUID murderer = MiniGameUtils.removeRandomElement(options);
        setRole(Bukkit.getPlayer(murderer), Role.MURDERER);
        UUID detective = MiniGameUtils.removeRandomElement(options);
        setRole(Bukkit.getPlayer(detective), Role.DETECTIVE);
        if (!options.isEmpty()) {
            UUID underdog = MiniGameUtils.removeRandomElement(options);
            setRole(Bukkit.getPlayer(underdog), Role.UNDERDOG);
        }
        for (UUID bystander : options)
            setRole(Bukkit.getPlayer(bystander), Role.BYSTANDER);

        for (Player player : players) {

            // Initial setup
            player.teleport(map.getLocation());
            player.setLevel(0);
            player.getInventory().clear();
            MiniGameUtils.clearPotionEffects(player);
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
                // meta.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
                bow.setItemMeta(meta);
                player.getInventory().setItem(slot, bow);

            } else if (role == Role.UNDERDOG) {

                player.getInventory().setItem(slot, new ItemStack(Material.SNOWBALL));

            }
            // The Hot Potato isn't really a role. It's just a random item.
            if (hotPotatoMode && player.getUniqueId().equals(potatoVictim)) {

                potato = new ItemStack(Material.BAKED_POTATO);
                ItemMeta meta = potato.getItemMeta();
                meta.setDisplayName(plugin.getConfigString("items.potato.potato_name"));
                meta.setLore(Arrays.asList(plugin.getConfigString("items.potato.potato_lore")));
                potato.setItemMeta(meta);
                // Just using addItem.. not setItem, sorry
                player.getInventory().addItem(potato);

            }
            player.getInventory().setItem(9, new ItemStack(Material.ARROW));

            // Show titles
            player.sendTitle(convertPlaceholders(plugin.getConfigString("titles.start.title"), player),
                    convertPlaceholders(plugin.getConfigString("titles.start.subtitle"), player),
                    5, 100, 10);

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
        this.droppedBow = null;
        this.arrowTimes = null;
        this.running = false;

        for (Player player : getOnlinePlayers()) {
            mute(player, false);
            player.setLevel(0);
            player.getInventory().clear();
            player.removePotionEffect(PotionEffectType.SATURATION);
            player.setGlowing(false);
            setRole(player, Role.DEAD);
        }

        this.roles = null;
        close();

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
        int score = -1;

        return string
                .replaceAll("%map%", map != null ? map.getName() : "None")
                .replaceAll("%role%", role != null ?
                        plugin.getConfigString("roles." + role.name().toLowerCase() + ".name") : "None")
                .replaceAll("%goal%", role != null ?
                        plugin.getConfigString("roles." + role.name().toLowerCase() + ".goal") : "None")
                .replaceAll("%alive%", String.valueOf(alive))
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
            return null;

        mute(player, true);

        Role role = setRole(player, Role.DEAD);
        if (role != null) {

            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();

            for (Player p : getOnlinePlayers())
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1, 1);

            if (role == Role.MURDERER)
                end(EndReason.MURDERER_KILLED);
            else if (role == Role.DETECTIVE)
                dropBow(player.getLocation());

            if (player.getUniqueId().equals(potatoVictim)) {
                potatoVictim = null;
            }

            if (getAlivePlayerCount() <= 1)
                end(EndReason.ALL_KILLED);

        }

        return role;
    }

    /**
     * Attempts to change the mute status of the given player
     *
     * @param player Player
     * @param mute {@code true} to mute, or {@code false} to unmute
     */
    public void mute(Player player, boolean mute) {
        byte status = (byte) (mute ? 1 : 0);
        player.sendPluginMessage(plugin, MUTE_CHANNEL, new byte[]{status});
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
        droppedBow.setInvulnerable(true);
        droppedBow.setRightArmPose(new EulerAngle(-1.48353, 0, -0.174533));
        droppedBow.getEquipment().setItemInMainHand(bow, true);

        for (Player player : getOnlinePlayers())
            player.sendMessage(plugin.getConfigString("items.detective.bow_dropped"));

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

        setRole(player, Role.DETECTIVE);
        player.getInventory().setItem(1, bow);

        for (Player p : getOnlinePlayers())
            p.sendMessage(plugin.getConfigString("items.detective.bow_pickedup"));

    }

    /**
     * Ends this game
     *
     * @param reason Reason
     * @throws IllegalStateException If this game is not running
     */
    public void end(EndReason reason) {

        for (Player player : getOnlinePlayers()) {
            // Show titles
            player.sendTitle(plugin.getConfigString("titles.end.title"),
                    plugin.getConfigString("titles.end.subtitles." + reason.name().toLowerCase()),
                    5, 100, 10);

        }


        stop();

    }

    private int tick = 0;

    @Override
    public void run() {

        Set<Player> players = getOnlinePlayers();

        // runs each second
        if (tick % 20 == 0) {

            this.formattedTime = MiniGameUtils.formatTime(this.time);
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
            
            String potatoMessage = null;
            // Almost forgot to check if potatomode was on! If I hadn't caught that we'd be killing a null object!
            // Added setting potatoVictim to null if the player died in the kill method
            if (isRunning() && hotPotatoMode && this.potatoTime >= 0
                    && this.potatoTime <= plugin.getConfig().getInt("time.potato_countdown")
                    && this.potatoVictim != null) {
                Player victim = Bukkit.getPlayer(potatoVictim);
                if (this.potatoTime == 0) {
                    potatoMessage = String.format(plugin.getConfigString("time.potato_burned"), victim.getName());
                    // bam, fireworks
                    Firework firework = victim.getWorld().spawn(victim.getLocation(), Firework.class);
                    FireworkMeta fireMeta = firework.getFireworkMeta();
                    fireMeta.addEffect(FireworkEffect.builder().withColor(Color.RED).flicker(true).build());
                    firework.setFireworkMeta(fireMeta);
                    firework.detonate();
                    kill(victim);
                } else {
                    potatoMessage = String.format(plugin.getConfigString("time.potato_warning"), this.potatoTime);
                }
            }

            boolean glow = time == plugin.getConfig().getInt("time.glow");

            for (Player player : players) {

                getScoreboard().getPlayerScoreboard(player.getUniqueId())
                        .setLines(convertPlaceholders(lines, player));

                if (graceMessage != null)
                    player.sendMessage(graceMessage);
                if (potatoMessage != null)
                    player.sendMessage(potatoMessage);

                if (glow) {
                    Role role = getRole(player);
                    if (role != Role.DEAD && role != Role.MURDERER)
                        player.setGlowing(true);
                }

            }

            if (isRunning()) {
                if (this.graceTime >= 0)
                    this.graceTime--;

                if (this.potatoTime >= 0)
                    this.potatoTime--;

                if (this.time <= 0)
                    end(EndReason.TIME_EXPIRED);
                else
                    this.time--;
            }

        }

        // runs every tick
        if (isRunning()) {
            for (Player player : players) {

                Integer arrowTime = arrowTimes.get(player.getUniqueId());
                if (arrowTime != null) {

                    int delta = arrowTime - tick;
                    if (delta <= 0) {
                        player.setExp(0f);
                        arrowTimes.remove(player.getUniqueId());
                        player.getInventory().setItem(9, new ItemStack(Material.ARROW));
                    }
                    player.setExp((float) delta / (20 * plugin.getConfig().getInt("time.bow-cooldown")));

                }

            }
        }

        tick++;

    }

    @Override
    public void onPlayerDamage(EntityDamageEvent event) {
        event.setCancelled(true);
        if (!isRunning())
            return;

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                && event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            kill((Player) event.getEntity());
    }

    @Override
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
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
                // Handling for if none of the above items were used to damage
                else if (player.getInventory().getItemInMainHand().getType() == Material.BAKED_POTATO) {
                	if (!potatoCooldown) {
                		player.getInventory().remove(Material.BAKED_POTATO);
                		((Player) event.getEntity()).getInventory().addItem(potato);
                		// haha funny sound
                		event.getEntity().getWorld().playSound(event.getEntity().getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);
                		// new potato victim
                		potatoVictim = event.getEntity().getUniqueId();
                		// particles!!!!
                		event.getEntity().getWorld().spawnParticle(Particle.VILLAGER_ANGRY, event.getEntity().getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3);
                		// set potato cooldown and set bukkitrunnable to remove it in a second
                		potatoCooldown = true;
                		new BukkitRunnable() {
							@Override
							public void run() {
								potatoCooldown = false;
								
							}
                		}.runTaskLater(plugin, 20);
                	}
                }

            }

        }

    }

    @Override
    public void onPlayerShootBow(EntityShootBowEvent event) {
        if (!isRunning())
            return;

        int arrowTime = tick + 20 * plugin.getConfig().getInt("time.bow-cooldown");
        arrowTimes.put(event.getEntity().getUniqueId(), arrowTime);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isRunning())
            return;

        Role role = getRole(event.getPlayer());
        if (droppedBow != null && (role == Role.BYSTANDER
                || (role == Role.UNDERDOG && !event.getPlayer().getInventory().contains(Material.SNOWBALL)))
                && event.getPlayer().getLocation().distanceSquared(droppedBow.getLocation()) <= 2.25)
            pickupBow(event.getPlayer());
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isRunning())
            kill(event.getPlayer());

        int online = 0;
        for (UUID player : getPlayers()) {
            if (!player.equals(event.getPlayer().getUniqueId()) && plugin.getServer().getPlayer(player) != null)
                online++;
        }
        if (online < 2) {
            plugin.getLogger().warning("This game is being released because less than 2 players are still online");
            close();
        }

    }

}
