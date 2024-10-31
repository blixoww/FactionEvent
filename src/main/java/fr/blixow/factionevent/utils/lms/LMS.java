package fr.blixow.factionevent.utils.lms;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LMS {

    private final String name;
    private Location arenaLocation;
    private int registrationTime = 120; // temps d'inscription en secondes
    private int prepTime = 20; // temps de préparation en secondes
    private HashMap<Player, Boolean> registeredPlayers;
    private LMSEvent eventInstance;
    private boolean isEventActive = false;
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.contains("lms.prefix") ? msg.getString("lms.prefix") : "§8[§cLMS§8]§7 ";

    public LMS(String name, Location arenaLocation) {
        this.name = name;
        this.arenaLocation = arenaLocation;
        this.registeredPlayers = new HashMap<>();
    }

    public void registerPlayer(Player player) {
        if (isEventActive) {
            Bukkit.broadcastMessage(prefix + (new StrManager(msg.getString("lms.started")).rePlayer(player).reLMS(name).toString()));
            return;
        }

        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        if (faction == null || faction.isWilderness()) {
            player.sendMessage(prefix + (new StrManager(msg.getString("lms.no_faction")).rePlayer(player).reLMS(name).toString()));
            return;
        }

        long minFactionTime = FactionEvent.getInstance().getConfig().getLong("lms.min_faction_time", 86400);
        long factionDate = faction.getFoundedDate();
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - factionDate;
        long diffSeconds = diff / 1000;

        if (diffSeconds < minFactionTime) {
            player.sendMessage(prefix + (new StrManager(msg.getString("lms.faction_time")).rePlayer(player).reLMS(name).toString()));
            return;
        }

        if (!registeredPlayers.containsKey(player)) {
            registeredPlayers.put(player, true);
            player.sendMessage(new StrManager(msg.getString("lms.registered")).rePlayer(player).reLMS(name).toString());
        } else {
            player.sendMessage(new StrManager(msg.getString("lms.already_registered")).rePlayer(player).reLMS(name).toString());
        }
    }

    public void unregisterPlayer(Player player) {
        if (isEventActive) {
            Bukkit.broadcastMessage(prefix + (new StrManager(msg.getString("lms.started")).rePlayer(player).reLMS(name).toString()));
            return;
        }
        if (registeredPlayers.containsKey(player)) {
            registeredPlayers.remove(player);
            player.sendMessage(new StrManager(msg.getString("lms.unregistered")).rePlayer(player).reLMS(name).toString());
        } else {
            player.sendMessage(new StrManager(msg.getString("lms.already_unregistered")).rePlayer(player).reLMS(name).toString());
        }
    }

    public void startRegistration() {
        if (isEventActive) {
            Bukkit.broadcastMessage(prefix + (new StrManager(msg.getString("lms.started")).reLMS(name).toString()));
            return;
        }
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), this::prepareEvent, registrationTime * 20L);
    }

    private void prepareEvent() {
        if (registeredPlayers.size() < 2) {
            Bukkit.broadcastMessage(prefix + (new StrManager(msg.getString("lms.any_register")).reLMS(name).toString()));
            return;
        }

        // Créer une liste temporaire pour éviter ConcurrentModificationException
        List<Player> playersToRemove = new ArrayList<>();

        for (Player player : registeredPlayers.keySet()) {
            if (!player.isOnline()) {
                playersToRemove.add(player); // Ajoutez le joueur à la liste de suppression
            } else {
                player.teleport(arenaLocation);
                FactionMessageTitle.sendPlayersTitle(20, 40, 20, "§aPréparez-vous au combat", "Le LMS commence dans " + prepTime + " secondes");
                player.sendMessage(prefix + (new StrManager(msg.getString("lms.teleport")).reLMS(name).toString()));
            }
        }
        // Retirer les joueurs déconnectés
        for (Player player : playersToRemove) {
            registeredPlayers.remove(player);
        }

        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), this::start, prepTime * 20L);
    }

    public void start() {
        eventInstance = new LMSEvent(this, registeredPlayers, FactionEvent.getInstance().getConfig(), this);
        isEventActive = true;
        eventInstance.startCombat();
    }

    public void stop() {
        if (eventInstance != null) {
            eventInstance.endEvent();
            eventInstance = null;
        }
        isEventActive = false;
        registeredPlayers.clear();
        Bukkit.broadcastMessage(prefix + (new StrManager(msg.getString("lms.ended")).reLMS(name).toString()));
    }

    public boolean saveLMS() {
        try {
            File file = FileManager.getFile("data/lms.yml");
            FileConfiguration fc = FileManager.getLMSDataFC();
            List<String> listLMS = new ArrayList<>();
            if (fc.contains("lmslist")) {
                listLMS = fc.getStringList("lmslist");
            }
            if (!listLMS.contains(getName())) {
                listLMS.add(getName());
            }
            String world_name = arenaLocation.getWorld().getName();
            int pos1_x = (int) arenaLocation.getX(), pos1_y = (int) arenaLocation.getY(), pos1_z = (int) arenaLocation.getZ();
            fc.set(getName() + "." + "worldname", world_name);
            fc.set(getName() + "." + "arenaLocation.x", pos1_x);
            fc.set(getName() + "." + "arenaLocation.y", pos1_y);
            fc.set(getName() + "." + "arenaLocation.z", pos1_z);
            fc.set("lmslist", listLMS);
            fc.save(file);
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;

    }


    public void setArenaLocation(Location arenaLocation) {
        this.arenaLocation = arenaLocation;
    }

    public String getName() {
        return name;
    }

    public Location getArenaLocation() {
        return arenaLocation;
    }

    public HashMap<Player, Boolean> getRegisteredPlayers() {
        return registeredPlayers;
    }

    public boolean isEventActive() {
        return isEventActive;
    }

    public long getRegistrationTime() {
        return registrationTime;
    }

    public long getPrepTime() {
        return prepTime;
    }

}
