package fr.blixow.factionevent.utils;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.EventManager;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class FactionMessageTitle {

    public static void sendFactionTitle(Faction faction, int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        if (!faction.isWilderness()) {
            HashMap<Player, EventManager> managerHashMap = FactionEvent.getInstance().getEventScoreboardOff();
            for (Player player : faction.getOnlinePlayers()) {
                EventManager eventManager = managerHashMap.get(player);
                if (managerHashMap.containsKey(player)) {
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
            HashMap<Player, EventManager> managerHashMap = FactionEvent.getInstance().getEventScoreboardOff();
            for (Player player : faction.getOnlinePlayers()) {
                if (managerHashMap.containsKey(player)) {
                    EventManager eventManager = managerHashMap.get(player);
                    if (eventManager.isActionbar()) {
                        Messages.sendActionBar(player, message);
                    }
                }
            }
        }
    }

    public static void sendPlayersMessage(String message, Player... players) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

}
