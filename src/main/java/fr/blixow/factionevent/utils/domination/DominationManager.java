package fr.blixow.factionevent.utils.domination;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DominationManager {

    public static void loadZones() {
        ArrayList<DominationZone> list = new ArrayList<>();
        try {
            FileConfiguration fc = FileManager.getDominationDataFC();
            if (fc.contains("zones")) {
                List<String> names = fc.getStringList("zones");
                for (String name : names) {
                    try {
                        String world = fc.getString(name + ".world", "world");
                        int x1 = fc.getInt(name + ".pos1.x");
                        int y1 = fc.getInt(name + ".pos1.y");
                        int z1 = fc.getInt(name + ".pos1.z");
                        int x2 = fc.getInt(name + ".pos2.x");
                        int y2 = fc.getInt(name + ".pos2.y");
                        int z2 = fc.getInt(name + ".pos2.z");
                        boolean enabled = fc.getBoolean(name + ".enabled", false);

                        Location loc1 = new Location(Bukkit.getWorld(world), x1, y1, z1);
                        Location loc2 = new Location(Bukkit.getWorld(world), x2, y2, z2);
                        list.add(new DominationZone(name, loc1, loc2, enabled));
                        Bukkit.getConsoleSender().sendMessage("[Domination] Zone chargée : " + name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        FactionEvent.getInstance().setListDominationZones(list);
    }

    public static boolean saveZone(DominationZone zone) {
        try {
            File file = FileManager.getDataFile("domination.yml");
            FileConfiguration fc = FileManager.getDominationDataFC();
            List<String> zones = new ArrayList<>();
            if (fc.contains("zones")) zones = fc.getStringList("zones");
            if (!zones.contains(zone.getName())) zones.add(zone.getName());

            fc.set("zones", zones);
            String n = zone.getName();
            if (zone.getPos1() != null) {
                fc.set(n + ".world", zone.getPos1().getWorld().getName());
                fc.set(n + ".pos1.x", zone.getPos1().getBlockX());
                fc.set(n + ".pos1.y", zone.getPos1().getBlockY());
                fc.set(n + ".pos1.z", zone.getPos1().getBlockZ());
            }
            if (zone.getPos2() != null) {
                fc.set(n + ".pos2.x", zone.getPos2().getBlockX());
                fc.set(n + ".pos2.y", zone.getPos2().getBlockY());
                fc.set(n + ".pos2.z", zone.getPos2().getBlockZ());
            }
            fc.set(n + ".enabled", zone.isEnabled());
            fc.save(file);
            // Reload in memory
            FactionEvent.getInstance().setDominationFileConfiguration(fc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteZone(String name) {
        try {
            File file = FileManager.getDataFile("domination.yml");
            FileConfiguration fc = FileManager.getDominationDataFC();
            List<String> zones = new ArrayList<>();
            if (fc.contains("zones")) zones = new ArrayList<>(fc.getStringList("zones"));
            zones.remove(name);
            fc.set("zones", zones);
            fc.set(name, null);
            fc.save(file);
            FactionEvent.getInstance().getListDominationZones().removeIf(z -> z.getName().equalsIgnoreCase(name));
            FactionEvent.getInstance().setDominationFileConfiguration(fc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static DominationZone getZone(String name) {
        return FactionEvent.getInstance().getListDominationZones().stream()
            .filter(z -> z.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public static List<String> getZoneNames() {
        return FactionEvent.getInstance().getListDominationZones().stream()
            .map(DominationZone::getName)
            .collect(Collectors.toList());
    }

    public static List<DominationZone> getEnabledZones() {
        return FactionEvent.getInstance().getListDominationZones().stream()
            .filter(DominationZone::isEnabled)
            .collect(Collectors.toList());
    }

    public static boolean isDominationStarted() {
        return FactionEvent.getInstance().getEventOn().getDominationEvent() != null;
    }
}
