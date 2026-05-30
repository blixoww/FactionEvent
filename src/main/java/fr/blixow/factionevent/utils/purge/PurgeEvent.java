package fr.blixow.factionevent.utils.purge;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.EventManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.RankingManager;
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

/**
 * Logique runtime d'une Purge : compte les kills, met à jour l'action bar,
 * distribue les récompenses (kill + classement final).
 *
 * Les gains sont scindés en deux :
 *   - argent : versé immédiatement via Vault
 *   - items  : ajoutés au PurgeManager (HashMap pending) → /purgereward
 */
public class PurgeEvent {

    private final long startTime;
    private final int duration;
    private boolean ended;

    private final Map<UUID, Integer> kills;
    private final Map<UUID, Set<UUID>> uniqueKills; // tueur → victimes déjà tuées (anti-farming)
    private final Map<UUID, String> playerNames;

    private final FileConfiguration msg;
    private final String prefix;

    private final double killMoney;
    private final List<LootItemParser.LootEntry> killItemPool;
    private final int killItemsMin;
    private final int killItemsMax;

    public PurgeEvent() {
        this.startTime = System.currentTimeMillis();
        this.kills = new HashMap<>();
        this.uniqueKills = new HashMap<>();
        this.playerNames = new HashMap<>();
        this.ended = false;

        this.msg = FileManager.getMessageFileConfiguration();
        this.prefix = msg.getString("purge.prefix", "§8[§cPURGE§8]§7 ");

        FileConfiguration cfg = FileManager.getConfig();
        this.duration = cfg.getInt("purge.max_duration", 1800);
        this.killMoney = cfg.getDouble("purge.kill_reward.money", 100.0);
        List<String> rawItems = cfg.getStringList("purge.kill_reward.items");
        this.killItemPool = LootItemParser.parse(rawItems);
        this.killItemsMin = cfg.getInt("purge.kill_reward.items_min", 0);
        this.killItemsMax = cfg.getInt("purge.kill_reward.items_max", 1);
    }

    public boolean isEnded() { return ended; }

    public void cancel() { ended = true; }

    public void handleKill(Player killer, Player victim) {
        if (ended) return;
        if (killer == null || victim == null) return;
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;

        UUID killerId = killer.getUniqueId();
        UUID victimId = victim.getUniqueId();

        // Anti-farming : vérifier si ce joueur a déjà été tué par ce tueur
        Set<UUID> alreadyKilled = uniqueKills.computeIfAbsent(killerId, k -> new HashSet<>());
        if (alreadyKilled.contains(victimId)) {
            killer.sendMessage(prefix + msg.getString("purge.already_killed",
                "§cVous avez déjà tué §e{target} §cdans cette Purge ! Kill non comptabilisé.")
                .replace("{target}", victim.getName()));
            return;
        }
        alreadyKilled.add(victimId);

        int newCount = kills.merge(killerId, 1, Integer::sum);
        playerNames.put(killerId, killer.getName());

        Economy eco = FactionEvent.getEconomy();
        if (eco != null && killMoney > 0) {
            try { eco.depositPlayer(killer.getName(), killMoney); } catch (Exception ignored) {}
        }

        List<ItemStack> drops = pickItems(killItemPool, killItemsMin, killItemsMax);
        for (ItemStack item : drops) {
            PurgeManager.addItemReward(killerId, item);
        }

        String killMsg = msg.getString("purge.kill_message",
            "§a+§e{money}$ §7pour avoir tué §c{target} §8(§e{kills} kills§8)")
            .replace("{money}", String.valueOf((int) killMoney))
            .replace("{target}", victim.getName())
            .replace("{kills}", String.valueOf(newCount));
        killer.sendMessage(prefix + killMsg);
        if (!drops.isEmpty()) {
            killer.sendMessage(prefix + msg.getString("purge.kill_items",
                "§a+§e{count} item(s) §7→ /purgereward").replace("{count}", String.valueOf(drops.size())));
        }
        try { killer.playSound(killer.getLocation(), Sound.ORB_PICKUP, 1.0f, 1.5f); } catch (Exception ignored) {}
    }

    public boolean checkTimer() {
        if (ended) return true;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsed >= duration) { distributeFinalRewards(); return true; }
        return false;
    }

    public void updateScoreboard() {
        if (ended) return;
        String bar = buildActionBar();
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                EventManager em = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                if (em == null) {
                    em = EventManager.loadFromFile(player);
                    FactionEvent.getInstance().getEventScoreboardOff().put(player, em);
                }
                if (em.isActionbar()) {
                    Messages.sendActionBar(player, bar);
                }
            } catch (Exception ignored) {}
        }
    }

    private String buildActionBar() {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        String time = DateManager.getFormattedTime((int) Math.max(0, duration - elapsed));
        StringBuilder sb = new StringBuilder("§8[§cPURGE§8] ");
        List<Map.Entry<UUID, Integer>> top = sortedTop(5);
        if (top.isEmpty()) {
            sb.append("§7Aucun kill");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> e : top) {
                if (rank > 1) sb.append(" §8| ");
                String name = playerNames.getOrDefault(e.getKey(), "?");
                sb.append("§7").append(rank).append(".§c").append(name)
                  .append("§8:§e").append(e.getValue());
                rank++;
            }
        }
        sb.append(" §8| §7⏱§c").append(time);
        return sb.toString();
    }

    private List<Map.Entry<UUID, Integer>> sortedTop(int n) {
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(kills.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        if (sorted.size() > n) return new ArrayList<>(sorted.subList(0, n));
        return sorted;
    }

    private void distributeFinalRewards() {
        if (ended) return;
        ended = true;
        FactionEvent.getInstance().getEventOn().setPurgeEvent(null);

        FileConfiguration cfg = FileManager.getConfig();
        List<Map.Entry<UUID, Integer>> top = sortedTop(5);

        Bukkit.broadcastMessage("\n§8§m-----------------------------------------------------\n"
            + "§r §8< §c§lPURGE TERMINÉE §8> §8§m-----------------------------------------------------\n"
            + buildSummary(top)
            + "§8§m-----------------------------------------------------");

        if (!top.isEmpty()) {
            FactionMessageTitle.sendPlayersTitle(20, 60, 20,
                "§c§lPURGE TERMINÉE",
                "§c" + playerNames.getOrDefault(top.get(0).getKey(), "?")
                    + " §7avec §e" + top.get(0).getValue() + " kills");
        } else {
            FactionMessageTitle.sendPlayersTitle(20, 60, 20,
                "§c§lPURGE TERMINÉE", "§7Aucun vainqueur");
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.playSound(p.getLocation(), Sound.WITHER_DEATH, 1.0f, 1.0f); } catch (Exception ignored) {}
        }

        Economy eco = FactionEvent.getEconomy();
        for (int i = 0; i < top.size(); i++) {
            UUID uuid = top.get(i).getKey();
            int rank = i + 1;
            String path = "purge.top_rewards." + rank;
            double money = cfg.getDouble(path + ".money", 0);
            int min = cfg.getInt(path + ".items_min", 0);
            int max = cfg.getInt(path + ".items_max", 0);
            List<String> items = cfg.getStringList(path + ".items");
            List<LootItemParser.LootEntry> pool = LootItemParser.parse(items);

            String name = playerNames.get(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) name = p.getName();

            if (eco != null && money > 0 && name != null) {
                try { eco.depositPlayer(name, money); } catch (Exception ignored) {}
            }

            List<ItemStack> drops = pickItems(pool, min, max);
            for (ItemStack item : drops) {
                PurgeManager.addItemReward(uuid, item);
            }

            if (p != null) {
                p.sendMessage(prefix + "§6§l" + rank + "ème place §8» §a+§e"
                    + (int) money + "$ §8| §a+§e" + drops.size()
                    + " items §7→ /purgereward");
                Messages.sendTitle(p, 10, 40, 10,
                    "§6§l" + rank + "ème",
                    "§7+§e" + (int) money + "$ §8| §7+§e" + drops.size() + " items");
            }
        }

        // Points classement faction — top 5 uniquement, additionnés par faction
        distributeFactionPoints(top, cfg);
    }

    private void distributeFactionPoints(List<Map.Entry<UUID, Integer>> top, FileConfiguration cfg) {
        int[] defaultPoints = {50, 35, 25, 15, 10};

        // Calcul des points par faction (addition si plusieurs joueurs d'une même faction dans le top 5)
        Map<Faction, Integer> factionPointsMap = new LinkedHashMap<>();
        Map<Faction, List<String>> factionPlayersMap = new LinkedHashMap<>();

        for (int i = 0; i < top.size(); i++) {
            UUID uuid = top.get(i).getKey();
            int pts = cfg.getInt("purge.faction_top_points." + (i + 1), defaultPoints[i]);

            FPlayer fp = FPlayers.getInstance().getByOfflinePlayer(Bukkit.getOfflinePlayer(uuid));
            if (fp == null) continue;
            Faction faction = fp.getFaction();
            if (faction == null || faction.isWilderness()) continue;

            factionPointsMap.merge(faction, pts, Integer::sum);
            factionPlayersMap.computeIfAbsent(faction, k -> new ArrayList<>())
                             .add(playerNames.getOrDefault(uuid, "?") + " §8(#" + (i + 1) + ")");
        }

        for (Map.Entry<Faction, Integer> entry : factionPointsMap.entrySet()) {
            Faction faction = entry.getKey();
            int pts = entry.getValue();
            RankingManager.addPoints(faction, pts);
            String players = String.join("§8, §7", factionPlayersMap.get(faction));
            Bukkit.broadcastMessage(prefix + "§7La faction §c" + faction.getTag()
                + " §7reçoit §a+" + pts + " pts classement §8(§7" + players + "§8)");
        }
    }

    private String buildSummary(List<Map.Entry<UUID, Integer>> top) {
        StringBuilder sb = new StringBuilder();
        if (top.isEmpty()) {
            sb.append("§7Aucun kill enregistré.\n");
            return sb.toString();
        }
        int rank = 1;
        for (Map.Entry<UUID, Integer> e : top) {
            String name = playerNames.getOrDefault(e.getKey(), "?");
            sb.append("§8» §7").append(rank).append(". §c").append(name)
              .append(" §8- §e").append(e.getValue()).append(" §7kills\n");
            rank++;
        }
        return sb.toString();
    }

    private List<ItemStack> pickItems(List<LootItemParser.LootEntry> pool, int min, int max) {
        List<ItemStack> result = new ArrayList<>();
        if (pool == null || pool.isEmpty() || max <= 0) return result;
        if (min < 0) min = 0;
        if (max < min) max = min;
        Random rng = new Random(System.nanoTime());
        int count = (max == min) ? min : (min + rng.nextInt(max - min + 1));
        if (count <= 0) return result;

        List<ItemStack> remainingItems = new ArrayList<>();
        List<Integer> remainingWeights = new ArrayList<>();
        for (LootItemParser.LootEntry e : pool) {
            remainingItems.add(e.item);
            remainingWeights.add(e.weight);
        }
        for (int i = 0; i < count && !remainingItems.isEmpty(); i++) {
            int totalWeight = 0;
            for (int w : remainingWeights) totalWeight += w;
            if (totalWeight <= 0) break;
            int roll = rng.nextInt(totalWeight);
            int cumul = 0;
            int picked = 0;
            for (int j = 0; j < remainingWeights.size(); j++) {
                cumul += remainingWeights.get(j);
                if (roll < cumul) { picked = j; break; }
            }
            result.add(remainingItems.get(picked).clone());
            remainingItems.remove(picked);
            remainingWeights.remove(picked);
        }
        return result;
    }

    public Map<UUID, Integer> getKills() { return kills; }
    public Map<UUID, String> getPlayerNames() { return playerNames; }
    public long getStartTime() { return startTime; }
    public int getDuration() { return duration; }
}
