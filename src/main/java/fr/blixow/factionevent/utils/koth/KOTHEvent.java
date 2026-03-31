package fr.blixow.factionevent.utils.koth;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class KOTHEvent {

    private final KOTH koth;
    private final LinkedHashMap<Player, Long> playersInKOTH;
    private boolean won = false;
    private final FileConfiguration config;
    private final long start_time;
    private int duration = 1800;
    private int win_time = 300;

    public KOTHEvent(KOTH koth){
        this.koth = koth;
        this.start_time = new Date().getTime();
        playersInKOTH = new LinkedHashMap<>();
        this.config = FileManager.getConfig();
        try { this.duration = config.contains("koth.max_duration") ? config.getInt("koth.max_duration") : 1800; } catch (Exception ignored){}
        try { this.win_time = config.contains("koth.win_time") ? config.getInt("koth.win_time") : 300; } catch (Exception ignored){}
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
                } catch (Exception exception){ exception.printStackTrace(); }
            } else {
                try {
                    String factionName = factionPlayer.getFaction().getTag();
                    String str = new StrManager(msg.getString("koth.king")).reKoth(koth.getName()).rePlayer(player).reFaction(factionName).toString();
                    if(playersInKOTH.size() == 1){ Bukkit.broadcastMessage(prefix + str); }
                } catch (Exception exception){ exception.printStackTrace(); }
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
                    try { str = new StrManager(msg.getString("koth.no_more_king")).reKoth(koth.getName()).toString(); }
                    catch (Exception exception){ exception.printStackTrace(); }
                    Bukkit.broadcastMessage(prefix + str);
                    playersInKOTH.remove(player); return;
                }
                playersInKOTH.remove(player);
                Player p2 = getFirstPlayer();
                long joined = getFirstPlayerJoined();
                playersInKOTH.replace(p2, joined, new Date().getTime());
                FPlayer factionPlayer = FPlayers.getInstance().getByPlayer(p2);
                if(factionPlayer.getFaction().isWilderness()){
                    String str = new StrManager(msg.getString("koth.new_king")).reKoth(koth.getName()).rePlayer(p2).reFaction(msg.getString("no-faction")).toString();
                    if(playersInKOTH.size() == 1){ Bukkit.broadcastMessage(prefix + str); }
                } else {
                    String factionName = FPlayers.getInstance().getByPlayer(p2).getFaction().getTag();
                    String str = new StrManager(msg.getString("koth.new_king")).reKoth(koth.getName()).rePlayer(p2).reFaction(factionName).toString();
                    if(playersInKOTH.size() == 1){ Bukkit.broadcastMessage(prefix + str); }
                }
            } else {
                playersInKOTH.remove(player);
            }
        }
    }

    public boolean isFirstPlayer(Player player){
        if(playersInKOTH.isEmpty()) return false;
        return player.equals(playersInKOTH.entrySet().iterator().next().getKey());
    }

    public Player getFirstPlayer(){
        if(playersInKOTH.isEmpty()) return null;
        return playersInKOTH.entrySet().iterator().next().getKey();
    }

    public Long getFirstPlayerJoined(){
        if(playersInKOTH.isEmpty()) return null;
        return playersInKOTH.entrySet().iterator().next().getValue();
    }

    public LinkedHashMap<Player, Long> getPlayersInKOTH() { return playersInKOTH; }
    public KOTH getKoth() { return koth; }

    public boolean checkTimer() {
        boolean ended = false;
        long current_time = new Date().getTime();
        long diff_start = (current_time - start_time) / 1000;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("koth.prefix");
        if(playersInKOTH.isEmpty()){
            String str = prefix + new StrManager(msg.getString("koth.no_winner")).reKoth(koth.getName() == null ? "ERREUR NULL" : koth.getName()).toString();
            if(diff_start >= duration){ ended = true; Bukkit.broadcastMessage(str); }
        } else {
            Player player = getFirstPlayer();
            long joined = getFirstPlayerJoined();
            if(!KOTHManager.isInKOTH(player, koth)){ removePlayer(player); return false; }
            long diff = (current_time - joined) / 1000;
            if(diff >= win_time){
                ended = true;
                grantVictory(player);
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
            FactionMessageTitle.sendFactionTitle(faction, 20, 40, 20, "§aKOTH remporté", "+" + 10 + " points au classement");
        }
        this.won = true;
        RankingManager.updateRanking(true);
    }

    /**
     * Met à jour l'action bar pour tous les joueurs en ligne pendant le KOTH.
     */
    public void updateScoreboard(){
        try {
            if (FactionEvent.getInstance().getEventOn().getKothEvent() == null) return;
        } catch (Exception e) { return; }

        if (playersInKOTH.isEmpty()) return;

        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        long current_time = new Date().getTime();
        long joined = getFirstPlayerJoined();
        long diff = (current_time - joined) / 1000;
        String formated_diff = DateManager.getFormattedTime((int) diff);
        String formated_maxtime = DateManager.getFormattedTime(win_time);
        String actionBar = new StrManager(msg.getString("koth.time_in")).reTime(formated_diff).reMaxTime(formated_maxtime).toString();

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if (eventManager == null) {
                    eventManager = EventManager.loadFromFile(player);
                    FactionEvent.getInstance().getEventScoreboardOff().put(player, eventManager);
                }
                if (eventManager.isActionbar()) {
                    Messages.sendActionBar(player, actionBar);
                }
            } catch (Exception ignored) {}
        }
    }

    public boolean isWon() { return won; }
}
