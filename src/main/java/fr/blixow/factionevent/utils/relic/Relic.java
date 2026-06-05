package fr.blixow.factionevent.utils.relic;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Conteneur prêt-à-lancer pour la Course à la Relique.
 * Contient le point de spawn (tiré au hasard) au moment du démarrage.
 */
public class Relic {
    private final Location spawnLocation;

    public Relic(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void start(Player... players) {
        FactionEvent.getInstance().getEventOn().start(this, players);
    }

    public void stop(Player... players) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("relic.prefix", "§8[§dRELIQUE§8]§7 ");
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        RelicEvent event = eventOn.getRelicEvent();
        if (event != null) {
            if (!event.isEnded()) {
                event.cleanup();
                Bukkit.broadcastMessage(prefix + msg.getString("relic.canceled",
                    "§7La Course à la Relique a été annulée."));
            }
            eventOn.setRelicEvent(null);
            return;
        }
        if (players.length > 0) {
            FactionMessageTitle.sendPlayersMessage(prefix + msg.getString("relic.not_started",
                "§cAucune Course à la Relique en cours."), players);
        }
    }
}
