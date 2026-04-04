package fr.blixow.factionevent.utils.domination;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DominationZone {
    private final String name;
    private Location pos1;
    private Location pos2;
    private boolean enabled;

    public DominationZone(String name, Location pos1, Location pos2, boolean enabled) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.enabled = enabled;
    }

    public String getName() { return name; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setPos1(Location loc) {
        int x = (int) Math.round(loc.getX());
        int y = (int) Math.round(loc.getY());
        int z = (int) Math.round(loc.getZ());
        this.pos1 = new Location(Bukkit.getWorld(loc.getWorld().getName()), x, y, z);
    }

    public void setPos2(Location loc) {
        int x = (int) Math.round(loc.getX());
        int y = (int) Math.round(loc.getY());
        int z = (int) Math.round(loc.getZ());
        this.pos2 = new Location(Bukkit.getWorld(loc.getWorld().getName()), x, y, z);
    }

    public boolean isPlayerInZone(Player player) {
        if (pos1 == null || pos2 == null) return false;
        double px = player.getLocation().getX();
        double py = player.getLocation().getY();
        double pz = player.getLocation().getZ();
        return isBetween(px, pos1.getX(), pos2.getX())
            && isBetween(py, pos1.getY(), pos2.getY())
            && isBetween(pz, pos1.getZ(), pos2.getZ());
    }

    private boolean isBetween(double pos, double v1, double v2) {
        double min = Math.min(v1, v2);
        double max = Math.max(v1, v2);
        return min <= pos && pos <= max;
    }

    @Override
    public String toString() {
        String enabledStr = enabled ? "§aActivée" : "§cDésactivée";
        String pos1Str = pos1 != null ? pos1.getBlockX() + "/" + pos1.getBlockY() + "/" + pos1.getBlockZ() : "§cnon définie";
        String pos2Str = pos2 != null ? pos2.getBlockX() + "/" + pos2.getBlockY() + "/" + pos2.getBlockZ() : "§cnon définie";
        return "§7Zone §c" + name + " §8[" + enabledStr + "§8]"
            + "\n  §8» §7Pos1: §f" + pos1Str
            + "\n  §8» §7Pos2: §f" + pos2Str;
    }
}
