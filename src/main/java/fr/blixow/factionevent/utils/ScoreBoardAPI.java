package fr.blixow.factionevent.utils;

import com.google.common.base.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.UUID;

public class ScoreBoardAPI {

    private final Scoreboard sb;
    private Objective obj;
    private final HashMap<Integer,String> teams = new HashMap<>();


    public ScoreBoardAPI(Scoreboard sb, String objectiveName, boolean init){
        this.sb = sb;
        init(objectiveName,init);
    }

    public ScoreBoardAPI(Scoreboard sb, String objectiveName) {
        this.sb = sb;
        init(objectiveName, false);
    }

    private void init(String objectiveName, Boolean init) {
        if (init) {

            try {
                this.obj = this.sb.registerNewObjective(objectiveName, "dummy");
            } catch(Exception e) {
                Bukkit.getLogger().severe("Couldn't instantiate ScoreboardAPI: Objective already exist !");
            }
        } else {
            try {
                this.obj = this.sb.getObjective(objectiveName);
                for (Team team : this.sb.getTeams()) {
                    Score score = this.obj.getScore(team.getName());
                    if(score.isScoreSet())
                        teams.put(score.getScore(), team.getName());
                }
            } catch(Exception e) {
                Bukkit.getLogger().severe("Couldn't instantiate ScoreboardAPI: Couldn't find Objective !");
            }
        }
    }

    @SuppressWarnings({ "deprecation", "unlikely-arg-type" })
    public void setLine(int line, String value) {
        String teamName;
        if (teams.containsKey(line)) {
            teamName = teams.get(line);
            editTeam(teamName, value);
            return;
        }

        do {
            teamName = UUID.randomUUID().toString().substring(0, 7).replaceAll("", "§") + "r";
        } while (this.sb.getTeams().contains(teamName));

        teams.put(line, teamName);

        final Team team = this.sb.registerNewTeam(teamName);
        team.addPlayer(this.getOfflinePlayerSkipLookup(teamName));

        this.obj.getScore(teamName).setScore(line);

        editTeam(teamName, value);
    }

    private void editTeam(String teamName, String value) {
        final Team team = this.sb.getTeam(teamName);
        team.setDisplayName("");
        if(value.length() <= 16) {
            team.setPrefix(value);
        } else {

            boolean colorcut = Character.toString(value.charAt(15)).equals("§");
            String prefix = colorcut ? value.substring(0, 15) : value.substring(0, 16);
            String color = "";
            for (int i = prefix.length()-1; i >= 0; i--) {
                if (Character.toString(prefix.charAt(i)).equals("§")) {
                    color = "§" +prefix.charAt(i+1);
                    if (i > 1 && Character.toString(prefix.charAt(i-2)).equals("§"))
                        color += color = "§" +prefix.charAt(i-1);
                }
            }
            team.setPrefix(prefix);

            team.setSuffix(color + (colorcut ? value.substring(15, value.length() > (31 - color.length()) ? (31 - color.length())
                    : value.length()) : value.substring(16, value.length() > (32 - color.length()) ? (32 - color.length()) : value.length())));

        }
    }

    public void setDisplayName(String value) {
        obj.setDisplayName(value);
    }

    @SuppressWarnings("deprecation")
    public void removeLine(int line) {
        String teamName = teams.get(line);
        this.sb.resetScores(teamName);
        Team team = this.sb.getTeam(teamName);

        team.getPlayers().forEach(team::removePlayer);
        team.unregister();
        teams.remove(line);
    }

    public void setScoreboardOf(Player... players) {
        for (Player p : players) {
            p.setScoreboard(this.sb);
        }
    }

    public Scoreboard getScoreboard() {
        return this.sb;
    }

    public Objective getObjective() {
        return this.obj;
    }

    private OfflinePlayer getOfflinePlayerSkipLookup(String name) {
        UUID invalidUserUUID = UUID.nameUUIDFromBytes("InvalidUsername".getBytes(Charsets.UTF_8));
        Class<?> gameProfileClass = null;
        Constructor<?> gameProfileConstructor = null;
        Constructor<?> craftOfflinePlayerConstructor = null;
        try {
            if (gameProfileConstructor == null) {
                gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                gameProfileConstructor = gameProfileClass.getDeclaredConstructor(UUID.class, String.class);
                gameProfileConstructor.setAccessible(true);
            }
            if (craftOfflinePlayerConstructor == null) {
                Class<?> serverClass = Bukkit.getServer().getClass();
                Class<?> craftOfflinePlayerClass = Class.forName(serverClass.getName()
                        .replace("CraftServer", "CraftOfflinePlayer"));
                craftOfflinePlayerConstructor = craftOfflinePlayerClass.getDeclaredConstructor(
                        serverClass, gameProfileClass);
                craftOfflinePlayerConstructor.setAccessible(true);
            }
            Object gameProfile = gameProfileConstructor.newInstance(invalidUserUUID, name);
            Object craftOfflinePlayer = craftOfflinePlayerConstructor.newInstance(Bukkit.getServer(), gameProfile);
            return (OfflinePlayer) craftOfflinePlayer;
        } catch (Throwable t) {
            return Bukkit.getOfflinePlayer(name);
        }
    }

}
