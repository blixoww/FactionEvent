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
import fr.blixow.factionevent.utils.guess.GuessEvent;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSEvent;
import fr.blixow.factionevent.utils.totem.TotemEditor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

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
        if (FactionEvent.getInstance().getEventOn().getLMSEvent() != null) {
            FactionEvent.getInstance().getEventOn().getLMSEvent().handlePlayerQuit(player);
        }
        FactionEvent.getInstance().getEventScoreboardOff().remove(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FactionEvent.getInstance().getEventScoreboardOff().put(player, EventManager.loadFromFile(player));
        // Le scoreboard sera réappliqué automatiquement lors du prochain updateScoreboard() (toutes les 5 ticks)
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (FactionEvent.getInstance().getEventOn().getTotemEvent() != null) {
            Location blockLoc = new Location(event.getBlock().getWorld(),
                    event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ());
            if (FactionEvent.getInstance().getEventOn().getTotemEvent().getBlocks().containsKey(blockLoc)) {
                // Annuler l'event (empêche le drop du bloc) et détruire manuellement
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                FactionEvent.getInstance().getEventOn().getTotemEvent().blockDestroyed(event.getBlock(), player);
            }
        }
    }

    // DTC

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHitEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getType().equals(EntityType.ENDER_CRYSTAL)) return;

        DTCEvent dtcEvent = DTCManager.getDTCEventByEntity(entity);
        if (dtcEvent == null) {
            return;
        }

        // Annuler le dégât natif (évite l'explosion de l'EnderCrystal)
        event.setCancelled(true);

        Entity damager = event.getDamager();
        Player player = null;

        if (damager instanceof Player) {
            player = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                player = (Player) projectile.getShooter();
            }
        }

        if (player != null) {
            double damage = event.getDamage();
            if (damage <= 0) damage = 1;
            dtcEvent.hit(player, damage);
        }
    }

    /**
     * Empêche l'EnderCrystal du DTC d'exploser ou d'être détruit par des dégâts indirects
     * (explosion d'un autre EnderCrystal, TNT, etc.)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnderCrystalDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getType().equals(EntityType.ENDER_CRYSTAL)) return;
        DTCEvent dtcEvent = DTCManager.getDTCEventByEntity(entity);
        if (dtcEvent != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Empêche l'explosion générée par l'EnderCrystal du DTC.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnderCrystalExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getType().equals(EntityType.ENDER_CRYSTAL)) return;
        DTCEvent dtcEvent = DTCManager.getDTCEventByEntity(entity);
        if (dtcEvent != null) {
            event.setCancelled(true);
        }

    }

    /**
     * Empêche que les EnderCrystal du DTC prennent feu (ex: flèches flame)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityCombust(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getType().equals(EntityType.ENDER_CRYSTAL)) return;
        DTCEvent dtcEvent = DTCManager.getDTCEventByEntity(entity);
        if (dtcEvent != null) {
            event.setCancelled(true);
            // Planifier une tâche pour s'assurer que le feu est éteint après le traitement de l'événement.
            Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
                if (entity.isValid()) {
                    entity.setFireTicks(0);
                }
            }, 1L);
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

    //LMS

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        LMSEvent lmsEvent = FactionEvent.getInstance().getEventOn().getLMSEvent();
        if (lmsEvent != null && lmsEvent.isParticipant(player)) {
            lmsEvent.handlePlayerDeath(player);
        }
    }


    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        LMSEvent lmsEvent = FactionEvent.getInstance().getEventOn().getLMSEvent();
        if (lmsEvent == null) return;

        LMS lms = lmsEvent.getLMS();
        if (lms == null) return;

        Player target = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Vérifie si les deux joueurs sont dans les participants inscrits
        if (!lmsEvent.isParticipant(target) || !lmsEvent.isParticipant(attacker)) return;

        if (lms.isPreparation()) {
            // Annule les dégâts pendant la phase de préparation
            event.setCancelled(true);
        } else if (lms.isStarted()) {
            // Autorise les combats entre alliés et trêve dans le LMS
            FPlayer fAttacker = FPlayers.getInstance().getByPlayer(attacker);
            FPlayer fTarget = FPlayers.getInstance().getByPlayer(target);
            Faction factionAttacker = fAttacker.getFaction();
            Faction factionTarget = fTarget.getFaction();

            if (factionAttacker != null && factionTarget != null) {
                if (factionAttacker.getRelationTo(factionTarget).isAlly() ||
                        factionAttacker.getRelationTo(factionTarget).isTruce()) {
                    event.setCancelled(false);
                }
            }
        }
    }

    // GUESS

    @EventHandler
    public void onPlayerGuess(PlayerCommandPreprocessEvent event) {
        GuessEvent currentEvent = FactionEvent.getInstance().getEventOn().getGuessEvent();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("guess.prefix");
        if (currentEvent != null) {
            Player player = event.getPlayer();

            if (!event.getMessage().startsWith("/answer")) {
                return;
            }

            String[] commandParts = event.getMessage().split(" ");
            if (commandParts.length < 2) {
                player.sendMessage(prefix + msg.getString("guess.answer_usage"));
                event.setCancelled(true);
                return;
            } else {
                String playerGuess = commandParts[1];
                currentEvent.checkGuess(player, playerGuess);
                event.setCancelled(true);
            }
            event.setCancelled(true);
        }
    }
}
