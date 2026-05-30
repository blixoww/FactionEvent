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
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = FileManager.getMessageFileConfiguration().getString("guess.prefix");

        if (args.length == 1) {
            String action = args[0].toLowerCase();

            // Action accessible à tous : /guess next -> affiche le temps avant le prochain guess planifié
            if (action.equals("next")) {
                long nextTs = FactionEvent.getInstance().getNextGuessTimestampMillis();
                if (nextTs > 0) {
                    long remainingMs = nextTs - System.currentTimeMillis();
                    if (remainingMs > 0) {
                        long seconds = remainingMs / 1000;
                        long mins = seconds / 60;
                        long secs = seconds % 60;
                        player.sendMessage(prefix + "§7Prochain Guess aléatoire dans §e" + mins + "m " + secs + "s§7.");
                    } else {
                        player.sendMessage(prefix + "§7Un Guess va bientôt démarrer.");
                    }
                } else {
                    player.sendMessage(prefix + "§7Aucun Guess aléatoire planifié.");
                }
                return true;
            }

            // Les actions administratives requièrent la permission
            if (!player.hasPermission("factionevent.admin.guess")) {
                player.sendMessage(msg.getString("prefix") + msg.getString("no-permissions"));
                return true;
            }

            switch (action) {
                case "start":
                    GuessEvent currentEvent = FactionEvent.getInstance().getEventOn().getGuessEvent();
                    if (currentEvent != null) {
                        player.sendMessage(prefix + msg.getString("guess.already_started"));
                    } else {
                        // Prefer create from prepared selection if exists
                        Guess prepared = GuessManager.createGuessFromPrepared();
                        if (prepared != null) {
                            FactionEvent.getInstance().getEventOn().start(prepared, player);
                        } else {
                            Guess loadedGuess = GuessManager.loadWordsFromConfig();
                            FactionEvent.getInstance().getEventOn().start(loadedGuess, player);
                        }
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
                    // Info est désormais restreint au staff (permission vérifiée ci-dessus)
                    // Premièrement, afficher la sélection préparée si disponible
                    List<String> prepared = GuessManager.getLastSelectedWords();
                    if (!prepared.isEmpty()) {
                        player.sendMessage(prefix + "§7Mots préparés pour le prochain Guess (§e" + prepared.size() + "§7) :");
                        for (String w : prepared) player.sendMessage("§8- §7" + w);
                    } else {
                        // Sinon afficher tous les mots du fichier
                        FileConfiguration guessFc = FileManager.getGuessDataFC();
                        List<String> words = new ArrayList<>();
                        if (guessFc != null && guessFc.contains("guess.words")) {
                            words = guessFc.getStringList("guess.words");
                        }
                        if (words.isEmpty()) {
                            player.sendMessage(prefix + "§cAucun mot disponible.");
                        } else {
                            player.sendMessage(prefix + "§7Mots disponibles (§e" + words.size() + "§7) :");
                            for (String word : words) {
                                player.sendMessage("§8- §7" + word);
                            }
                        }
                    }
                    break;
                default:
                    player.sendMessage(prefix + msg.getString("guess.usage"));
                    break;
            }

            return true;
        }

        player.sendMessage(prefix + msg.getString("guess.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if (args.length == 1) {
            List<String> actions = Arrays.asList("start", "stop", "info", "next");
            for (String act : actions) {
                if (act.toLowerCase().startsWith(args[0].toLowerCase())) {
                    stringList.add(act);
                }
            }
        }
        return stringList;
    }
}
