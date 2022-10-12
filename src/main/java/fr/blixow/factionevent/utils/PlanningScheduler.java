package fr.blixow.factionevent.utils;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.enumeration.DayEnum;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.meteorite.MeteoriteManager;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.List;

public class PlanningScheduler extends BukkitRunnable {


    @Override
    public void run() {
        try {
            LocalDateTime localDateTime = LocalDateTime.now();

            String day = String.valueOf(DayEnum.valueOf(localDateTime.getDayOfWeek().toString()).getValeur());
            FileConfiguration planning = FileManager.getPlanningDataFC();

            for(KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                String nom = koth.getName();
                String path_koth = day + ".koth." + nom;
                List<String> stringList = planning.getStringList(path_koth);
                for(String str : stringList){
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];

                    String time = h + "h" + m;
                    if(time.equals(localDateTime.getHour() + "h" + localDateTime.getMinute())){
                        KOTH kothToLaunch = KOTH.getKOTH(nom);
                        assert kothToLaunch != null;
                        kothToLaunch.start();
                    }
                }
            }

            for(DTC dtc : FactionEvent.getInstance().getListDTC()) {
                String nom = dtc.getName();
                String path_dtc = day + ".dtc." + nom;
                List<String> stringList = planning.getStringList(path_dtc);
                for(String str : stringList){
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];

                    String time = h + "h" + m;
                    if(time.equals(localDateTime.getHour() + "h" + localDateTime.getMinute())){
                        DTC dtcToLaunch = DTCManager.getDTCbyName(nom);
                        assert dtcToLaunch != null;
                        dtcToLaunch.start();
                    }
                }
            }

            for(Totem totem : FactionEvent.getInstance().getListTotem()) {
                String nom = totem.getName();
                String path_totem = day + ".totem." + nom;
                List<String> stringList = planning.getStringList(path_totem);
                for(String str : stringList){
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];

                    String time = h + "h" + m;
                    if(time.equals(localDateTime.getHour() + "h" + localDateTime.getMinute())){
                        Totem totemToLaunch = TotemManager.getTotem(nom);
                        assert totemToLaunch != null;
                        totemToLaunch.start();
                    }
                }
            }

            for(Meteorite totem : FactionEvent.getInstance().getListMeteorite()) {
                String nom = totem.getName();
                String path_meteorite = day + ".meteorite." + nom;
                List<String> stringList = planning.getStringList(path_meteorite);
                for(String str : stringList){
                    String m = "", h = "";
                    h = str.split("h")[0].length() == 1 ? "0" + str.split("h")[0] : str.split("h")[0];
                    m = str.split("h")[1].length() == 1 ? "0" + str.split("h")[1] : str.split("h")[1];

                    String time = h + "h" + m;
                    if(time.equals(localDateTime.getHour() + "h" + localDateTime.getMinute())){
                        Meteorite meteoriteToLaunch = MeteoriteManager.getMeteoriteByName(nom);
                        assert meteoriteToLaunch != null;
                        meteoriteToLaunch.start();
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
