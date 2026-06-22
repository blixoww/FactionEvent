package fr.blixow.factionevent.events;

import fr.redfaction.api.events.FactionCreateEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.FactionUtil;
import fr.redfaction.entity.Faction;
import fr.redfaction.api.events.FactionDisbandEvent;
import fr.blixow.factionevent.manager.RankingManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

public class ManagerFactionEvent implements Listener {

    @EventHandler
    public void onCreation(FactionCreateEvent event) {
        try {
            Faction faction = event.getFaction();
            String id = FactionUtil.id(faction);
            FileConfiguration fc = FileManager.getClassementFC();
            File file = FileManager.getDataFile("classement.yml");
            if (!fc.contains(id)) {
                fc.set(id, "0-0-0-0-0");
            }
            fc.save(file);
            try {
                RankingManager.update();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @EventHandler
    public void onDisband(FactionDisbandEvent event) {
        try {
            Faction faction = event.getFaction();
            String id = FactionUtil.id(faction);
            FileConfiguration fc = FileManager.getClassementFC();
            File file = FileManager.getDataFile("classement.yml");
            if (fc.contains(id)) {
                fc.set(id, null);
            }
            fc.save(file);
            RankingManager.update();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }
}