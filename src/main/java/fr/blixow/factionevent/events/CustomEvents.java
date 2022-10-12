package fr.blixow.factionevent.events;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.EventManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTCEvent;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.totem.TotemEditor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class CustomEvents implements Listener {

    // KOTH

    @EventHandler
    public void onMoveKoth(PlayerMoveEvent event){
        Player player = event.getPlayer();
        if(FactionEvent.getInstance().getEventOn().getKothEvent() != null){
            KOTHEvent kothEvent = FactionEvent.getInstance().getEventOn().getKothEvent();
            if(KOTHManager.isInKOTH(player, kothEvent.getKoth())){ kothEvent.addPlayer(player); }
            else { kothEvent.removePlayer(player); }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();
        if(FactionEvent.getInstance().getEventOn().getKothEvent() != null){
            KOTHEvent kothEvent = FactionEvent.getInstance().getEventOn().getKothEvent();
            if(kothEvent.getPlayersInKOTH().containsKey(player)){ kothEvent.removePlayer(player); }
        }
        FactionEvent.getInstance().getEventScoreboardOff().remove(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        FactionEvent.getInstance().getEventScoreboardOff().put(player, EventManager.loadFromFile(player));
    }

    // TOTEM

    @EventHandler
    public void onPlace(BlockPlaceEvent event){
        Player player = event.getPlayer();
        if(FactionEvent.getInstance().getPlayerTotemEditorHashMap().containsKey(player)){
            FileConfiguration fc = FileManager.getMessageFileConfiguration();
            TotemEditor totemEditor = FactionEvent.getInstance().getPlayerTotemEditorHashMap().get(player);
            String str = new StrManager(fc.getString("totem.block_placed")).rePlayer(player).reLocation(event.getBlock().getLocation()).toString();
            player.sendMessage(fc.getString("totem.prefix") + str);
            totemEditor.addBlocks(event.getBlock());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event){
        Player player = event.getPlayer();
        if(FactionEvent.getInstance().getEventOn().getTotemEvent() != null){
            FactionEvent.getInstance().getEventOn().getTotemEvent().blockDestroyed(event.getBlock(), player);
        }
    }

    // DTC
    @EventHandler
    public void onHitEntity(EntityDamageByEntityEvent event){
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();
        if(damager instanceof Player){
            Player player = (Player) damager;
            if(entity.getType().equals(EntityType.ENDER_CRYSTAL)){
                DTCEvent dtcEvent = DTCManager.getDTCEventByEntity(entity);
                if(dtcEvent != null){ event.setCancelled(true); dtcEvent.hit(player, event.getFinalDamage()); }
            }
        }

    }

}
