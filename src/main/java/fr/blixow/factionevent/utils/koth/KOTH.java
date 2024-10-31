package fr.blixow.factionevent.utils.koth;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.PlanningManager;
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

public class KOTH {
    private String nom;
    private Location pos1;
    private Location pos2;

    public KOTH(String nom, Location pos1, Location pos2) {
        this.nom = nom;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public String getName() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        int x = (int) Math.round(pos1.getX());
        int y = (int) Math.round(pos1.getY());
        int z = (int) Math.round(pos1.getZ());
        this.pos1 = new Location(Bukkit.getServer().getWorld(pos1.getWorld().getName()), x, y, z);
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        int x = (int) Math.round(pos2.getX());
        int y = (int) Math.round(pos2.getY());
        int z = (int) Math.round(pos2.getZ());
        this.pos2 = new Location(Bukkit.getServer().getWorld(pos2.getWorld().getName()), x, y, z);
    }
    public boolean saveKOTH() {
        try {
            File file = FileManager.getFile("data/koth.yml");
            FileConfiguration fc = FileManager.getKothDataFC();
            List<String> listKOTH = new ArrayList<>();
            if (fc.contains("kothlist")) {
                listKOTH = fc.getStringList("kothlist");
            }
            if (!listKOTH.contains(getName())) {
                listKOTH.add(getName());
            }
            String world_name = pos1.getWorld().getName();
            int pos1_x = (int) pos1.getX(), pos1_y = (int) pos1.getY(), pos1_z = (int) pos1.getZ();
            int pos2_x = (int) pos2.getX(), pos2_y = (int) pos2.getY(), pos2_z = (int) pos2.getZ();
            fc.set(getName() + "." + "worldname", world_name);
            fc.set(getName() + "." + "pos1.x", pos1_x);
            fc.set(getName() + "." + "pos1.y", pos1_y);
            fc.set(getName() + "." + "pos1.z", pos1_z);
            fc.set(getName() + "." + "pos2.x", pos2_x);
            fc.set(getName() + "." + "pos2.y", pos2_y);
            fc.set(getName() + "." + "pos2.z", pos2_z);
            fc.set("kothlist", listKOTH);
            fc.save(file);
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;

    }

    public void start(Player... players) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.contains("koth.prefix") ? msg.getString("koth.prefix") : "§8[§cKOTH§8]§7 ";
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        FactionMessageTitle.sendPlayersTitle(20,40, 20,"§aKOTH en cours", "préparez-vous au combat");
        eventOn.start(this, players);
    }

    public void stop(Player... players) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.contains("koth.prefix") ? msg.getString("koth.prefix") : "§8[§cKOTH§8]§7 ";
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        KOTHEvent kothEvent = eventOn.getKothEvent();
        if (kothEvent != null) {
            if (!kothEvent.isWon()) {
                Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("koth.canceled")).reKoth(this.nom).toString());
            }
            kothEvent.getScoreBoardAPI().getObjective().unregister();
            kothEvent = null;
            eventOn.setKothEvent(null);
            return;
        } else {
            FactionMessageTitle.sendPlayersMessage(prefix + new StrManager(msg.getString("koth.not_started")).reKoth(this.nom).toString(), players);
        }
        FactionMessageTitle.sendPlayersMessage(prefix + new StrManager(msg.getString("koth.not_started")).reKoth(this.nom).toString(), players);
    }

    @Override
    public String toString() {
        return "§8§m-----§r§8[§cKOTH§8]§m-----\n\n§8» §cNom : §7" + nom + "\n§8» §cPosition :\n§8-> §7X = §f" + pos1.getBlock().getLocation().getBlockX() + "\n§8-> §7Y = §f" + pos1.getBlock().getLocation().getBlockY() + "\n§8-> §7Z = §f" + pos1.getBlock().getLocation().getBlockZ();
    }
}
