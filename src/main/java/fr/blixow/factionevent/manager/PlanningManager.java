package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.totem.Totem;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class PlanningManager {

    /**
     * Entrée de planning avec heure, minutes, type et nom — comparable pour trier.
     */
    public static class PlanningEntry implements Comparable<PlanningEntry> {
        public final int totalMinutes; // heure * 60 + minutes
        public final String displayLine;

        public PlanningEntry(int heure, int minutes, String displayLine) {
            this.totalMinutes = heure * 60 + minutes;
            this.displayLine = displayLine;
        }

        @Override
        public int compareTo(PlanningEntry other) {
            return Integer.compare(this.totalMinutes, other.totalMinutes);
        }
    }

    public static HashMap<String, List<String>> getDailyEvents(String path) {
        HashMap<String, List<String>> map = new HashMap<>();
        FileConfiguration fc = FileManager.getPlanningDataFC();
        if (fc.contains(path)) {
            List<String> listKoth = new ArrayList<>();
            for (KOTH event : FactionEvent.getInstance().getListKOTH()) {
                String nom = event.getName();
                String path_event = path + ".koth." + nom;
                if (fc.contains(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        listKoth.add(str + "|" + nom);
                    }
                }
            }
            if (!listKoth.isEmpty()) map.put("koth", listKoth);

            List<String> listTotem = new ArrayList<>();
            for (Totem event : FactionEvent.getInstance().getListTotem()) {
                String nom = event.getName();
                String path_event = path + ".totem." + nom;
                if (fc.contains(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        listTotem.add(str + "|" + nom);
                    }
                }
            }
            if (!listTotem.isEmpty()) map.put("totem", listTotem);

            List<String> listDtc = new ArrayList<>();
            for (DTC event : FactionEvent.getInstance().getListDTC()) {
                String nom = event.getName();
                String path_event = path + ".dtc." + nom;
                if (fc.contains(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        listDtc.add(str + "|" + nom);
                    }
                }
            }
            if (!listDtc.isEmpty()) map.put("dtc", listDtc);

            List<String> listLms = new ArrayList<>();
            for (LMS event : FactionEvent.getInstance().getListLMS()) {
                String nom = event.getName();
                String path_event = path + ".lms." + nom;
                if (fc.contains(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        listLms.add(str + "|" + nom);
                    }
                }
            }
            if (!listLms.isEmpty()) map.put("lms", listLms);
        }
        return map;
    }

    /**
     * Retourne les events du jour triés par heure croissante.
     * Format de chaque ligne : "§eKOTH §6nom §7à §c08h30"
     */
    public static ArrayList<String> getWeeklyEvents(String path) {
        List<PlanningEntry> entries = new ArrayList<>();
        FileConfiguration fc = FileManager.getPlanningDataFC();

        if (fc.contains(path)) {
            // KOTH
            for (KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                String nom = koth.getName();
                List<String> stringList = fc.getStringList(path + ".koth." + nom);
                for (String str : stringList) {
                    PlanningEntry e = parseEntry(str, "§eKOTH §6" + nom);
                    if (e != null) entries.add(e);
                }
            }
            // Totem
            for (Totem totem : FactionEvent.getInstance().getListTotem()) {
                String nom = totem.getName();
                List<String> stringList = fc.getStringList(path + ".totem." + nom);
                for (String str : stringList) {
                    PlanningEntry e = parseEntry(str, "§eTotem §6" + nom);
                    if (e != null) entries.add(e);
                }
            }
            // DTC
            for (DTC dtc : FactionEvent.getInstance().getListDTC()) {
                String nom = dtc.getName();
                List<String> stringList = fc.getStringList(path + ".dtc." + nom);
                for (String str : stringList) {
                    PlanningEntry e = parseEntry(str, "§eDTC §6" + nom);
                    if (e != null) entries.add(e);
                }
            }
            // LMS
            for (LMS lms : FactionEvent.getInstance().getListLMS()) {
                String nom = lms.getName();
                List<String> stringList = fc.getStringList(path + ".lms." + nom);
                for (String str : stringList) {
                    PlanningEntry e = parseEntry(str, "§eLMS §6" + nom);
                    if (e != null) entries.add(e);
                }
            }
        }

        Collections.sort(entries);

        ArrayList<String> result = new ArrayList<>();
        for (PlanningEntry e : entries) {
            result.add(e.displayLine);
        }
        return result;
    }

    /**
     * Parse une chaîne "Xh Y" ou "XhY" et retourne un PlanningEntry trié.
     */
    private static PlanningEntry parseEntry(String str, String typeAndName) {
        try {
            String[] parts = str.split("h");
            int heure = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            String h = heure < 10 ? "0" + heure : String.valueOf(heure);
            String m = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
            String display = typeAndName + " §7à §c" + h + "h" + m;
            return new PlanningEntry(heure, minutes, display);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDate(String path) {
        if (pathExists(path + ".date")) {
            return FileManager.getFileConfiguration("data/planning.yml").getString(path + ".date");
        }
        return null;
    }

    public static boolean pathExists(String path) {
        return FileManager.getFileConfiguration("data/planning.yml").contains(path);
    }

}
