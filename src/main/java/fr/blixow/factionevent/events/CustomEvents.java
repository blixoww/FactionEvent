package fr.blixow.factionevent.events;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.EventManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.domination.DominationEvent;
import fr.blixow.factionevent.utils.dtc.DTCEvent;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.guess.GuessEvent;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSEvent;
import fr.blixow.factionevent.utils.purge.PurgeEvent;
import fr.blixow.factionevent.utils.totem.TotemEditor;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public class CustomEvents implements Listener {

    /**
     * Sauvegarde de la power FPlayer avant traitement de la mort (anti power-loss pendant LMS).
     * UUID → power sauvegardée
     */
    private final HashMap<UUID, Double> lmsSavedPower = new HashMap<>();

    // KOTH

    @EventHandler
    public void onMoveKoth(PlayerMoveEvent event) {
        // Ignorer les simples rotations (tête) pour réduire le nombre d'appels
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        KOTHEvent kothEvent = FactionEvent.getInstance().getEventOn().getKothEvent();
        if (kothEvent != null) {
            Player player = event.getPlayer();
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
        if (FactionEvent.getInstance().getEventOn().getDominationEvent() != null) {
            FactionEvent.getInstance().getEventOn().getDominationEvent().removePlayer(player);
        }
        FactionEvent.getInstance().getEventScoreboardOff().remove(player);
        lmsSavedPower.remove(player.getUniqueId());
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
     * Empêche que les EnderCrystal du DTC prennent feu (tous les cas d'inflammation)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityCombust(EntityCombustEvent event) {
        cancelCombustIfDTC(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        cancelCombustIfDTC(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityCombustByBlock(EntityCombustByBlockEvent event) {
        cancelCombustIfDTC(event);
    }

    private void cancelCombustIfDTC(EntityCombustEvent event) {
        Entity entity = event.getEntity();
        if (!entity.getType().equals(EntityType.ENDER_CRYSTAL)) return;
        DTCEvent dtcEvent = DTCManager.getDTCEventByEntity(entity);
        if (dtcEvent != null) {
            event.setCancelled(true);
            entity.setFireTicks(-1);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        String format = event.getFormat();

        if (!faction.isWilderness()) {
            // Snapshot de la map pour éviter ConcurrentModificationException depuis le thread async
            Map<Faction, Integer> rankings = new java.util.LinkedHashMap<>(FactionEvent.getInstance().getFactionRankings());
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

    // DOMINATION

    @EventHandler
    public void onMoveDomination(PlayerMoveEvent event) {
        // Ignorer les simples rotations
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        DominationEvent dominationEvent = FactionEvent.getInstance().getEventOn().getDominationEvent();
        if (dominationEvent != null) {
            dominationEvent.updatePlayerMovement(event.getPlayer());
        }
    }

    //LMS

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLMSDeathSavePower(PlayerDeathEvent event) {
        Player player = event.getEntity();
        LMSEvent lmsEvent = FactionEvent.getInstance().getEventOn().getLMSEvent();
        if (lmsEvent == null || !lmsEvent.isParticipant(player)) return;
        // Sauvegarder la power avant que Factions ne la réduise
        try {
            FPlayer fp = FPlayers.getInstance().getByPlayer(player);
            if (fp != null) lmsSavedPower.put(player.getUniqueId(), fp.getPower());
        } catch (Exception ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        LMSEvent lmsEvent = FactionEvent.getInstance().getEventOn().getLMSEvent();
        if (lmsEvent != null && lmsEvent.isParticipant(player)) {
            // Empêcher le drop des items du kit
            event.getDrops().clear();
            event.setDroppedExp(0);
            // Ne pas comptabiliser comme une vraie mort dans les stats Minecraft
            try {
                player.decrementStatistic(org.bukkit.Statistic.DEATHS);
            } catch (Exception ignored) {}
            Player killer = player.getKiller();
            if (killer != null) {
                try {
                    killer.decrementStatistic(org.bukkit.Statistic.PLAYER_KILLS);
                } catch (Exception ignored) {}
            }
            // Déléguer la mort à LMSEvent
            lmsEvent.handlePlayerDeath(player, killer);
        }
        // Domination kill bonus
        DominationEvent dominationEvent = FactionEvent.getInstance().getEventOn().getDominationEvent();
        if (dominationEvent != null) {
            Player killer = player.getKiller();
            dominationEvent.handleKill(killer, player);
        }
        // Purge kill reward
        PurgeEvent purgeEvent = FactionEvent.getInstance().getEventOn().getPurgeEvent();
        if (purgeEvent != null) {
            Player killer = player.getKiller();
            purgeEvent.handleKill(killer, player);
        }
    }

    @EventHandler
    public void onLMSRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Restaurer la power si sauvegardée (anti power-loss LMS)
        if (lmsSavedPower.containsKey(player.getUniqueId())) {
            final double savedPower = lmsSavedPower.remove(player.getUniqueId());
                org.bukkit.Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
                try {
                    FPlayer fp = FPlayers.getInstance().getByPlayer(player);
                    if (fp != null) fp.alterPower(savedPower - fp.getPower());
                } catch (Exception ignored) {}
            }, 2L);
        }

        // Restaurer l'inventaire si le joueur était dans un LMS
        LMSEvent lmsEvent = FactionEvent.getInstance().getEventOn().getLMSEvent();
        if (lmsEvent != null && lmsEvent.isPendingRestore(player.getUniqueId())) {
            lmsEvent.handlePlayerRespawn(player);
        }
    }

    /** Bloque toutes les commandes pour les participants LMS actifs. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLMSCommand(PlayerCommandPreprocessEvent event) {
        LMSEvent lmsEvent = FactionEvent.getInstance().getEventOn().getLMSEvent();
        if (lmsEvent == null || !lmsEvent.isEventActive()) return;
        Player player = event.getPlayer();
        if (!lmsEvent.isParticipant(player)) return;
        event.setCancelled(true);
        player.sendMessage(FileManager.getMessageFileConfiguration().getString("lms.prefix",
            "§8[§cLMS§8]§7 ") + FileManager.getMessageFileConfiguration().getString("lms.commands_blocked",
            "§cLes commandes sont bloquées pendant le LMS !"));
    }

    // PURGE — override des protections Factions sur les portes, trappes et portillons

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractDoorPurge(PlayerInteractEvent event) {
        if (FactionEvent.getInstance().getEventOn().getPurgeEvent() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        // Détection générique par nom de Material — compatible vanilla ET moddé (MCP, etc.)
        String name = block.getType().name().toUpperCase();
        boolean isTrapdoor  = name.contains("TRAPDOOR") || name.contains("TRAP_DOOR");
        boolean isDoor       = name.contains("DOOR") && !isTrapdoor;
        boolean isFenceGate  = name.contains("FENCE_GATE");
        boolean isButton     = name.contains("BUTTON");
        boolean isIron       = name.contains("IRON");

        if (!isDoor && !isTrapdoor && !isFenceGate && !isButton) return;

        // Les boutons fonctionnent toujours (redstone), même si Factions a annulé l'interaction
        if (isButton) {
            if (event.isCancelled()) event.setCancelled(false);
            return;
        }

        // Portes / trappes / portillons en fer : pas d'ouverture directe par clic
        if (isIron) return;

        // Si Factions n'a pas annulé l'interaction, vanilla gère déjà l'ouverture
        if (!event.isCancelled()) return;

        // — Manipulation legacy via getData() / setData() —
        if (isDoor) {
            // Porte double-bloc : trouver le bloc du bas
            Block bottom = block;
            if ((block.getData() & 0x8) != 0) {
                bottom = block.getRelative(BlockFace.DOWN);
            }
            if (bottom.getType() == block.getType()) {
                byte data = bottom.getData();
                bottom.setData((byte) (data ^ 0x4), true);
                block.getWorld().playEffect(block.getLocation(), Effect.DOOR_TOGGLE, 0);
                event.setCancelled(true);
            }
        } else {
            // Trapdoor ou portillon (bloc simple)
            byte data = block.getData();
            block.setData((byte) (data ^ 0x4), true);
            block.getWorld().playEffect(block.getLocation(), Effect.DOOR_TOGGLE, 0);
            event.setCancelled(true);
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

        // Pendant la phase de grâce : aucun dégât entre participants
        if (lms.isPreparation() || lmsEvent.isGracePeriodActive()) {
            if (lmsEvent.isParticipant(target) || lmsEvent.isParticipant(attacker)) {
                event.setCancelled(true);
                return;
            }
        }

        if (lms.isStarted() && !lmsEvent.isGracePeriodActive()) {
            // Seuls les participants peuvent se frapper entre eux
            if (!lmsEvent.isParticipant(target) || !lmsEvent.isParticipant(attacker)) return;
            // On autorise les dégâts même entre alliés / trêves — ignorer les relations
            event.setCancelled(false);
        }
    }

    // GUESS

    /**
     * Empêche l'ouverture des coffres de loot Domination par des joueurs
     * n'appartenant pas à la faction gagnante.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // Récupère le holder de l'inventaire et vérifie que c'est bien un coffre
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;
        if (!(holder instanceof org.bukkit.block.Chest)) return;

        Chest chest = (org.bukkit.block.Chest) holder;
        Location blockLoc = chest.getLocation();
        if (blockLoc == null || blockLoc.getWorld() == null) return;

        // Normalise la location (coordonnées de bloc)
        Location key = new Location(blockLoc.getWorld(), blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());

        Faction allowedFaction = FactionEvent.getInstance().getDominationLootChests().get(key);
        if (allowedFaction == null) return; // Ce coffre n'est pas un coffre de Domination

        FPlayer fp = FPlayers.getInstance().getByPlayer(player);
        if (fp == null || !fp.getFaction().equals(allowedFaction)) {
            event.setCancelled(true);
            player.sendMessage("§8[§cDOMINATION§8]§c Ce coffre appartient à la faction §7"
                + allowedFaction.getTag() + "§c !");
        }
    }

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
