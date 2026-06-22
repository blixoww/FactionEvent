package fr.blixow.factionevent.events;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.FactionUtil;
import fr.redfaction.entity.Faction;
import fr.redfaction.api.events.FactionDisbandEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;

public class DisbandFactionEvent implements Listener {

    @EventHandler
    public void onDisband(FactionDisbandEvent event){
        try {
            Faction faction = event.getFaction();
            String id = FactionUtil.id(faction);
            FileConfiguration fc = FileManager.getClassementFC();
            File file = FileManager.getDataFile("classement.yml");
            if(fc.contains(id)){
                fc.set(id, null);
            }
            fc.save(file);
        } catch (Exception exception){
            exception.printStackTrace();
        }

    }
}
