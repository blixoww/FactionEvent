package fr.blixow.factionevent.utils;

import fr.redfaction.api.RedFactionAPI;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import fr.redfaction.main.RedFaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Pont d'adaptation entre FactionEvent et l'API RedFaction.
 *
 * <p>Centralise les différences de sémantique avec l'ancienne API FactionsUUID :
 * dans RedFaction un joueur sans faction renvoie {@code null} (il n'y a pas de
 * "faction wilderness"), les identifiants de faction sont des {@link UUID}, et
 * les factions ne possèdent pas de méthode {@code sendMessage}.</p>
 */
public final class FactionUtil {

    private FactionUtil() {}

    // ---- Joueurs ----

    /** FPlayer d'un joueur connecté (créé si nécessaire). */
    public static FPlayer fplayer(Player player) {
        return RedFactionAPI.get().getFPlayer(player);
    }

    /** FPlayer par UUID (null si inconnu), remplace getByOfflinePlayer. */
    public static FPlayer fplayer(UUID uuid) {
        return RedFactionAPI.get().getFPlayerByUUID(uuid);
    }

    /** Faction d'un joueur, ou null s'il n'en a pas. */
    public static Faction faction(Player player) {
        FPlayer fp = fplayer(player);
        return fp == null ? null : fp.getFaction();
    }

    // ---- Factions ----

    /**
     * Équivalent de l'ancien {@code faction.isWilderness()} : dans RedFaction,
     * l'absence de faction se traduit par une référence nulle.
     */
    public static boolean isWilderness(Faction faction) {
        return faction == null;
    }

    /** Toutes les factions connues (inclut SafeZone et WarZone). */
    public static Collection<Faction> allFactions() {
        RedFaction plugin = RedFaction.getInstance();
        if (plugin == null) return Collections.emptyList();
        return plugin.getFactionManager().getAllFactions();
    }

    /** Faction par nom (insensible à la casse), ou null. Remplace getByTag. */
    public static Faction byName(String name) {
        if (name == null) return null;
        RedFaction plugin = RedFaction.getInstance();
        if (plugin == null) return null;
        return plugin.getFactionManager().getFactionByName(name);
    }

    /** Clé String stable d'une faction (pour le classement.yml). */
    public static String id(Faction faction) {
        return faction.getId().toString();
    }

    /** Envoie un message à tous les membres en ligne d'une faction. */
    public static void sendMessage(Faction faction, String message) {
        if (faction == null) return;
        for (UUID uuid : faction.getMembers().keySet()) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) online.sendMessage(message);
        }
    }

    /** Date de fondation d'une faction (epoch millis), 0 si inconnue/legacy. */
    public static long foundedDate(Faction faction) {
        return faction == null ? 0L : faction.getFoundedDate();
    }
}
