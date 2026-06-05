package fr.blixow.factionevent.utils.relic;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Registre statique des points de spawn de la Relique + persistance YAML (data/relic.yml).
 * Mêmes conventions que {@link fr.blixow.factionevent.utils.domination.DominationManager}.
 */
public class RelicManager {

    private static final Random RANDOM = new Random();

    public static void loadSpawns() {
        ArrayList<RelicSpawn> list = new ArrayList<>();
        try {
            FileConfiguration fc = FileManager.getRelicDataFC();
            if (fc.contains("spawns")) {
                List<String> names = fc.getStringList("spawns");
                for (String name : names) {
                    try {
                        String world = fc.getString(name + ".world", "world");
                        double x = fc.getDouble(name + ".x");
                        double y = fc.getDouble(name + ".y");
                        double z = fc.getDouble(name + ".z");
                        float yaw = (float) fc.getDouble(name + ".yaw", 0);
                        float pitch = (float) fc.getDouble(name + ".pitch", 0);
                        Location loc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
                        list.add(new RelicSpawn(name, loc));
                        Bukkit.getConsoleSender().sendMessage("[Relique] Spawn chargé : " + name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        FactionEvent.getInstance().setListRelicSpawns(list);
    }

    public static boolean saveSpawn(RelicSpawn spawn) {
        try {
            File file = FileManager.getDataFile("relic.yml");
            FileConfiguration fc = FileManager.getRelicDataFC();
            List<String> spawns = new ArrayList<>();
            if (fc.contains("spawns")) spawns = fc.getStringList("spawns");
            if (!spawns.contains(spawn.getName())) spawns.add(spawn.getName());
            fc.set("spawns", spawns);
            String n = spawn.getName();
            if (spawn.getLocation() != null) {
                fc.set(n + ".world", spawn.getLocation().getWorld().getName());
                fc.set(n + ".x", spawn.getLocation().getX());
                fc.set(n + ".y", spawn.getLocation().getY());
                fc.set(n + ".z", spawn.getLocation().getZ());
                fc.set(n + ".yaw", spawn.getLocation().getYaw());
                fc.set(n + ".pitch", spawn.getLocation().getPitch());
            }
            fc.save(file);
            FactionEvent.getInstance().setRelicFileConfiguration(fc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteSpawn(String name) {
        try {
            File file = FileManager.getDataFile("relic.yml");
            FileConfiguration fc = FileManager.getRelicDataFC();
            List<String> spawns = new ArrayList<>();
            if (fc.contains("spawns")) spawns = new ArrayList<>(fc.getStringList("spawns"));
            spawns.removeIf(s -> s.equalsIgnoreCase(name));
            fc.set("spawns", spawns);
            // Supprimer la clé en respectant la casse réelle stockée
            for (String key : new ArrayList<>(fc.getKeys(false))) {
                if (key.equalsIgnoreCase(name)) fc.set(key, null);
            }
            fc.save(file);
            FactionEvent.getInstance().getListRelicSpawns().removeIf(s -> s.getName().equalsIgnoreCase(name));
            FactionEvent.getInstance().setRelicFileConfiguration(fc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static RelicSpawn getSpawn(String name) {
        return FactionEvent.getInstance().getListRelicSpawns().stream()
            .filter(s -> s.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public static List<String> getSpawnNames() {
        return FactionEvent.getInstance().getListRelicSpawns().stream()
            .map(RelicSpawn::getName)
            .collect(Collectors.toList());
    }

    public static RelicSpawn getRandomSpawn() {
        List<RelicSpawn> spawns = FactionEvent.getInstance().getListRelicSpawns();
        if (spawns == null || spawns.isEmpty()) return null;
        return spawns.get(RANDOM.nextInt(spawns.size()));
    }

    public static boolean isRelicStarted() {
        return FactionEvent.getInstance().getEventOn().getRelicEvent() != null;
    }
}
