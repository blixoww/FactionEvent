package fr.blixow.factionevent.utils.domination;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Représente une instance de Domination prête à être lancée,
 * avec la liste des zones actives (enabled) au moment du démarrage.
 */
public class Domination {
    private final List<DominationZone> activeZones;

    public Domination(List<DominationZone> activeZones) {
        this.activeZones = activeZones;
    }

    public List<DominationZone> getActiveZones() {
        return activeZones;
    }

    public void start(Player... players) {
        FactionEvent.getInstance().getEventOn().start(this, players);
    }

    public void stop(Player... players) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("domination.prefix", "§8[§cDOMINATION§8]§7 ");
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        DominationEvent event = eventOn.getDominationEvent();
        if (event != null) {
            if (!event.isEnded()) {
                Bukkit.broadcastMessage(prefix + msg.getString("domination.canceled",
                    "§7La Domination a été annulée."));
            }
            eventOn.setDominationEvent(null);
            return;
        }
        if (players.length > 0) {
            FactionMessageTitle.sendPlayersMessage(prefix + msg.getString("domination.not_started",
                "§cAucune Domination en cours."), players);
        }
    }
}
