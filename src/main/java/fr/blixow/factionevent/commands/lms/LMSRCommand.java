package fr.blixow.factionevent.commands.lms;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LMSRCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande peut uniquement être exécutée par un joueur.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("lms.prefix");
        String message = prefix;

        // Vérifier les arguments
        if (args.length < 1) {
            message += new StrManager(msg.getString("lms.usage_2")).toString();
            player.sendMessage(message);
            return true;
        }

        String action = args[0].toLowerCase();

        LMS lms = LMSManager.getStartedLMS();

        if (action.equals("register")) {
            if (lms == null) {
                message += new StrManager(msg.getString("lms.not_found")).toString();
                player.sendMessage(message);
                return true;
            }
            lms.registerPlayer(player);
            return true;
        }
        if (action.equals("unregister")) {
            if (lms == null) {
                message += new StrManager(msg.getString("lms.not_found")).toString();
                player.sendMessage(message);
                return true;
            }
            lms.unregisterPlayer(player);
            return true;
        }
        message += new StrManager(msg.getString("lms.usage_2")).toString();
        player.sendMessage(message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("register", "unregister");
        }
        return Collections.emptyList();
    }
}
