package fr.blixow.factionevent.commands.purge;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.purge.Purge;
import fr.blixow.factionevent.utils.purge.PurgeEvent;
import fr.blixow.factionevent.utils.purge.PurgeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class PurgeCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }
        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("purge.prefix", "§8[§cPURGE§8]§7 ");

        if (!player.hasPermission("factionevent.admin.purge")) {
            player.sendMessage(msg.getString("prefix") + msg.getString("no-permissions"));
            return true;
        }

        if (args.length == 0) { sendUsage(player, prefix); return true; }

        switch (args[0].toLowerCase()) {
            case "start": {
                if (PurgeManager.isPurgeStarted()) {
                    player.sendMessage(prefix + msg.getString("purge.already_started",
                        "§cUne Purge est déjà en cours."));
                    return true;
                }
                new Purge().start(player);
                break;
            }
            case "stop": {
                new Purge().stop(player);
                break;
            }
            case "top": {
                PurgeEvent ev = FactionEvent.getInstance().getEventOn().getPurgeEvent();
                if (ev == null) {
                    player.sendMessage(prefix + msg.getString("purge.not_started",
                        "§cAucune Purge en cours."));
                    return true;
                }
                long elapsed = (System.currentTimeMillis() - ev.getStartTime()) / 1000;
                int remaining = (int) Math.max(0, ev.getDuration() - elapsed);
                player.sendMessage("§8§m      §r §8[ §cTOP PURGE §8] §m      ");
                player.sendMessage("§8» §7Temps restant : §c" + DateManager.getFormattedTime(remaining));
                List<Map.Entry<UUID, Integer>> top = new ArrayList<>(ev.getKills().entrySet());
                top.sort((a, b) -> b.getValue() - a.getValue());
                if (top.isEmpty()) {
                    player.sendMessage("  §7Aucun kill enregistré pour le moment.");
                } else {
                    int rank = 1;
                    for (Map.Entry<UUID, Integer> e : top) {
                        if (rank > 5) break;
                        String name = ev.getPlayerNames().getOrDefault(e.getKey(), "?");
                        player.sendMessage("  §8» §7" + rank + ". §c" + name
                            + " §8- §e" + e.getValue() + " §7kills");
                        rank++;
                    }
                }
                break;
            }
            default:
                sendUsage(player, prefix);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("start", "stop", "top")) {
                if (s.startsWith(args[0].toLowerCase())) suggestions.add(s);
            }
        }
        return suggestions;
    }

    private void sendUsage(Player player, String prefix) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        player.sendMessage(prefix + msg.getString("purge.usage",
            "§cUsage : §7/purge <start|stop|top>"));
    }
}
