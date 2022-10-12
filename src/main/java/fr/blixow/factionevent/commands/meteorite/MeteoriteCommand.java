package fr.blixow.factionevent.commands.meteorite;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.meteorite.MeteoriteManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MeteoriteCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String message = msg.getString("meteorite.prefix");
            Player player = (Player)sender;
            if(!player.hasPermission("factionevent.admin.meteorite")){
                message += msg.getString("no-permissions");
                player.sendMessage(message);
                return true;
            }
            if(args.length != 2){
                message += msg.getString("meteorite.usage");
                player.sendMessage(message);
                return true;
            }
            Meteorite meteorite = MeteoriteManager.getMeteoriteByName(args[0]);
            if(!args[1].equalsIgnoreCase("create") && meteorite == null){
                message += new StrManager(msg.getString("meteorite.doesnt_exist")).reMeteorite(args[0]).toString();
                player.sendMessage(message);
                return true;
            }
            switch (args[1]) {
                case "create":
                    if (meteorite == null) {
                        Meteorite meteorite_creation = new Meteorite(args[0], player.getLocation());
                        MeteoriteManager.registerNewMeteorite(meteorite_creation);
                        message += new StrManager(msg.getString("meteorite.created")).reMeteorite(meteorite_creation.getName()).toString();
                        meteorite_creation.save();
                    } else { message += new StrManager(msg.getString("meteorite.already_exist")).reMeteorite(meteorite.getName()).toString(); }
                    player.sendMessage(message);
                    break;
                case "pos":
                    meteorite.setLocation(player.getLocation());
                    meteorite.save();
                    message += new StrManager(msg.getString("meteorite.pos_updated")).reMeteorite(meteorite.getName()).toString();
                    player.sendMessage(message);
                    break;
                case "start":
                    meteorite.start(player);
                    break;
                case "stop":
                    meteorite.stop(player);
                    break;
                case "info":
                    player.sendMessage(meteorite.toString());
                    break;
                default:
                    player.sendMessage(msg.getString("meteorite.usage"));
                    break;
            }
            return true;
        }
        sender.sendMessage("Vous devez être un joueur !");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if(args.length == 1){
            for(String str : MeteoriteManager.getMeteoriteListNames()){
                if(str.toLowerCase().startsWith(args[0].toLowerCase())){ stringList.add(str); } }
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
