package fr.blixow.factionevent.utils.fallingchest;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.RankingManager;
import fr.blixow.factionevent.manager.StrManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class FallingChestManager {

    // Indique qu'un spawn est "en cours" (FallingBlock dans les airs ou pose planifiée)
    private static boolean spawning = false;
    private static Location chestLocation = null;
    private static boolean active = false;
    private static boolean firstOpened = false;
    private static BukkitTask timeoutTask = null;
    private static boolean disappearScheduled = false;
    // Timestamp (ms) du prochain spawn, -1 si aucun planifié
    private static long nextSpawnTimestampMs = -1L;

    public static boolean isActive() { return active; }
    public static boolean isSpawning() { return spawning; }
    public static Location getChestLocation() { return chestLocation; }
    public static long getNextSpawnTimestampMs() { return nextSpawnTimestampMs; }

    private static String prefix() {
        return FileManager.getMessageFileConfiguration().getString("falling_chest.prefix", "§8[§6Coffre§8] §7");
    }

    private static String msg(String key) {
        String val = FileManager.getMessageFileConfiguration().getString("falling_chest." + key);
        return val != null ? val : "§c[MSG MANQUANT: falling_chest." + key + "]";
    }

    // Appelé au démarrage du plugin
    public static void startScheduler() {
        scheduleNext();
    }

    // Appelé à l'arrêt du plugin
    public static void onDisable() {
        cancelTimeoutTask();
        if (active && chestLocation != null) {
            removeChest(false);
        }
        spawning = false;
    }

    private static void cancelTimeoutTask() {
        if (timeoutTask != null) { timeoutTask.cancel(); timeoutTask = null; }
    }

    private static void scheduleNext() {
        FileConfiguration config = FileManager.getConfig();
        int minDelay = config.getInt("falling_chest.respawn_delay_min", 3600);
        int maxDelay = config.getInt("falling_chest.respawn_delay_max", 7200);
        if (maxDelay < minDelay) maxDelay = minDelay;
        int delay = minDelay + (int) (Math.random() * (maxDelay - minDelay + 1));

        nextSpawnTimestampMs = System.currentTimeMillis() + (delay * 1000L);
        Bukkit.getConsoleSender().sendMessage("[FactionEvent] Prochain coffre tombant dans " + (delay / 60) + " min " + (delay % 60) + " sec.");

        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
            if (!active && !spawning) {
                nextSpawnTimestampMs = -1L;
                spawn();
            }
        }, delay * 20L);
    }

    /**
     * Démarre le spawn du coffre :
     * 1. Cherche une position d'atterrissage valide.
     * 2. Spawne un FallingBlock visuel depuis le haut.
     * 3. Calcule le temps de chute (physique de Minecraft) et programme la pose du coffre.
     */
    public static void spawn() {
        FileConfiguration config = FileManager.getConfig();
        String worldName = config.getString("falling_chest.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Bukkit.getConsoleSender().sendMessage("[FactionEvent] Falling chest: monde '" + worldName + "' introuvable.");
            scheduleNext();
            return;
        }

        int xMin = config.getInt("falling_chest.range.x_min", -500);
        int xMax = config.getInt("falling_chest.range.x_max", 500);
        int zMin = config.getInt("falling_chest.range.z_min", -500);
        int zMax = config.getInt("falling_chest.range.z_max", 500);
        int yMax = config.getInt("falling_chest.range.y_max", 120);
        int yMin = config.getInt("falling_chest.range.y_min", 5);

        if (xMax <= xMin) xMax = xMin + 1;
        if (zMax <= zMin) zMax = zMin + 1;
        if (yMax < yMin) yMax = yMin + 1;

        Random random = new Random();
        Location targetLoc = null;

        // Jusqu'à 40 tentatives pour trouver un bloc d'air au-dessus d'un bloc solide
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = xMin + random.nextInt(xMax - xMin);
            int z = zMin + random.nextInt(zMax - zMin);

            for (int y = yMax; y >= yMin; y--) {
                Block ground = world.getBlockAt(x, y, z);
                Block above = world.getBlockAt(x, y + 1, z);
                if (ground.getType().isSolid() && above.getType() == Material.AIR) {
                    targetLoc = above.getLocation();
                    break;
                }
            }
            if (targetLoc != null) break;
        }

        if (targetLoc == null) {
            Bukkit.getConsoleSender().sendMessage("[FactionEvent] Falling chest: aucune position valide trouvée, nouvelle tentative planifiée.");
            scheduleNext();
            return;
        }

        // Hauteur de spawn du FallingBlock visuel
        final int targetX = targetLoc.getBlockX();
        final int targetY = targetLoc.getBlockY();
        final int targetZ = targetLoc.getBlockZ();
        int spawnY = world.getMaxHeight() - 5;
        // S'assurer que la colonne est libre (sinon descendre)
        while (spawnY > targetY + 1 && world.getBlockAt(targetX, spawnY, targetZ).getType() != Material.AIR) {
            spawnY--;
        }
        if (spawnY <= targetY) {
            scheduleNext();
            return;
        }

        final int fallDistance = spawnY - targetY; // blocs à tomber

        // Calculer le délai de chute en ticks.
        // Physique Minecraft : v0=0, accélération g=0.04 bloc/tick² (gravité entity),
        // chaque tick: velocity -= 0.04; pos += velocity (avec drag)
        // En pratique pour un FallingBlock, on simule :
        long fallTicks = computeFallTicks(fallDistance);

        spawning = true;
        disappearScheduled = false;

        // Spawn du bloc visuel
        Location spawnLoc = new Location(world, targetX + 0.5, spawnY, targetZ + 0.5);
        FallingBlock fb = world.spawnFallingBlock(spawnLoc, Material.CHEST, (byte) 0);
        fb.setDropItem(false);
        fb.setHurtEntities(false);

        final Location landLocation = new Location(world, targetX, targetY, targetZ);

        // Programmer la pose du coffre au moment de l'atterrissage
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
            // Tuer le FallingBlock visuel s'il existe encore
            if (!fb.isDead()) fb.remove();
            spawning = false;

            // Vérifier que l'emplacement est toujours libre
            Block block = landLocation.getBlock();
            if (block.getType() != Material.AIR) {
                Bukkit.getConsoleSender().sendMessage("[FactionEvent] Falling chest: emplacement occupé à l'atterrissage, réessai planifié.");
                scheduleNext();
                return;
            }

            placeLandedChest(block);
        }, fallTicks);
    }

    /**
     * Calcule le nombre de ticks nécessaires pour qu'une entité tombe d'une hauteur donnée.
     * Simulation de la physique Minecraft (drag 0.98, gravité 0.04 par tick).
     */
    private static long computeFallTicks(int blocks) {
        double velocity = 0.0;
        double pos = 0.0;
        int ticks = 0;
        while (pos < blocks && ticks < 2000) {
            velocity = (velocity - 0.04) * 0.98;
            pos -= velocity; // velocity est négative, pos augmente
            ticks++;
        }
        return Math.max(ticks, 20); // minimum 1 seconde
    }

    /**
     * Pose le coffre sur le bloc d'atterrissage, le remplit et le broadcaste.
     */
    private static void placeLandedChest(Block block) {
        block.setType(Material.CHEST);
        chestLocation = block.getLocation();
        active = true;
        firstOpened = false;
        disappearScheduled = false;

        // Remplir le coffre
        Chest chest = (Chest) block.getState();
        fillInventory(chest.getInventory());

        // Broadcast
        broadcastSpawn();

        // Timeout
        FileConfiguration config = FileManager.getConfig();
        int lifetime = config.getInt("falling_chest.lifetime", 900);
        timeoutTask = Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
            if (active) {
                Bukkit.broadcastMessage(prefix() + msg("timeout"));
                removeChest(true);
            }
        }, lifetime * 20L);
    }

    private static void fillInventory(Inventory inventory) {
        FileConfiguration config = FileManager.getConfig();
        List<String> entries = config.getStringList("falling_chest.items");

        List<ItemStack> loot = new ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length < 2) continue;
            try {
                Material mat = Material.getMaterial(parts[0].toUpperCase());
                int amount = Integer.parseInt(parts[1]);
                if (mat != null && mat != Material.AIR && amount > 0) {
                    loot.add(new ItemStack(mat, amount));
                }
            } catch (NumberFormatException ignored) {}
        }

        Collections.shuffle(loot);

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) slots.add(i);
        Collections.shuffle(slots, new Random());

        for (int i = 0; i < Math.min(loot.size(), slots.size()); i++) {
            inventory.setItem(slots.get(i), loot.get(i));
        }
    }

    // Appelé par le listener quand un joueur ouvre le coffre
    public static void onOpen(Player player) {
        if (!active || chestLocation == null || firstOpened) return;
        firstOpened = true;

        FileConfiguration config = FileManager.getConfig();
        int points = config.getInt("falling_chest.win_points", 5);

        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = (fPlayer != null) ? fPlayer.getFaction() : null;

        if (faction != null && !faction.isWilderness()) {
            RankingManager.addPoints(faction, points);
            String opened = new StrManager(msg("opened_faction"))
                    .rePlayer(player)
                    .reCustom("{faction}", faction.getTag())
                    .reCustom("{points}", String.valueOf(points))
                    .toString();
            Bukkit.broadcastMessage(prefix() + opened);
        } else {
            String opened = new StrManager(msg("opened"))
                    .rePlayer(player)
                    .toString();
            Bukkit.broadcastMessage(prefix() + opened);
        }
    }

    // Appelé par le listener quand un joueur ferme le coffre
    public static void onClose(Player player) {
        if (!active || chestLocation == null || disappearScheduled) return;

        Block block = chestLocation.getBlock();
        if (block.getType() != Material.CHEST) {
            removeChest(true);
            return;
        }

        Chest chest = (Chest) block.getState();
        if (!isInventoryEmpty(chest.getInventory())) return;

        // Le coffre est vide — disparition après un court délai
        disappearScheduled = true;
        FileConfiguration config = FileManager.getConfig();
        int disappearDelay = config.getInt("falling_chest.disappear_delay", 5);

        String looted = new StrManager(msg("looted")).rePlayer(player).toString();
        Bukkit.broadcastMessage(prefix() + looted);

        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
            if (active) removeChest(true);
        }, disappearDelay * 20L);
    }

    private static boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) return false;
        }
        return true;
    }

    // Suppression forcée depuis une commande admin (sans replanifier)
    public static void forceRemove() {
        spawning = false;
        removeChest(false);
    }

    private static void removeChest(boolean doScheduleNext) {
        if (chestLocation != null) {
            Block block = chestLocation.getBlock();
            if (block.getType() == Material.CHEST) {
                Chest chest = (Chest) block.getState();
                chest.getInventory().clear();
                block.setType(Material.AIR);
            }
        }
        active = false;
        firstOpened = false;
        disappearScheduled = false;
        chestLocation = null;
        cancelTimeoutTask();
        if (doScheduleNext) scheduleNext();
    }

    private static void broadcastSpawn() {
        FileConfiguration cfg = FileManager.getMessageFileConfiguration();
        String bar = cfg.getString("falling_chest.bar", "§8§m-------------------------------------------------");
        String title = cfg.getString("falling_chest.spawn_title", "§r §8< §6§lCoffre Tombant §8>");
        String spawnMsg = new StrManager(msg("spawn"))
                .reCustom("{x}", String.valueOf(chestLocation.getBlockX()))
                .reCustom("{y}", String.valueOf(chestLocation.getBlockY()))
                .reCustom("{z}", String.valueOf(chestLocation.getBlockZ()))
                .toString();
        int lifetime = FileManager.getConfig().getInt("falling_chest.lifetime", 900);
        String lifetimeMsg = new StrManager(msg("spawn_lifetime"))
                .reCustom("{minutes}", String.valueOf(lifetime / 60))
                .toString();

        Bukkit.broadcastMessage(bar);
        Bukkit.broadcastMessage(title);
        Bukkit.broadcastMessage(prefix() + spawnMsg);
        Bukkit.broadcastMessage(prefix() + lifetimeMsg);
        Bukkit.broadcastMessage(bar);
    }
}
