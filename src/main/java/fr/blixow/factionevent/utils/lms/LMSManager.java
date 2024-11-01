package fr.blixow.factionevent.utils.lms;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LMSManager {

    public static boolean isLMSStarted(LMS lms) {
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        return !eventOn.canStartAnEvent() && eventOn.getLMSEvent() != null && eventOn.getLMSEvent().getLMS().equals(lms);
    }

    public static LMS getLMS(String lmsName) {
        return FactionEvent.getInstance().getListLMS()
                .stream()
                .filter(lms -> lms.getName().equalsIgnoreCase(lmsName))
                .findFirst()
                .orElse(null);
    }

    public static LMS getStartingRegistration() {
        return FactionEvent.getInstance().getListLMS()
                .stream()
                .filter(LMS::isRegistration)
                .findFirst()
                .orElse(null);
    }

    public static List<String> getListLMSNames() {
        return FactionEvent.getInstance().getListLMS().stream()
                .map(LMS::getName)
                .collect(Collectors.toList());
    }

    public static void loadLMSfromFile() {
        ArrayList<LMS> lmsArrayList = new ArrayList<>();
        try {
            FileConfiguration fc = FileManager.getLMSDataFC();
            if (fc.contains("lmslist")) {
                List<String> lmslist = fc.getStringList("lmslist");
                for (String lms : lmslist) {
                    try {
                        String world_name = fc.getString(lms + ".worldname");
                        int pos1_x = fc.getInt(lms + ".arenaLocation.x");
                        int pos1_y = fc.getInt(lms + ".arenaLocation.y");
                        int pos1_z = fc.getInt(lms + ".arenaLocation.z");
                        Location loc1 = new Location(Bukkit.getServer().getWorld(world_name), pos1_x, pos1_y, pos1_z);
                        lmsArrayList.add(new LMS(lms, loc1));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        FactionEvent.getInstance().setListLMS(lmsArrayList);
    }
}
