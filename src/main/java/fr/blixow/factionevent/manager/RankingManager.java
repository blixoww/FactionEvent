package fr.blixow.factionevent.manager;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import fr.blixow.factionevent.FactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class RankingManager {

    public static int getPoints(Faction faction){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".points";
            if(fc.contains(path)){ return Integer.parseInt(fc.getString(path)); }
            return 0;
        } catch (Exception exception){
            exception.printStackTrace();
            return 0;
        }
    }

    public static void addPoints(Faction faction, int points){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".points";
            int current_points = getPoints(faction);
            current_points += points;
            fc.set(path, current_points);
            fc.save(FileManager.getDataFile("classement.yml"));
            faction.sendMessage("§a+" + points + " points §7classement ajouté à votre faction.");
            logsMessage(faction.getTag() + " (ID=" + faction.getId() + ") : " + points + " points classement ajouté à votre faction.");
        } catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public static int getKothWins(Faction faction){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".koth";
            if(fc.contains(path)){ return Integer.parseInt(fc.getString(path)); }
            return 0;
        } catch (Exception exception){
            exception.printStackTrace();
            return 0;
        }
    }

    public static void addKothWins(Faction faction){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".koth";
            int koth_wins = getKothWins(faction) + 1;
            fc.set(path, koth_wins);
            fc.save(FileManager.getDataFile("classement.yml"));
        } catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public static int getTotemWins(Faction faction){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".totem";
            if(fc.contains(path)){ return Integer.parseInt(fc.getString(path)); }
            return 0;
        } catch (Exception exception){
            exception.printStackTrace();
            return 0;
        }
    }

    public static void addTotemWins(Faction faction){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".totem";
            int totem_wins = getTotemWins(faction) + 1;
            fc.set(path, totem_wins);
            fc.save(FileManager.getDataFile("classement.yml"));
        } catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public static int getDTCWins(Faction faction){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".dtc";
            if(fc.contains(path)){ return Integer.parseInt(fc.getString(path)); }
            return 0;
        } catch (Exception exception){
            exception.printStackTrace();
            return 0;
        }
    }

    public static void addDTCWins(Faction faction){
        try {
            FileConfiguration fc = FileManager.getClassementFC();
            String path = faction.getId() + ".dtc";
            int dtc_wins = getDTCWins(faction) + 1;
            fc.set(path, dtc_wins);
            fc.save(FileManager.getDataFile("classement.yml"));
        } catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public static void logsMessage(String message){
        try {
            FileConfiguration logs = FileManager.getLogsFC();
            File file = FileManager.getLogsFile();
            LocalDateTime localDateTime = LocalDateTime.now();
            String path = localDateTime.getHour() + "h";
            List<String> stringList = new ArrayList<>();
            if(logs.contains(path)){ stringList = logs.getStringList(path); }
            stringList.add(message);
            logs.set(path, stringList);
            logs.save(file);
            //Bukkit.broadcastMessage("LOGS: " + message);
        } catch (Exception exception){
            exception.printStackTrace();
        }

    }

    public static String getFactionsInformations(FileConfiguration fc, String id){
        try {
            if(fc.contains(id)){
                String points = "0", koth = "0", totem = "0", dtc = "0", meteorite = "0";
                if(fc.contains(id + ".points")){ points = String.valueOf(fc.getInt(id + ".points")); }
                if(fc.contains(id + ".koth")){ koth = String.valueOf(fc.getInt(id + ".koth")); }
                if(fc.contains(id + ".totem")){ totem = String.valueOf(fc.getInt(id + ".totem")); }
                if(fc.contains(id + ".dtc")){ dtc = String.valueOf(fc.getInt(id + ".dtc")); }
                if(fc.contains(id + ".meteorite")){ meteorite = String.valueOf(fc.getInt(id + ".meteorite")); }
                return points + "-" + koth + "-" + totem + "-" + dtc + "-" + meteorite;
            }
        } catch (Exception exception){ exception.printStackTrace(); }
        return "0-0-0-0-0";
    }

    public static void updateRanking(){
        FileConfiguration fileConfiguration = FileManager.getClassementFC();
        LinkedHashMap<Faction, Integer> factionRankings = new LinkedHashMap<>();
        for(Faction faction : Factions.getInstance().getAllFactions()){
            if(!faction.isWilderness() && !faction.isSafeZone() && !faction.isWarZone()){
                String[] factionInformations = getFactionsInformations(fileConfiguration, faction.getId()).split("-");
                int points = 0;
                try {
                    if(factionInformations.length == 5){
                        points = Integer.parseInt(factionInformations[0]);
                        factionRankings.put(faction, points);
                    }
                } catch (Exception exception){
                    exception.printStackTrace();
                }
            }
        }

        factionRankings = sortFactionRankings(factionRankings);
        FactionEvent.getInstance().setFactionRankings(factionRankings);
        Bukkit.broadcastMessage("§8[§cClassement§8] §7Le classement des factions a été mis à jour.");
    }

    public static LinkedHashMap<Faction, Integer> sortFactionRankings(LinkedHashMap<Faction, Integer> unsortedMap){
        LinkedHashMap<Faction, Integer> sortedMap = new LinkedHashMap<>();
        ArrayList<Faction> factionsAlreadySet = new ArrayList<>();
        try {
            while(unsortedMap.size() != 0){
                Faction current_faction = null;
                int current_max = 0;
                for(Faction faction : unsortedMap.keySet()){
                    int points = unsortedMap.get(faction);
                    if((points > current_max || current_faction == null) && !factionsAlreadySet.contains(faction)){
                        current_faction = faction; current_max = points;
                    }
                }
                if(current_max == 0){
                    for(Faction faction : unsortedMap.keySet()){
                        sortedMap.put(faction, 0);
                        factionsAlreadySet.add(faction);
                    }
                } else { sortedMap.put(current_faction, current_max); factionsAlreadySet.add(current_faction); }
                for (Faction faction : factionsAlreadySet) { unsortedMap.remove(faction); }
            }
        } catch (Exception exception){ exception.printStackTrace(); }
        return sortedMap;
    }

    public static void runTaskUpdateRankings(){
        new BukkitRunnable() {
            @Override
            public void run() { updateRanking(); }
        }.runTaskTimerAsynchronously(FactionEvent.getInstance(), 20L, 3600 * 20L);
    }

}
