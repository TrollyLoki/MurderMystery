package net.trollyloki.murdermystery.game;

import net.trollyloki.murdermystery.MurderMysteryPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.Map;

public class GameScoreboard {

    private static final String NAME = "murder_mystery";

    private final Map<UUID, Scoreboard> scoreboards;
    private final Map<UUID, List<String>> oldLines;

    public GameScoreboard(Game game) {
        MurderMysteryPlugin plugin = game.getPlugin();
        this.scoreboards = new HashMap<>();
        this.oldLines = new HashMap<>();

        List<UUID> players = game.getPlayers();
        for (UUID uuid : players) {
            Scoreboard scoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();
            scoreboards.put(uuid, scoreboard);

            Objective objective = scoreboard.registerNewObjective(NAME, "dummy",
                    plugin.getConfigString("scoreboard.title"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            Team team = scoreboard.registerNewTeam(NAME);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            for (UUID u : players) {
                String name = plugin.getServer().getOfflinePlayer(u).getName();
                if (name != null)
                    team.addEntry(name);
            }
        }
    }

    /**
     * Updates the scoreboard for the given player to the given lines
     *
     * @param player Player
     * @param lines New lines
     */
    public void update(Player player, ArrayList<String> lines) {
        player.setScoreboard(scoreboards.get(player.getUniqueId()));

        HashMap<String, Integer> add = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (add.containsKey(line)) {
                while (add.containsKey(line))
                    line += ChatColor.RESET;
                lines.set(i, line);
            }
            add.put(line, lines.size() - i);
        }

        LinkedList<String> remove = new LinkedList<>();
        List<String> oldLines = this.oldLines.get(player.getUniqueId());
        if (oldLines != null) {
            for (int i = 0; i < oldLines.size(); i++) {
                String line = oldLines.get(i);
                Integer newIndex = add.get(line);
                if (newIndex == null) // line no longer exists so remove it
                    remove.add(line);
                else if (newIndex == oldLines.size() - i) // line already exists so don't add it
                    add.remove(line);
            }
        }

        this.oldLines.put(player.getUniqueId(), lines); // update list of old lines
        Objective objective = player.getScoreboard().getObjective(NAME);
        for (String line : remove)
            objective.getScoreboard().resetScores(line);
        for (Map.Entry<String, Integer> line : add.entrySet())
            objective.getScore(line.getKey()).setScore(line.getValue());

    }

}
