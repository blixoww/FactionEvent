package fr.blixow.factionevent.utils.domination;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.Messages;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;

import java.util.*;

public class DominationEvent {

    // ── Zone multiplier ────────────────────────────────────────────────────
    private enum ZoneMultiplier {
        STAGNANT("§c×0.5 §8(stagnante)"),
        NORMAL("§7×1 §8(normale)"),
        HOT("§6×2 §8(disputée)");

        final String display;
        ZoneMultiplier(String d) { this.display = d; }
    }

    // ── Zone runtime state ─────────────────────────────────────────────────
    static class ZoneState {
        Faction ownerFaction;
        Faction contestFaction;
        int contestProgress;
        long lastBroadcastMs;
        // Multiplier system
        ZoneMultiplier multiplier;
        long ownerSinceMs;      // when current owner last captured it (uncontested)
        long lastContestedMs;   // when last actively contested
        int scoreSkipCounter;   // stagnant: award 1pt every other tick (= ×0.5)

        ZoneState() {
            ownerFaction = null;
            contestFaction = null;
            contestProgress = 0;
            lastBroadcastMs = 0;
            multiplier = ZoneMultiplier.NORMAL;
            ownerSinceMs = 0;
            lastContestedMs = 0;
            scoreSkipCounter = 0;
        }
    }

    // ── Core fields ────────────────────────────────────────────────────────
    private final Domination domination;
    private final Map<String, ZoneState> zoneStates;
    private final Map<String, Integer> factionScores;
    private final Set<UUID> participants;
    private final long startTime;
    private int duration;
    private int scoreToWin;
    private boolean ended;

    // Tick counters (base = 1 second)
    private int scoreTick;
    private int snowballTick;

    // Anti-snowball tracking
    private String dominantFactionId;
    private long dominantSince;

    // AFK detection
    private final Map<UUID, int[]> lastBlockPos;
    private final Map<UUID, Long> lastMoveTime;

    // Config values
    private final int captureBase;
    private final int capturePerExtra;
    private final int captureDecay;
    private final int killCaptureBonus;
    private final int afkSeconds;
    private final int snowballInterval;
    private final int snowballDuration;
    private final int snowballTrigger;
    private final int stagnantSeconds;
    private final int hotSeconds;

    // Messages
    private final FileConfiguration msg;
    private final String prefix;

    public DominationEvent(Domination domination) {
        this.domination = domination;
        this.startTime = System.currentTimeMillis();
        this.zoneStates = new LinkedHashMap<>();
        this.factionScores = new LinkedHashMap<>();
        this.participants = new HashSet<>();
        this.lastBlockPos = new HashMap<>();
        this.lastMoveTime = new HashMap<>();
        this.ended = false;
        this.scoreTick = 0;
        this.snowballTick = 0;
        this.dominantFactionId = null;
        this.dominantSince = 0;

        this.msg = FileManager.getMessageFileConfiguration();
        this.prefix = msg.getString("domination.prefix", "§8[§cDOMINATION§8]§7 ");

        FileConfiguration config = FileManager.getConfig();
        this.duration         = config.getInt("domination.max_duration", 1800);
        this.scoreToWin       = config.getInt("domination.score_to_win", 150);
        this.captureBase      = config.getInt("domination.capture_base", 5);
        this.capturePerExtra  = config.getInt("domination.capture_per_extra", 2);
        this.captureDecay     = config.getInt("domination.capture_decay", 3);
        this.killCaptureBonus = config.getInt("domination.kill_capture_bonus", 10);
        this.afkSeconds       = config.getInt("domination.afk_seconds", 30);
        this.snowballInterval = config.getInt("domination.snowball_interval", 60);
        this.snowballDuration = config.getInt("domination.snowball_duration", 25);
        this.snowballTrigger  = config.getInt("domination.snowball_trigger", 60);
        this.stagnantSeconds  = config.getInt("domination.zone_stagnant_seconds", 120);
        this.hotSeconds       = config.getInt("domination.zone_hot_seconds", 30);

        for (DominationZone zone : domination.getActiveZones()) {
            zoneStates.put(zone.getName(), new ZoneState());
        }
    }

    // ── Called every second ────────────────────────────────────────────────
    public void updateScoreboard() {
        updateCaptures();

        scoreTick++;
        if (scoreTick >= 5) {
            addScoreForControlledZones();
            scoreTick = 0;
        }

        snowballTick++;
        if (snowballTick >= snowballInterval) {
            applyAntiSnowball();
            snowballTick = 0;
        }

        String globalBar = buildGlobalActionBar();
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                EventManager em = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if (em == null) {
                    em = EventManager.loadFromFile(player);
                    FactionEvent.getInstance().getEventScoreboardOff().put(player, em);
                }
                if (em.isActionbar()) {
                    Messages.sendActionBar(player, buildPlayerActionBar(player, globalBar));
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Timer check ───────────────────────────────────────────────────────
    public boolean checkTimer() {
        if (ended) return true;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsed >= duration) { grantVictory(); return true; }
        if (scoreToWin > 0) {
            for (int score : factionScores.values()) {
                if (score >= scoreToWin) { grantVictory(); return true; }
            }
        }
        return false;
    }

    // ── Kill handling ─────────────────────────────────────────────────────
    public void handleKill(Player killer, Player victim) {
        if (killer == null || victim == null) return;
        FPlayer fKiller = FPlayers.getInstance().getByPlayer(killer);
        FPlayer fVictim = FPlayers.getInstance().getByPlayer(victim);
        if (fKiller == null || fVictim == null) return;
        Faction killerFaction = fKiller.getFaction();
        Faction victimFaction = fVictim.getFaction();
        if (killerFaction.isWilderness() && victimFaction.isWilderness()) return;

        for (DominationZone zone : domination.getActiveZones()) {
            if (!zone.isPlayerInZone(victim)) continue;
            ZoneState state = zoneStates.get(zone.getName());
            if (state == null) continue;

            if (!killerFaction.isWilderness()) {
                if (state.contestFaction != null && state.contestFaction.equals(killerFaction)) {
                    state.contestProgress = Math.min(100, state.contestProgress + killCaptureBonus);
                } else if (state.contestFaction == null && !killerFaction.equals(state.ownerFaction)) {
                    state.contestFaction = killerFaction;
                    state.contestProgress = Math.min(100, killCaptureBonus);
                }
                factionScores.merge(killerFaction.getId(), 1, Integer::sum);
            }
            if (!victimFaction.isWilderness() && state.contestFaction != null
                    && state.contestFaction.equals(victimFaction)) {
                state.contestProgress = Math.max(0, state.contestProgress - killCaptureBonus);
                if (state.contestProgress == 0) state.contestFaction = null;
            }
            break;
        }
    }

    // ── AFK / movement tracking ───────────────────────────────────────────
    public void updatePlayerMovement(Player player) {
        int bx = player.getLocation().getBlockX();
        int by = player.getLocation().getBlockY();
        int bz = player.getLocation().getBlockZ();
        int[] prev = lastBlockPos.get(player.getUniqueId());
        if (prev == null || prev[0] != bx || prev[1] != by || prev[2] != bz) {
            lastMoveTime.put(player.getUniqueId(), System.currentTimeMillis());
            lastBlockPos.put(player.getUniqueId(), new int[]{bx, by, bz});
        }
    }

    public void removePlayer(Player player) {
        lastBlockPos.remove(player.getUniqueId());
        lastMoveTime.remove(player.getUniqueId());
    }

    public boolean isEnded() { return ended; }
    public Domination getDomination() { return domination; }
    public Map<String, Integer> getFactionScores() { return factionScores; }
    public Map<String, ZoneState> getZoneStates() { return zoneStates; }

    // ═══════════════════════════════════════════════════════════════════════
    // CAPTURE LOGIC
    // ═══════════════════════════════════════════════════════════════════════

    private boolean isAFK(Player player) {
        Long last = lastMoveTime.get(player.getUniqueId());
        if (last == null) return false;
        return (System.currentTimeMillis() - last) > afkSeconds * 1000L;
    }

    private void updateCaptures() {
        long now = System.currentTimeMillis();
        for (DominationZone zone : domination.getActiveZones()) {
            ZoneState state = zoneStates.get(zone.getName());
            if (state == null) continue;

            // Count non-AFK players per faction in zone
            Map<String, Integer> countByFaction = new LinkedHashMap<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!zone.isPlayerInZone(p)) continue;
                if (isAFK(p)) continue;
                FPlayer fp = FPlayers.getInstance().getByPlayer(p);
                if (fp == null || fp.getFaction().isWilderness()) continue;
                participants.add(p.getUniqueId());
                countByFaction.merge(fp.getFaction().getId(), 1, Integer::sum);
            }

            // Strict majority detection
            String dominantId = null;
            int dominantCount = 0;
            int totalCount = 0;
            for (Map.Entry<String, Integer> e : countByFaction.entrySet()) {
                totalCount += e.getValue();
                if (e.getValue() > dominantCount) {
                    dominantCount = e.getValue();
                    dominantId = e.getKey();
                }
            }
            if (dominantId != null && dominantCount <= totalCount - dominantCount) {
                dominantId = null; // tied
            }
            int advantage = dominantCount - (totalCount - dominantCount);

            if (dominantId == null && totalCount > 1) {
                // Contested
                state.lastContestedMs = now;
                if (state.contestFaction != null) {
                    state.contestProgress = Math.max(0, state.contestProgress - captureDecay);
                    if (state.contestProgress == 0) state.contestFaction = null;
                    broadcastIfNeeded(zone, state, null, "domination.zone_contested", null, 8000);
                }
            } else if (totalCount == 0 || dominantId == null) {
                // Empty: decay
                if (state.contestFaction != null) {
                    state.contestProgress = Math.max(0, state.contestProgress - captureDecay);
                    if (state.contestProgress == 0) state.contestFaction = null;
                }
            } else {
                Faction dominant = getFactionById(dominantId);
                if (dominant == null) continue;

                // Owner defending uncontested: nothing to do
                if (dominant.equals(state.ownerFaction) && state.contestFaction == null) {
                    updateZoneMultiplier(zone, state, now);
                    continue;
                }

                if (dominant.equals(state.contestFaction)) {
                    int rate = captureBase + (capturePerExtra * Math.max(0, advantage - 1));
                    int old = state.contestProgress;
                    state.contestProgress = Math.min(100, state.contestProgress + rate);

                    if (old < 50 && state.contestProgress >= 50) {
                        broadcastIfNeeded(zone, state, null, "domination.zone_capturing", dominant, 0);
                    }
                    if (state.contestProgress >= 100) {
                        Faction prev = state.ownerFaction;
                        state.ownerFaction = dominant;
                        state.contestFaction = null;
                        state.contestProgress = 0;
                        state.ownerSinceMs = now;
                        onZoneCaptured(zone, dominant, prev);
                    }
                } else {
                    // New faction challenging
                    if (state.contestFaction == null) {
                        state.contestFaction = dominant;
                        state.contestProgress = captureBase;
                        state.lastContestedMs = now;
                        broadcastIfNeeded(zone, state, null, "domination.zone_capturing", dominant, 0);
                    } else {
                        // Push back current contester
                        state.lastContestedMs = now;
                        state.contestProgress = Math.max(0, state.contestProgress - captureDecay);
                        if (state.contestProgress == 0) {
                            state.contestFaction = dominant;
                        }
                    }
                }
            }
            updateZoneMultiplier(zone, state, now);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DYNAMIC ZONE MULTIPLIER
    // ═══════════════════════════════════════════════════════════════════════

    private void updateZoneMultiplier(DominationZone zone, ZoneState state, long now) {
        ZoneMultiplier prev = state.multiplier;
        ZoneMultiplier next;

        boolean isHot = state.contestFaction != null
            || (state.lastContestedMs > 0 && (now - state.lastContestedMs) < hotSeconds * 1000L);
        boolean isStagnant = !isHot
            && state.ownerFaction != null
            && state.ownerSinceMs > 0
            && (now - state.ownerSinceMs) > stagnantSeconds * 1000L;

        if (isHot) next = ZoneMultiplier.HOT;
        else if (isStagnant) next = ZoneMultiplier.STAGNANT;
        else next = ZoneMultiplier.NORMAL;

        if (next != prev) {
            state.multiplier = next;
            announceMultiplierChange(zone, state, next);
        }
    }

    private void announceMultiplierChange(DominationZone zone, ZoneState state, ZoneMultiplier to) {
        long now = System.currentTimeMillis();
        if (now - state.lastBroadcastMs < 3000) return;
        state.lastBroadcastMs = now;

        String ownerStr = state.ownerFaction != null ? "§c" + state.ownerFaction.getTag() : "§7Neutre";
        String message;
        switch (to) {
            case HOT:
                message = "§6⚡ §eZone §c" + zone.getName() + " §e" + to.display
                    + " §8— §eLes points sont §6§ldoublés §ependant la dispute !";
                break;
            case STAGNANT:
                message = "§7💤 §8Zone §c" + zone.getName() + " §8(" + ownerStr + "§8) " + to.display
                    + " §8— §7Capturez-la pour regagner des points normaux.";
                break;
            default:
                message = "§f⚡ §7Zone §c" + zone.getName() + " §7de retour à " + to.display + " §8.";
                break;
        }
        Bukkit.broadcastMessage(prefix + message);
    }

    private void onZoneCaptured(DominationZone zone, Faction newOwner, Faction previousOwner) {
        String capturedMsg = "\n§8§m-----------------------------------------------------\n"
            + "§r §8< §6§lDOMINATION §8> §8§m-----------------------------------------------------\n"
            + "§7🏁 §c" + newOwner.getTag() + " §7contrôle la zone §c" + zone.getName() + " §7!\n"
            + "§8§m-----------------------------------------------------";
        Bukkit.broadcastMessage(capturedMsg);
        FactionMessageTitle.sendPlayersTitle(5, 30, 10, "§a🏁 Zone capturée !", "§c" + newOwner.getTag() + " §7contrôle §c" + zone.getName());
        playSoundAll(Sound.LEVEL_UP);

        if (previousOwner != null && !previousOwner.isWilderness()) {
            String lostMsg = "\n§8§m-----------------------------------------------------\n"
                + "§r §8< §6§lDOMINATION §8> §8§m-----------------------------------------------------\n"
                + "§7❌ §c" + previousOwner.getTag() + " §7perd la zone §c" + zone.getName() + " §7!\n"
                + "§8§m-----------------------------------------------------";
            Bukkit.broadcastMessage(lostMsg);
            FactionMessageTitle.sendFactionTitle(previousOwner, 5, 30, 10,
                "§c❌ Zone perdue !", "§7La zone §c" + zone.getName() + " §7a changé de mains");
        }

        ZoneState state = zoneStates.get(zone.getName());
        if (state != null) state.lastBroadcastMs = System.currentTimeMillis();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SCORE
    // ═══════════════════════════════════════════════════════════════════════

    private void addScoreForControlledZones() {
        for (DominationZone zone : domination.getActiveZones()) {
            ZoneState state = zoneStates.get(zone.getName());
            if (state == null || state.ownerFaction == null) continue;
            String fid = state.ownerFaction.getId();
            switch (state.multiplier) {
                case HOT:
                    // ×2 : 2 points per tick
                    factionScores.merge(fid, 2, Integer::sum);
                    break;
                case NORMAL:
                    // ×1 : 1 point per tick
                    factionScores.merge(fid, 1, Integer::sum);
                    break;
                case STAGNANT:
                    // ×0.5 : 1 point every other tick
                    state.scoreSkipCounter++;
                    if (state.scoreSkipCounter % 2 == 0) {
                        factionScores.merge(fid, 1, Integer::sum);
                    }
                    break;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ANTI-SNOWBALL
    // ═══════════════════════════════════════════════════════════════════════

    private void applyAntiSnowball() {
        Map<String, Integer> zoneCount = new HashMap<>();
        int totalZones = domination.getActiveZones().size();
        if (totalZones == 0) return;

        for (DominationZone zone : domination.getActiveZones()) {
            ZoneState state = zoneStates.get(zone.getName());
            if (state == null || state.ownerFaction == null) continue;
            zoneCount.merge(state.ownerFaction.getId(), 1, Integer::sum);
        }

        String newDominantId = null;
        for (Map.Entry<String, Integer> e : zoneCount.entrySet()) {
            if (e.getValue() == totalZones) { newDominantId = e.getKey(); break; }
        }

        if (newDominantId != null && newDominantId.equals(dominantFactionId)) {
            long dominantFor = (System.currentTimeMillis() - dominantSince) / 1000;
            if (dominantFor >= snowballTrigger) {
                applyEffects(getFactionById(dominantFactionId), zoneCount);
            }
        } else {
            dominantFactionId = newDominantId;
            dominantSince = System.currentTimeMillis();
        }
    }

    private void applyEffects(Faction dominantFaction, Map<String, Integer> zoneCount) {
        if (dominantFaction == null) return;
        int ticks = snowballDuration * 20;
        for (Player player : Bukkit.getOnlinePlayers()) {
            FPlayer fp = FPlayers.getInstance().getByPlayer(player);
            if (fp == null || fp.getFaction().isWilderness()) continue;
            Faction faction = fp.getFaction();
            int zonesOwned = zoneCount.getOrDefault(faction.getId(), 0);

            if (faction.equals(dominantFaction)) {
                // Faction dominante : Ralentissement + Faiblesse
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ticks, 0, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks, 0, true), true);
                player.sendMessage(prefix + "§cVotre faction domine trop ! §7Les ennemis se renforcent §8(" + snowballDuration + "s§8)§7.");
            } else if (zonesOwned == 0) {
                // Factions sans zone : Résistance + Régénération + Force II (comeback bonus)
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, ticks, 0, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks, 0, true), true);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, ticks, 1, true), true);
                player.sendMessage(prefix + "§aBoost comeback ! §7Résistance + Régénération + Force II §8(" + snowballDuration + "s§8)§7.");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VICTORY & REWARDS
    // ═══════════════════════════════════════════════════════════════════════

    private void grantVictory() {
        ended = true;
        FactionEvent.getInstance().getEventOn().setDominationEvent(null);

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(factionScores.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        if (sorted.isEmpty() || sorted.get(0).getValue() <= 0) {
            Bukkit.broadcastMessage("\n§8§m-----------------------------------------------------\n"
                + "§r §8< §6§lDOMINATION TERMINÉE §8> §8§m-----------------------------------------------------\n"
                + "§7Le temps est écoulé sans vainqueur.\n"
                + "§8§m-----------------------------------------------------");
            RankingManager.updateRanking(true);
            return;
        }

        FileConfiguration config = FileManager.getConfig();
        int winPoints     = config.getInt("domination.win_points", 30);
        int secondPoints  = config.getInt("domination.rewards.second_points", 15);
        int thirdPoints   = config.getInt("domination.rewards.third_points", 8);
        double winMoney   = config.getDouble("domination.rewards.winner_money", 5000.0);
        double secMoney   = config.getDouble("domination.rewards.second_money", 2500.0);
        double thirdMoney = config.getDouble("domination.rewards.third_money", 1000.0);
        String spawnCmd   = config.getString("domination.rewards.spawn_command", "spawn {player}");

        Faction winner = getFactionById(sorted.get(0).getKey());
        if (winner == null) return;
        int winnerScore = sorted.get(0).getValue();

        Faction second = sorted.size() > 1 ? getFactionById(sorted.get(1).getKey()) : null;
        Faction third  = sorted.size() > 2 ? getFactionById(sorted.get(2).getKey()) : null;

        // ── Announce ──────────────────────────────────────────────────────
        Bukkit.broadcastMessage("\n§8§m-----------------------------------------------------\n"
            + "§r §8< §6§lVICTOIRE DOMINATION §8> §8§m-----------------------------------------------------\n"
            + "§7🏆 §c" + winner.getTag() + " §7remporte la Domination avec §e" + winnerScore + " §7points !\n"
            + buildScoreSummary()
            + "§8§m-----------------------------------------------------");
        FactionMessageTitle.sendPlayersTitle(20, 60, 20,
            "§6§lVICTOIRE DOMINATION", "§c" + winner.getTag() + " §7avec §e" + winnerScore + "pts");
        playSoundAll(Sound.LEVEL_UP);

        // ── Winner ────────────────────────────────────────────────────────
        RankingManager.addDominationWins(winner);
        RankingManager.addPoints(winner, winPoints);
        FactionMessageTitle.sendFactionTitle(winner, 20, 60, 20,
            "§6§l🏆 VICTOIRE !", "§a+" + winPoints + " pts §8| §a+" + (int) winMoney + "$");
        giveMoneyToFaction(winner, winMoney, winPoints, true);

        // ── 2nd place consolation ─────────────────────────────────────────
        if (second != null && !second.isWilderness()) {
            RankingManager.addPoints(second, secondPoints);
            FactionMessageTitle.sendFactionTitle(second, 10, 40, 10,
                "§e§l2ème place", "§a+" + secondPoints + " pts §8| §e+" + (int) secMoney + "$");
            giveMoneyToFaction(second, secMoney, secondPoints, false);
        }

        // ── 3rd place consolation ─────────────────────────────────────────
        if (third != null && !third.isWilderness()) {
            RankingManager.addPoints(third, thirdPoints);
            FactionMessageTitle.sendFactionTitle(third, 10, 40, 10,
                "§7§l3ème place", "§a+" + thirdPoints + " pts §8| §e+" + (int) thirdMoney + "$");
            giveMoneyToFaction(third, thirdMoney, thirdPoints, false);
        }

        RankingManager.updateRanking(true);

        // ── Teleport non-winners to spawn ─────────────────────────────────
        final Faction finalWinner = winner;
        final String finalSpawnCmd = spawnCmd;
        // Délai de 2 secondes pour laisser le temps aux titres/messages d'apparaître
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    FPlayer fp = FPlayers.getInstance().getByPlayer(p);
                    // Ne téléporte pas les joueurs de la faction gagnante
                    if (fp != null && fp.getFaction().equals(finalWinner)) continue;
                    // Ne téléporte que les joueurs proches d'une zone (dans une zone ou dans 20 blocs)
                    if (!isNearAnyZone(p, 20)) continue;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        finalSpawnCmd.replace("%player%", p.getName()).replace("{player}", p.getName()));
                } catch (Exception ignored) {}
            }
        }, 40L);

        // ── Loot chests ───────────────────────────────────────────────────
        Bukkit.broadcastMessage(prefix + "§6⚠ §c" + winner.getTag()
            + " §6a §e1 minute §6pour récupérer les coffres dans les zones !");
        Bukkit.broadcastMessage(prefix + "§7Les autres joueurs ont été téléportés au spawn.");

        final List<Location> chestLocations = spawnLootChests(config);
        if (!chestLocations.isEmpty()) {
            // Enregistrer les coffres : seule la faction gagnante peut les ouvrir
            for (Location loc : chestLocations) {
                Location key = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                FactionEvent.getInstance().getDominationLootChests().put(key, winner);
            }
            int chestDuration = config.getInt("domination.rewards.loot_chest.duration", 60);
            Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
                for (Location loc : chestLocations) {
                    Location key = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                    FactionEvent.getInstance().getDominationLootChests().remove(key);
                    if (loc.getBlock().getType() == Material.CHEST) {
                        try { ((Chest) loc.getBlock().getState()).getInventory().clear(); } catch (Exception ignored) {}
                        loc.getBlock().setType(Material.AIR);
                    }
                }
                Bukkit.broadcastMessage(prefix + "§cLes coffres de Domination ont disparu !");
            }, chestDuration * 20L);
        }
    }

    private void giveMoneyToFaction(Faction faction, double amount, int points, boolean isWinner) {
        try {
            Economy eco = FactionEvent.getEconomy();
            for (Player p : getOnlineFactionPlayers(faction)) {
                if (eco != null && amount > 0) {
                    eco.depositPlayer(p.getName(), amount);
                }
                if (amount > 0) {
                    p.sendMessage(prefix + (isWinner
                        ? "§6🏆 §aVictoire ! §7+" + points + " pts classement §8| §a+" + (int) amount + "$"
                        : "§e🥈 Lot de consolation : §a+" + points + " pts §8| §e+" + (int) amount + "$"));
                }
            }
        } catch (Exception ignored) {}
    }

    // ── Loot chest placement ──────────────────────────────────────────────

    private List<Location> spawnLootChests(FileConfiguration config) {
        List<Location> placed = new ArrayList<>();
        List<String> items = config.getStringList("domination.rewards.loot_chest.items");
        int slotsToFill = config.getInt("domination.rewards.loot_chest.items_per_chest", 3);
        if (items.isEmpty()) return placed;

        for (DominationZone zone : domination.getActiveZones()) {
            Location loc;
            if (zone.getChestLocation() != null) {
                // Position définie manuellement via /domination setchest
                loc = zone.getChestLocation().clone();
                // Si le bloc à cette position est de l'air, descendre jusqu'au premier sol
                World world = loc.getWorld();
                if (world != null && loc.getBlock().getType() == Material.AIR) {
                    int x = loc.getBlockX(), z = loc.getBlockZ();
                    boolean found = false;
                    for (int y = loc.getBlockY(); y >= 1; y--) {
                        Block floor = world.getBlockAt(x, y, z);
                        Block above = world.getBlockAt(x, y + 1, z);
                        if (floor.getType().isSolid() && above.getType() == Material.AIR) {
                            loc = new Location(world, x, y + 1, z);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Bukkit.getLogger().warning("[Domination] Impossible de trouver un sol valide pour le chest de la zone " + zone.getName() + " (chest manuel).");
                        continue;
                    }
                }
            } else {
                // Fallback : centre XZ entre pos1/pos2, Y médian de la zone, puis descente
                loc = findZoneCenter(zone);
                if (loc == null) continue;
            }
            try {
                loc.getBlock().setType(Material.CHEST);
                Chest chest = (Chest) loc.getBlock().getState();
                fillChest(chest.getInventory(), items, slotsToFill);
                placed.add(loc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return placed;
    }

    /**
     * Trouve la position du coffre automatique :
     * - Centre XZ entre pos1 et pos2
     * - Scan DEPUIS le Y de base (min Y original entre pos1/pos2, avant /expand) VERS LE HAUT
     *   jusqu'au premier bloc solide suivi d'au moins 1 bloc d'air au-dessus.
     * - Explore un petit rayon si le centre exact est bloqué.
     */
    private Location findZoneCenter(DominationZone zone) {
        if (zone.getPos1() == null || zone.getPos2() == null) return null;
        World world = zone.getPos1().getWorld();
        if (world == null) return null;

        int cx = (zone.getPos1().getBlockX() + zone.getPos2().getBlockX()) / 2;
        int cz = (zone.getPos1().getBlockZ() + zone.getPos2().getBlockZ()) / 2;

        // Utiliser le Y original (avant /expand) comme base du scan
        int baseY = zone.getBaseY();
        int maxScan = Math.min(baseY + 50, world.getMaxHeight() - 2);

        int[][] offsets = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{2,0},{-2,0},{0,2},{0,-2}};
        for (int[] off : offsets) {
            int x = cx + off[0];
            int z = cz + off[1];
            // Scan de bas en haut depuis baseY
            for (int y = baseY; y <= maxScan; y++) {
                Block floor  = world.getBlockAt(x, y, z);
                Block above1 = world.getBlockAt(x, y + 1, z);
                if (floor.getType().isSolid() && above1.getType() == Material.AIR) {
                    return new Location(world, x, y + 1, z);
                }
            }
        }
        return null;
    }

    private void fillChest(Inventory inv, List<String> items, int slotsToFill) {
        if (items.isEmpty()) return;
        boolean debug = FileManager.getConfig().getBoolean("domination.rewards.loot_chest.debug", false);

        // Copie + mélange aléatoire RÉEL avec une nouvelle instance de Random à chaque appel
        List<String> pool = new ArrayList<>(items);
        Random rng = new Random(System.nanoTime()); // seed différente à chaque coffre
        Collections.shuffle(pool, rng);

        Set<String> usedMaterials = new HashSet<>(); // anti-doublon par nom de matériau (clé = matName)
        List<ItemStack> toPlace   = new ArrayList<>();

        for (String itemStr : pool) {
            if (toPlace.size() >= slotsToFill) break;
            if (itemStr == null || itemStr.trim().isEmpty()) continue;

            // Format: MATERIAL[:amount[:ENCHANT1=level,ENCHANT2=level,...]]
            // On sépare uniquement sur les 2 premiers ':' pour ne pas casser les enchants
            String[] parts = itemStr.trim().split(":", 3);
            String matName = parts[0].toUpperCase().trim();

            // Anti-doublon : on skip si ce matériau est déjà choisi pour ce coffre
            if (usedMaterials.contains(matName)) {
                if (debug) Bukkit.getLogger().info("[DominationLoot] skip doublon matériau: " + matName);
                continue;
            }

            Material mat = null;
            try { mat = Material.valueOf(matName); } catch (IllegalArgumentException ignored) {}
            if (mat == null) {
                // Item custom (RUBY, COBALT_SWORD...) : on l'inclut quand même si le serveur gère
                // un Material custom via son nom — dernière tentative via getMaterial
                try { mat = Material.getMaterial(matName); } catch (Exception ignored) {}
            }
            if (mat == null) {
                if (debug) Bukkit.getLogger().info("[DominationLoot] Material inconnu (ignoré): " + matName);
                // On marque quand même pour éviter les doublons de clé
                usedMaterials.add(matName);
                continue;
            }

            int amount = 1;
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                try { amount = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {}
            }

            ItemStack stack = new ItemStack(mat, Math.max(1, amount));
            if (debug) Bukkit.getLogger().info("[DominationLoot] item: " + matName + " x" + amount);

            // Enchantements (3ème partie, séparateur ',' ou ';')
            if (parts.length > 2 && !parts[2].trim().isEmpty()) {
                for (String pair : parts[2].trim().split("[,;]")) {
                    if (pair == null || pair.trim().isEmpty()) continue;
                    String[] kv = pair.trim().contains("=") ? pair.trim().split("=", 2) : new String[]{pair.trim(), "1"};
                    String enchName = kv[0].toUpperCase().trim();
                    int level = 1;
                    try { level = Integer.parseInt(kv[1].trim()); } catch (Exception ignored) {}
                    try {
                        Enchantment ench = resolveEnchantment(enchName);
                        if (ench != null) {
                            ItemMeta meta = stack.getItemMeta();
                            if (meta != null) {
                                meta.addEnchant(ench, Math.max(1, level), true);
                                stack.setItemMeta(meta);
                                if (debug) Bukkit.getLogger().info("[DominationLoot] enchant: " + enchName + " lvl=" + level);
                            } else {
                                stack.addUnsafeEnchantment(ench, Math.max(1, level));
                            }
                        } else if (debug) {
                            Bukkit.getLogger().info("[DominationLoot] enchant non résolu: " + enchName);
                        }
                    } catch (Exception ex) {
                        if (debug) ex.printStackTrace();
                    }
                }
            }

            usedMaterials.add(matName);
            toPlace.add(stack);
        }

        // Slots aléatoires distincts dans les 27 cases du coffre
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots, rng);
        for (int i = 0; i < toPlace.size() && i < slots.size(); i++) {
            inv.setItem(slots.get(i), toPlace.get(i));
        }
        if (debug) Bukkit.getLogger().info("[DominationLoot] coffre rempli: " + toPlace.size() + "/" + slotsToFill + " items.");
    }

    // Map d'alias pour correspondre des noms courants (SHARPNESS, PROTECTION...) aux noms Bukkit
    private static final Map<String, String> ENCHANT_ALIAS = new HashMap<>();
    static {
        ENCHANT_ALIAS.put("SHARPNESS", "DAMAGE_ALL");
        ENCHANT_ALIAS.put("SMITE", "DAMAGE_UNDEAD");
        ENCHANT_ALIAS.put("BANE_OF_ARTHROPODS", "DAMAGE_ARTHROPODS");
        ENCHANT_ALIAS.put("PROTECTION", "PROTECTION_ENVIRONMENTAL");
        ENCHANT_ALIAS.put("FIRE_PROTECTION", "PROTECTION_FIRE");
        ENCHANT_ALIAS.put("PROJECTILE_PROTECTION", "PROTECTION_PROJECTILE");
        ENCHANT_ALIAS.put("BLAST_PROTECTION", "PROTECTION_EXPLOSIONS");
        ENCHANT_ALIAS.put("FEATHER_FALLING", "PROTECTION_FALL");
        ENCHANT_ALIAS.put("UNBREAKING", "DURABILITY");
        ENCHANT_ALIAS.put("POWER", "ARROW_DAMAGE");
        ENCHANT_ALIAS.put("PUNCH", "ARROW_KNOCKBACK");
        ENCHANT_ALIAS.put("FLAME", "ARROW_FIRE");
        ENCHANT_ALIAS.put("INFINITY", "ARROW_INFINITE");
        ENCHANT_ALIAS.put("LOOTING", "LOOT_BONUS_MOBS");
        ENCHANT_ALIAS.put("FORTUNE", "LOOT_BONUS_BLOCKS");
        ENCHANT_ALIAS.put("EFFICIENCY", "DIG_SPEED");
        ENCHANT_ALIAS.put("FIRE_ASPECT", "FIRE_ASPECT");
        ENCHANT_ALIAS.put("PROTECTION_ENVIRONMENTAL", "PROTECTION_ENVIRONMENTAL");
        ENCHANT_ALIAS.put("THORNS", "THORNS");
    }

    // Tentative de résolution d'un enchantement à partir d'un nom tolérant (SHARPNESS, sharpness, DAMAGE_ALL...)
    private Enchantment resolveEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        String n = name.trim();
        // Try direct match (some servers expect uppercase names like DAMAGE_ALL)
        try {
            Enchantment e = Enchantment.getByName(n);
            if (e != null) return e;
        } catch (Throwable ignored) {}

        // Try alias map (common names)
        try {
            String key = n.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (ENCHANT_ALIAS.containsKey(key)) {
                String mapped = ENCHANT_ALIAS.get(key);
                try {
                    Enchantment e2 = Enchantment.getByName(mapped);
                    if (e2 != null) return e2;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Normalize and try to match against available enchantments
        String alt = n.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        try {
            for (org.bukkit.enchantments.Enchantment ench : org.bukkit.enchantments.Enchantment.values()) {
                try {
                    String cand = ench.toString();
                    if (cand != null && cand.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(alt)) return ench;
                } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Method m = ench.getClass().getMethod("getName");
                    Object obj = m.invoke(ench);
                    if (obj instanceof String) {
                        String cand2 = ((String) obj).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                        if (cand2.equalsIgnoreCase(alt)) return ench;
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════

    private String buildScoreSummary() {
        StringBuilder sb = new StringBuilder();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(factionScores.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        int rank = 1;
        for (Map.Entry<String, Integer> e : sorted) {
            if (rank > 5) break;
            Faction f = getFactionById(e.getKey());
            if (f != null) {
                sb.append("§8» §7").append(rank).append(". §c").append(f.getTag())
                  .append(" §8- §e").append(e.getValue()).append(" §7pts\n");
            }
            rank++;
        }
        return sb.toString();
    }

    private String buildGlobalActionBar() {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(factionScores.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        String timeStr = DateManager.getFormattedTime((int) Math.max(0, duration - elapsed));

        StringBuilder bar = new StringBuilder("§8[§cDOM§8] ");
        if (sorted.isEmpty()) {
            bar.append("§7Aucun score encore");
        } else {
            int shown = 0;
            for (Map.Entry<String, Integer> e : sorted) {
                if (shown >= 3) break;
                Faction f = getFactionById(e.getKey());
                if (f == null) continue;
                int zones = countZonesOwned(e.getKey());
                if (shown > 0) bar.append(" §8| ");
                bar.append("§c").append(f.getTag())
                   .append("§8:§e").append(e.getValue()).append("pts")
                   .append(" §7(§c").append(zones).append("§7z)");
                shown++;
            }
        }
        bar.append(" §8| §7⏱§c").append(timeStr);
        return bar.toString();
    }

    private String buildPlayerActionBar(Player player, String globalBar) {
        for (DominationZone zone : domination.getActiveZones()) {
            if (!zone.isPlayerInZone(player)) continue;
            ZoneState state = zoneStates.get(zone.getName());
            if (state == null) continue;
            String ownerStr = state.ownerFaction != null ? "§c" + state.ownerFaction.getTag() : "§7Neutre";
            String multStr  = " §8[" + state.multiplier.display + "§8]";
            if (state.contestFaction != null && state.contestProgress > 0) {
                return "§7Zone §c" + zone.getName() + multStr + " §8» §7Capture: §c"
                    + state.contestFaction.getTag() + " §e" + state.contestProgress + "§7%"
                    + " §8| §7Contrôle: " + ownerStr;
            } else {
                return "§7Zone §c" + zone.getName() + multStr + " §8» " + ownerStr
                    + " §8| " + globalBar;
            }
        }
        return globalBar;
    }

    private int countZonesOwned(String factionId) {
        int count = 0;
        for (ZoneState s : zoneStates.values()) {
            if (s.ownerFaction != null && s.ownerFaction.getId().equals(factionId)) count++;
        }
        return count;
    }

    private void broadcastIfNeeded(DominationZone zone, ZoneState state, String fallback,
                                   String msgKey, Faction faction, long cooldownMs) {
        long now = System.currentTimeMillis();
        if (now - state.lastBroadcastMs < Math.max(cooldownMs, 5000)) return;
        state.lastBroadcastMs = now;
        String msgStr = msg.getString(msgKey, fallback != null ? fallback : "");
        if (msgStr == null || msgStr.isEmpty()) return;
        String formatted = faction != null
            ? new StrManager(msgStr).reFaction(faction.getTag()).reCustom("{zone}", zone.getName()).toString()
            : new StrManager(msgStr).reCustom("{zone}", zone.getName()).toString();
        Bukkit.broadcastMessage(prefix + formatted);
    }

    private void playSoundAll(Sound sound) {
        try {
            for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }

    /**
     * Retourne true si le joueur est dans une zone ou dans un rayon de {@code range} blocs autour d'une zone.
     */
    private boolean isNearAnyZone(Player player, int range) {
        Location pLoc = player.getLocation();
        for (DominationZone zone : domination.getActiveZones()) {
            if (zone.getPos1() == null || zone.getPos2() == null) continue;
            if (!pLoc.getWorld().equals(zone.getPos1().getWorld())) continue;
            double minX = Math.min(zone.getPos1().getX(), zone.getPos2().getX()) - range;
            double maxX = Math.max(zone.getPos1().getX(), zone.getPos2().getX()) + range;
            double minY = Math.min(zone.getPos1().getY(), zone.getPos2().getY()) - range;
            double maxY = Math.max(zone.getPos1().getY(), zone.getPos2().getY()) + range;
            double minZ = Math.min(zone.getPos1().getZ(), zone.getPos2().getZ()) - range;
            double maxZ = Math.max(zone.getPos1().getZ(), zone.getPos2().getZ()) + range;
            if (pLoc.getX() >= minX && pLoc.getX() <= maxX
                    && pLoc.getY() >= minY && pLoc.getY() <= maxY
                    && pLoc.getZ() >= minZ && pLoc.getZ() <= maxZ) {
                return true;
            }
        }
        return false;
    }

    private Faction getFactionById(String id) {
        try {
            for (Faction f : Factions.getInstance().getAllFactions()) {
                if (f.getId().equals(id)) return f;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private List<Player> getOnlineFactionPlayers(Faction faction) {
        List<Player> list = new ArrayList<>();
        for (FPlayer fp : FPlayers.getInstance().getOnlinePlayers()) {
            if (fp.getFaction().equals(faction)) {
                Player p = Bukkit.getPlayer(fp.getName());
                if (p != null && p.isOnline()) list.add(p);
            }
        }
        return list;
    }
}

