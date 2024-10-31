package fr.blixow.factionevent.utils.dtc;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.utils.ScoreBoardAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.beans.EventHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DTCEvent {

    private DTC dtc;
    private final long started;
    private Entity entity;
    private double vie;
    private int duration = 1800;
    private final double max_vie;
    private final HashMap<Faction, Double> damageMap;
    private ScoreBoardAPI scoreBoardAPI;
    private int nombre_coup;
    private int check_hit = 1;

    public DTCEvent(DTC dtc) {
        this.dtc = dtc;
        this.started = new Date().getTime();
        this.entity = this.dtc.getLocation().getWorld().spawn(this.dtc.getLocation(), EnderCrystal.class);
        this.damageMap = new HashMap<>();
        this.nombre_coup = 0;
        FileConfiguration cf = FileManager.getConfig();

        this.max_vie = cf.getDouble("dtc.max_life", 50000.0D);
        this.vie = this.max_vie;
        try {
            if (cf.contains("dtc.max_duration")) {
                this.duration = cf.getInt("dtc.max_duration");
            }
        } catch (Exception ignored) {
        }
        try {
            if (cf.contains("dtc.check_hit")) {
                this.check_hit = cf.getInt("dtc.check_hit");
            }
        } catch (Exception ignored) {
        }
        if (check_hit < 1) {
            check_hit = 1;
        }
    }

    public void hit(Player player, double damage) {
        Faction faction = FPlayers.getInstance().getByPlayer(player).getFaction();
        FileConfiguration fc = FileManager.getMessageFileConfiguration();
        if (!faction.isWilderness() && !faction.isSafeZone() && !faction.isWarZone()) {
            double current_faction_damage = 0;
            if (damageMap.containsKey(faction)) {
                current_faction_damage = damageMap.get(faction);
            }
            current_faction_damage += damage;
            damageMap.put(faction, current_faction_damage);
        }
        vie -= damage;
        if (vie < 1 && vie > 0) {
            vie = 1;
        }
        int current_vie = vie < 0 ? 0 : (int) vie;

        String vie_restante_actionbar = "§8[§cDTC§8] §7Vie du coeur §8: §7" + current_vie + "§f/§7" + (int) max_vie;
        if (fc.contains("dtc.prefix") && fc.contains("dtc.life_left_actionbar")) {
            vie_restante_actionbar = fc.getString("dtc.prefix") + new StrManager(fc.getString("dtc.life_left_actionbar")).reCurrentDTCLife(current_vie, (int) max_vie).toString();
        }
        for (Faction faction1 : damageMap.keySet()) {
            FactionMessageTitle.sendFactionActionBar(faction1, vie_restante_actionbar);
        }
        if (isDead()) {
            vie = 0;
            grantVictory();
        } else {
            nombre_coup++;

            if (nombre_coup % check_hit == 0) {
                updateScoreboard();
            }
        }
    }

    public void updateScoreboard() {
        if (vie > 0) {
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            Faction highest_faction = getHighestDamageFaction();
            long timeRemaining = duration - ((new Date().getTime() - started) / 1000);
            String timeRemainingString = DateManager.getFormattedTime((int) timeRemaining);
            String left = "§c" + (int) Math.ceil(vie) + "§4❤ §f/ §c" + (int) Math.ceil(max_vie) + "§4❤";
            String factionName = "Aucune";
            if (highest_faction != null && !highest_faction.isWilderness() && !highest_faction.isSafeZone() && !highest_faction.isWarZone()) {
                factionName = highest_faction.getTag();
            }
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            scoreBoardAPI = new ScoreBoardAPI(board, "DTC", true);
            try {
                String title = msg.getString("dtc.scoreboard.title");
                List<String> stringList = msg.getStringList("dtc.scoreboard.lines");
                int size = stringList.size();
                scoreBoardAPI.setDisplayName(title);
                for (String line : stringList) {
                    String line2 = new StrManager(line).reCurrentDTCLife((int) Math.ceil(vie), (int) Math.ceil(max_vie)).reFaction(factionName).reTime(timeRemainingString).toString();
                    scoreBoardAPI.setLine(size, line2);
                    size--;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                scoreBoardAPI.setDisplayName("§8[§cDTC§8]");
                scoreBoardAPI.setLine(8, "");
                scoreBoardAPI.setLine(7, "§c» §eFaction");
                scoreBoardAPI.setLine(6, "§7" + factionName);
                scoreBoardAPI.setLine(5, "");
                scoreBoardAPI.setLine(4, "§c» §eVie");
                scoreBoardAPI.setLine(3, left);
                scoreBoardAPI.setLine(2, "");
                scoreBoardAPI.setLine(1, "§c» §eTemps");
                scoreBoardAPI.setLine(0, "§7" + timeRemainingString);
            }
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
        } else {
            scoreBoardAPI.getObjective().unregister();
        }

    }

    public boolean checkTimer() {
        long now = new Date().getTime();
        int diff = (int) ((now - started) / 1000);
        return diff > duration;
    }

    private Faction getHighestDamageFaction() {
        Faction max_faction = null;
        double max_damage = -1;
        double faction_damage;
        for (Faction faction : damageMap.keySet()) {
            faction_damage = damageMap.get(faction);
            if (faction_damage > max_damage) {
                max_faction = faction;
                max_damage = faction_damage;
            }
        }
        return max_faction;
    }

    private void grantVictory() {
        Faction max_faction = getHighestDamageFaction();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        if (max_faction != null) {
            double max_damage = damageMap.get(max_faction);
            int max_damage_dealt = max_damage > max_vie ? (int) max_vie : (int) max_damage;
            String win_msg = "§8[§cDTC-2§8] §7La faction §c" + max_faction.getTag() + "§7 remporte le DTC §8(§7Dégâts: §4" + max_damage_dealt + " ❤§8)";
            if (msg.contains("dtc.prefix") && msg.contains("dtc.win")) {
                win_msg = msg.getString("dtc.prefix") + new StrManager(msg.getString("dtc.win")).reFaction(max_faction.getTag()).reDamageDealtDTC(max_damage_dealt).toString();
            }
            int points = 10;
            FileConfiguration config = FileManager.getConfig();
            try {
                if (config.contains("dtc.win_points")) {
                    points = config.getInt("dtc.win_points");
                    if (points < 1) {
                        points = 1;
                    }
                }
            } catch (Exception ignored) {
            }
            Bukkit.broadcastMessage(win_msg);
            RankingManager.addDTCWins(max_faction);
            RankingManager.addPoints(max_faction, points);
            FactionMessageTitle.sendFactionTitle(max_faction, 20, 40, 20, "§aNexus remporté", "+10 points au classement");

        }
        dtc.stop();
        RankingManager.updateRanking(true);
    }

    public boolean isDead() {
        try {
            return vie <= 0;
        } catch (Exception ignored) {
            return true;
        }

    }

    public DTC getDtc() {
        return dtc;
    }

    public void setDtc(DTC dtc) {
        this.dtc = dtc;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }


    public ScoreBoardAPI getScoreBoardAPI() {
        return scoreBoardAPI;
    }
}
