package fr.blixow.factionevent.utils;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.totem.Totem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class PlanningScheduler extends BukkitRunnable {

    @Override
    public void run() {
        try {
            LocalDateTime now = LocalDateTime.now();
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            List<ScheduledEvent> planning = FactionEvent.getInstance().getPlanning();

            boolean allDone = true;
            for (ScheduledEvent event : planning) {
                if (!event.isStarted()) {
                    allDone = false;
                    break;
                }
            }
            // Si tous les events sont démarrés, recharger le planning (nouveaux events de la semaine)
            if (allDone && !planning.isEmpty()) {
                FactionEvent.getInstance().reloadPlanning();
                return;
            }

            for (ScheduledEvent event : planning) {
                if (event.isStarted()) continue;

                Duration duration = Duration.between(now, event.getTime());
                long secondsUntil = duration.getSeconds();

                // Déclenchement : dans les 30 secondes après l'heure prévue
                if (secondsUntil <= 0 && secondsUntil > -30) {
                    event.setStarted(true);
                    startEvent(event);
                    continue;
                }

                // Avertissement 5 minutes (entre 300s et 90s pour éviter le chevauchement)
                if (secondsUntil <= 300 && secondsUntil > 90 && !event.isWarned5min()) {
                    sendWarning(event, msg, 5);
                    event.setWarned5min(true);
                }
                // Avertissement 1 minute (entre 90s et 30s)
                // On n'envoie pas ce message si l'event démarre dans moins de 90s au total
                // (pour éviter d'avoir "commence dans 1 min" et "a commencé" en même temps)
                else if (secondsUntil <= 90 && secondsUntil > 30 && !event.isWarned1min()) {
                    sendWarning(event, msg, 1);
                    event.setWarned1min(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendWarning(ScheduledEvent event, FileConfiguration msg, int minutes) {
        String type = event.getType().toLowerCase();
        String name = event.getName();
        String key = type + ".starting_in_" + minutes + "mins";

        String format = msg.getString(key);
        if (format == null) return;

        StrManager strManager = new StrManager(format);
        switch (type) {
            case "koth":   strManager.reKoth(name);   break;
            case "totem":  strManager.reTotem(name);  break;
            case "dtc":    strManager.reDTC(name);    break;
            case "lms":    strManager.reLMS(name);    break;
        }

        Bukkit.broadcastMessage(strManager.toString());
    }

    private void startEvent(ScheduledEvent event) {
        String type = event.getType().toLowerCase();
        String name = event.getName();

        switch (type) {
            case "koth":
                for (KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                    if (koth.getName().equalsIgnoreCase(name)) {
                        koth.start();
                        return;
                    }
                }
                Bukkit.getConsoleSender().sendMessage("[FactionEvent] KOTH introuvable pour le planning : " + name);
                break;
            case "totem":
                for (Totem totem : FactionEvent.getInstance().getListTotem()) {
                    if (totem.getName().equalsIgnoreCase(name)) {
                        totem.start();
                        return;
                    }
                }
                Bukkit.getConsoleSender().sendMessage("[FactionEvent] Totem introuvable pour le planning : " + name);
                break;
            case "dtc":
                for (DTC dtc : FactionEvent.getInstance().getListDTC()) {
                    if (dtc.getName().equalsIgnoreCase(name)) {
                        dtc.start();
                        return;
                    }
                }
                Bukkit.getConsoleSender().sendMessage("[FactionEvent] DTC introuvable pour le planning : " + name);
                break;
            case "lms":
                for (LMS lms : FactionEvent.getInstance().getListLMS()) {
                    if (lms.getName().equalsIgnoreCase(name)) {
                        lms.startRegistration();
                        return;
                    }
                }
                Bukkit.getConsoleSender().sendMessage("[FactionEvent] LMS introuvable pour le planning : " + name);
                break;
        }
    }
}


