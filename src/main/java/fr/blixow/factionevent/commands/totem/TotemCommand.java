package fr.blixow.factionevent.commands.totem;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemEditor;
import fr.blixow.factionevent.utils.totem.TotemManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TotemCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String tPrefix = msg.getString("totem.prefix");
            if (!player.hasPermission("factionevent.admin.totem")) {
                player.sendMessage(msg.getString("no-permissions"));
                return true;
            }
            if (args.length == 2) {
                String str;
                Totem totem = TotemManager.getTotem(args[0]);
                switch (args[1]) {
                    case "create":
                        if (totem != null) {
                            str = new StrManager(msg.getString("totem.already_exist")).reTotem(args[0]).toString();
                            player.sendMessage(tPrefix + str);
                            break;
                        }
                        Totem creation = new Totem(args[0], player.getLocation(), new HashMap<>());
                        if (TotemManager.addTotem(creation)) {
                            str = tPrefix + new StrManager(msg.getString("totem.created")).reTotem(creation.getName()).toString();
                            player.sendMessage(str);
                        }
                        creation.saveTotem();
                        break;
                    case "edit":
                        if (totem == null) {
                            str = new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[0]).toString();
                            player.sendMessage(tPrefix + str);
                            break;
                        }
                        if (TotemEditor.isAlreadyEdited(player, totem)) {
                            break;
                        }
                        TotemEditor totemEditor = new TotemEditor(totem);
                        player.sendMessage(tPrefix + new StrManager(msg.getString("totem.editing")).reTotem(totem.getName()).toString());
                        FactionEvent.getInstance().getPlayerTotemEditorHashMap().put(player, totemEditor);
                        break;
                    case "info":
                        if (totem == null) {
                            str = new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[0]).toString();
                            player.sendMessage(tPrefix + str);
                            break;
                        }
                        player.sendMessage(totem.toString());
                        break;
                    case "start":
                        if (totem == null) {
                            str = new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[0]).toString();
                            player.sendMessage(tPrefix + str);
                            break;
                        }
                        totem.start(player);
                        break;
                    case "stop":
                        if (totem == null) {
                            str = new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[0]).toString();
                            player.sendMessage(tPrefix + str);
                            break;
                        }
                        if (totem.stop()) {
                            str = tPrefix + new StrManager(msg.getString("totem.canceled")).reTotem(totem.getName()).toString();
                            Bukkit.broadcastMessage(str);
                        } else {
                            str = tPrefix + new StrManager(msg.getString("totem.not_started")).reTotem(totem.getName()).toString();
                            player.sendMessage(str);
                        }
                        break;
                    case "pos":
                        if (totem == null) {
                            str = new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[0]).toString();
                            player.sendMessage(tPrefix + str);
                            break;
                        }
                        totem.setLocation(player.getLocation());
                        player.sendMessage(tPrefix + new StrManager(msg.getString("totem.pos_updated")).reTotem(totem.getName()).toString());
                        totem.saveTotem();
                        break;
                    case "save":
                        if (totem == null) {
                            str = new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[0]).toString();
                            player.sendMessage(tPrefix + str);
                            break;
                        }
                        TotemEditor totemEditor1 = TotemEditor.getTotemEditorByPlayer(player);
                        if (totemEditor1 == null) {
                            player.sendMessage(tPrefix + msg.getString("totem.not_editing"));
                            break;
                        }
                        if (!totemEditor1.getTotem().equals(totem)) {
                            player.sendMessage(tPrefix + msg.getString("totem.not_the_good_one_to_save"));
                            break;
                        }
                        if (totem.saveTotem()) {
                            player.sendMessage(tPrefix + new StrManager(msg.getString("totem.save_success")).reTotem(args[0].toString()));
                        } else {
                            player.sendMessage(tPrefix + msg.getString("totem.save_failed"));
                        }
                        totemEditor1.save();
                        break;
                }
                return true;
            }
            player.sendMessage(tPrefix + msg.getString("totem.usage"));
            return true;
        }
        sender.sendMessage("Vous devez être un joueur.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if (args.length == 1) {
            for (Totem totem : FactionEvent.getInstance().getListTotem()) {
                if (totem.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    stringList.add(totem.getName());
                }
            }
        } else if (args.length == 2) {
            List<String> actions = new ArrayList<>();
            actions.add("create");
            actions.add("edit");
            actions.add("save");
            actions.add("start");
            actions.add("info");
            actions.add("pos");
            actions.add("stop");
            for (String str : actions) {
                if (str.toLowerCase().startsWith(args[1].toLowerCase())) {
                    stringList.add(str);
                }
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[args.length - 1])) {
                    stringList.add(player.getName());
                }
            }
        }
        return stringList;
    }
}
