package net.trollyloki.MurderMystery.game;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import net.trollyloki.MurderMystery.Main;

public class Timer implements Runnable {

    // Main class for bukkit scheduling
    private static JavaPlugin timerPlugin;

    // Our scheduled task's assigned id, needed for canceling
    private static Integer assignedTaskId;

    // Seconds
    private static int timerSeconds;
    private static int graceSeconds;
    private static int secondsLeft;
    private static int murderCompassSeconds;

    // Construct a timer, you could create multiple so for example if
    // you do not want these "actions"
    public static void CountdownTimer(JavaPlugin plugin, int seconds, int grace, int murderCompass) {
        // Initializing fields
        timerPlugin = plugin;

        timerSeconds = seconds;
        graceSeconds = grace;
        secondsLeft = seconds + grace;
        murderCompassSeconds = murderCompass;
        scheduleTimer();
        Main.sendDebug("Initiated Timer");
        
    }

    /**
     * Runs the timer once, decrements seconds etc...
     * Really wish we could make it protected/private so you couldn't access it
     */
    
    private ScoreboardManager manager;
    
    public void run() {
        // Is the timer up?
        if (secondsLeft < 1) {
            // Do what was supposed to happen after the timer
            Run.timeExpired();

            // Cancel timer
            if (assignedTaskId != null) Bukkit.getScheduler().cancelTask(assignedTaskId);
            return;
        }

        // Are we just starting?
        if (secondsLeft == timerSeconds + graceSeconds) {
        	// Create Scoreboard Objective
        	manager = Bukkit.getScoreboardManager();
        	
        	for (Player nextPlayer : Run.allPlayers) {
        		Scoreboard sb = manager.getNewScoreboard();
        		Objective objective = sb.registerNewObjective(Main.getPlugin().getConfig().getString("timer.objective"), "dummy", "timer");
            	objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            	objective.setDisplayName(Main.getConfigString(false, "timer.scoreboard.title"));
            	
            	Team team = sb.registerNewTeam(Main.getPlugin().getConfig().getString("timer.team"));
            	team.setOption(Option.DEATH_MESSAGE_VISIBILITY, OptionStatus.NEVER);
            	team.setOption(Option.NAME_TAG_VISIBILITY, OptionStatus.NEVER);
            	
            	for (Player player : Run.allPlayers) {
                	team.addEntry(player.getName());
            	}
            	
            	nextPlayer.setScoreboard(sb);
            	
        	}
        	
        	Main.sendDebug("Created timer scoreboard objectives and team");
        	
        }
        
        // Grace Messages
        if (graceSeconds > 0) graceSeconds--;
        if (graceSeconds <= 5 && Run.grace) {
        	
        	String message = Main.getConfigString(false, "lang.messages.grace-ending")
					.replaceAll("%time%", String.valueOf(graceSeconds));
        	if (graceSeconds <= 0) {
        		Run.grace = false;
        		message = Main.getConfigString(false, "lang.messages.grace-ended");
        	}
        	
        	for (Player nextPlayer : Run.allPlayers) {
        		nextPlayer.sendMessage(message);
        	}
        	
        }
        
        // Update Scoreboard
    	forceUpdate();
    	
    	if (secondsLeft == murderCompassSeconds) {
    		Run.murderer.getInventory().setItem(8, new ItemStack(Material.COMPASS));
    	}
    	
        secondsLeft--;
    }
    
    public static String changeTime(String action, int amount) {
    	if (!Run.gameRunning) {
    		return "not-running";
    	}
    	
    	else if (action.equalsIgnoreCase("set")) {
    		if (amount < 1) {
    			return "time.under-time";
    		}
    		
       		secondsLeft = amount;
			forceUpdate();
			return "time.success";
    	}
    	
    	else if (action.equalsIgnoreCase("add")) {
    		if (secondsLeft + amount < 1) {
    			return "time.under-time";
    		}
    		
       		secondsLeft += amount;
			forceUpdate();
			return "time.success";
		}
		
    	else if (action.equalsIgnoreCase("remove")) {
    		if (secondsLeft - amount < 1) {
    			return "time.under-time";
    		}
    		
       		secondsLeft -= amount;
			forceUpdate();
			return "time.success";
		}
		
		else {
			return "time.action";
		}

    }
    
    public static void endTimer() {
    	if (assignedTaskId != null) Bukkit.getScheduler().cancelTask(assignedTaskId);
    	
    	for (Player nextPlayer : Run.allPlayers) {
    		nextPlayer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    	}
    	
    	Main.sendDebug("Reset scoreboards");
    }
    
    public static void forceUpdate() {
    	for (Player nextPlayer : Run.allPlayers) {
    		String role;
    		if (Run.murderer == nextPlayer) role = (Main.getConfigString(false, "titles.murderer.role"));
    		else if (Run.detective == nextPlayer) role = (Main.getConfigString(false, "titles.detective.role"));
    		else if (Run.deputy == nextPlayer) role = (Main.getConfigString(false, "titles.deputy.role"));
    		else if (Run.bystanders.contains(nextPlayer)) role = (Main.getConfigString(false, "titles.bystander.role"));
    		else role = (Main.getConfigString(false, "titles.dead.role"));
    		
    		Scoreboard sb = nextPlayer.getScoreboard();
    		if (sb == Bukkit.getScoreboardManager().getMainScoreboard()) continue;
    		Objective objective = sb.getObjective(DisplaySlot.SIDEBAR);
    		if (objective != null) objective.unregister();
    		
    		objective = sb.registerNewObjective(Main.getPlugin().getConfig().getString("timer.objective"), "dummy", "timer");
        	objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        	objective.setDisplayName(Main.getConfigString(false, "timer.scoreboard.title"));
        	nextPlayer.setScoreboard(sb);
        	
    		List<String> lines = Main.getConfigStringList(false, "timer.scoreboard.lines");
    		int i = (lines.size() - 1);
    		for (String nextLine : lines) {
    			if (nextLine.equalsIgnoreCase("")) {
    				StringBuilder strBld = new StringBuilder();
    				for (int a = 0; a <= i; a++) {
    					strBld.append(" ");
    				}
    				nextLine = strBld.toString();
    			}
    			
        		nextLine = nextLine
        			.replaceAll("%map%", Main.getConfigString(false, "maps." + Run.map + ".name"))
        			.replaceAll("%time%", convertToTime(secondsLeft))
        			.replaceAll("%role%", role)
        			.replaceAll("%players%", String.valueOf(Run.getPlayersRemaining()));
        		Score score = objective.getScore(nextLine);
        		score.setScore(i);
        		i--;
        	}
    	}
    	
    	//Main.sendDebug("Updated Timer");
    }
    
    public static String convertToTime(int seconds) {
    	int minutes = 0;
    	int hours = 0;
    	while (seconds > 59) {
    		while (seconds > 3599) {
    			seconds -= 3600;
    			hours++;
    		}
    		seconds -= 60;
    		minutes++;
    	}
    	
    	String strHours = String.valueOf(hours);
    	String strMins = String.valueOf(minutes);
    	String strSecs = String.valueOf(seconds);
    	if (seconds < 10) strSecs = "0" + strSecs;
    	
    	if (hours == 0) strHours = "";
    	else strHours = strHours + ":";
    	String time = strHours + strMins + ":" + strSecs;
    	return time;
    }

    /**
     * Gets the total seconds this timer was set to run for
     *
     * @return Total seconds timer should run
     */
    public static int getTotalSeconds() {
        return timerSeconds;
    }

    /**
     * Gets the seconds left this timer should run
     *
     * @return Seconds left timer should run
     */
    public static int getSecondsLeft() {
        return secondsLeft;
    }

    /**
     * Schedules this instance to "run" every second
     */
    public static void scheduleTimer() {
        // Initialize our assigned task's id, for later use so we can cancel
        assignedTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(timerPlugin, new Timer(), 0L, 20L);
    }

}