package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.event.EventOn;

public class EventOnManager {

    public static EventOn getEventOn(){
        return FactionEvent.getInstance().getEventOn();
    }

}
