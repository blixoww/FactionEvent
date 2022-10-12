package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.enumeration.DayEnum;
import fr.blixow.factionevent.utils.CastUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

import static java.time.temporal.TemporalAdjusters.firstDayOfYear;

public class DateManager {

    public static List<String> listJour = Arrays.asList("Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche");

    public static String getFormattedDate(LocalDateTime localDateTime){
        int year = localDateTime.getYear();
        int month = localDateTime.getMonthValue();
        int weekOfYear = localDateTime.get(WeekFields.of(Locale.FRANCE).weekOfYear());
        int day = localDateTime.getDayOfMonth();
        int hours = localDateTime.getHour();
        int minutes = localDateTime.getMinute();
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-ww-dd-HH-mm"));
    }

    public static String getFormattedDateForPlanningCheck(int year, int month, int day, int hours, int min){
        String annee = String.valueOf(year);
        String mois = (String.valueOf(month).length() == 1) ? "0" + month : String.valueOf(month);
        String jour = (String.valueOf(day).length() == 1) ? "0" + day : String.valueOf(day);
        String heures = (String.valueOf(hours).length() == 1) ? "0" + hours : String.valueOf(hours);
        String minutes = (String.valueOf(min).length() == 1) ? "0" + min : String.valueOf(min);
        String str = jour + "/" + mois + "/" + annee + "-" + heures + "-" + minutes;
        return str;
    }


    public static HashMap<String, String> getWeeklyDateFromWY(int week, int year){
        HashMap<String, String> map = new HashMap<>();
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime first = localDateTime.with(firstDayOfYear());
        int i = 1;
        boolean bool = true;
        while(i < 365 && bool){
            if(getWeekOfYear(first) == week){
                LocalDateTime localDateTime1 = first.with(WeekFields.of(Locale.FRANCE).getFirstDayOfWeek());
                for(int j = 0; j < 7; j++){
                    String day_name = DayEnum.valueOf(localDateTime1.getDayOfWeek().toString()).getValeur();
                    String day = String.valueOf(localDateTime1.getDayOfMonth());
                    String months = String.valueOf(localDateTime1.getMonthValue());
                    if(day.length() == 1){ day = "0" + day; }
                    if(months.length() == 1){ months = "0" + months; }
                    String concat = day + "/" + months + "/" + localDateTime1.getYear();
                    map.put(day_name, concat);
                    localDateTime1 = localDateTime1.plusDays(1);
                }
                bool = false;
            } else {
                i += 7;
                first = first.plusDays(7);
            }
        }
        return map;
    }

    public static ArrayList<Integer> getTabDateEntier(String[] tab){
        ArrayList<Integer> tabEntier = new ArrayList<>();
        for(String str : tab){
            if(!CastUtils.isInteger(str)){ return new ArrayList<>(); }
            tabEntier.add(Integer.parseInt(str));
        }
        return tabEntier;
    }
    
    public static int getCurrentYear(){ return LocalDateTime.now().getYear(); }
    public static int getWeekOfYear(LocalDateTime localDateTime){ return localDateTime.get(WeekFields.of(Locale.FRANCE).weekOfYear()); }

    public static String[] splitDateFormat(String date){
        return date.split("-");
    }

    public static String getFormattedTime(int total_seconds){
        int seconds = total_seconds % 60;
        int minutes = (int) (total_seconds / 60);
        String str = "";
        if(minutes == 0 && seconds == 0){
            str = "0s";
        } else if(minutes == 0){
            str = seconds + "s";
        } else if(seconds == 0){
            str = minutes + "m";
        } else {
            str = minutes + "m" + seconds + "s";
        }
        return str;
    }

}
