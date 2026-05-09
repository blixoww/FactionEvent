package fr.blixow.factionevent.utils.lms;

import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.LootItemParser;
import fr.blixow.factionevent.utils.Messages;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class LMSEvent {

    private final HashMap<Player, Boolean> participants;
    private boolean eventActive;
    private boolean gracePeriodActive = false;

    private final FileConfiguration config;
    private final LMS lms;
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.contains("lms.prefix") ? msg.getString("lms.prefix") : "§8[§cLMS§8]§7 ";

    /** Kills par tueur (UUID → nombre de kills) */
    private final HashMap<UUID, Integer> kills = new HashMap<>();
    /** Nom des tueurs (UUID → nom) */
    private final HashMap<UUID, String> killerNames = new HashMap<>();

    /**
     * UUID des joueurs qui viennent de mourir et ont besoin d'une restauration d'inventaire
     * au prochain PlayerRespawnEvent.
     */
    private final Set<UUID> pendingRestore = new HashSet<>();

    public LMSEvent(LMS lms) {
        this.lms = lms;
        this.participants = lms.getRegisteredPlayers();
        this.config = FileManager.getConfig();
        this.eventActive = false;
    }

    // ───────────────────────────────────────────────────────────────
    //  DÉMARRAGE DE L'EVENT
    // ───────────────────────────────────────────────────────────────

    public void startEvent() {
        eventActive = true;
        gracePeriodActive = true;

        int graceSec = lms.getGracePeriod();

        // Donner le kit à chaque participant + message de départ
        String modeTxt = lms.getMode() == LMSMode.DUO
            ? "§7Il n'y aura que §cdeux §7survivants (mode §eDUO§7) !"
            : "§7Il n'y aura qu'§cun seul §7survivant (mode §eSOLO§7) !";

        String startBroadcast = new StrManager(msg.getString("lms.started",
            "§8§m-----------------------------------------------------\n"
            + "§r §8< §6§lLAST MAN STANDING §8> §8§m-----------------------------------------------------\n"
            + "§c⚔ Le combat {lms} commence !\n"
            + "§7Mode : §e{mode} §8| §7Kit fourni — votre inventaire est sauvegardé\n"
            + "§7Toutes les alliances sont ignorées — tout le monde peut vous attaquer !\n"
            + "§7Le combat commencera dans §c{grace}s §7(période de grâce)\n"
            + "§8§m-----------------------------------------------------"))
            .reLMS(lms.getName())
            .reCustom("{mode}", lms.getMode().getDisplayName())
            .reCustom("{grace}", String.valueOf(graceSec))
            .toString();
        Bukkit.broadcastMessage(startBroadcast);

        for (Player player : getAlivePlayers()) {
            lms.saveAndEquipPlayer(player);
            player.sendMessage(prefix + msg.getString("lms.kit_given",
                "§7Votre inventaire a été sauvegardé. Un kit §epvp §7vous a été attribué !"));
        }

        // Countdown 5-4-3-2-1 (les 5 dernières secondes avant la fin de la période de grâce)
        for (int i = 5; i >= 1; i--) {
            final int n = i;
            final boolean isLast = (i == 1);
            Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
                if (!eventActive) return;
                String subtitle = isLast
                    ? (lms.getMode() == LMSMode.DUO ? "§7Il n'y aura que §cdeux §7survivants !" : "§7Il n'y aura qu'§cun seul §7survivant !")
                    : "§7Préparez-vous au combat...";
                for (Player p : getAlivePlayers()) {
                    Messages.sendTitle(p, 5, 15, 5, "§c§l" + n, subtitle);
                }
            }, (graceSec - 5 + (5 - i)) * 20L);
        }

        // Fin de la période de grâce : combat actif
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
            if (!eventActive) return;
            gracePeriodActive = false;
            String graceEndMsg = msg.getString("lms.grace_ended",
                "§c⚔ Le combat commence ! Que le meilleur gagne !");
            Bukkit.broadcastMessage(prefix + graceEndMsg);
            FactionMessageTitle.sendPlayersTitle(10, 30, 10, "§c§l⚔ COMBAT !", "§7Bonne chance !");
        }, graceSec * 20L);
    }

    // ───────────────────────────────────────────────────────────────
    //  MORT D'UN JOUEUR
    // ───────────────────────────────────────────────────────────────

    /**
     * Appelé depuis PlayerDeathEvent.
     * @param player  joueur mort
     * @param killer  tueur (peut être null)
     */
    public void handlePlayerDeath(Player player, Player killer) {
        if (!participants.containsKey(player)) return;
        participants.remove(player);

        // Marquer pour restauration lors du respawn
        pendingRestore.add(player.getUniqueId());

        // Si un tueur LMS existe : enregistrer le kill
        if (killer != null && participants.containsKey(killer)) {
            UUID killerId = killer.getUniqueId();
            int killCount = kills.merge(killerId, 1, Integer::sum);
            killerNames.put(killerId, killer.getName());

            // Message global de kill
            String killMsg = msg.getString("lms.kill_broadcast",
                "§c{killer} §7a éliminé §c{victim} §8(§e{kills} élim.§8)")
                .replace("{killer}", killer.getName())
                .replace("{victim}", player.getName())
                .replace("{kills}", String.valueOf(killCount));
            Bukkit.broadcastMessage(prefix + killMsg);
        } else {
            // Mort sans tueur identifiable
            String killMsg = msg.getString("lms.eliminated_broadcast",
                "§c{victim} §7est éliminé !")
                .replace("{victim}", player.getName());
            Bukkit.broadcastMessage(prefix + killMsg);
        }

        player.sendMessage(prefix + msg.getString("lms.eliminated",
            "§cVous avez été éliminé du Last Man Standing."));

        checkWinCondition();
    }

    /** Appelé depuis PlayerRespawnEvent — restaure l'inventaire. */
    public void handlePlayerRespawn(Player player) {
        if (!pendingRestore.contains(player.getUniqueId())) return;
        pendingRestore.remove(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () ->
            lms.restorePlayer(player), 1L);
    }

    /** Appelé depuis PlayerQuitEvent. */
    public void handlePlayerQuit(Player player) {
        if (!participants.containsKey(player)) return;
        participants.remove(player);
        pendingRestore.remove(player.getUniqueId());

        // Restaurer l'inventaire immédiatement (avant la déco)
        lms.restoreInventoryOnly(player);

        String quitMsg = msg.getString("lms.quit_broadcast",
            "§c{player} §7a quitté le LMS et est éliminé.")
            .replace("{player}", player.getName());
        Bukkit.broadcastMessage(prefix + quitMsg);

        checkWinCondition();
    }

    // ───────────────────────────────────────────────────────────────
    //  CONDITION DE VICTOIRE
    // ───────────────────────────────────────────────────────────────

    private void checkWinCondition() {
        if (!eventActive) return;
        List<Player> alive = getAlivePlayers();

        if (alive.isEmpty()) {
            Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.no_winner",
                "§cLe LMS §e{lms} §cs'est terminé sans vainqueur.")).reLMS(lms.getName()).toString());
            distributeKillRewards();
            endEvent();
            lms.resetPhase();
            return;
        }

        if (lms.getMode() == LMSMode.SOLO) {
            if (alive.size() == 1) {
                grantVictory(alive);
                distributeKillRewards();
                endEvent();
                lms.resetPhase();
            }
        } else { // DUO
            // Vérifier si tous les joueurs restants appartiennent à la même faction
            if (alive.size() <= 1) {
                grantVictory(alive);
                distributeKillRewards();
                endEvent();
                lms.resetPhase();
            } else {
                Faction firstFaction = null;
                try { firstFaction = FPlayers.getInstance().getByPlayer(alive.get(0)).getFaction(); }
                catch (Exception ignored) {}
                if (firstFaction != null) {
                    final Faction finalFirstFaction = firstFaction;
                    boolean allSame = alive.stream().allMatch(p -> {
                        try {
                            return FPlayers.getInstance().getByPlayer(p)
                                .getFaction().getId().equals(finalFirstFaction.getId());
                        } catch (Exception e) { return false; }
                    });
                    if (allSame) {
                        // Tous de la même faction — les 2 meilleurs survivants remportent
                        List<Player> winners = alive.size() > 2 ? alive.subList(0, 2) : alive;
                        grantVictory(new ArrayList<>(winners));
                        distributeKillRewards();
                        endEvent();
                        lms.resetPhase();
                    }
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    //  RÉCOMPENSES — VICTOIRE
    // ───────────────────────────────────────────────────────────────

    private void grantVictory(List<Player> winners) {
        if (winners == null || winners.isEmpty()) return;

        Economy eco = FactionEvent.getEconomy();
        double winMoney = config.getDouble("lms.win_reward.money", 5000);
        int itemsMin = config.getInt("lms.win_reward.items_min", 2);
        int itemsMax = config.getInt("lms.win_reward.items_max", 4);
        List<String> itemsRaw = config.getStringList("lms.win_reward.items");
        List<LootItemParser.LootEntry> pool = LootItemParser.parse(itemsRaw);

        // Faction gagnante (prise du 1er gagnant)
        Faction winnerFaction = null;
        int factionPoints = config.getInt("lms.win_points", 20);
        try {
            winnerFaction = FPlayers.getInstance().getByPlayer(winners.get(0)).getFaction();
        } catch (Exception ignored) {}

        // Broadcast victoire
        String winnerNames = winners.stream().map(Player::getName).collect(Collectors.joining("§7, §c"));
        String winnerFactionTag = (winnerFaction != null && !winnerFaction.isWilderness())
            ? winnerFaction.getTag() : "?";

        if (lms.getMode() == LMSMode.DUO && winners.size() >= 2) {
            String winMsg = msg.getString("lms.winner_duo",
                "§8§m-----------------------------------------------------\n"
                + "§r §8< §6§lVICTOIRE LMS DUO §8> §8§m-----------------------------------------------------\n"
                + "§c{winners} §8(§c{faction}§8) §7remportent le §cLast Man Standing §c{lms} §7!\n"
                + "§8§m-----------------------------------------------------")
                .replace("{winners}", winnerNames)
                .replace("{faction}", winnerFactionTag)
                .replace("{lms}", lms.getName());
            Bukkit.broadcastMessage(winMsg);
        } else {
            Player solo = winners.get(0);
            String winMsg = new StrManager(msg.getString("lms.winner",
                "§8§m-----------------------------------------------------\n"
                + "§r §8< §6§lVICTOIRE LMS §8> §8§m-----------------------------------------------------\n"
                + "§c{player} §8(§c{faction}§8) §7remporte le §cLast Man Standing §c{lms} §7!\n"
                + "§8§m-----------------------------------------------------"))
                .rePlayer(solo).reLMS(lms.getName()).reFaction(winnerFactionTag).toString();
            Bukkit.broadcastMessage(winMsg);
        }

        // Points faction
        if (winnerFaction != null && !winnerFaction.isWilderness()) {
            RankingManager.addLMSWins(winnerFaction);
            RankingManager.addPoints(winnerFaction, factionPoints);
            FactionMessageTitle.sendFactionTitle(winnerFaction, 20, 60, 20,
                "§6§lLMS GAGNÉ !", "§a+" + factionPoints + " pts de classement");
        }

        // Récompenses individuelles
        for (Player winner : winners) {
            if (!winner.isOnline()) continue;
            // Argent
            if (eco != null && winMoney > 0) {
                try { eco.depositPlayer(winner.getName(), winMoney); } catch (Exception ignored) {}
            }
            // Items
            List<ItemStack> drops = pickItems(pool, itemsMin, itemsMax);
            for (ItemStack item : drops) {
                LMSRewardManager.addItemReward(winner.getUniqueId(), item);
            }
            winner.sendMessage(prefix + "§6§lVICTOIRE ! §a+§e" + (int) winMoney
                + "$ §8| §a+§e" + drops.size() + " items §7→ /lmsreward");
            Messages.sendTitle(winner, 20, 60, 20,
                "§6§l👑 VICTOIRE", "§7+§e" + (int) winMoney + "$ §8| §7+§e" + drops.size() + " items");
            try { winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1f, 1f); } catch (Exception ignored) {}
        }

        RankingManager.updateRanking(true);
    }

    // ───────────────────────────────────────────────────────────────
    //  RÉCOMPENSES — TOP 3 KILLS
    // ───────────────────────────────────────────────────────────────

    private void distributeKillRewards() {
        if (kills.isEmpty()) return;

        Economy eco = FactionEvent.getEconomy();

        // Tri par kills décroissant
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(kills.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        // Résumé global
        StringBuilder sb = new StringBuilder("§8§m     §r §8[ §cTOP ÉLIMINATIONS LMS §8] §8§m     \n");
        int rank = 1;
        for (Map.Entry<UUID, Integer> e : sorted) {
            if (rank > 3) break;
            sb.append("§8» §7").append(rank++).append(". §c")
              .append(killerNames.getOrDefault(e.getKey(), "?"))
              .append(" §8- §e").append(e.getValue()).append(" §7élim.\n");
        }
        Bukkit.broadcastMessage(sb.toString());

        // Distribution des récompenses top 3
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            UUID uuid = sorted.get(i).getKey();
            int r = i + 1;
            String path = "lms.kill_top_rewards." + r;
            double money = config.getDouble(path + ".money", 0);
            int min = config.getInt(path + ".items_min", 0);
            int max = config.getInt(path + ".items_max", 0);
            List<String> rawItems = config.getStringList(path + ".items");
            List<LootItemParser.LootEntry> pool = LootItemParser.parse(rawItems);

            String playerName = killerNames.get(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) playerName = p.getName();

            if (eco != null && money > 0 && playerName != null) {
                try { eco.depositPlayer(playerName, money); } catch (Exception ignored) {}
            }

            List<ItemStack> drops = pickItems(pool, min, max);
            for (ItemStack item : drops) {
                LMSRewardManager.addItemReward(uuid, item);
            }

            if (p != null) {
                p.sendMessage(prefix + "§6§l" + r + "ème meilleur killer §8» §a+§e"
                    + (int) money + "$ §8| §a+§e" + drops.size() + " items §7→ /lmsreward");
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    //  ACTIONS À LA FIN
    // ───────────────────────────────────────────────────────────────

    public void endEvent() {
        eventActive = false;
        gracePeriodActive = false;
        participants.clear();
        pendingRestore.clear();
        FactionEvent.getInstance().getEventOn().setLMSEvent(null);
    }

    // ───────────────────────────────────────────────────────────────
    //  SCOREBOARD / ACTION BAR
    // ───────────────────────────────────────────────────────────────

    public void updateScoreboard() {
        if (!eventActive) return;
        int remaining = participants.size();
        if (remaining == 0) return;
        String bar = prefix + (gracePeriodActive ? "§7Phase de grâce — " : "§c⚔ ") 
            + "§7Joueurs restants : §e" + remaining;
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(online);
                if (eventManager == null) {
                    eventManager = EventManager.loadFromFile(online);
                    FactionEvent.getInstance().getEventScoreboardOff().put(online, eventManager);
                }
                if (eventManager.isActionbar()) {
                    Messages.sendActionBar(online, bar);
                }
            } catch (Exception ignored) {}
        }
    }

    // ───────────────────────────────────────────────────────────────
    //  HELPERS
    // ───────────────────────────────────────────────────────────────

    public List<Player> getAlivePlayers() {
        List<Player> alive = new ArrayList<>();
        for (Map.Entry<Player, Boolean> e : participants.entrySet()) {
            if (e.getKey() != null && e.getKey().isOnline()) alive.add(e.getKey());
        }
        return alive;
    }

    private List<ItemStack> pickItems(List<LootItemParser.LootEntry> pool, int min, int max) {
        List<ItemStack> result = new ArrayList<>();
        if (pool == null || pool.isEmpty() || max <= 0) return result;
        if (min < 0) min = 0;
        if (max < min) max = min;
        Random rng = new Random(System.nanoTime());
        int count = (max == min) ? min : (min + rng.nextInt(max - min + 1));
        if (count <= 0) return result;
        List<ItemStack> items = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        for (LootItemParser.LootEntry e : pool) { items.add(e.item); weights.add(e.weight); }
        for (int i = 0; i < count && !items.isEmpty(); i++) {
            int total = 0;
            for (int w : weights) total += w;
            if (total <= 0) break;
            int roll = rng.nextInt(total), cumul = 0, picked = 0;
            for (int j = 0; j < weights.size(); j++) {
                cumul += weights.get(j);
                if (roll < cumul) { picked = j; break; }
            }
            result.add(items.get(picked).clone());
            items.remove(picked);
            weights.remove(picked);
        }
        return result;
    }

    // ───────────────────────────────────────────────────────────────
    //  GETTERS
    // ───────────────────────────────────────────────────────────────

    public boolean isParticipant(Player player) { return participants.containsKey(player); }
    public boolean isEventActive() { return eventActive; }
    public boolean isGracePeriodActive() { return gracePeriodActive; }
    public boolean isPendingRestore(UUID uuid) { return pendingRestore.contains(uuid); }
    public LMS getLMS() { return lms; }
    public HashMap<UUID, Integer> getKills() { return kills; }
    public HashMap<UUID, String> getKillerNames() { return killerNames; }

    /** LMS n'a pas de timer — toujours false. */
    public boolean checkTimer() { return false; }

    /** @deprecated utiliser handlePlayerDeath(player, killer) */
    @Deprecated
    public void handlePlayerDeath(Player player) {
        handlePlayerDeath(player, player.getKiller());
    }

    /** @deprecated utiliser handlePlayerQuit(player) */
    @Deprecated
    public void handlePlayerQuit(Player player, boolean ignored) {
        handlePlayerQuit(player);
    }
}
