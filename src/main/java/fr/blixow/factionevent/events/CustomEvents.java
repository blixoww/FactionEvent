package fr.blixow.factionevent.events;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.EventManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTCEvent;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.totem.TotemEditor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class CustomEvents implements Listener {

    // KOTH

    @EventHandler
    public void onMoveKoth(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (FactionEvent.getInstance().getEventOn().getKothEvent() != null) {
            KOTHEvent kothEvent = FactionEvent.getInstance().getEventOn().getKothEvent();
            if (KOTHManager.isInKOTH(player, kothEvent.getKoth())) {
                kothEvent.addPlayer(player);
            } else {
                kothEvent.removePlayer(player);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (FactionEvent.getInstance().getEventOn().getKothEvent() != null) {
            KOTHEvent kothEvent = FactionEvent.getInstance().getEventOn().getKothEvent();
            if (kothEvent.getPlayersInKOTH().containsKey(player)) {
                kothEvent.removePlayer(player);
            }
        }
        FactionEvent.getInstance().getEventScoreboardOff().remove(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FactionEvent.getInstance().getEventScoreboardOff().put(player, EventManager.loadFromFile(player));
    }

    // TOTEM

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (FactionEvent.getInstance().getPlayerTotemEditorHashMap().containsKey(player)) {
            FileConfiguration fc = FileManager.getMessageFileConfiguration();
            TotemEditor totemEditor = FactionEvent.getInstance().getPlayerTotemEditorHashMap().get(player);
            String str = new StrManager(fc.getString("totem.block_placed")).rePlayer(player).reLocation(event.getBlock().getLocation()).toString();
            player.sendMessage(fc.getString("totem.prefix") + str);
            totemEditor.addBlocks(event.getBlock());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (FactionEvent.getInstance().getEventOn().getTotemEvent() != null) {
            if (FactionEvent.getInstance().getEventOn().getTotemEvent().getBlocks().containsKey(event.getBlock().getLocation())) {
                ItemStack hand = player.getInventory().getItemInHand();
                if (hand.getType().toString().contains("SWORD") || player.isOp() || player.getGameMode() == GameMode.CREATIVE) {
                    FactionEvent.getInstance().getEventOn().getTotemEvent().blockDestroyed(event.getBlock(), player);
                } else {
                    for (Location location : FactionEvent.getInstance().getEventOn().getTotemEvent().getBlocks().keySet()) {
                        if (location.getBlock() != null) {
                            event.setCancelled(true);
                            FileConfiguration fc = FileManager.getMessageFileConfiguration();
                            String str = new StrManager(fc.getString("totem.sword")).rePlayer(player).reLocation(event.getBlock().getLocation()).toString();
                            player.sendMessage(fc.getString("totem.prefix") + str);
                            break;
                        }
                    }
                }
            }
        }
    }

    // DTC
    @EventHandler
    public void onHitEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();
        if (damager instanceof Player) {
            Player player = (Player) damager;
            if (entity.getType().equals(EntityType.ENDER_CRYSTAL)) {
                DTCEvent dtcEvent = DTCManager.getDTCEventByEntity(entity);
                if (dtcEvent != null) {
                    event.setCancelled(true);
                    dtcEvent.hit(player, event.getFinalDamage());
                }
            }
        }

    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        String format = event.getFormat();

        if (!faction.isWilderness()) {
            Map<Faction, Integer> rankings = FactionEvent.getInstance().getFactionRankings();
            int factionRank = 1;
            for (Faction faction1 : rankings.keySet()) {
                if (faction1.equals(faction)) {
                    String prefix = FileManager.getMessageFileConfiguration().getString("chat_format.faction_rank_prefix");
                    String newFormat = prefix.replace("%faction_rank%", String.valueOf(factionRank)) + " [FACTION] " + format;
                    event.setFormat(newFormat);
                    break;
                }
                factionRank++;
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (FactionEvent.getInstance().getEventOn().getLMSEvent() != null) {
            FactionEvent.getInstance().getEventOn().getLMSEvent().handlePlayerDeath(player);
        }
    }

}
