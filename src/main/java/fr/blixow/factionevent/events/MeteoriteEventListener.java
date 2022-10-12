package fr.blixow.factionevent.events;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.meteorite.MeteoriteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class MeteoriteEventListener implements Listener {

    @EventHandler
    public void onBreak(BlockBreakEvent event){
        if(FactionEvent.getInstance().getEventOn().getMeteoriteEvent() == null){ return; }
        MeteoriteEvent meteoriteEvent = FactionEvent.getInstance().getEventOn().getMeteoriteEvent();
        meteoriteEvent.blockBreak(event.getBlock());
    }

}
