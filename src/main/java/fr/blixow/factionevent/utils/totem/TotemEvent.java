package fr.blixow.factionevent.utils.totem;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.Messages;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TotemEvent {
    private final Totem totem;
    private final long started;
    private int duration = 1800;

    private String controllingTag;
    private Faction controllingFaction;

    private HashMap<Location, Material> blocks;
    private final HashMap<String, Integer> factionScores = new HashMap<>();
    private static final String INDEPENDENT_TAG = "Indépendant";

    /**
     * Normalise une Location pour qu'elle soit cohérente comme clé de HashMap.
     * Utilise les coordonnées de bloc (entières) pour éviter les problèmes de précision double.
     */
    private static Location normalizeLocation(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public TotemEvent(Totem totem) {
        this.totem = totem;
        this.started = new Date().getTime();
        this.controllingTag = null;
        this.controllingFaction = null;
        FileConfiguration config = FileManager.getConfig();
        try { if (config.contains("totem.max_duration")) duration = config.getInt("totem.max_duration"); } catch (Exception ignored) {}
        copyBlocks();
    }

    /**
     * Retourne le tag de faction d'un joueur, ou INDEPENDENT_TAG s'il n'a pas de faction valide.
     */
    private String getPlayerTag(Player player) {
        try {
            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
            if (fPlayer == null) return INDEPENDENT_TAG;
            Faction f = fPlayer.getFaction();
            if (f == null || f.isWilderness() || f.isSafeZone() || f.isWarZone()) return INDEPENDENT_TAG;
            return f.getTag();
        } catch (Exception e) { return INDEPENDENT_TAG; }
    }

    /**
     * Retourne la faction d'un joueur ou null s'il n'en a pas de valide.
     */
    private Faction getFactionOfPlayer(Player player) {
        try {
            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
            if (fPlayer == null) return null;
            Faction f = fPlayer.getFaction();
            if (f == null || f.isWilderness() || f.isSafeZone() || f.isWarZone()) return null;
            return f;
        } catch (Exception e) { return null; }
    }

    private void grantVictory(Player player) {
        FileConfiguration config = FileManager.getConfig();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String playerTag = getPlayerTag(player);
        Faction playerFaction = getFactionOfPlayer(player);
        String win = new StrManager(msg.getString("totem.win")).rePlayer(player).reFaction(playerTag).reTotem(totem.getName()).toString();
        Bukkit.broadcastMessage(win);
        if (playerFaction != null) rewardFaction(playerFaction, config);
        totem.stop();
    }

    private void grantVictoryByScore() {
        FileConfiguration config = FileManager.getConfig();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        if (factionScores.isEmpty()) {
            Bukkit.broadcastMessage(new StrManager(msg.getString("totem.no_winner", "§7Le Totem §c{totem} §7s'est terminé sans vainqueur.")).reTotem(totem.getName()).toString());
            totem.stop();
            return;
        }
        String winnerTag = null;
        int maxScore = -1;
        for (Map.Entry<String, Integer> entry : factionScores.entrySet()) {
            if (entry.getValue() > maxScore) { maxScore = entry.getValue(); winnerTag = entry.getKey(); }
        }
        if (winnerTag == null) { totem.stop(); return; }
        String winMsg = new StrManager(msg.getString("totem.win_timeout", msg.getString("totem.win", "§aLa faction §e{faction} §aremporte le totem §e{totem}§a !"))).reFaction(winnerTag).reTotem(totem.getName()).toString();
        Bukkit.broadcastMessage(winMsg);
        if (!INDEPENDENT_TAG.equals(winnerTag)) {
            Faction winnerFaction = findFactionByTag(winnerTag);
            if (winnerFaction != null) rewardFaction(winnerFaction, config);
        }
        totem.stop();
    }

    private Faction findFactionByTag(String tag) {
        try {
            for (Faction f : com.massivecraft.factions.Factions.getInstance().getAllFactions()) {
                if (f.getTag().equals(tag)) return f;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void rewardFaction(Faction f, FileConfiguration config) {
        if (f != null && !f.isWilderness() && !f.isSafeZone() && !f.isWarZone()) {
            int points = 10;
            try { if (config.contains("totem.win_points")) { points = config.getInt("totem.win_points"); if (points < 1) points = 1; } } catch (Exception ignored) {}
            RankingManager.addTotemWins(f);
            RankingManager.addPoints(f, points);
            FactionMessageTitle.sendFactionTitle(f, 20, 40, 20, "§aTotem remporté", "+" + points + " points au classement");
        }
        RankingManager.updateRanking(true);
    }

    public void blockDestroyed(Block block, Player player) {
        Location blockLoc = normalizeLocation(block.getLocation());
        if (!blocks.containsKey(blockLoc)) return;

        String playerTag = getPlayerTag(player);
        FileConfiguration msg = FileManager.getMessageFileConfiguration();

        // Premier joueur à casser un bloc : il prend le contrôle
        if (this.controllingTag == null) {
            this.controllingTag = playerTag;
            this.controllingFaction = getFactionOfPlayer(player);
        }

        if (this.controllingTag.equals(playerTag)) {
            // Même faction/groupe qui contrôle : compter le bloc
            factionScores.merge(playerTag, 1, Integer::sum);
            blocks.remove(blockLoc);

            if (blocks.isEmpty()) { grantVictory(player); return; }

            String left = new StrManager(msg.getString("totem.blocks_left")).reBlocks(blocks.size(), totem.getBlocks().size()).toString();
            FactionMessageTitle.sendPlayersActionBar(left);
        } else {
            // Une autre faction/joueur prend le contrôle : reset des blocs
            reset(player, this.controllingTag, this.controllingFaction);
            // Le bloc cassé compte pour la nouvelle faction
            factionScores.merge(playerTag, 1, Integer::sum);
            blocks.remove(blockLoc);
            // Remettre le bloc cassé en AIR (car setAllBlocks dans reset() l'a restauré)
            block.setType(Material.AIR);

            if (blocks.isEmpty()) { grantVictory(player); return; }

            String left = new StrManager(msg.getString("totem.blocks_left")).reBlocks(blocks.size(), totem.getBlocks().size()).toString();
            FactionMessageTitle.sendPlayersActionBar(left);
        }
        updateScoreboard();
    }

    private void reset(Player player, String oldTag, Faction oldFaction) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String newTag = getPlayerTag(player);
        Faction newFaction = getFactionOfPlayer(player);

        this.controllingTag = newTag;
        this.controllingFaction = newFaction;

        String lost_control_title = msg.getString("totem.lost_control_title");
        String lost_control_subtitle = new StrManager(msg.getString("totem.lost_control_subtitle")).reFaction(newTag).toString();
        String took_control_title = msg.getString("totem.took_control_title");
        String took_control_subtitle = msg.getString("totem.took_control_subtitle");

        // Envoyer les messages de perte/prise de contrôle
        if (oldFaction != null) FactionMessageTitle.sendFactionTitle(oldFaction, 20, 40, 20, lost_control_title, lost_control_subtitle);
        if (newFaction != null) FactionMessageTitle.sendFactionTitle(newFaction, 20, 40, 20, took_control_title, took_control_subtitle);

        // Remettre tous les blocs
        copyBlocks();
        setAllBlocks();
    }

    public void setAllBlocks() {
        blocks.forEach((k, v) -> k.getBlock().setType(v));
    }

    public void start() {
        setAllBlocks();
        updateScoreboard();
        FactionMessageTitle.sendPlayersTitle(20, 40, 20, "§aTotem en cours", "préparez-vous au combat");
    }

    public void copyBlocks() {
        this.blocks = new HashMap<>();
        if (this.totem.getBlocks() != null) {
            for (Map.Entry<Location, Material> entry : totem.getBlocks().entrySet()) {
                this.blocks.put(normalizeLocation(entry.getKey()), entry.getValue());
            }
        }
    }

    public boolean checkTimer() {
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        if (eventOn.getTotemEvent() == null || !eventOn.getTotemEvent().equals(this)) return true;
        long diff = (new Date().getTime() - started) / 1000;
        if (diff < duration) return false;
        // Temps écoulé : victoire par score
        grantVictoryByScore();
        return true;
    }

    /**
     * updateScoreboard envoie uniquement l'action bar (pas de scoreboard).
     */
    public void updateScoreboard() {
        try {
            if (FactionEvent.getInstance().getEventOn().getTotemEvent() == null) return;
        } catch (Exception e) { return; }

        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String actionBarMsg = new StrManager(msg.getString("totem.blocks_left", "§7Blocs restants : §c{blocks}§7/§c{maxblocks}"))
                .reBlocks(blocks.size(), totem.getBlocks().size()).toString();

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                EventManager em = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if (em == null) {
                    em = EventManager.loadFromFile(player);
                    FactionEvent.getInstance().getEventScoreboardOff().put(player, em);
                }
                if (em.isActionbar()) Messages.sendActionBar(player, actionBarMsg);
            } catch (Exception ignored) {}
        }
    }

    // Getters
    public Totem getTotem() { return totem; }
    public HashMap<Location, Material> getBlocks() { return blocks; }
    public Faction getFaction() { return controllingFaction; }
    public String getControllingTag() { return controllingTag; }
    public long getStarted() { return started; }
    public HashMap<String, Integer> getFactionScores() { return factionScores; }
}
