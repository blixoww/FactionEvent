package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.totem.Totem;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class PlanningManager {

    public static HashMap<String, List<String>> getDailyEvents(String path) {
        HashMap<String, List<String>> map = new HashMap<>();
        FileConfiguration fc = FileManager.getPlanningDataFC();
        if (pathExists(path)) {
            List<String> list = new ArrayList<>();
            for (KOTH event : FactionEvent.getInstance().getListKOTH()) {
                String nom = event.getName();
                String path_event = path + ".koth." + nom;
                if (pathExists(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        list.add(str + "|" + nom);
                    }
                }
            }

            for (Totem event : FactionEvent.getInstance().getListTotem()) {
                String nom = event.getName();
                String path_event = path + ".totem." + nom;
                if (pathExists(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        list.add(str + "|" + nom);
                    }
                }
            }

            for (DTC event : FactionEvent.getInstance().getListDTC()) {
                String nom = event.getName();
                String path_event = path + ".dtc." + nom;
                if (pathExists(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        list.add(str + "|" + nom);
                    }
                }
            }

            for (LMS event : FactionEvent.getInstance().getListLMS()) {
                String nom = event.getName();
                String path_event = path + ".lms." + nom;
                if (pathExists(path_event)) {
                    List<String> stringList = fc.getStringList(path_event);
                    for (String str : stringList) {
                        list.add(str + "|" + nom);
                    }
                }
            }

            if (!list.isEmpty()) {
                map.put("events", list);
            }
        }
        return map;
    }

    public static ArrayList<String> getWeeklyEvents(String path) {
        ArrayList<String> listeEvents = new ArrayList<>();
        FileConfiguration fc = FileManager.getPlanningDataFC();
        String m = "", h = "";
        if(pathExists(path)){
            for(KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                String nom = koth.getName();
                String path_koth = path + ".koth." + nom;
                List<String> stringList = fc.getStringList(path_koth);
                for(String str : stringList){
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§eKOTH §6" + nom + " §7à §c" + h + "h" + m);
                }
            }


            for(Totem totem : FactionEvent.getInstance().getListTotem()){
                String nom = totem.getName();
                String path_totem = path + ".totem." + nom;
                List<String> stringList = fc.getStringList(path_totem);
                for(String str : stringList){
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§eTotem §6" + nom + " §7à §c" + h + "h" + m);
                }
            }


            for(DTC dtc : FactionEvent.getInstance().getListDTC()){
                String nom = dtc.getName();
                String path_dtc = path + ".dtc." + nom;
                List<String> stringList = fc.getStringList(path_dtc);
                for(String str : stringList){
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§eDTC / Nexus §6" + nom + " §7à §c" + h + "h" + m);
                }
            }

            for (LMS lms : FactionEvent.getInstance().getListLMS()) {
                String nom = lms.getName();
                String path_lms = path + ".lms." + nom;
                List<String> stringList = fc.getStringList(path_lms);
                for (String str : stringList) {
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];
                    listeEvents.add("§eLMS §6" + nom + " §7à §c" + h + "h" + m);
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

}
