package fr.blixow.factionevent.utils.koth;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.Messages;
import fr.blixow.factionevent.utils.ScoreBoardAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KOTHEvent {

    private final KOTH koth;
    private final LinkedHashMap<Player, Long> playersInKOTH;
    private ScoreBoardAPI scoreBoardAPI;
    private boolean won = false;
    private final FileConfiguration config;
    // Timings
    private final long start_time;
    private int duration = 1800;
    private int win_time = 300;

    public KOTHEvent(KOTH koth){
        this.koth = koth;
        this.start_time = new Date().getTime();
        playersInKOTH = new LinkedHashMap<>();
        this.config = FileManager.getConfig();
        try { this.duration = config.contains("koth.max_duration") ? config.getInt("koth.max_duration") : 1800; } catch (Exception ignored){}
        try {this.win_time = config.contains("koth.win_time") ? config.getInt("koth.win_time") : 300; } catch (Exception ignored){}
    }

    public void addPlayer(Player player){
        if(!playersInKOTH.containsKey(player)){
            long joined = 0;
            if(playersInKOTH.isEmpty()){ joined = new Date().getTime(); }
            playersInKOTH.put(player, joined);
            FPlayer factionPlayer = FPlayers.getInstance().getByPlayer(player);
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String prefix = msg.getString("koth.prefix");
            if(factionPlayer.getFaction().isWilderness()){
                try {
                    String factionless = msg.getString("no-faction");
                    String str = new StrManager(msg.getString("koth.king")).reKoth(koth.getName()).rePlayer(player).reFaction(factionless).toString();
                    if(playersInKOTH.size() == 1){ Bukkit.broadcastMessage(prefix + str); }
                } catch (Exception exception){
                    FactionEvent.getInstance().getLogger().warning("Erreur addPlayer() factionless");
                    exception.printStackTrace();
                }

            } else {
                try {
                    String factionName = factionPlayer.getFaction().getTag();
                    String message = msg.getString("koth.king");
                    String str = new StrManager(message).reKoth(koth.getName()).rePlayer(player).reFaction(factionName).toString();
                    if(playersInKOTH.size() == 1){ Bukkit.broadcastMessage(prefix + str); }
                } catch (Exception exception){
                    FactionEvent.getInstance().getLogger().warning("Erreur addPlayer() w/ faction");
                    exception.printStackTrace();
                }

            }
        }
    }

    public void removePlayer(Player player){
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("koth.prefix");
        if(playersInKOTH.containsKey(player)){
            if(isFirstPlayer(player)){
                if(playersInKOTH.size() == 1){
                    String str = "";
                    try {str = new StrManager(msg.getString("koth.no_more_king")).reKoth(koth.getName()).toString(); }
                    catch (Exception exception){ exception.printStackTrace(); }
                    Bukkit.broadcastMessage(prefix + str);
                    playersInKOTH.remove(player); return;
                }
                playersInKOTH.remove(player);
                LinkedHashMap<Player, Long> linkedHashMap = playersInKOTH;
                linkedHashMap.remove(player);
                Player p2 = getFirstPlayer();
                long joined = getFirstPlayerJoined();
                playersInKOTH.replace(p2, joined, new Date().getTime());
                FPlayer factionPlayer = FPlayers.getInstance().getByPlayer(p2);
                if(factionPlayer.getFaction().isWilderness()){
                    String str = new StrManager(msg.getString("koth.new_king")).reKoth(koth.getName()).rePlayer(p2).reFaction(msg.getString("no-faction")).toString();
                    if(playersInKOTH.size() == 1){ Bukkit.broadcastMessage(prefix + str); }
                } else {
                    FPlayer fPlayer = FPlayers.getInstance().getByPlayer(p2);
                    String factionName = fPlayer.getFaction().getTag();
                    String str = new StrManager(msg.getString("koth.new_king")).reKoth(koth.getName()).rePlayer(p2).reFaction(factionName).toString();
                    if(playersInKOTH.size() == 1){ Bukkit.broadcastMessage(prefix + str); }
                }
            } else {
                playersInKOTH.remove(player);
            }
        }
    }

    public boolean isFirstPlayer(Player player){
        if(playersInKOTH.isEmpty()){ return false; }
        Map.Entry<Player,Long> entry = playersInKOTH.entrySet().iterator().next();
        Player key= entry.getKey();
        return player.equals(key);
    }

    public Player getFirstPlayer(){
        if(playersInKOTH.isEmpty()){ return null; }
        Map.Entry<Player,Long> entry = playersInKOTH.entrySet().iterator().next();
        return entry.getKey();
    }

    public Long getFirstPlayerJoined(){
        if(playersInKOTH.isEmpty()){ return null; }
        Map.Entry<Player,Long> entry = playersInKOTH.entrySet().iterator().next();
        return entry.getValue();
    }

    public LinkedHashMap<Player, Long> getPlayersInKOTH() { return playersInKOTH; }

    public KOTH getKoth() { return koth; }

    public boolean checkTimer() {
        boolean ended = false;
        long current_time = new Date().getTime();
        long diff_start = Math.round((current_time - start_time) / (long) 100) / 10;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("koth.prefix");
        if(playersInKOTH.isEmpty()){
            String kothName = koth.getName() == null ? "ERREUR NULL" :  koth.getName();
            String str = prefix + new StrManager(msg.getString("koth.no_winner")).reKoth(kothName).toString();
            int ending_time = duration;
            if(diff_start >= ending_time){ ended = true; Bukkit.broadcastMessage(str); }
        } else {
            Player player = getFirstPlayer();
            long joined = getFirstPlayerJoined();
            if(!KOTHManager.isInKOTH(player, koth)){ removePlayer(player); return false; }

            String formated_maxtime = DateManager.getFormattedTime(win_time);
            long diff = Math.round((current_time - joined) / (long) 100) / 10;
            String formated_diff = DateManager.getFormattedTime((int) diff);
            if(diff >= win_time){
                ended = true;
                grantVictory(player);
            }
            String str = prefix + new StrManager(msg.getString("koth.time_in")).reTime(formated_diff).reMaxTime(formated_maxtime).toString();
            if(!ended){
                Messages.sendActionBar(player, str);
            }
        }
        return ended;
    }

    public void grantVictory(Player player){
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("koth.prefix");
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        String str = prefix + new StrManager(msg.getString("koth.winner")).rePlayer(player).reKoth(koth.getName()).toString();
        Faction faction = fPlayer.getFaction();
        Bukkit.broadcastMessage(str);
        if(!faction.isWilderness()){
            int points = 10;
            try { if(config.contains("koth.win_points")){ points = config.getInt("koth.win_points"); if(points < 1){ points = 1; } } } catch (Exception ignored){}
            RankingManager.addKothWins(faction);
            RankingManager.addPoints(faction, points);
        }
        this.won = true;
    }

    public void updateScoreboard(){
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        long timeRemaining = duration - ((new Date().getTime() - start_time) / 1000);
        String timeRemainingString = DateManager.getFormattedTime((int) timeRemaining);
        String factionName = "Aucune";
        long current_time = new Date().getTime();
        long joined = current_time;
        if(!playersInKOTH.isEmpty()){
            Player player = getFirstPlayer();
            joined = getFirstPlayerJoined();
            Faction fac = FPlayers.getInstance().getByPlayer(player).getFaction();
            if(!fac.isWarZone() && !fac.isSafeZone() && !fac.isWilderness()){ factionName = fac.getTag(); }
        }
        String formated_maxtime = DateManager.getFormattedTime(win_time);
        long diff = Math.round((current_time - joined) / (long) 100) / 10;
        String formated_diff = DateManager.getFormattedTime((int) diff);
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreBoardAPI = new ScoreBoardAPI(board, "KOTH", true);
        try {
            String title = msg.getString("koth.scoreboard.title");
            List<String> stringList = msg.getStringList("koth.scoreboard.lines");
            int size = stringList.size();
            scoreBoardAPI.setDisplayName(title);
            for(String line : stringList){
                String line2 = new StrManager(line)
                        .reKoth(koth.getName())
                        .reFaction(factionName)
                        .reTime(timeRemainingString)
                        .reCustom("\\{time_capture}", formated_diff)
                        .reCustom("\\{maxtime}", formated_maxtime)
                        .toString();
                scoreBoardAPI.setLine(size, line2);
                size--;
            }
        } catch (Exception exception){
            exception.printStackTrace();
            scoreBoardAPI.setDisplayName("§8[§cKOTH§8]");
            scoreBoardAPI.setLine(8, "");
            scoreBoardAPI.setLine(7, "§c» §eFaction");
            scoreBoardAPI.setLine(6, "§7" + factionName);
            scoreBoardAPI.setLine(5, "");
            scoreBoardAPI.setLine(4, "§c» §eCapture");
            scoreBoardAPI.setLine(3, "§7" + formated_diff + "§f/§7" + formated_maxtime);
            scoreBoardAPI.setLine(2, "");
            scoreBoardAPI.setLine(1, "§c» §eTemps");
            scoreBoardAPI.setLine(0, "§7" + timeRemainingString);
        }
        for(Player player : FactionEvent.getInstance().getEventScoreboardOff().keySet()){
            try {
                EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if(eventManager.isScoreboard()){
                    player.setScoreboard(scoreBoardAPI.getScoreboard());
                    scoreBoardAPI.getObjective().setDisplaySlot(DisplaySlot.SIDEBAR);
                }
            } catch (Exception exception){
                exception.printStackTrace();
            }
        }
    }

    public ScoreBoardAPI getScoreBoardAPI() { return scoreBoardAPI; }

    public boolean isWon() { return won; }
}