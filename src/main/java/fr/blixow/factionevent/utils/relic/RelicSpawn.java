package fr.blixow.factionevent.utils.relic;

import org.bukkit.Location;

/**
 * Point de spawn nommé pour la Course à la Relique.
 * Au lancement de l'event, l'un de ces points est tiré au hasard.
 */
public class RelicSpawn {
    private final String name;
    private Location location;

    public RelicSpawn(String name, Location location) {
        this.name = name;
        setLocation(location);
    }

    public String getName() { return name; }
    public Location getLocation() { return location; }

    public void setLocation(Location loc) {
        // Position exacte : la relique apparaît précisément à l'endroit défini (aucun arrondi/zone).
        this.location = (loc == null) ? null : loc.clone();
    }

    @Override
    public String toString() {
        String locStr = location != null
            ? location.getWorld().getName() + " " + location.getBlockX() + "/" + location.getBlockY() + "/" + location.getBlockZ()
            : "§cnon définie";
        return "§7Spawn §c" + name + " §8» §f" + locStr;
    }
}
