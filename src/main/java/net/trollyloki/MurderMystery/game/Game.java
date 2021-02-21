package net.trollyloki.murdermystery.game;

import net.trollyloki.murdermystery.MurderMysteryPlugin;
import net.trollyloki.murdermystery.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Game extends BukkitRunnable {

    private final MurderMysteryPlugin plugin;
    private final ArrayList<UUID> players;
    private final GameScoreboard scoreboard;

    /**
     * Constructs a new game including all players
     *
     * @param plugin Plugin running this game
     */
    public Game(MurderMysteryPlugin plugin) {
        this(plugin, new ArrayList<>(plugin.getServer().getOnlinePlayers()));
    }

    /**
     * Constructs a new game including the given players
     *
     * @param plugin Plugin running this game
     */
    public Game(MurderMysteryPlugin plugin, Player... players) {
        this(plugin, new ArrayList<>(Arrays.asList(players)));
    }

    /**
     * Constructs a new game including the given players
     *
     * @param plugin Plugin running this game
     */
    protected Game(MurderMysteryPlugin plugin, ArrayList<Player> players) {
        this.plugin = plugin;
        this.players = new ArrayList<>();
        for (Player player : players)
            this.players.add(player.getUniqueId());

        this.scoreboard = new GameScoreboard(this);
        plugin.getGameListener().registerGame(this);

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



    private boolean running = false;
    private Map map;
    private HashMap<UUID, Role> roles;
    private int time;

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
        return roles.getOrDefault(player.getUniqueId(), Role.DEAD);
    }

    /**
     * Gets the players in this game that are alive
     *
     * @return Set of players
     */
    public Set<UUID> getAlivePlayers() {
        return Collections.unmodifiableSet(roles.keySet());
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

        // Assign Roles
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

                player.teleport(map.getLocation());

            }
        }

        runTaskTimer(plugin, 0, 20);

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

        cancel();

        this.running = false;

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                player.sendMessage("Game stopped");

            }
        }

    }

    /**
     * Releases this game
     */
    public void release() {
        if (isRunning())
            stop();

        plugin.getGameListener().unregisterGame(this);
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                player.setScoreboard(player.getServer().getScoreboardManager().getMainScoreboard());

            }
        }

    }

    private static final String SPACER = ChatColor.GRAY + "=-=-=-=-=-=-=-=";

    @Override
    public void run() {

        String time = Utils.formatTime(this.time);
        int alivePlayers = getAlivePlayers().size();

        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                ArrayList<String> lines = new ArrayList<>();
                lines.add(SPACER);
                lines.add("");
                lines.add(ChatColor.GREEN + "Map: " + ChatColor.YELLOW + map.getName());
                lines.add("");
                lines.add(ChatColor.GREEN + "Time Left: " + ChatColor.YELLOW + time);
                lines.add("");
                String role = getRole(player).name().toLowerCase();
                lines.add(ChatColor.GREEN + "Role: " + ChatColor.YELLOW + plugin.getConfigString("scoreboard.roles." + role));
                lines.add("");
                lines.add(ChatColor.GREEN + "Alive Players: " + ChatColor.YELLOW + alivePlayers);
                lines.add("");
                lines.add(ChatColor.GREEN + "Score: " + ChatColor.YELLOW + 0);
                lines.add("");
                lines.add(SPACER);
                scoreboard.update(player, lines);

            }

        }

        this.time--;
    }

}
