package fr.blixow.factionevent.commands.dtc;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DTCCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            if(!player.hasPermission("factionevent.admin.dtc")){ player.sendMessage(FileManager.getMessageFileConfiguration().getString("no-permissions")); return true; }
            if(args.length == 2){
                DTC dtc = DTCManager.getDTCbyName(args[0]);
                FileConfiguration msg = FileManager.getMessageFileConfiguration();
                String dtc_prefix = msg.getString("dtc.prefix");
                String message = "";
                if(!args[1].equalsIgnoreCase("create") && dtc == null){
                    message = dtc_prefix + new StrManager(msg.getString("dtc.doesnt_exist")).reDTC(args[0]).toString();
                    player.sendMessage(message);
                    return true;
                }
                switch(args[1]){
                    case "create":
                        if(dtc == null){
                            DTC dtc_creation = new DTC(args[0], player.getLocation());
                            FactionEvent.getInstance().getListDTC().add(dtc_creation);
                            message = dtc_prefix + new StrManager(msg.getString("dtc.created")).reDTC(dtc_creation.getName()).toString();
                            player.sendMessage(message);
                            dtc_creation.saveDTC();
                            break;
                        }
                        message = msg.getString("dtc.prefix") + new StrManager(msg.getString("dtc.already_exist")).reDTC(dtc.getName()).toString();
                        player.sendMessage(message);
                        break;
                    case "info":
                        player.sendMessage(dtc.toString());
                        break;
                    case "pos":
                        dtc.setLocation(player.getLocation());
                        message = dtc_prefix + new StrManager(msg.getString("dtc.pos_updated")).reDTC(dtc.getName()).toString();
                        player.sendMessage(message);
                        dtc.saveDTC();
                        break;
                    case "start":
                        dtc.start(player);
                        break;
                    case "stop":
                        dtc.stop(player);
                        break;
                }
            } else {
                player.sendMessage(FileManager.getMessageFileConfiguration().getString("dtc.usage"));
            }
            return true;
        }
        sender.sendMessage("Vous devez être un joueur");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if(args.length == 1){
            for(String str : DTCManager.getDTCNames()){ if(str.toLowerCase().startsWith(args[0].toLowerCase())){ stringList.add(str); } }
        } else if(args.length == 2){
            List<String> actions = Arrays.asList("create", "info", "pos", "start", "stop");
            for(String act : actions){
                if(act.toLowerCase().startsWith(args[1].toLowerCase())){ stringList.add(act); }
            }
        } else {
            for(Player player : Bukkit.getOnlinePlayers()){
                if(player.getName().toLowerCase().startsWith(args[args.length - 1].toLowerCase())){ stringList.add(player.getName()); }
            }
        }
        return stringList;
    }
}
