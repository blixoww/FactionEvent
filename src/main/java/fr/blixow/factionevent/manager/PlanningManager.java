package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.totem.Totem;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlanningManager {

    public static HashMap<String, List<String>> getDailyEvents(String path) {
        HashMap<String, List<String>> map = new HashMap<>();
        FileConfiguration fc = FileManager.getPlanningDataFC();
        if (pathExists(path)) {
            List<String> list = new ArrayList<>();
            List<String> listTotem = new ArrayList<>();
            List<String> listDTC = new ArrayList<>();
            List<String> listMeteorite = new ArrayList<>();
            for (KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                String nom = koth.getName();
                String path_koth = path + ".koth." + nom;
                if (pathExists(path_koth)) {
                    List<String> stringList = fc.getStringList(path_koth);
                    for (String str : stringList) {
                        list.add(str + "|" + nom);
                    }
                }
            }

            for (Totem totem : FactionEvent.getInstance().getListTotem()) {
                String nom = totem.getName();
                String path_totem = path + ".totem." + nom;
                if (pathExists(path_totem)) {
                    List<String> stringList = fc.getStringList(path_totem);
                    for (String str : stringList) {
                        listTotem.add(str + "|" + nom);
                    }
                }
            }

            for (DTC dtc : FactionEvent.getInstance().getListDTC()) {
                String nom = dtc.getName();
                String path_dtc = path + ".dtc." + nom;
                if (pathExists(path_dtc)) {
                    List<String> stringList = fc.getStringList(path_dtc);
                    for (String str : stringList) {
                        listDTC.add(str + "|" + nom);
                    }
                }
            }

            for (Meteorite meteorite : FactionEvent.getInstance().getListMeteorite()) {
                String nom = meteorite.getName();
                String path_meteorite = path + ".meteorite." + nom;
                if (pathExists(path_meteorite)) {
                    List<String> stringList = fc.getStringList(path_meteorite);
                    for (String str : stringList) {
                        listMeteorite.add(str + "|" + nom);
                    }
                }
            }
            if (!list.isEmpty()) {
                map.put("koth", list);
            }
            if (!listTotem.isEmpty()) {
                map.put("totem", listTotem);
            }
            if (!listDTC.isEmpty()) {
                map.put("dtc", listDTC);
            }
            if (!listMeteorite.isEmpty()) {
                map.put("meteorite", listMeteorite);
            }

        }
        return map;
    }

    public static ArrayList<String> getWeeklyEvents(String path) {
        ArrayList<String> listeEvents = new ArrayList<>();
        FileConfiguration fc = FileManager.getPlanningDataFC();
        if (pathExists(path)) {
            for (KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                String nom = koth.getName();
                String path_koth = path + ".koth." + nom;
                List<String> stringList = fc.getStringList(path_koth);
                for (String str : stringList) {
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§e- §cKOTH §6" + nom + " §7à §e" + h + "h" + m);
                }
            }

            for (Totem totem : FactionEvent.getInstance().getListTotem()) {
                String nom = totem.getName();
                String path_totem = path + ".totem." + nom;
                List<String> stringList = fc.getStringList(path_totem);
                for (String str : stringList) {
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§e- §cTotem §6" + nom + " §7à §e" + h + "h" + m);
                }
            }


            for (DTC dtc : FactionEvent.getInstance().getListDTC()) {
                String nom = dtc.getName();
                String path_dtc = path + ".dtc." + nom;
                List<String> stringList = fc.getStringList(path_dtc);
                for (String str : stringList) {
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§e- §cDTC §6" + nom + " §7à §e" + h + "h" + m);
                }
            }


            for (Meteorite meteorite : FactionEvent.getInstance().getListMeteorite()) {
                String nom = meteorite.getName();
                String path_meteorite = path + ".meteorite." + nom;
                List<String> stringList = fc.getStringList(path_meteorite);
                for (String str : stringList) {
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§e- §cMétéorite §6" + nom + " §7à §e" + h + "h" + m);
                }
            }
        }
        return listeEvents;
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

    public static boolean timeNotTaken(String path, String date) {
        FileConfiguration fc = FileManager.getFileConfiguration("data/planning.yml");
        List<String> koth_list = new ArrayList<>(), meteorite_liste = new ArrayList<>(), dtc_list = new ArrayList<>(), totem_list = new ArrayList<>();
        String koth_path = path + ".koth", dtc_path = path + ".dtc", meteorite_path = path + ".meteorite", totem_path = path + ".totem";
        if (fc.contains(koth_path)) {
            for (KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                String nom = koth.getName();
                if (fc.contains(koth_path + "." + nom)) {
                    koth_list = fc.getStringList(koth_path + "." + nom);
                    if (koth_list.contains(date)) {
                        return false;
                    }
                }
            }
        }
        if (fc.contains(koth_path)) {
            koth_list = fc.getStringList(koth_path);
        }
        if (fc.contains(dtc_path)) {
            dtc_list = fc.getStringList(dtc_path);
        }
        if (fc.contains(meteorite_path)) {
            meteorite_liste = fc.getStringList(meteorite_path);
        }
        if (fc.contains(totem_path)) {
            totem_list = fc.getStringList(totem_path);
        }
        for (String dtc : dtc_list) {
            if (dtc.equalsIgnoreCase(date)) {
                return false;
            }
        }
        for (String totem : totem_list) {
            if (totem.equalsIgnoreCase(date)) {
                return false;
            }
        }
        for (String meteorite : meteorite_liste) {
            if (meteorite.equalsIgnoreCase(date)) {
                return false;
            }
        }
        for (String koth : koth_list) {
            if (koth.equalsIgnoreCase(date)) {
                return false;
            }
        }
        return true;
    }

}
