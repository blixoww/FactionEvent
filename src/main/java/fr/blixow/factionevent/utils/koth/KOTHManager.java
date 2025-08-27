package fr.blixow.factionevent.utils.koth;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class KOTHManager {

    public static boolean isKOTHStarted(KOTH koth) {
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        return !eventOn.canStartAnEvent() && eventOn.getKothEvent() != null && eventOn.getKothEvent().getKoth().equals(koth);
        //return FactionEvent.getInstance().getListActiveKOTH().containsKey(koth);
    }

    public static KOTHEvent getKOTHEventFromKOTH(KOTH koth) {
        if (isKOTHStarted(koth)) {
            return FactionEvent.getInstance().getEventOn().getKothEvent();
        }
        return null;
    }

    public static KOTH getKOTH(String kothName) {
        return FactionEvent.getInstance().getListKOTH()
            .stream()
            .filter(koth -> koth.getName().equalsIgnoreCase(kothName))
            .findFirst()
            .orElse(null);
    }

    public static List<String> getListKothNames() {
        return FactionEvent.getInstance().getListKOTH().stream()
            .map(KOTH::getName)
            .collect(Collectors.toList());
    }

    public static boolean isInKOTH(Player player, KOTH koth) {
        double x0 = player.getLocation().getX(), y0 = player.getLocation().getY(), z0 = player.getLocation().getZ();
        Location loc1 = koth.getPos1();
        double x1 = loc1.getX(), y1 = loc1.getY(), z1 = loc1.getZ();
        Location loc2 = koth.getPos2();
        double x2 = loc2.getX(), y2 = loc2.getY(), z2 = loc2.getZ();
        boolean x_valid = isBetween(x0, x1, x2);
        boolean y_valid = isBetween(y0, y1, y2);
        boolean z_valid = isBetween(z0, z1, z2);
        return (x_valid && y_valid && z_valid);
    }

    private static boolean isBetween(double pos, double val1, double val2) {
        if (val1 <= val2) {
            return (val1 <= pos && pos <= val2);
        }
        return isBetween(pos, val2, val1);
    }

    public static void loadKOTH() {
        ArrayList<KOTH> kothArrayList = new ArrayList<>();
        try {
            FileConfiguration fc = FileManager.getKothDataFC();
            if (fc.contains("kothlist")) {
                List<String> kothNameList = fc.getStringList("kothlist");
                for (String kn : kothNameList) {
                    try {
                        System.out.println("Chargement du KOTH : " + kn);
                        // Nom du monde
                        String world_name = fc.getString(kn + ".worldname");
                        // Position 1
                        int pos1_x = fc.getInt(kn + ".pos1.x");
                        int pos1_y = fc.getInt(kn + ".pos1.y");
                        int pos1_z = fc.getInt(kn + ".pos1.z");
                        Location loc1 = new Location(Bukkit.getServer().getWorld(world_name), pos1_x, pos1_y, pos1_z);
                        // Position 2
                        int pos2_x = fc.getInt(kn + ".pos2.x");
                        int pos2_y = fc.getInt(kn + ".pos2.y");
                        int pos2_z = fc.getInt(kn + ".pos2.z");
                        Location loc2 = new Location(Bukkit.getServer().getWorld(world_name), pos2_x, pos2_y, pos2_z);
                        // Ajouter le koth à la liste
                        kothArrayList.add(new KOTH(kn, loc1, loc2));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        FactionEvent.getInstance().setListKOTH(kothArrayList);
    }
}
