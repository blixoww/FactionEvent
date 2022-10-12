package fr.blixow.factionevent.utils.totem;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.CastUtils;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Totem {

    private String nom;
    private Location location;
    private HashMap<Location, Material> blocks;

    public Totem(String nom, Location location, HashMap<Location, Material> blocks) {
        this.nom = nom;
        this.location = location;
        this.blocks = blocks;
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

    public HashMap<Location, Material> getBlocks() {
        return blocks;
    }

    public void setBlocks(HashMap<Location, Material> blocks) {
        this.blocks = blocks;
    }

    public boolean saveTotem() {
        try {
            // todo sauvegerde du totem dans fichier totem.yml
            File file = FileManager.getDataFile("totem.yml");
            FileConfiguration fc = FileManager.getTotemDataFC();
            List<String> totemList = new ArrayList<>();
            if (fc.contains("totemlist")) {
                totemList = fc.getStringList("totemlist");
            }
            if (!totemList.contains(this.nom)) {
                totemList.add(this.nom);
                fc.set("totemlist", totemList);
            }
            fc.set(this.nom + ".worldname", location.getWorld().getName());
            fc.set(this.nom + ".position.x", location.getX());
            fc.set(this.nom + ".position.y", location.getY());
            fc.set(this.nom + ".position.z", location.getZ());
            List<String> blocksListString = new ArrayList<>();
            blocks.forEach((k, v) -> {
                String formatedLoc = CastUtils.getStringFormattedLocation(k);
                String mats = v.name();
                blocksListString.add(formatedLoc);
                fc.set(this.nom + ".blocks." + formatedLoc + ".type", mats);
            });
            fc.set(this.nom + ".blockslist", blocksListString);
            fc.save(file);
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public void start(Player... players) {
        try {
            FactionEvent.getInstance().getEventOn().start(this, players);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    public boolean stop() {
        try {
            EventOn eventOn = FactionEvent.getInstance().getEventOn();
            if (eventOn.canStartAnEvent() || eventOn.getTotemEvent() == null || !eventOn.getTotemEvent().getTotem().equals(this)) {
                return false;
            }
            TotemEvent totemEvent = eventOn.getTotemEvent();
            totemEvent = null;
            eventOn.setTotemEvent(null);
            clearBlocks();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    public void clearBlocks() {
        blocks.forEach((k, v) -> {
            k.getBlock().setType(Material.AIR);
        });
    }

    @Override
    public String toString() {
        return "§8§m-----§r§8[§cTotem§8]§m-----\n\n§8» §cNom : §7" + nom + "\n§8» §cPosition :\n§8-> §7X = §f" + getLocation().getBlockX() + "\n§8-> §7Y = §f" + getLocation().getBlockY() + "\n§8-> §7Z = §f" + getLocation().getBlockZ();
    }

}
