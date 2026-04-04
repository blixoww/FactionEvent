package fr.blixow.factionevent.utils.domination;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DominationZone {
    private final String name;
    private Location pos1;
    private Location pos2;
    private boolean enabled;
    private Location chestLocation; // position du coffre, optionnelle
    // Y de base (min Y entre pos1/pos2 au moment du setpos1+setpos2), indépendant du /expand
    private int baseY = Integer.MIN_VALUE;

    public DominationZone(String name, Location pos1, Location pos2, boolean enabled) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.enabled = enabled;
        this.chestLocation = null;
    }

    public String getName() { return name; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Location getChestLocation() { return chestLocation; }
    public void setChestLocation(Location loc) {
        if (loc == null) { this.chestLocation = null; return; }
        int x = (int) Math.round(loc.getX());
        int y = (int) Math.round(loc.getY());
        int z = (int) Math.round(loc.getZ());
        this.chestLocation = new Location(Bukkit.getWorld(loc.getWorld().getName()), x, y, z);
    }

    public void setPos1(Location loc) {
        int x = (int) Math.round(loc.getX());
        int y = (int) Math.round(loc.getY());
        int z = (int) Math.round(loc.getZ());
        this.pos1 = new Location(Bukkit.getWorld(loc.getWorld().getName()), x, y, z);
        refreshBaseY();
    }

    public void setPos2(Location loc) {
        int x = (int) Math.round(loc.getX());
        int y = (int) Math.round(loc.getY());
        int z = (int) Math.round(loc.getZ());
        this.pos2 = new Location(Bukkit.getWorld(loc.getWorld().getName()), x, y, z);
        refreshBaseY();
    }

    /** Met à jour baseY = Y minimum entre pos1 et pos2 (appelé à chaque setpos) */
    private void refreshBaseY() {
        if (pos1 != null && pos2 != null) {
            baseY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        } else if (pos1 != null) {
            baseY = pos1.getBlockY();
        } else if (pos2 != null) {
            baseY = pos2.getBlockY();
        }
    }

    /**
     * Retourne le Y de base (min Y original entre pos1/pos2, avant tout /expand).
     * Si jamais défini, retourne le min Y actuel entre pos1 et pos2.
     */
    public int getBaseY() {
        if (baseY != Integer.MIN_VALUE) return baseY;
        if (pos1 != null && pos2 != null) return Math.min(pos1.getBlockY(), pos2.getBlockY());
        if (pos1 != null) return pos1.getBlockY();
        if (pos2 != null) return pos2.getBlockY();
        return 64;
    }

    /** Permet de restaurer baseY depuis la sauvegarde */
    public void setBaseY(int y) { this.baseY = y; }

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
        String chestStr = chestLocation != null ? chestLocation.getBlockX() + "/" + chestLocation.getBlockY() + "/" + chestLocation.getBlockZ() : "§cnon définie";
        return "§7Zone §c" + name + " §8[" + enabledStr + "§8]"
            + "\n  §8» §7Pos1: §f" + pos1Str
            + "\n  §8» §7Pos2: §f" + pos2Str
            + "\n  §8» §7Chest: §f" + chestStr;
    }
}
