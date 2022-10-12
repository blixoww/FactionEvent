package fr.blixow.factionevent.utils.meteorite;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Meteorite {

    private String nom;
    private Location location;

    public Meteorite(String nom, Location location) {
        this.nom = nom;
        this.location = location;
    }

    public void save() {
        try {
            FileConfiguration fc = FileManager.getMeteoriteDataFC();
            File file = FileManager.getDataFile("meteorite.yml");
            List<String> meteoriteList = new ArrayList<>();
            if (fc.contains("meteoritelist")) {
                meteoriteList = fc.getStringList("meteoritelist");
            }
            if (!meteoriteList.contains(this.nom)) {
                meteoriteList.add(this.nom);
            }
            fc.set("meteoritelist", meteoriteList);
            fc.set(this.nom + ".worldname", location.getWorld().getName());
            fc.set(this.nom + ".position.x", location.getBlockX());
            fc.set(this.nom + ".position.y", location.getBlockY());
            fc.set(this.nom + ".position.z", location.getBlockZ());
            fc.save(file);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void start(Player... players) {
        FactionEvent.getInstance().getEventOn().start(this, players);
    }

    public void stop(Player... players) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String message = msg.getString("meteorite.prefix") == null ? "§8[§cMétéorite§8]§7 " : msg.getString("meteorite.prefix");
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        MeteoriteEvent meteoriteEvent = eventOn.getMeteoriteEvent();
        if (meteoriteEvent == null) {
            message += new StrManager(msg.getString("meteorite.not_started")).reMeteorite(this.nom).toString();
            FactionMessageTitle.sendPlayersMessage(message, players);
        } else {
            if (eventOn.canStartAnEvent() || eventOn.getMeteoriteEvent() == null || eventOn.getMeteoriteEvent().getMeteorite().equals(this)) {
                meteoriteEvent.stop();
                if (players.length > 0) {
                    message += new StrManager(msg.getString("meteorite.canceled")).reMeteorite(this.nom).toString();
                } else {
                    if (meteoriteEvent.getBlocksArrayList().isEmpty()) {
                        message += new StrManager(msg.getString("meteorite.ended_all")).reMeteorite(this.nom).toString();
                    } else {
                        message += new StrManager(msg.getString("meteorite.ended_time")).reMeteorite(this.nom).toString();
                    }
                }
                Bukkit.broadcastMessage(message);
                meteoriteEvent = null;
                FactionEvent.getInstance().getEventOn().setMeteoriteEvent(null);
            }


        }
    }

    public String getName() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "§8§m-----§r§8[§cMétéorite§8]§m-----\n\n§8» §cNom : §7" + nom + "\n§8» §cPosition :\n§8-> §7X = §f" + getLocation().getBlockX() + "\n§8-> §7Y = §f" + getLocation().getBlockY() + "\n§8-> §7Z = §f" + getLocation().getBlockZ();
    }
}
