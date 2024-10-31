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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LMSEvent {

    private final LMS lastManStanding;
    private final HashMap<Player, Boolean> participants;
    private boolean eventActive;
    private ScoreBoardAPI scoreBoardAPI;
    private final FileConfiguration config;
    private final LMS lms;
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.contains("lms.prefix") ? msg.getString("lms.prefix") : "§8[§cLMS§8]§7 ";

    public LMSEvent(LMS lastManStanding, HashMap<Player, Boolean> participants, FileConfiguration config, LMS lms) {
        this.lastManStanding = lastManStanding;
        this.participants = new HashMap<>();
        this.config = FileManager.getConfig();
        this.lms = lms;
        this.eventActive = false;
    }

    public void startCombat() {
        eventActive = true;
        Bukkit.broadcastMessage(prefix + (new StrManager(msg.getString("lms.start")).reLMS(lms.getName()).toString()));
    }

    public void handlePlayerDeath(Player player) {
        if (!eventActive) return;

        participants.remove(player);
        player.sendMessage(prefix + (new StrManager(msg.getString("lms.eliminated")).rePlayer(player).reLMS(lms.getName()).toString()));

        if (participants.size() == 1) {
            Map.Entry<Player, Boolean> entry = participants.entrySet().iterator().next();
            grantVictory(entry.getKey());
            endEvent();
        }
    }

    public void endEvent() {
        participants.clear();
        eventActive = false;
    }

    public void grantVictory(Player player) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        String str = prefix + new StrManager(msg.getString("lms.winner")).rePlayer(player).reLMS(lms.getName()).toString();
        Faction faction = fPlayer.getFaction();
        Bukkit.broadcastMessage(str);
        if(!faction.isWilderness()){
            int points = 10;
            try { if(config.contains("lms.win_points")){ points = config.getInt("lms.win_points"); if(points < 1){ points = 1; } } } catch (Exception ignored){}
            RankingManager.addLMSWins(faction);
            RankingManager.addPoints(faction, points);
            FactionMessageTitle.sendFactionTitle(faction, 20,40, 20,"§aLMS remporté", "+20 points au classement");
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
            String title = msg.getString("lms.scoreboard.title");
            List<String> stringList = msg.getStringList("lms.scoreboard.lines");
            int size = stringList.size();
            scoreBoardAPI.setDisplayName(title);

            // Met à jour chaque ligne du scoreboard sans les éléments liés au temps
            for (String line : stringList) {
                String line2 = new StrManager(line)
                        .reLMS(lms.getName())
                        .reFaction(factionName)
                        .toString();
                scoreBoardAPI.setLine(size, line2);
                size--;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            scoreBoardAPI.setDisplayName("§8[§cLMS§8]");
            scoreBoardAPI.setLine(6, "");
            scoreBoardAPI.setLine(5, "§c» §eFaction");
            scoreBoardAPI.setLine(4, "§7" + factionName);
            scoreBoardAPI.setLine(3, "");
        }

        // Affecte le scoreboard aux joueurs participants
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
        return lastManStanding;
    }

}
