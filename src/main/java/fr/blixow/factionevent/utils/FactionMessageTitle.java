package fr.blixow.factionevent.utils;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.EventManager;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FactionMessageTitle {

    /**
     * Retourne la liste des joueurs en ligne appartenant à une faction.
     * Remplace faction.getOnlinePlayers() qui n'existe pas dans cette version de MassiveCraft.
     */
    private static List<Player> getOnlineFactionPlayers(Faction faction) {
        List<Player> players = new ArrayList<>();
        for (FPlayer fPlayer : FPlayers.getInstance().getOnlinePlayers()) {
            if (fPlayer.getFaction().equals(faction)) {
                Player player = Bukkit.getPlayer(fPlayer.getName());
                if (player != null && player.isOnline()) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    public static void sendFactionTitle(Faction faction, int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        if (!faction.isWilderness()) {
            HashMap<Player, EventManager> managerHashMap = FactionEvent.getInstance().getEventScoreboardOff();
            for (Player player : getOnlineFactionPlayers(faction)) {
                if (managerHashMap.containsKey(player)) {
                    EventManager eventManager = managerHashMap.get(player);
                    if (eventManager.isTitle()) {
                        Messages.sendTitle(player, fadeIn, stay, fadeOut, title, subtitle);
                    }
                }
            }
        }
    }

    public static void sendPlayersTitle(int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        HashMap<Player, EventManager> managerHashMap = FactionEvent.getInstance().getEventScoreboardOff();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (managerHashMap.containsKey(player)) {
                EventManager eventManager = managerHashMap.get(player);
                if (eventManager.isTitle()) {
                    Messages.sendTitle(player, fadeIn, stay, fadeOut, title, subtitle);
                }
            }
        }
    }

    public static void sendFactionActionBar(Faction faction, String message) {
        if (!faction.isWilderness()) {
            for (Player player : getOnlineFactionPlayers(faction)) {
                Messages.sendActionBar(player, message);
            }
        }
    }

    public static void sendPlayersMessage(String message, Player... players) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

}
