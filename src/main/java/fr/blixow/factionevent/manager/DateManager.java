package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.utils.CastUtils;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;

public class DateManager {

    public static ArrayList<Integer> getIntegerList(String[] tab){
        ArrayList<Integer> tabEntier = new ArrayList<>();
        for(String str : tab){
            if(!CastUtils.isInteger(str)){ return new ArrayList<>(); }
            tabEntier.add(Integer.parseInt(str));
        }
        return tabEntier;
    }

    public static int getWeekOfYear(LocalDateTime localDateTime){ return localDateTime.get(WeekFields.of(Locale.FRANCE).weekOfYear()); }

    public static String getFormattedTime(int totalSeconds) {
        int seconds = totalSeconds % 60;
        int minutes = totalSeconds / 60;
        String str = "";
        if (minutes == 0 && seconds == 0) {
            str = "0s";
        } else if (minutes == 0) {
            str = seconds + "s";
        } else if (seconds == 0) {
            str = minutes + "m";
        } else {
            str = minutes + "m" + seconds + "s";
        }
        return str;
    }

    public static String formatTime(String time) {
        String[] timeParts = time.split("h");
        String hours = timeParts[0];
        if (hours.length() == 1) {
            hours = "0" + hours;
        }
        String minutes = timeParts[1];
        if (minutes.length() == 1) {
            minutes = "0" + minutes;
        }
        return hours + "-" + minutes;
    }

}
