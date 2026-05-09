package fr.blixow.factionevent.utils.lms;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Stockage centralisé des récompenses LMS en attente de réclamation (/lmsreward).
 */
public class LMSRewardManager {

    private static final HashMap<UUID, List<ItemStack>> pendingRewards = new HashMap<>();

    public static void addItemReward(UUID uuid, ItemStack item) {
        if (uuid == null || item == null) return;
        pendingRewards.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item.clone());
        saveRewards();
    }

    public static List<ItemStack> getRewards(UUID uuid) {
        List<ItemStack> list = pendingRewards.get(uuid);
        return list == null ? Collections.emptyList() : list;
    }

    public static boolean hasRewards(UUID uuid) {
        List<ItemStack> list = pendingRewards.get(uuid);
        return list != null && !list.isEmpty();
    }

    public static List<ItemStack> claimAll(UUID uuid) {
        List<ItemStack> list = pendingRewards.remove(uuid);
        saveRewards();
        return list == null ? new ArrayList<>() : list;
    }

    public static List<ItemStack> claimUpTo(UUID uuid, int max) {
        List<ItemStack> list = pendingRewards.get(uuid);
        if (list == null || list.isEmpty() || max <= 0) return new ArrayList<>();
        int n = Math.min(max, list.size());
        List<ItemStack> head = new ArrayList<>(list.subList(0, n));
        List<ItemStack> tail = new ArrayList<>(list.subList(n, list.size()));
        if (tail.isEmpty()) pendingRewards.remove(uuid);
        else pendingRewards.put(uuid, tail);
        saveRewards();
        return head;
    }

    public static void loadRewards() {
        pendingRewards.clear();
        try {
            FileConfiguration fc = FileManager.getLMSRewardsDataFC();
            if (fc == null) return;
            ConfigurationSection sec = fc.getConfigurationSection("rewards");
            if (sec == null) return;
            for (String key : sec.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<?> raw = fc.getList("rewards." + key);
                    if (raw == null) continue;
                    List<ItemStack> items = new ArrayList<>();
                    for (Object o : raw) {
                        if (o instanceof ItemStack) items.add((ItemStack) o);
                    }
                    if (!items.isEmpty()) pendingRewards.put(uuid, items);
                } catch (Exception ignored) {}
            }
            Bukkit.getConsoleSender().sendMessage("[LMS] " + pendingRewards.size()
                + " joueur(s) avec des récompenses en attente.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void saveRewards() {
        try {
            File file = FileManager.getDataFile("lmsRewards.yml");
            FileConfiguration fc = FileManager.getLMSRewardsDataFC();
            if (fc == null) return;
            fc.set("rewards", null);
            for (Map.Entry<UUID, List<ItemStack>> entry : pendingRewards.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    fc.set("rewards." + entry.getKey().toString(), entry.getValue());
                }
            }
            fc.save(file);
            FactionEvent.getInstance().setLMSRewardsFileConfiguration(fc);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static HashMap<UUID, List<ItemStack>> getPendingRewards() {
        return pendingRewards;
    }
}

