package fr.blixow.factionevent.utils.purge;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Conteneur représentant une instance de Purge prête à être lancée.
 * La Purge est un event global (pas de zones) : pendant sa durée, les portes
 * dans les claims deviennent ouvrables par tous, et chaque kill rapporte
 * argent + items configurés.
 */
public class Purge {

    public Purge() {}

    public void start(Player... players) {
        FactionEvent.getInstance().getEventOn().start(this, players);
    }

    public void stop(Player... players) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("purge.prefix", "§8[§cPURGE§8]§7 ");
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        PurgeEvent event = eventOn.getPurgeEvent();
        if (event != null) {
            if (!event.isEnded()) {
                Bukkit.broadcastMessage(prefix + msg.getString("purge.canceled",
                    "§7La Purge a été annulée."));
                event.cancel();
            }
            eventOn.setPurgeEvent(null);
            return;
        }
        if (players.length > 0) {
            FactionMessageTitle.sendPlayersMessage(prefix + msg.getString("purge.not_started",
                "§cAucune Purge en cours."), players);
        }
    }
}
