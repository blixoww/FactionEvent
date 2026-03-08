package fr.blixow.factionevent.commands.guess;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.guess.GuessEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class AnswerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("guess.prefix", "§8[§eGuess§8] §7");

        GuessEvent currentEvent = FactionEvent.getInstance().getEventOn().getGuessEvent();
        if (currentEvent == null) {
            player.sendMessage(prefix + msg.getString("guess.not_started", "§cAucun event Guess en cours."));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(prefix + msg.getString("guess.answer_usage", "§cUsage: /answer <mot>"));
            return true;
        }

        // Joindre les mots si la réponse est en plusieurs parties
        String answer = String.join(" ", args);
        currentEvent.checkGuess(player, answer);
        return true;
    }
}

