package fr.blixow.factionevent.commands.koth;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KothCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String prefix = msg.getString("koth.prefix");
            if (!player.hasPermission("factionevent.admin.koth")) {
                player.sendMessage(msg.getString("prefix") + msg.getString("no-permissions"));
                return true;
            }
            if (args.length == 2) {
                KOTH koth = KOTHManager.getKOTH(args[0]);
                String message = prefix + "";
                if (!args[1].equals("create") && koth == null) {
                    message += new StrManager(msg.getString("koth.doesnt_exist")).reKoth(args[0]).toString();
                    player.sendMessage(message);
                    return true;
                }
                switch (args[1]) {
                    case "create":
                        if (KOTHManager.getKOTH(args[0]) != null) {
                            player.sendMessage(prefix + new StrManager(msg.getString("koth.doesnt_exist")).reKoth(args[0]));
                            break;
                        }
                        FactionEvent.getInstance().getListKOTH().add(new KOTH(args[0], player.getLocation(), player.getLocation()));
                        player.sendMessage(prefix + new StrManager(msg.getString("koth.created")).reKoth(args[0]).toString());
                        break;
                    case "start":
                        koth.start(player);
                        break;
                    case "stop":
                        koth.stop(player);
                        break;
                    case "pos1":
                        koth.setPos1(player.getLocation());
                        player.sendMessage(prefix + new StrManager(msg.getString("koth.pos1_updated")).reKoth(args[0]).toString());
                        break;
                    case "pos2":
                        koth.setPos2(player.getLocation());
                        player.sendMessage(prefix + new StrManager(msg.getString("koth.pos2_updated")).reKoth(args[0]).toString());
                        break;
                    case "save":
                        if (koth.saveKOTH()) {
                            player.sendMessage(prefix + new StrManager(msg.getString("koth.save_success")).reKoth(args[0].toString()));
                        } else {
                            player.sendMessage(prefix + msg.getString("koth.save_failed"));
                        }
                        break;
                    case "info":
                        player.sendMessage(koth.toString());
                        break;
                    default:
                        player.sendMessage(prefix + new StrManager(msg.getString("koth.usage")).reKoth(args[0]));
                        break;
                }
            } else {
                player.sendMessage(prefix + msg.getString("koth.usage"));
            }
            return true;
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (KOTH koth : FactionEvent.getInstance().getListKOTH()) {
                if (koth.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(koth.getName());
                }
            }
        } else if (args.length == 2) {
            List<String> list_args_2 = Arrays.asList("create", "start", "stop", "pos1", "pos2", "info", "save");
            for (String str : list_args_2) {
                if (str.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(str);
                }
            }
        }
        return list;
    }
}
