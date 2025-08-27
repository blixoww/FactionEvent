package fr.blixow.factionevent.utils.lms;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.ScoreBoardAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LMSEvent {

    private final HashMap<Player, Boolean> participants;
    private boolean eventActive;
    private ScoreBoardAPI scoreBoardAPI;
    private final FileConfiguration config;
    private final LMS lms;
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.contains("lms.prefix") ? msg.getString("lms.prefix") : "§8[§cLMS§8]§7 ";

    public LMSEvent(LMS lms, HashMap<Player, Boolean> participants, FileConfiguration config) {
        this.lms = lms;
        this.participants = lms.getRegisteredPlayers();
        this.config = FileManager.getConfig();
        this.eventActive = false;
    }

    public void startEvent() {
        eventActive = true;
        participants.forEach((player, aBoolean) -> player.sendMessage(prefix + new StrManager(msg.getString("lms.started")).reLMS(lms.getName()).toString()));
        updateScoreboard();
    }


    public void handlePlayerDeath(Player player) {
        participants.remove(player);
        player.sendMessage(prefix + new StrManager(msg.getString("lms.eliminated")).reLMS(lms.getName()).toString());
        System.out.println("Player " + player.getName() + " has been eliminated from the LMS event.");
        System.out.println("Remaining players: " + participants.size());
        if (participants.size() == 1) {
            if (lms.isPreparation()) {
                lms.unregisterPlayer(player);
                Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.canceled")).rePlayer(player).reLMS(lms.getName()).toString());
                endEvent();
            }
            Map.Entry<Player, Boolean> entry = participants.entrySet().iterator().next();
            grantVictory(entry.getKey());
            endEvent();
        }
    }

    public void handlePlayerQuit(Player player) {
        participants.remove(player);
        System.out.println("Player " + player.getName() + " has been eliminated from the LMS event.");
        System.out.println("Remaining players: " + participants.size());
        if (participants.size() == 1) {
            if (lms.isPreparation()) {
                lms.unregisterPlayer(player);
                Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.canceled")).rePlayer(player).reLMS(lms.getName()).toString());
                endEvent();
            }
            Map.Entry<Player, Boolean> entry = participants.entrySet().iterator().next();
            grantVictory(entry.getKey());
            endEvent();
        }
    }

    public void endEvent() {
        participants.clear();
        eventActive = false;
        this.getScoreBoardAPI().getObjective().unregister();
    }

    public void grantVictory(Player player) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        String str = new StrManager(msg.getString("lms.winner")).rePlayer(player).reLMS(lms.getName()).reFaction(faction.getTag()).toString();
        Bukkit.broadcastMessage(str);
        if(!faction.isWilderness()){
            int points = 10;
            try { if(config.contains("lms.win_points")){ points = config.getInt("lms.win_points"); if(points < 1){ points = 1; } } } catch (Exception ignored){}
            RankingManager.addLMSWins(faction);
            RankingManager.addPoints(faction, points);
            FactionMessageTitle.sendFactionTitle(faction, 20,40, 20,"§aLMS remporté", "+" + config.getInt("lms.win_points") +" points au classement");
        }
        RankingManager.updateRanking(true);
    }

    public void updateScoreboard() {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String factionName = "Aucune";

        // Récupère le nom de la faction du joueur
        if (!participants.isEmpty()) {
            Map.Entry<Player, Boolean> entry = participants.entrySet().iterator().next();
            Player player = entry.getKey();
            Faction fac = FPlayers.getInstance().getByPlayer(player).getFaction();
            if (!fac.isWarZone() && !fac.isSafeZone() && !fac.isWilderness()) {
                factionName = fac.getTag();
            }
        }

        // Crée le nouveau scoreboard
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreBoardAPI = new ScoreBoardAPI(board, "LMS", true);

        try {
            // Récupère le titre et les lignes du scoreboard depuis la config
            String title = msg.getString("scoreboard.title", "§8[§cLMS§8]");
            List<String> stringList = msg.getStringList("scoreboard.lines");
            int totalPlayersRemaining = participants.size();  // Nombre de joueurs restants
            scoreBoardAPI.setDisplayName(title);

            // Met à jour chaque ligne du scoreboard avec les valeurs actuelles
            int lineNumber = stringList.size();
            for (String line : stringList) {
                String formattedLine = line
                        .replace("{player}", participants.isEmpty() ? "N/A" : participants.keySet().iterator().next().getName())
                        .replace("{faction}", factionName)
                        .replace("{total}", String.valueOf(totalPlayersRemaining));

                scoreBoardAPI.setLine(lineNumber, formattedLine);
                lineNumber--;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            // Définit un affichage par défaut en cas d'erreur dans la configuration
            scoreBoardAPI.setDisplayName("§8[§cLMS§8]");
            scoreBoardAPI.setLine(6, "");
            scoreBoardAPI.setLine(5, "§c» §eFaction");
            scoreBoardAPI.setLine(4, "§7" + factionName);
            scoreBoardAPI.setLine(3, "");
        }

        // Assigne le scoreboard aux joueurs participants
        for (Player player : FactionEvent.getInstance().getEventScoreboardOff().keySet()) {
            try {
                EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if (eventManager.isScoreboard()) {
                    player.setScoreboard(scoreBoardAPI.getScoreboard());
                    scoreBoardAPI.getObjective().setDisplaySlot(DisplaySlot.SIDEBAR);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public ScoreBoardAPI getScoreBoardAPI() { return scoreBoardAPI; }

    public LMS getLMS() {
        return lms;
    }

    public boolean checkTimer() {
        return eventActive;
    }

}
