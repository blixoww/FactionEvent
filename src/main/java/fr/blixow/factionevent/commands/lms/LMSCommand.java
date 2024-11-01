package fr.blixow.factionevent.commands.lms;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSManager;
import fr.blixow.factionevent.utils.lms.Phase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LMSCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            String prefix = FileManager.getMessageFileConfiguration().getString("lms.prefix");
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            Player player = (Player) sender;
            if (!player.hasPermission("factionevent.admin.lms")) {
                player.sendMessage(prefix + FileManager.getMessageFileConfiguration().getString("no-permissions"));
                return true;
            }
            if (args.length == 2) {
                LMS lms = LMSManager.getLMS(args[0]);
                if (!args[1].equals("create") && lms == null) {
                    player.sendMessage(prefix + new StrManager(msg.getString("lms.not_found")).reLMS(args[0]).toString());
                    return true;
                }
                switch (args[1]) {
                    case "create":
                        if (LMSManager.getLMS(args[0]) != null) {
                            player.sendMessage(prefix + new StrManager(msg.getString("lms.already_exists")).reLMS(args[0]).toString());
                            break;
                        }
                        FactionEvent.getInstance().getListLMS().add(new LMS(args[0], player.getLocation(), Phase.NOT_STARTED));
                        player.sendMessage(prefix + new StrManager(msg.getString("lms.created")).reLMS(args[0]).toString());
                        break;
                    case "start":
                        lms.startRegistration();
                        break;
                    case "stop":
                        lms.stop();
                        break;
                    case "pos":
                        lms.setArenaLocation(player.getLocation());
                        player.sendMessage(prefix + new StrManager(msg.getString("lms.set_arena_position")).reLMS(args[0]).toString());
                        break;
                    case "save":
                        if (lms.saveLMS()) {
                            player.sendMessage(prefix + new StrManager(msg.getString("lms.save_success")).reLMS(args[0]).toString());
                        } else {
                            player.sendMessage(prefix + new StrManager(msg.getString("lms.save_failed")).reLMS(args[0]).toString());
                        }
                        break;
                    case "info":
                        player.sendMessage(lms.toString());
                        break;
                    default:
                        player.sendMessage(prefix + new StrManager(msg.getString("lms.usage")).reLMS(args[0]).toString());
                        break;
                }
            } else {
                player.sendMessage(prefix + msg.getString("lms.usage"));
            }
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (LMS lms : FactionEvent.getInstance().getListLMS()) {
                if (lms.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(lms.getName());
                }
            }
        } else if (args.length == 2) {
            List<String> stringList = Arrays.asList("create", "start", "stop", "pos", "save", "info");
            for (String str : stringList) {
                if (str.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(str);
                }
            }
        }
        return list;
    }
}
