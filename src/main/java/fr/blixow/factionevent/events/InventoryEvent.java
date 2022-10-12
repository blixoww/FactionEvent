package fr.blixow.factionevent.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryEvent implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event){
        if(event.getInventory().getName().equals("§cPlanning des event faction")){
            event.setCancelled(true);
        }
    }
}

