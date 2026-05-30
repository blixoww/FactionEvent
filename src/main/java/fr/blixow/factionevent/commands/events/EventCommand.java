package fr.blixow.factionevent.commands.events;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.EventManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!FactionEvent.getInstance().getEventScoreboardOff().containsKey(player)) {
                    EventManager eventManager = EventManager.loadFromFile(player);
                    FactionEvent.getInstance().getEventScoreboardOff().put(player, eventManager);
                }
                if (args.length == 0) {
                    EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                    String title = eventManager.isTitle() ? "§aActivé" : "§cDésactivé";
                    String actionbar = eventManager.isActionbar() ? "§aActivé" : "§cDésactivé";
                    player.sendMessage("§7§m----§r§7[ §cEVENT §7]§m----");
                    player.sendMessage("");
                    player.sendMessage("§8» §7Title: " + title);
                    player.sendMessage("§8» §7Actionbar: " + actionbar);
                    player.sendMessage("");
                } else if (args.length == 1) {
                    EventManager eventManager = FactionEvent.getInstance().getEventScoreboardOff().get(player);
                    switch (args[0]) {
                        case "title":
                            eventManager.switchTitle(player);
                            eventManager.saveFile();
                            break;
                        case "actionbar":
                            eventManager.switchActionbar(player);
                            eventManager.saveFile();
                            break;
                    }
                } else {
                    player.sendMessage("§7Commande: §f/" + label.toLowerCase() + " <title/actionbar>");
                }
                return true;
            }
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return true;
        }
    }

    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param alias   The alias used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed and command label
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = Arrays.asList("title", "actionbar");
        List<String> stringList = new ArrayList<>();
        if (args.length == 1) {
            for (String l : list) {
                if (l.toLowerCase().startsWith(args[0].toLowerCase())) {
                    stringList.add(l);
                }
            }
            if (stringList.isEmpty()) {
                stringList = list;
            }
        }
        return stringList;
    }
}


