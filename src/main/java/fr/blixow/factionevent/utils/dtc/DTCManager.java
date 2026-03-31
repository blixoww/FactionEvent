package fr.blixow.factionevent.utils.dtc;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import java.util.ArrayList;
import java.util.List;

public class DTCManager {

    public static DTC getDTCbyName(String name){
        for(DTC dtc : FactionEvent.getInstance().getListDTC()){
            if(dtc.getName().toLowerCase().equalsIgnoreCase(name)){ return dtc; }
        }
        return null;
    }

    public static boolean isDTCStarted(){
        return FactionEvent.getInstance().getEventOn().getDtcEvent() != null;
    }

    public static boolean isDTCStarted(DTC dtc){
        if(isDTCStarted()){
            return FactionEvent.getInstance().getEventOn().getDtcEvent().getDtc().equals(dtc);
        }
        return false;
    }

    public static DTCEvent getDTCEvent(DTC dtc){
        if(isDTCStarted(dtc)){
            return FactionEvent.getInstance().getEventOn().getDtcEvent();
        }
        return null;
    }

    public static DTCEvent getDTCEventByEntity(Entity entity){
        if(entity == null) return null;
        if(isDTCStarted()){
            try {
                DTCEvent dtcEvent = FactionEvent.getInstance().getEventOn().getDtcEvent();
                if (dtcEvent == null) return null;
                Entity stored = dtcEvent.getEntity();
                if (stored != null) {
                    // compare UUIDs pour plus de robustesse
                    try {
                        if (stored.getUniqueId().equals(entity.getUniqueId())) {
                            return dtcEvent;
                        }
                    } catch (Exception ignored) {}
                    // fallback : comparaison par proximité (si l'entité a été re-créée)
                    try {
                        Location l1 = stored.getLocation();
                        Location l2 = entity.getLocation();
                        if (l1 != null && l2 != null && l1.getWorld() != null && l1.getWorld().equals(l2.getWorld())) {
                            double dist = l1.distance(l2);
                            if (dist <= 2.0D) {
                                return dtcEvent;
                            }
                        }
                    } catch (Exception ignored) {}
                } else {
                    // stored null : fallback en comparant la position connue du DTC
                    try {
                        Location dtcLoc = dtcEvent.getDtc().getLocation();
                        Location l2 = entity.getLocation();
                        if (dtcLoc != null && l2 != null && dtcLoc.getWorld() != null && dtcLoc.getWorld().equals(l2.getWorld())) {
                            double dist = dtcLoc.distance(l2);
                            if (dist <= 2.0D) return dtcEvent;
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static void loadDTCfromFile(){
        ArrayList<DTC> listDTC = new ArrayList<>();
        FileConfiguration fc = FileManager.getDtcDataFC();
        if(fc.contains("dtclist")){
            List<String> stringList = fc.getStringList("dtclist");
            for(String dtcName : stringList){
                try {
                    String worldName = fc.getString(dtcName + ".worldname");
                    double x = fc.getDouble(dtcName + ".position.x"), y = fc.getDouble(dtcName + ".position.y"), z = fc.getDouble(dtcName + ".position.z");
                    Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                    listDTC.add(new DTC(dtcName, location));
                } catch (Exception exception){
                    exception.printStackTrace();
                }
            }
        }
        FactionEvent.getInstance().setListDTC(listDTC);
    }

    public static ArrayList<String> getDTCNames(){
        ArrayList<String> arrayList = new ArrayList<>();
        for(DTC dtc : FactionEvent.getInstance().getListDTC()){ arrayList.add(dtc.getName()); }
        return arrayList;
    }


}
