package fr.blixow.factionevent.utils.lms;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.perms.Role;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class LMS {

    private final String name;
    private Location arenaLocation;
    private int registrationTime = 120; // 2 minutes d'inscription
    private int prepTime = 30;
    private int gracePeriod = 60; // 1 minute de grâce avant le combat
    private LMSMode mode = LMSMode.SOLO;

    /** Joueurs inscrits → true = inscrit */
    private final HashMap<Player, Boolean> registeredPlayers;

    /** Sauvegarde des inventaires avant le kit */
    private final HashMap<UUID, ItemStack[]> savedInventories = new HashMap<>();
    /** Sauvegarde des armures avant le kit */
    private final HashMap<UUID, ItemStack[]> savedArmor = new HashMap<>();

    private LMSEvent eventInstance;
    private Phase phase = null;
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.contains("lms.prefix") ? msg.getString("lms.prefix") : "§8[§cLMS§8]§7 ";

    public LMS(String name, Location arenaLocation, Phase phase) {
        this.name = name;
        this.arenaLocation = arenaLocation;
        this.registeredPlayers = new HashMap<>();
        this.phase = phase;
        try {
            FileConfiguration cfg = FileManager.getConfig();
            if (cfg != null) {
                if (cfg.contains("lms.registration_time")) this.registrationTime = cfg.getInt("lms.registration_time");
                if (cfg.contains("lms.prep_time")) this.prepTime = cfg.getInt("lms.prep_time");
                if (cfg.contains("lms.grace_period")) this.gracePeriod = cfg.getInt("lms.grace_period");
            }
        } catch (Exception ignored) {}
    }

    // ───────────────────────────────────────────────────────────────
    //  INSCRIPTION
    // ───────────────────────────────────────────────────────────────

    public void registerPlayer(Player player) {
        if (!phase.equals(Phase.REGISTRATION)) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.not_started")).reLMS(name).toString());
            return;
        }
        if (registeredPlayers.containsKey(player) && registeredPlayers.get(player)) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.already_registered")).rePlayer(player).reLMS(name).toString());
            return;
        }

        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        if (faction == null || faction.isWilderness()) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.no_faction")).rePlayer(player).reLMS(name).toString());
            return;
        }

        // Vérification rôle : chef ou officier uniquement, sauf si aucun n'est en ligne
        Role role = fPlayer.getRole();
        boolean isLeaderOrOfficer = role == Role.ADMIN || role == Role.MODERATOR || role == Role.COLEADER;
        if (!isLeaderOrOfficer) {
            boolean anyLeaderOrOfficerOnline = false;
            for (FPlayer fp : FPlayers.getInstance().getOnlinePlayers()) {
                if (fp.getFaction().getId().equals(faction.getId())
                        && (fp.getRole() == Role.ADMIN || fp.getRole() == Role.MODERATOR || fp.getRole() == Role.COLEADER)) {
                    anyLeaderOrOfficerOnline = true;
                    break;
                }
            }
            if (anyLeaderOrOfficerOnline) {
                player.sendMessage(prefix + msg.getString("lms.leader_only",
                    "§cSeul le chef ou un officier de votre faction peut s'inscrire."));
                return;
            }
        }

        // Limite par faction : 1 en SOLO, 2 en DUO
        int maxPerFaction = (mode == LMSMode.DUO) ? 2 : 1;
        int countForFaction = 0;
        for (Player rp : registeredPlayers.keySet()) {
            FPlayer rfp = FPlayers.getInstance().getByPlayer(rp);
            if (rfp != null && rfp.getFaction().getId().equals(faction.getId())) {
                countForFaction++;
            }
        }
        if (countForFaction >= maxPerFaction) {
            if (mode == LMSMode.DUO) {
                player.sendMessage(prefix + msg.getString("lms.duo_faction_full",
                    "§cVotre faction a déjà §e2 §cjoueurs inscrits en mode DUO."));
            } else {
                player.sendMessage(prefix + new StrManager(msg.getString("lms.same_faction")).rePlayer(player).reLMS(name).toString());
            }
            return;
        }

        // Age minimum de faction
        long minFactionTime = FactionEvent.getInstance().getConfig().getLong("lms.min_faction_time", 86400);
        long factionDate = faction.getFoundedDate();
        long diffSeconds = (System.currentTimeMillis() - factionDate) / 1000;
        if (diffSeconds < minFactionTime) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.faction_time")).rePlayer(player).reLMS(name).toString());
            return;
        }

        registeredPlayers.put(player, true);
        player.sendMessage(prefix + new StrManager(msg.getString("lms.registered")).rePlayer(player).reLMS(name).toString());
    }

    public void unregisterPlayer(Player player) {
        if (!phase.equals(Phase.REGISTRATION)) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.not_started")).reLMS(name).toString());
            return;
        }
        if (!registeredPlayers.containsKey(player)) {
            player.sendMessage(prefix + new StrManager(msg.getString("lms.not_registered")).rePlayer(player).reLMS(name).toString());
            return;
        }
        registeredPlayers.remove(player);
        player.sendMessage(prefix + new StrManager(msg.getString("lms.unregistered")).rePlayer(player).reLMS(name).toString());
    }

    // ───────────────────────────────────────────────────────────────
    //  DÉMARRAGE
    // ───────────────────────────────────────────────────────────────

    public void startRegistration(LMSMode lmsMode, Player... players) {
        this.mode = lmsMode;

        // Vérifier qu'aucun LMS n'est déjà actif
        if (FactionEvent.getInstance().getEventOn().getLMSEvent() != null) {
            for (Player p : players)
                p.sendMessage(prefix + new StrManager(msg.getString("lms.already_started")).reLMS(name).toString());
            return;
        }
        // Vérifier que cet LMS n'est pas déjà en cours
        if (!phase.equals(Phase.NOT_STARTED)) {
            for (Player p : players)
                p.sendMessage(prefix + new StrManager(msg.getString("lms.already_started")).reLMS(name).toString());
            return;
        }

        EventOn eventOn = FactionEvent.getInstance().getEventOn();

        // Si un autre event est en cours → file d'attente (phase reste NOT_STARTED)
        if (!eventOn.canStartAnEvent()) {
            String queueMsg = prefix + new StrManager(msg.getString("lms.adding_to_queue")).reLMS(name).toString();
            FactionMessageTitle.sendPlayersMessage(queueMsg, players);
            eventOn.getQueue().add(this);
            return;
        }

        // Démarrage immédiat
        phase = Phase.REGISTRATION;
        try {
            eventOn.start(this, players); // réserve le slot + setup schedulers
            String regMsg = new StrManager(msg.getString("lms.registration_started"))
                .reLMS(name)
                .reCustom("{mode}", mode.getDisplayName())
                .toString();
            Bukkit.broadcastMessage(regMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Rappel à 1 minute
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), () -> {
            if (!isRegistration()) return;
            String reminder = new StrManager(msg.getString("lms.registration_reminder",
                "§8§m-----------------------------------------------------\n"
                + "§r §8< §6§lLMS - 1 MINUTE RESTANTE §8>\n"
                + "§7Le §cLast Man Standing §c{lms} §7ferme ses inscriptions dans §c1 minute §7!\n"
                + "§7▶ Inscrivez-vous encore avec §e/lmsr register\n"
                + "§8§m-----------------------------------------------------"))
                .reLMS(name).toString();
            Bukkit.broadcastMessage(reminder);
        }, 60 * 20L);

        // Fin inscriptions → préparation
        Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), this::prepareEvent, registrationTime * 20L);
    }

    /** Appelé depuis la file d'attente EventOn — utilise le mode déjà stocké. */
    public void startRegistration(Player... players) {
        startRegistration(this.mode, players);
    }

    private boolean isNotEnoughPlayers() {
        return registeredPlayers.size() < 2;
    }

    private void prepareEvent() {
        if (isNotEnoughPlayers()) {
            Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.any_register")).reLMS(name).toString());
            resetEvent();
        } else {
            phase = Phase.PREPARATION;
            List<Player> toRemove = new ArrayList<>();
            for (Player player : registeredPlayers.keySet()) {
                if (!player.isOnline()) {
                    toRemove.add(player);
                } else if (registeredPlayers.get(player)) {
                    player.teleport(arenaLocation);
                    FactionMessageTitle.sendPlayersTitle(20, 60, 20, "§aPréparez-vous",
                        "§7LMS §e" + mode.getDisplayName() + " §7— le kit vous sera donné au lancement !");
                    player.sendMessage(prefix + new StrManager(msg.getString("lms.teleport")).reLMS(name).toString());
                }
            }
            for (Player player : toRemove) registeredPlayers.remove(player);

            Bukkit.getScheduler().runTaskLater(FactionEvent.getInstance(), this::startMainEvent, prepTime * 20L);
        }
    }

    public void startMainEvent() {
        if (isNotEnoughPlayers()) {
            Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.any_register")).reLMS(name).toString());
            resetEvent();
            FactionEvent.getInstance().getEventOn().setLMSEvent(null);
            return;
        }

        LMSEvent lmsEvent = FactionEvent.getInstance().getEventOn().getLMSEvent();
        if (lmsEvent == null) {
            lmsEvent = new LMSEvent(this);
            FactionEvent.getInstance().getEventOn().setLMSEvent(lmsEvent);
        }
        eventInstance = lmsEvent;
        phase = Phase.COMBAT;
        eventInstance.startEvent();
    }

    // ───────────────────────────────────────────────────────────────
    //  INVENTAIRE — Sauvegarde / Restauration / Kit
    // ───────────────────────────────────────────────────────────────

    public void saveAndEquipPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        savedInventories.put(uuid, player.getInventory().getContents().clone());
        savedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.updateInventory();
        try {
            String kitCmd = FileManager.getConfig().getString("lms.kit_command", "kit pvp %player%");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), kitCmd.replace("%player%", player.getName()));
        } catch (Exception ignored) {}
    }

    public void restorePlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (savedInventories.containsKey(uuid)) {
            player.getInventory().clear();
            player.getInventory().setContents(savedInventories.remove(uuid));
        }
        if (savedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(savedArmor.remove(uuid));
        }
        player.updateInventory();
        try {
            String spawnCmd = FileManager.getConfig().getString("lms.spawn_command", "spawn %player%");
            final String finalCmd = spawnCmd.replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(FactionEvent.getInstance(), () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
        } catch (Exception ignored) {}
    }

    /** Restaure uniquement l'inventaire (déconnexion - le joueur ne peut pas être téléporté). */
    public void restoreInventoryOnly(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (savedInventories.containsKey(uuid)) {
            player.getInventory().clear();
            player.getInventory().setContents(savedInventories.remove(uuid));
        }
        if (savedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(savedArmor.remove(uuid));
        }
        player.updateInventory();
    }

    public boolean hasSavedInventory(UUID uuid) {
        return savedInventories.containsKey(uuid);
    }

    public void clearSavedInventory(UUID uuid) {
        savedInventories.remove(uuid);
        savedArmor.remove(uuid);
    }

    // ───────────────────────────────────────────────────────────────
    //  STOP / RESET
    // ───────────────────────────────────────────────────────────────

    public void stop() {
        if (eventInstance != null) {
            eventInstance.endEvent();
            eventInstance = null;
        } else {
            FactionEvent.getInstance().getEventOn().setLMSEvent(null);
        }
        resetPhase();
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("lms.ended",
            "§cLe LMS §e{lms}§c est terminé.")).reLMS(name).toString());
    }

    private void resetEvent() {
        FactionEvent.getInstance().getEventOn().setLMSEvent(null);
        resetPhase();
    }

    public void resetPhase() {
        phase = Phase.NOT_STARTED;
        registeredPlayers.clear();
        savedInventories.clear();
        savedArmor.clear();
    }

    // ───────────────────────────────────────────────────────────────
    //  SAUVEGARDE FICHIER
    // ───────────────────────────────────────────────────────────────

    public boolean saveLMS() {
        try {
            File file = FileManager.getFile("data/lms.yml");
            FileConfiguration fc = FileManager.getLMSDataFC();
            List<String> listLMS = fc.getStringList("lmslist");
            if (!listLMS.contains(getName())) listLMS.add(getName());
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

    // ───────────────────────────────────────────────────────────────
    //  GETTERS / SETTERS
    // ───────────────────────────────────────────────────────────────

    public void setArenaLocation(Location arenaLocation) { this.arenaLocation = arenaLocation; }
    public String getName() { return name; }
    public Location getArenaLocation() { return arenaLocation; }
    public HashMap<Player, Boolean> getRegisteredPlayers() { return registeredPlayers; }
    public long getRegistrationTime() { return registrationTime; }
    public long getPrepTime() { return prepTime; }
    public LMSMode getMode() { return mode; }
    public int getGracePeriod() { return gracePeriod; }

    public boolean isRegistration() { return phase != null && phase.equals(Phase.REGISTRATION); }
    public boolean isPreparation() { return phase != null && phase.equals(Phase.PREPARATION); }
    public boolean isStarted() { return phase != null && phase.equals(Phase.COMBAT); }

    @Override
    public String toString() {
        return "§8§m-----§r§8[§cLMS§8]§m-----\n\n§8» §cNom : §7" + name
            + "\n§8» §cMode : §7" + mode.getDisplayName()
            + "\n§8» §cPosition :\n§8-> §7X = §f" + arenaLocation.getBlockX()
            + "\n§8-> §7Y = §f" + arenaLocation.getBlockY()
            + "\n§8-> §7Z = §f" + arenaLocation.getBlockZ();
    }
}
