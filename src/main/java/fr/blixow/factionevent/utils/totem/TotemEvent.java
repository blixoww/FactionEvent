package fr.blixow.factionevent.utils.totem;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.ScoreBoardAPI;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class TotemEvent {
    private ScoreBoardAPI scoreBoardAPI;
    private final Totem totem;
    private final long started;
    private int duration = 1800;
    private Faction faction;
    private HashMap<Location, Material> blocks;

    public TotemEvent(Totem totem){
        this.totem = totem;
        this.started = new Date().getTime();
        this.faction = null;
        FileConfiguration config = FileManager.getConfig();
        try { if(config.contains("totem.max_duration")){ duration = config.getInt("totem.max_duration"); } } catch (Exception ignored){}
        copyBlocks();
    }

    private boolean endedTime(){
        long now = new Date().getTime();
        long diff = now - started;
        return (diff / 1000) > duration;
    }

    private void grantVictory(Player player){
        FileConfiguration config = FileManager.getConfig();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        Faction fPlayerFaction = FPlayers.getInstance().getByPlayer(player).getFaction();
        String win = new StrManager(msg.getString("totem.win")).rePlayer(player).reFaction(fPlayerFaction.getTag()).reTotem(totem.getName()).toString();
        Bukkit.broadcastMessage(msg.getString("totem.prefix") + win);
        totem.stop();
        if(faction != null && !faction.isWilderness() && !faction.isSafeZone() && !faction.isWarZone()){
            int points = 10;
            try { if(config.contains("totem.win_points")){ points = config.getInt("totem.win_points"); if(points < 1){ points = 1; } } } catch (Exception ignored){}
            RankingManager.addTotemWins(faction);
            RankingManager.addPoints(faction, 10);
            FactionMessageTitle.sendFactionTitle(faction, 20,40, 20,"§aTotem remporté", "+10 points au classement");
        }
        RankingManager.updateRanking(true);
    }

    public void blockDestroyed(Block block, Player player){
        if(!blocks.containsKey(block.getLocation())){ return; }
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction fPlayerFaction = fPlayer.getFaction();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        if(this.faction == null){ this.faction = fPlayerFaction; }
        if(this.faction.equals(fPlayerFaction)){
            // update score
            blocks.remove(block.getLocation());
            if(blocks.isEmpty()){
                grantVictory(player);
                return;
            }
            String left = new StrManager(msg.getString("totem.blocks_left")).reBlocks(blocks.size(), totem.getBlocks().size()).toString();
            FactionMessageTitle.sendFactionActionBar(faction, left);
        } else {
            reset(player, this.faction);
            blockDestroyed(block, player);
        }
        updateScoreboard();
    }

    private void reset(Player player, Faction oldFaction){
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        Faction newFaction = FPlayers.getInstance().getByPlayer(player).getFaction();
        this.faction = newFaction;
        String lost_control_title = msg.getString("totem.lost_control_title");
        String lost_control_subtitle = new StrManager(msg.getString("totem.lost_control_subtitle")).reFaction(newFaction.getTag()).toString();
        String took_control_title = msg.getString("totem.took_control_title");
        String took_control_subtitle = msg.getString("totem.took_control_subtitle");
        FactionMessageTitle.sendFactionTitle(oldFaction, 20, 40, 20, lost_control_title, lost_control_subtitle);
        FactionMessageTitle.sendFactionTitle(newFaction, 20, 40, 20, took_control_title, took_control_subtitle);
        copyBlocks();
        setAllBlocks();
    }

    public void setAllBlocks(){
        blocks.forEach((k, v) -> {
            k.getBlock().setType(v);
            //CEvents.getInstance().getLogger().info("§cBlockType= §7" + k.getBlock().getType() + "\n§7Infos: §7" + k.getBlock().toString());
        });
    }

    public void start(){
        setAllBlocks();
        updateScoreboard();
        FactionMessageTitle.sendPlayersTitle(20,40, 20,"§aTotem en cours", "préparez-vous au combat");
    }

    public void copyBlocks(){
        this.blocks = new HashMap<>();
        if(this.totem.getBlocks() != null){ this.blocks.putAll(totem.getBlocks()); }
    }



    public boolean checkTimer(){
        if(!FactionEvent.getInstance().getEventOn().getTotemEvent().equals(this)){ return true; }
        long now = new Date().getTime();
        long diff = (now - started) / 1000;
        if(diff < duration){
            updateScoreboard();
            return false;
        }
        totem.stop();
        return true;
    }

    public void updateScoreboard(){
        long timeRemaining = duration - ((new Date().getTime() - started) / 1000);
        String timeRemainingString = DateManager.getFormattedTime((int) timeRemaining);
        String left = "§7" + blocks.size() + "§f/§7" + totem.getBlocks().size();
        String factionName = "Aucune";
        if(faction != null && !faction.isWilderness()){ factionName = faction.getTag(); }
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreBoardAPI = new ScoreBoardAPI(board, "§8[§cTotem§8]", true);
        scoreBoardAPI.setDisplayName("§8[§cTOTEM§8]");
        scoreBoardAPI.setLine(8, "");
        scoreBoardAPI.setLine(7, "§c» §eFaction");
        scoreBoardAPI.setLine(6, "§7" + factionName);
        scoreBoardAPI.setLine(5, "");
        scoreBoardAPI.setLine(4, "§c» §eBlocks");
        scoreBoardAPI.setLine(3, left);
        scoreBoardAPI.setLine(2, "");
        scoreBoardAPI.setLine(1, "§c» §eTemps");
        scoreBoardAPI.setLine(0, "§7" + timeRemainingString);
        for(Player player : FactionEvent.getInstance().getEventScoreboardOff().keySet()){
            try {
                EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if(eventManager.isScoreboard()){
                    player.setScoreboard(scoreBoardAPI.getScoreboard());
                    scoreBoardAPI.getObjective().setDisplaySlot(DisplaySlot.SIDEBAR);
                }
                //mapi2.setTotemEventScoreboard(player, this, factionName, left);
            } catch (Exception exception){
                exception.printStackTrace();
            }
        }
    }

    // Getter
    public Totem getTotem() { return totem; }
    public HashMap<Location, Material> getBlocks() { return blocks; }
    public Faction getFaction() { return faction; }
    public long getStarted() { return started; }
    public ScoreBoardAPI getScoreBoardAPI() {
        return scoreBoardAPI;
    }
}
