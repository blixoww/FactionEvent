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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String prefix = msg.getString("guess.prefix");

            if (args.length < 1) {
                player.sendMessage(prefix + msg.getString("guess.answer_usage"));
                return true;
            }

            GuessEvent currentEvent = FactionEvent.getInstance().getEventOn().getGuessEvent();
            if (currentEvent == null) {
                player.sendMessage(prefix + msg.getString("guess.not_started"));
                return true;
            }
        }

        sender.sendMessage("Vous devez être un joueur.");
        return true;
    }
}

