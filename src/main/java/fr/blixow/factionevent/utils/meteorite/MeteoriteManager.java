package fr.blixow.factionevent.utils.meteorite;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MeteoriteManager {

    public static void loadMeteoritesFromFile(){
        FileConfiguration fc = FileManager.getMeteoriteDataFC();
        ArrayList<Meteorite> meteoriteArrayList = new ArrayList<>();
        try {
            if(fc.contains("meteoritelist")){
                List<String> stringList = fc.getStringList("meteoritelist");
                for(String str : stringList){
                    String worldname = fc.getString(str + ".worldname");
                    int x = fc.getInt(str + ".position.x");
                    int y = fc.getInt(str + ".position.y");
                    int z = fc.getInt(str + ".position.z");
                    Location location = new Location(Bukkit.getWorld(worldname), x, y, z);
                    meteoriteArrayList.add(new Meteorite(str, location));
                    FactionEvent.getInstance().getLogger().info("§8[§cMétéorite§8] §7Chargement de la météorite : " + str);
                }
            }
        } catch (Exception exception){ exception.printStackTrace(); }

        if(meteoriteArrayList.isEmpty()){ FactionEvent.getInstance().getLogger().warning("Aucune météorite n'a été chargé depuis le fichier data/meteorite.yml"); }
        FactionEvent.getInstance().setListMeteorite(meteoriteArrayList);
    }

    public static MeteoriteEvent getMeteoriteEventByMeteorite(Meteorite meteorite){
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        if(eventOn.getMeteoriteEvent() != null){
            if(eventOn.getMeteoriteEvent().getMeteorite().equals(meteorite)){
                return eventOn.getMeteoriteEvent();
            }
        }
        return null;
    }

    public static Meteorite getMeteoriteByName(String name){
        for(Meteorite meteorite : FactionEvent.getInstance().getListMeteorite()){
            if(meteorite.getName().equalsIgnoreCase(name)){ return meteorite; } }
        return null;
    }

    public static void registerNewEvent(MeteoriteEvent meteoriteEvent){
        try { FactionEvent.getInstance().getEventOn().setMeteoriteEvent(meteoriteEvent); } catch (Exception exception){ exception.printStackTrace(); }
    }

    public static void registerNewMeteorite(Meteorite meteorite){
        try { FactionEvent.getInstance().getListMeteorite().add(meteorite); } catch (Exception exception){ exception.printStackTrace(); }
    }

    public static ArrayList<String> getMeteoriteListNames(){
        ArrayList<String> arrayList = new ArrayList<>();
        for(Meteorite meteorite : FactionEvent.getInstance().getListMeteorite()){ arrayList.add(meteorite.getName()); }
        return arrayList;
    }

    public static boolean isMeteoriteStarted(Meteorite meteorite){
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        if(eventOn.getMeteoriteEvent() != null){ return eventOn.getMeteoriteEvent().getMeteorite().equals(meteorite); }
        return false;
    }

    public static MeteoriteEvent getMeteoriteEvent(Meteorite meteorite){
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        if(isMeteoriteStarted(meteorite)){ return eventOn.getMeteoriteEvent(); }
        return null;
    }

    public static MeteoriteEvent getEventFromBlock(Block block){
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        if(eventOn.getMeteoriteEvent() != null){
            if(eventOn.getMeteoriteEvent().getBlocksArrayList().contains(block)){ return eventOn.getMeteoriteEvent(); }
        }
        return null;
    }

    public static boolean playerInChunk(Location location){
        for(Entity entity : location.getChunk().getEntities()){
            if(entity instanceof Player){ return true; }
        }
        return false;
    }

    public static double lowestPlayerDistance(Location base){
        double distance = 10000;
        for(Player player : Bukkit.getOnlinePlayers()){
            double player_distance = player.getLocation().distance(base) + 1;
            if(player_distance < distance){ distance = player_distance; }
        }
        return distance;
    }

}
