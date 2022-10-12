package fr.blixow.factionevent.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class CastUtils {

    public static boolean isInteger(String str){
        try {
            int entier = Integer.parseInt(str);
            return true;
        } catch (Exception exception){
            return false;
        }
    }
    // Format x,y,z
    public static Location getLocationFromStringFormat(String worldName, String position){
        String[] split = position.split(",");
        if(split.length == 3){
            if(isInteger(split[0]) && isInteger(split[1]) && isInteger(split[2])){
                int x = Integer.parseInt(split[0]);
                int y = Integer.parseInt(split[1]);
                int z = Integer.parseInt(split[2]);
                return new Location(Bukkit.getWorld(worldName), x, y, z);
            }
            return null;
        }
        return null;
    }

    public static String getStringFormattedLocation(Location location){
        int x = (int) location.getX();
        int y = (int) location.getY();
        int z = (int) location.getZ();
        return x + "," + y + "," + z;
    }

}
