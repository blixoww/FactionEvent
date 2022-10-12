package fr.blixow.factionevent.utils.totem;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.CastUtils;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TotemManager {

    public static boolean isTotemStarted(Totem totem) {
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        try {
            return !eventOn.canStartAnEvent() && eventOn.getTotemEvent() != null && eventOn.getTotemEvent().getTotem().equals(totem);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static ArrayList<String> getListTotemNames() {
        ArrayList<String> stringArrayList = new ArrayList<>();
        for (Totem totem : FactionEvent.getInstance().getListTotem()) {
            stringArrayList.add(totem.getName());
        }
        return stringArrayList;
    }

    public static boolean exist(Totem totem) {
        return FactionEvent.getInstance().getListTotem().contains(totem);
    }

    public static Totem getTotem(String nom) {
        ArrayList<Totem> totemList = FactionEvent.getInstance().getListTotem();
        for (Totem totem : totemList) {
            if (totem.getName().equalsIgnoreCase(nom)) {
                return totem;
            }
        }
        return null;
    }

    public static boolean addTotem(Totem totem) {
        try {
            if (FactionEvent.getInstance().getListTotem().contains(totem)) {
                return false;
            }
            FileConfiguration fc = FileManager.getTotemDataFC();
            List<String> list_totem = new ArrayList<>();
            if (fc.contains("totemlist")) {
                list_totem = fc.getStringList("totemlist");
            }
            list_totem.add(totem.getName());
            fc.set("totemlist", list_totem);
            fc.save(FileManager.getDataFile("totem.yml"));
            return FactionEvent.getInstance().getListTotem().add(totem);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public static TotemEvent getTotemEventByTotem(Totem totem) {
        if (isTotemStarted(totem)) {
            return FactionEvent.getInstance().getEventOn().getTotemEvent();
        }
        return null;
    }

    public static boolean removeTotem(Totem totem) {
        if (!FactionEvent.getInstance().getListTotem().contains(totem)) {
            return false;
        }
        return FactionEvent.getInstance().getListTotem().remove(totem);
    }

    public static void loadTotems() {
        try {
            FileConfiguration fc = FileManager.getTotemDataFC();
            List<String> totemList = new ArrayList<>();
            if (fc.contains("totemlist")) {
                totemList = fc.getStringList("totemlist");
                for (String totemName : totemList) {
                    String worldName = fc.getString(totemName + ".worldname");
                    int x = fc.getInt(totemName + ".position.x");
                    int y = fc.getInt(totemName + ".position.y");
                    int z = fc.getInt(totemName + ".position.z");
                    Location location = new Location(Bukkit.getServer().getWorld(worldName), x, y, z);
                    Bukkit.getConsoleSender().sendMessage(location.toString());
                    List<String> blocksListLocation = new ArrayList<>();
                    HashMap<Location, Material> blocks = new HashMap<>();
                    if (fc.contains(totemName + ".blockslist")) {
                        blocksListLocation = fc.getStringList(totemName + ".blockslist");
                    }
                    for (String blockPos : blocksListLocation) {
                        Location loc = CastUtils.getLocationFromStringFormat(worldName, blockPos);
                        Material material = Material.valueOf(fc.getString(totemName + ".blocks." + blockPos + ".type"));
                        blocks.put(loc, material);
                    }
                    Totem totem = new Totem(totemName, location, blocks);
                    FactionEvent.getInstance().getListTotem().add(totem);
                }
            } else {
                FactionEvent.getInstance().getLogger().info("Aucun totem n'a été récupéré depuis le fichier de config : totem.yml");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
