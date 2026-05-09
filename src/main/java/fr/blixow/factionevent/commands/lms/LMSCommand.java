package fr.blixow.factionevent.commands.lms;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSManager;
import fr.blixow.factionevent.utils.lms.LMSMode;
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
        if (!(sender instanceof Player)) return true;
        String prefix = FileManager.getMessageFileConfiguration().getString("lms.prefix");
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        Player player = (Player) sender;

        if (!player.hasPermission("factionevent.admin.lms")) {
            player.sendMessage(prefix + msg.getString("no-permissions"));
            return true;
        }

        // /lms <nom> <action> [mode]
        if (args.length >= 2) {
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
                    // /lms <nom> start [solo|duo]
                    LMSMode mode = LMSMode.SOLO;
                    if (args.length >= 3) {
                        mode = LMSMode.fromString(args[2]);
                    }
                    lms.startRegistration(mode, player);
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
                    sendUsage(player, prefix, msg);
                    break;
            }
        } else {
            sendUsage(player, prefix, msg);
        }
        return true;
    }

    private void sendUsage(Player player, String prefix, FileConfiguration msg) {
        player.sendMessage(prefix + msg.getString("lms.usage",
            "§cUsage : §7/lms <nom> <action> [solo|duo] §8| §7Actions : §fcreate/start/stop/pos/save/info"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            for (LMS lms : FactionEvent.getInstance().getListLMS()) {
                if (lms.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(lms.getName());
            }
        } else if (args.length == 2) {
            for (String s : Arrays.asList("create", "start", "stop", "pos", "save", "info")) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) list.add(s);
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("start")) {
            for (String s : Arrays.asList("solo", "duo")) {
                if (s.startsWith(args[2].toLowerCase())) list.add(s);
            }
        }
        return list;
    }
}
