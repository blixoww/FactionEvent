package fr.blixow.factionevent.commands.guess;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.guess.Guess;
import fr.blixow.factionevent.utils.guess.GuessEvent;
import fr.blixow.factionevent.utils.guess.GuessManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GuessCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String prefix = FileManager.getMessageFileConfiguration().getString("guess.prefix");
            if (!player.hasPermission("factionevent.admin.guess")) {
                player.sendMessage(prefix + FileManager.getMessageFileConfiguration().getString("no-permissions"));
                return true;
            }
            if (args.length == 1) {
                String action = args[0];
                switch (action) {
                    case "start":
                        GuessEvent currentEvent = FactionEvent.getInstance().getEventOn().getGuessEvent();
                        if (currentEvent != null) {
                            player.sendMessage(prefix + msg.getString("guess.already_started"));
                        } else {
                            Guess loadedGuess = GuessManager.loadWordsFromConfig();
                            FactionEvent.getInstance().getEventOn().start(loadedGuess, player);
                        }
                        break;
                    case "stop":
                        if (FactionEvent.getInstance().getEventOn().getGuessEvent() == null) {
                            player.sendMessage(prefix + msg.getString("guess.not_started"));
                        } else {
                            FactionEvent.getInstance().getEventOn().getGuessEvent().getGuess().stop();
                            player.sendMessage(prefix + msg.getString("guess.canceled"));
                        }
                        break;
                    case "info":
                        if (FactionEvent.getInstance().getEventOn().getGuessEvent() == null) {
                            //Show all words in the config
                            List<String> words = FileManager.getConfig().getStringList("guess.words");
                            player.sendMessage(prefix + "§7Mots disponibles :");
                            for (String word : words) {
                                player.sendMessage("§8- §7" + word);
                            }
                        }
                        break;
                    default:
                        player.sendMessage(prefix + msg.getString("guess.usage"));
                        break;
                }
            } else {
                player.sendMessage(prefix + msg.getString("guess.usage"));
            }
            return true;
        }
        sender.sendMessage("Vous devez être un joueur.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if (args.length == 1) {
            List<String> actions = Arrays.asList("start", "stop", "info");
            for (String act : actions) {
                if (act.toLowerCase().startsWith(args[0].toLowerCase())) {
                    stringList.add(act);
                }
            }
        }
        return stringList;
    }
}
