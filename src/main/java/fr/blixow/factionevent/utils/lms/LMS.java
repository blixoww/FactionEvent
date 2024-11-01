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
    private int registrationTime = 30; // Temps d'inscription en secondes
    private int prepTime = 30; // Temps de préparation en secondes
    private final HashMap<Player, Boolean> registeredPlayers; // Stocke les joueurs inscrits
    private LMSEvent eventInstance;
    private boolean isEventActive = false;
    private boolean isPreparation = false;
    private boolean isRegister = false; // Indique si l'inscription est active
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.contains("lms.prefix") ? msg.getString("lms.prefix") : "§8[§cLMS§8]§7 ";

    public LMS(String name, Location arenaLocation) {
        this.name = name;
        this.arenaLocation = arenaLocation;
        this.registeredPlayers = new HashMap<>();
    }

    public void registerPlayer(Player player) {
        // Vérifie si l'inscription est active
        if (!isRegister) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.not_started")).reLMS(name).toString());
            return;
        }

        // Vérifie si le joueur est déjà inscrit
        if (registeredPlayers.containsKey(player) && registeredPlayers.get(player)) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.already_registered")).rePlayer(player).reLMS(name).toString());
            return;
        }

        // Vérifie si le joueur a une faction
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        if (faction == null || faction.isWilderness()) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.no_faction")).rePlayer(player).reLMS(name).toString());
            return;
        }

        // Vérifie s'il y a déjà un joueur de la même faction inscrit à l'événement
        for (Player registeredPlayer : registeredPlayers.keySet()) {
            FPlayer registeredFPlayer = FPlayers.getInstance().getByPlayer(registeredPlayer);
            Faction registeredFaction = registeredFPlayer.getFaction();

            if (registeredFaction.getId().equals(faction.getId())) {
                player.sendMessage(prefix + new StrManager(msg.getString("lms.same_faction"))
                        .rePlayer(player).reLMS(name).toString());
                return;
            }
        }

        // Vérifie le temps minimum d'existence de la faction
        long minFactionTime = FactionEvent.getInstance().getConfig().getLong("lms.min_faction_time", 86400);
        long factionDate = faction.getFoundedDate();
        long currentTime = System.currentTimeMillis();
        long diffSeconds = (currentTime - factionDate) / 1000;

        if (diffSeconds < minFactionTime) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.faction_time")).rePlayer(player).reLMS(name).toString());
            return;
        }

        // Inscrit le joueur
        registeredPlayers.put(player, true);
        player.sendMessage(prefix + new StrManager(msg.getString("lms.registered")).rePlayer(player).reLMS(name).toString());
    }

    public void unregisterPlayer(Player player) {
        // Vérifie si l'inscription est active
        if (!isRegister) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.not_started")).reLMS(name).toString());
            return;
        }

        // Vérifie si le joueur est inscrit
        if (!registeredPlayers.containsKey(player) || !registeredPlayers.get(player)) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.not_registered")).rePlayer(player).reLMS(name).toString());
            return;
        }

        // Désinscrit le joueur
        registeredPlayers.put(player, false);
        player.sendMessage(prefix + new StrManager(msg.getString("lms.unregistered")).rePlayer(player).reLMS(name).toString());
    }

    public void startRegistration() {
        // Vérifie si l'événement est déjà actif ou si l'inscription est déjà en cours
        if (isEventActive || isPreparation || isRegister) {
            Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.too_late")).reLMS(name).toString());
            return;
        }

        // Démarre l'inscription
        isRegister = true;
        Bukkit.broadcastMessage(new StrManager(msg.getString("lms.registration_started")).reLMS(name).toString());
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), this::prepareEvent, registrationTime * 20L);
    }

    private boolean isNotEnoughPlayers() {
        long registeredCount = registeredPlayers.values().stream().filter(Boolean::booleanValue).count();
        return registeredCount < 2;
    }

    private void prepareEvent() {
        if (isNotEnoughPlayers()) {
            Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.any_register")).reLMS(name).toString());
            resetEvent();
        } else {
            isRegister = false;
            isPreparation = true; // Marque le début de la phase de préparation
            List<Player> playersToRemove = new ArrayList<>();

            // Prépare les joueurs
            for (Player player : registeredPlayers.keySet()) {
                if (!player.isOnline()) {
                    // Si le joueur est déconnecté pendant la préparation, il sera retiré
                    playersToRemove.add(player);
                } else if (registeredPlayers.get(player)) {
                    player.teleport(arenaLocation);
                    FactionMessageTitle.sendPlayersTitle(20, 60, 20, "§aPréparez-vous", "Le LMS commence dans 30 secondes");
                    player.sendMessage(prefix + new StrManager(msg.getString("lms.teleport")).reLMS(name).toString());
                }
            }

            // Retire les joueurs déconnectés
            for (Player player : playersToRemove) {
                registeredPlayers.remove(player);
            }

            // Démarre l'événement après le temps de préparation
            Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), this::start, prepTime * 20L);
        }
    }

    public void start() {
        // Vérifie si au moins deux joueurs sont présents pour commencer l'événement
        if (isNotEnoughPlayers()) {
            Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.any_register")).reLMS(name).toString());
            resetEvent();
            return;
        }

        isPreparation = false;
        isEventActive = true;
        eventInstance = new LMSEvent(this, registeredPlayers, FactionEvent.getInstance().getConfig());
        eventInstance.startCombat();
    }

    public void stop() {
        // Termine l'événement en cours
        if (eventInstance != null) {
            eventInstance.endEvent();
            eventInstance = null;
        }
        resetEvent();
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.ended")).reLMS(name).toString());
    }

    private void resetEvent() {
        // Réinitialise l'état de l'événement
        isRegister = false;
        isPreparation = false;
        isEventActive = false;
        registeredPlayers.clear();
    }

    public boolean saveLMS() {
        // Enregistre les informations de l'événement dans le fichier
        try {
            File file = FileManager.getFile("data/lms.yml");
            FileConfiguration fc = FileManager.getLMSDataFC();
            List<String> listLMS = fc.getStringList("lmslist");
            if (!listLMS.contains(getName())) {
                listLMS.add(getName());
            }
            String worldName = arenaLocation.getWorld().getName();
            fc.set(getName() + ".worldname", worldName);
            fc.set(getName() + ".arenaLocation.x", arenaLocation.getBlockX());
            fc.set(getName() + ".arenaLocation.y", arenaLocation.getBlockY());
            fc.set(getName() + ".arenaLocation.z", arenaLocation.getBlockZ());
            fc.set("lmslist", listLMS);
            fc.save(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    public boolean isRegister() {
        return isRegister;
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

    @Override
    public String toString() {
        return "§8§m-----§r§8[§cLMS§8]§m-----\n\n§8» §cNom : §7" + name +
                "\n§8» §cPosition :\n§8-> §7X = §f" + arenaLocation.getBlockX() +
                "\n§8-> §7Y = §f" + arenaLocation.getBlockY() +
                "\n§8-> §7Z = §f" + arenaLocation.getBlockZ();
    }

    public boolean isRegistration() {
        return isRegister;
    }
}
