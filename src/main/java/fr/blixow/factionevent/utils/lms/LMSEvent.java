package fr.blixow.factionevent.utils.lms;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class LMSEvent {

    private final HashMap<Player, Boolean> participants;
    private boolean eventActive;
    private final FileConfiguration config;
    private final LMS lms;
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.contains("lms.prefix") ? msg.getString("lms.prefix") : "§8[§cLMS§8]§7 ";

    public LMSEvent(LMS lms) {
        this.lms = lms;
        this.participants = lms.getRegisteredPlayers();
        this.config = FileManager.getConfig();
        this.eventActive = false;
    }

    public void startEvent() {
        eventActive = true;
        participants.forEach((player, aBoolean) -> {
            if (player != null && player.isOnline()) {
                player.sendMessage(prefix + new StrManager(msg.getString("lms.started", "§aLe LMS §e{lms}§a a commencé !")).reLMS(lms.getName()).toString());
            }
        });
    }

    public boolean isParticipant(Player player) {
        return participants.containsKey(player);
    }

    public boolean isEventActive() {
        return eventActive;
    }

    public void handlePlayerDeath(Player player) {
        if (!participants.containsKey(player)) return;
        participants.remove(player);
        if (player.isOnline()) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.eliminated", "§cVous avez été éliminé du LMS §e{lms}§c !")).reLMS(lms.getName()).toString());
        }
        if (participants.isEmpty()) {
            Bukkit.broadcastMessage(prefix + "§cLe LMS s'est terminé sans vainqueur.");
            endEvent(); lms.resetPhase(); return;
        }
        if (participants.size() == 1) {
            if (lms.isPreparation()) {
                Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.canceled", "§cLe LMS a été annulé.")).reLMS(lms.getName()).toString());
                endEvent(); lms.resetPhase(); return;
            }
            grantVictory(participants.entrySet().iterator().next().getKey());
            endEvent(); lms.resetPhase();
        }
    }

    public void handlePlayerQuit(Player player) {
        if (!participants.containsKey(player)) return;
        participants.remove(player);
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.eliminated", "§c{player} §7a quitté le LMS §e{lms}§7 et est éliminé.")).rePlayer(player).reLMS(lms.getName()).toString());
        if (participants.isEmpty()) {
            Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.canceled", "§cLe LMS a été annulé.")).reLMS(lms.getName()).toString());
            endEvent(); lms.resetPhase(); return;
        }
        if (participants.size() == 1) {
            if (lms.isPreparation()) {
                Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.canceled", "§cLe LMS a été annulé.")).reLMS(lms.getName()).toString());
                endEvent(); lms.resetPhase(); return;
            }
            if (lms.isStarted()) {
                Player winner = participants.entrySet().iterator().next().getKey();
                if (winner.isOnline()) grantVictory(winner);
                endEvent(); lms.resetPhase();
            }
        }
    }

    public void endEvent() {
        participants.clear();
        eventActive = false;
        FactionEvent.getInstance().getEventOn().setLMSEvent(null);
    }

    public void grantVictory(Player player) {
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer == null) return;
        Faction faction = fPlayer.getFaction();
        if (faction == null) return;
        String str = new StrManager(msg.getString("lms.winner", "§c{player} §7remporte le LMS §e{lms} §7pour §c{faction}§7 !"))
                .rePlayer(player).reLMS(lms.getName()).reFaction(faction.getTag()).toString();
        Bukkit.broadcastMessage(str);
        if (!faction.isWilderness()) {
            int points = 10;
            try { if (config.contains("lms.win_points")) points = Math.max(1, config.getInt("lms.win_points")); } catch (Exception ignored) {}
            RankingManager.addLMSWins(faction);
            RankingManager.addPoints(faction, points);
            if (player.isOnline()) FactionMessageTitle.sendFactionTitle(faction, 20, 40, 20, "§aLMS remporté", "+" + points + " points au classement");
        }
        RankingManager.updateRanking(false);
    }

    /**
     * updateScoreboard — sans scoreboard, ne fait rien (LMS n'a pas d'action bar périodique pertinente).
     */
    public void updateScoreboard() {
        // Pas de scoreboard, pas d'action bar périodique pour le LMS
    }

    public boolean checkTimer() { return eventActive; }
    public LMS getLMS() { return lms; }
}
