package fr.blixow.factionevent.commands.lms;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSManager;
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
            Player player = (Player) sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String prefix = msg.getString("lms.prefix");
            if (args.length == 2) {
                LMS lms = LMSManager.getLMS(args[0]);
                String message = prefix;
                if (!args[1].equals("create") && lms == null) {
                    message += new StrManager(msg.getString("lms.doesnt_exist")).reLMS(args[0]).toString();
                    player.sendMessage(message);
                    return true;
                }
                List<String> argsList = Arrays.asList("create", "start", "stop", "pos", "save", "info");
                for (String list : argsList) {
                    if (args[1].equals(list)) {
                        if (!player.hasPermission("factionevent.admin.lms")) {
                            player.sendMessage(prefix + new StrManager(msg.getString("lms.no_permission")));
                            return true;
                        }
                        break;
                    }
                }
                switch (args[1]) {
                    case "register":
                        lms.registerPlayer(player);
                        break;
                    case "unregister":
                        lms.unregisterPlayer(player);
                        break;
                    case "create":
                        if (LMSManager.getLMS(args[0]) != null) {
                            player.sendMessage(prefix + new StrManager(msg.getString("lms.doesnt_exist")).reLMS(args[0]));
                            break;
                        }
                        FactionEvent.getInstance().getListLMS().add(new LMS(args[0], player.getLocation()));
                        player.sendMessage(prefix + new StrManager(msg.getString("lms.created")).reKoth(args[0]).toString());
                        break;
                    case "start":
                        lms.start();
                        break;
                    case "stop":
                        lms.stop();
                        break;
                    case "pos":
                        lms.setArenaLocation(player.getLocation());
                        player.sendMessage(prefix + new StrManager(msg.getString("lms.set_arena_position")).reKoth(args[0]).toString());
                        break;
                    case "save":
                        if (lms.saveLMS()) {
                            player.sendMessage(prefix + new StrManager(msg.getString("lms.save_success")).reLMS(args[0].toString()));
                        } else {
                            player.sendMessage(prefix + msg.getString("lms.save_failed"));
                        }
                        break;
                    case "info":
                        player.sendMessage(lms.toString());
                        break;
                    default:
                        player.sendMessage(prefix + new StrManager(msg.getString("lms.usage")).reLMS(args[0]));
                        break;
                }
            } else {
                player.sendMessage(prefix + msg.getString("lms.usage"));
            }
            return true;
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
