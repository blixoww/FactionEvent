package fr.blixow.factionevent.utils.dtc;

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

public class DTC {

    private final String name;
    private Location location;
    public DTC(String name, Location location) {
        this.name = name;
        this.location = location;
    }
    public void start(Player... players) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String dtc_prefix = msg.getString("dtc.prefix");
        String message = "";
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        if (eventOn.canStartAnEvent()) {
            message = dtc_prefix + new StrManager(msg.getString("dtc.already_started")).reDTC(this.name).toString();
            FactionMessageTitle.sendPlayersMessage(message, players);
            FactionMessageTitle.sendPlayersTitle(20,40, 20,"§aNexus en cours", "préparez-vous au combat");
            eventOn.start(this);
            return;
        }
        message = dtc_prefix + new StrManager(msg.getString("dtc.started")).reDTC(this.getName()).toString();
        Bukkit.broadcastMessage(message);
        eventOn.start(this, players);
    }

    public void stop(Player... players) {
        DTCEvent dtcEvent = DTCManager.getDTCEvent(this);
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String dtc_prefix = msg.getString("dtc.prefix");
        String message = "";
        if (dtcEvent != null) {
            dtcEvent.getEntity().remove();
            if (dtcEvent.getScoreBoardAPI() != null) {
                dtcEvent.getScoreBoardAPI().getObjective().unregister();
            }
            if (!dtcEvent.isDead()) {
                if (players.length > 0) {
                    message = dtc_prefix + new StrManager(msg.getString("dtc.canceled")).reDTC(this.name).toString();
                } else {
                    message = dtc_prefix + new StrManager(msg.getString("dtc.ended")).reDTC(this.name).toString();
                }
                Bukkit.broadcastMessage(message);

            }
            dtcEvent = null;
            FactionEvent.getInstance().getEventOn().setDtcEvent(null);
        } else {
            message = dtc_prefix + new StrManager(msg.getString("dtc.not_started")).reDTC(this.name).toString();
            for (Player p : players) {
                p.sendMessage(message);
            }
        }
    }

    public void saveDTC() {
        try {
            FileConfiguration fc = FileManager.getDtcDataFC();
            File file = FileManager.getDataFile("dtc.yml");
            String worldName = location.getWorld().getName();
            double x = location.getX(), y = location.getY(), z = location.getZ();
            List<String> stringList = new ArrayList<>();
            if (fc.contains("dtclist")) {
                stringList = fc.getStringList("dtclist");
            }
            if (!stringList.contains(this.name)) {
                stringList.add(this.name);
            }
            fc.set("dtclist", stringList);
            fc.set(this.name + ".worldname", worldName);
            fc.set(this.name + ".position.x", x);
            fc.set(this.name + ".position.y", y);
            fc.set(this.name + ".position.z", z);
            fc.save(file);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "§8§m-----§r§8[§cDTC§8]§m-----\n\n§8» §cNom : §7" + name + "\n§8» §cPosition :\n§8-> §7X = §f" + getLocation().getBlockX() + "\n§8-> §7Y = §f" + getLocation().getBlockY() + "\n§8-> §7Z = §f" + getLocation().getBlockZ();
    }
}