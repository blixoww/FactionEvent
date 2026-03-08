package fr.blixow.factionevent.commands.planning;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSManager;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlanningRemoveCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("prefix");
        String pPrefix = msg.getString("planning.prefix");

        if (!player.hasPermission("factionevent.admin.planningremove")) {
            player.sendMessage(prefix + msg.getString("no-permissions"));
            return true;
        }
        if (args.length != 4) {
            player.sendMessage(msg.getString("planning.usage"));
            return true;
        }

        String[] splitDate = args[3].split("-");
        if (splitDate.length != 2) {
            player.sendMessage(msg.getString("planning.time_syntaxe"));
            return true;
        }
        ArrayList<Integer> tabEntier = DateManager.getIntegerList(splitDate);
        if (tabEntier.isEmpty()) {
            player.sendMessage(msg.getString("planning.time_syntaxe"));
            return true;
        }

        int heure = tabEntier.get(0);
        int minutes = tabEntier.get(1);
        String timeStr = heure + "h" + minutes;
        String valeurJour = args[2];
        FileConfiguration fc = FileManager.getPlanningDataFC();

        switch (args[0].toLowerCase()) {
            case "koth": {
                KOTH koth = KOTHManager.getKOTH(args[1]);
                if (koth == null) {
                    player.sendMessage(prefix + new StrManager(msg.getString("koth.doesnt_exist")).reKoth(args[1]).toString());
                    return true;
                }
                String path = valeurJour + ".koth." + koth.getName();
                if (!fc.contains(path)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet événement n'existe pas dans le planning.")).reType("koth").reKoth(koth.getName()).reTime(valeurJour).toString());
                    return true;
                }
                List<String> list = new ArrayList<>(fc.getStringList(path));
                if (!list.remove(timeStr)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet horaire n'existe pas.")).reType("koth").reKoth(koth.getName()).reTime(valeurJour + " " + timeStr).toString());
                    return true;
                }
                try {
                    fc.set(path, list);
                    fc.save(FileManager.getDataFile("planning.yml"));
                    FactionEvent.getInstance().reloadPlanning();
                    player.sendMessage(pPrefix + new StrManager(msg.getString("planning.remove", "§7Supprimé.")).reType("koth").reKoth(koth.getName()).reTime(valeurJour + " " + timeStr).toString());
                } catch (Exception e) { e.printStackTrace(); }
                break;
            }
            case "totem": {
                Totem totem = TotemManager.getTotem(args[1]);
                if (totem == null) {
                    player.sendMessage(prefix + new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[1]).toString());
                    return true;
                }
                String path = valeurJour + ".totem." + totem.getName();
                if (!fc.contains(path)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet événement n'existe pas.")).reType("totem").reTotem(totem.getName()).reTime(valeurJour).toString());
                    return true;
                }
                List<String> list = new ArrayList<>(fc.getStringList(path));
                if (!list.remove(timeStr)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet horaire n'existe pas.")).reType("totem").reTotem(totem.getName()).reTime(valeurJour + " " + timeStr).toString());
                    return true;
                }
                try {
                    fc.set(path, list);
                    fc.save(FileManager.getDataFile("planning.yml"));
                    FactionEvent.getInstance().reloadPlanning();
                    player.sendMessage(pPrefix + new StrManager(msg.getString("planning.remove", "§7Supprimé.")).reType("totem").reTotem(totem.getName()).reTime(valeurJour + " " + timeStr).toString());
                } catch (Exception e) { e.printStackTrace(); }
                break;
            }
            case "dtc": {
                DTC dtc = DTCManager.getDTCbyName(args[1]);
                if (dtc == null) {
                    player.sendMessage(prefix + new StrManager(msg.getString("dtc.doesnt_exist")).reDTC(args[1]).toString());
                    return true;
                }
                String path = valeurJour + ".dtc." + dtc.getName();
                if (!fc.contains(path)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet événement n'existe pas.")).reType("dtc").reDTC(dtc.getName()).reTime(valeurJour).toString());
                    return true;
                }
                List<String> list = new ArrayList<>(fc.getStringList(path));
                if (!list.remove(timeStr)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet horaire n'existe pas.")).reType("dtc").reDTC(dtc.getName()).reTime(valeurJour + " " + timeStr).toString());
                    return true;
                }
                try {
                    fc.set(path, list);
                    fc.save(FileManager.getDataFile("planning.yml"));
                    FactionEvent.getInstance().reloadPlanning();
                    player.sendMessage(pPrefix + new StrManager(msg.getString("planning.remove", "§7Supprimé.")).reType("dtc").reDTC(dtc.getName()).reTime(valeurJour + " " + timeStr).toString());
                } catch (Exception e) { e.printStackTrace(); }
                break;
            }
            case "lms": {
                LMS lms = LMSManager.getLMS(args[1]);
                if (lms == null) {
                    player.sendMessage(prefix + new StrManager(msg.getString("lms.doesnt_exist", "§cLe LMS §e{lms} §cn'existe pas.")).reLMS(args[1]).toString());
                    return true;
                }
                String path = valeurJour + ".lms." + lms.getName();
                if (!fc.contains(path)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet événement n'existe pas.")).reType("lms").reLMS(lms.getName()).reTime(valeurJour).toString());
                    return true;
                }
                List<String> list = new ArrayList<>(fc.getStringList(path));
                if (!list.remove(timeStr)) {
                    player.sendMessage(prefix + new StrManager(msg.getString("planning.not_exist", "§cCet horaire n'existe pas.")).reType("lms").reLMS(lms.getName()).reTime(valeurJour + " " + timeStr).toString());
                    return true;
                }
                try {
                    fc.set(path, list);
                    fc.save(FileManager.getDataFile("planning.yml"));
                    FactionEvent.getInstance().reloadPlanning();
                    player.sendMessage(pPrefix + new StrManager(msg.getString("planning.remove", "§7Supprimé.")).reType("lms").reLMS(lms.getName()).reTime(valeurJour + " " + timeStr).toString());
                } catch (Exception e) { e.printStackTrace(); }
                break;
            }
            default:
                player.sendMessage(pPrefix + "§cType invalide. Utilisez: koth/totem/dtc/lms");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        List<String> customs = new ArrayList<>();
        if (args.length == 1) {
            customs = new ArrayList<>(Arrays.asList("koth", "totem", "dtc", "lms"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("koth")) customs = KOTHManager.getListKothNames();
            else if (args[0].equalsIgnoreCase("totem")) customs = TotemManager.getListTotemNames();
            else if (args[0].equalsIgnoreCase("dtc")) customs = DTCManager.getDTCNames();
            else if (args[0].equalsIgnoreCase("lms")) customs = LMSManager.getListLMSNames();
        } else if (args.length == 3) {
            customs = new ArrayList<>(Arrays.asList("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"));
        } else if (args.length == 4) {
            LocalDateTime now = LocalDateTime.now();
            int minutes = now.getMinute();
            now = minutes < 30 ? now.plusMinutes(30 - minutes) : now.plusMinutes(60 - minutes);
            String hours = now.getHour() < 10 ? "0" + now.getHour() : String.valueOf(now.getHour());
            String minute = now.getMinute() < 10 ? "0" + now.getMinute() : String.valueOf(now.getMinute());
            stringList.add(hours + "-" + minute);
        }
        for (String str : customs) {
            if (str.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) stringList.add(str);
        }
        return stringList;
    }
}
