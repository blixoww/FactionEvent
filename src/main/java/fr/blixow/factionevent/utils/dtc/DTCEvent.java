package fr.blixow.factionevent.utils.dtc;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.Messages;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Date;
import java.util.HashMap;

public class DTCEvent {

    private DTC dtc;
    private final long started;
    private Entity entity;
    private double vie;
    private int duration = 1800;
    private final double max_vie;
    private final HashMap<Faction, Double> damageMap;

    public DTCEvent(DTC dtc) {
        this.dtc = dtc;
        this.started = new Date().getTime();
        try {
            Location loc = this.dtc.getLocation();
            if (loc == null || loc.getWorld() == null) {
                FactionEvent.getInstance().getLogger().warning("[DTC] emplacement invalide pour le DTC " + dtc.getName());
                this.entity = null;
            } else {
                this.entity = loc.getWorld().spawn(loc, EnderCrystal.class);
                if (this.entity == null) {
                    FactionEvent.getInstance().getLogger().warning("[DTC] impossible de spawn l'EnderCrystal pour le DTC " + dtc.getName());
                } else {
                    // Empêche l'affichage du feu dès la création
                    this.entity.setFireTicks(0);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (entity != null && entity.isValid()) {
                                entity.setFireTicks(0);
                            }
                        }
                    }.runTaskLater(FactionEvent.getInstance(), 5L);
                }
            }
        } catch (Exception e) {
            FactionEvent.getInstance().getLogger().warning("[DTC] exception lors du spawn de l'EnderCrystal: " + e.getMessage());
            this.entity = null;
        }

        this.damageMap = new HashMap<>();
        FileConfiguration cf = FileManager.getConfig();
        this.max_vie = cf.getDouble("dtc.max_life", 25000.0D);
        this.vie = this.max_vie;
        try { if (cf.contains("dtc.max_duration")) this.duration = cf.getInt("dtc.max_duration"); } catch (Exception ignored) {}

    }

    public void hit(Player player, double damage) {
        // feedback visuel/sonore amélioré
        try {
            if (entity != null && entity.isValid()) {
                // Empêche le feu à chaque hit
                entity.setFireTicks(0);
                Location base = entity.getLocation().clone();
                base.add(0, 1, 0);

                // Effets centraux
                base.getWorld().playEffect(base, Effect.CRIT, 0);
                base.getWorld().playEffect(base, Effect.MAGIC_CRIT, 0);
            }
        } catch (Exception ignored) {}

        Faction faction = FPlayers.getInstance().getByPlayer(player).getFaction();
        if (!faction.isWilderness() && !faction.isSafeZone() && !faction.isWarZone()) {
            double current = damageMap.getOrDefault(faction, 0.0);
            damageMap.put(faction, current + damage);
        }
        vie -= damage;
        if (vie < 1 && vie > 0) vie = 1;

        if (isDead()) {
            vie = 0;
            updateScoreboard();
            grantVictory();
        } else {
            // mise à jour immédiate à chaque hit
            updateScoreboard();
        }
    }

    /**
     * updateScoreboard envoie uniquement l'action bar (pas de scoreboard).
     */
    public void updateScoreboard() {
        try {
            if (FactionEvent.getInstance().getEventOn().getDtcEvent() == null) return;
        } catch (Exception e) { return; }

        if (entity != null && entity.isValid()) {
            entity.setFireTicks(0);
        }

        if (vie <= 0) return;

        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        int current_vie = (int) Math.ceil(vie);
        String actionBar = new StrManager(msg.getString("dtc.life_left_actionbar", "§c{currentlife}§4❤ §f/ §c{maxlife}§4❤"))
                .reCurrentDTCLife(current_vie, (int) Math.ceil(max_vie)).toString();

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                EventManager em = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if (em == null) {
                    em = EventManager.loadFromFile(player);
                    FactionEvent.getInstance().getEventScoreboardOff().put(player, em);
                }
                if (em.isActionbar()) Messages.sendActionBar(player, actionBar);
            } catch (Exception ignored) {}
        }
    }

    public boolean checkTimer() {
        return (int) ((new Date().getTime() - started) / 1000) > duration;
    }

    private Faction getHighestDamageFaction() {
        Faction max_faction = null;
        double max_damage = -1;
        for (Faction faction : damageMap.keySet()) {
            double d = damageMap.get(faction);
            if (d > max_damage) { max_damage = d; max_faction = faction; }
        }
        return max_faction;
    }

    private void grantVictory() {
        Faction max_faction = getHighestDamageFaction();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        if (max_faction != null) {
            double max_damage = damageMap.get(max_faction);
            int max_damage_dealt = max_damage > max_vie ? (int) max_vie : (int) max_damage;
            String win_msg = "§8[§cDTC§8] §7La faction §c" + max_faction.getTag() + "§7 remporte le DTC §8(§7Dégâts: §4" + max_damage_dealt + " ❤§8)";
            if (msg.contains("dtc.prefix") && msg.contains("dtc.win")) {
                win_msg = msg.getString("dtc.prefix") + new StrManager(msg.getString("dtc.win")).reFaction(max_faction.getTag()).reDamageDealtDTC(max_damage_dealt).toString();
            }
            int points = 10;
            FileConfiguration config = FileManager.getConfig();
            try { if (config.contains("dtc.win_points")) { points = config.getInt("dtc.win_points"); if (points < 1) points = 1; } } catch (Exception ignored) {}
            Bukkit.broadcastMessage(win_msg);
            RankingManager.addDTCWins(max_faction);
            RankingManager.addPoints(max_faction, points);
            FactionMessageTitle.sendFactionTitle(max_faction, 20, 40, 20, "§aNexus remporté", "+" + points + " points au classement");
        }
        dtc.stop();
        RankingManager.updateRanking(true);
    }

    public boolean isDead() {
        try { return vie <= 0; } catch (Exception ignored) { return true; }
    }

    public DTC getDtc() { return dtc; }
    public void setDtc(DTC dtc) { this.dtc = dtc; }
    public Entity getEntity() { return entity; }
    public void setEntity(Entity entity) { this.entity = entity; }
}
