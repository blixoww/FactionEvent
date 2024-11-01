package fr.blixow.factionevent.utils;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.enumeration.DayEnum;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.totem.Totem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.util.List;

public class PlanningScheduler extends BukkitRunnable {

    private int messageCounter = 0;

    @Override
    public void run() {
        try {
            LocalDateTime localDateTime = LocalDateTime.now();
            String day = String.valueOf(DayEnum.valueOf(localDateTime.getDayOfWeek().toString()).getValeur());
            FileConfiguration planning = FileManager.getPlanningDataFC();
            FileConfiguration messageConfiguration = FileManager.getMessageFileConfiguration();

            for (KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                String nom = koth.getName();
                String path_koth = day + ".koth." + nom;
                List<String> stringList = planning.getStringList(path_koth);
                for (String str : stringList) {
                    int hour = Integer.parseInt(str.split("h")[0]);
                    int minute = Integer.parseInt(str.split("h")[1]);
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime eventTime = LocalDateTime.of(localDateTime.getYear(), localDateTime.getMonth(), localDateTime.getDayOfMonth(), hour, minute);
                    if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(5)) && messageCounter < 1) {
                        String message = messageConfiguration.getString("koth.prefix") + new StrManager(messageConfiguration.getString("koth.starting_in_5mins")).reKoth(koth.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    } else if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(1)) && messageCounter < 2) {
                        String message = messageConfiguration.getString("koth.prefix") + new StrManager(messageConfiguration.getString("koth.starting_in_1mins")).reKoth(koth.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    }
                    else if (hour == now.getHour() && minute == now.getMinute()) {
                        koth.start();
                        this.messageCounter = 0;
                    }
                }
            }

            for (DTC dtc : FactionEvent.getInstance().getListDTC()) {
                String nom = dtc.getName();
                String path_dtc = day + ".dtc." + nom;
                List<String> stringList = planning.getStringList(path_dtc);
                for (String str : stringList) {
                    int hour = Integer.parseInt(str.split("h")[0]);
                    int minute = Integer.parseInt(str.split("h")[1]);
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime eventTime = LocalDateTime.of(localDateTime.getYear(), localDateTime.getMonth(), localDateTime.getDayOfMonth(), hour, minute);
                    if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(5)) && messageCounter < 1) {
                        String message = messageConfiguration.getString("dtc.prefix") + new StrManager(messageConfiguration.getString("dtc.starting_in_5mins")).reDTC(dtc.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    } else if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(1)) && messageCounter < 2) {
                        String message = messageConfiguration.getString("dtc.prefix") + new StrManager(messageConfiguration.getString("dtc.starting_in_1mins")).reDTC(dtc.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    }
                    else if (hour == now.getHour() && minute == now.getMinute()) {
                        dtc.start();
                        this.messageCounter = 0;
                    }
                }
            }

            for (Totem totem : FactionEvent.getInstance().getListTotem()) {
                String nom = totem.getName();
                String path_totem = day + ".totem." + nom;
                List<String> stringList = planning.getStringList(path_totem);
                for (String str : stringList) {
                    int hour = Integer.parseInt(str.split("h")[0]);
                    int minute = Integer.parseInt(str.split("h")[1]);
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime eventTime = LocalDateTime.of(localDateTime.getYear(), localDateTime.getMonth(), localDateTime.getDayOfMonth(), hour, minute);
                    if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(5)) && messageCounter < 1) {
                        String message = messageConfiguration.getString("totem.prefix") + new StrManager(messageConfiguration.getString("totem.starting_in_5mins")).reTotem(totem.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    } else if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(1)) && messageCounter < 2) {
                        String message = messageConfiguration.getString("totem.prefix") + new StrManager(messageConfiguration.getString("totem.starting_in_1mins")).reTotem(totem.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    }
                    else if (hour == now.getHour() && minute == now.getMinute()) {
                        totem.start();
                        this.messageCounter = 0;
                    }
                }
            }

            for (LMS lms : FactionEvent.getInstance().getListLMS()) {
                String nom = lms.getName();
                String path_lms = day + ".lms." + nom;
                List<String> stringList = planning.getStringList(path_lms);
                for (String str : stringList) {
                    int hour = Integer.parseInt(str.split("h")[0]);
                    int minute = Integer.parseInt(str.split("h")[1]);
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime eventTime = LocalDateTime.of(localDateTime.getYear(), localDateTime.getMonth(), localDateTime.getDayOfMonth(), hour, minute);
                    if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(5)) && messageCounter < 1) {
                        String message = new StrManager(messageConfiguration.getString("lms.starting_in_5mins")).reLMS(lms.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    } else if (eventTime.isAfter(localDateTime) && eventTime.isBefore(localDateTime.plusMinutes(1)) && messageCounter < 2) {
                        String message = new StrManager(messageConfiguration.getString("lms.starting_in_1mins")).reLMS(lms.getName()).toString();
                        Bukkit.broadcastMessage(message);
                        this.messageCounter++;
                    }
                    else if (hour == now.getHour() && minute == now.getMinute()) {
                        lms.startRegistration();
                        this.messageCounter = 0;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
