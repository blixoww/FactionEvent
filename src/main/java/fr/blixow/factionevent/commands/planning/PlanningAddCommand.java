package fr.blixow.factionevent.commands.planning;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
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

public class PlanningAddCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String prefix = msg.getString("prefix");
            String pPrefix = msg.getString("planning.prefix");
            if (!player.hasPermission("factionevent.admin.planningadd")) {
                player.sendMessage(prefix + msg.getString("no-permissions"));
                return true;
            }
            if (args.length == 4) {
                String[] split_date = args[3].split("-");
                if (split_date.length == 2) {
                    ArrayList<Integer> tabEntier = DateManager.getIntegerList(split_date);
                    if (!tabEntier.isEmpty()) {
                        int heure = tabEntier.get(0), minutes = tabEntier.get(1);
                        FileConfiguration fc = FileManager.getPlanningDataFC();

                        switch (args[0]) {
                            case "koth":
                                KOTH koth = KOTHManager.getKOTH(args[1]);
                                if (koth == null) {
                                    player.sendMessage(prefix + new StrManager(msg.getString("koth.doesnt_exist")).reKoth(args[1]));
                                    return true;
                                }
                                List<String> kothList = new ArrayList<>();
                                try {
                                    String valeurJour = args[2];
                                    String kothPath = valeurJour + ".koth";

                                    if (fc.contains(kothPath + "." + koth.getName())) {
                                        kothList = fc.getStringList(kothPath + "." + koth.getName());
                                    }

                                    kothList.add(heure + "h" + minutes);
                                    fc.set(kothPath + "." + koth.getName(), kothList);
                                    fc.save(FileManager.getDataFile("planning.yml"));
                                    player.sendMessage(pPrefix + new StrManager(msg.getString("koth.setup")).reKoth(koth.getName()).reTime(valeurJour + " " + heure + "h" + minutes + "m").toString());

                                    FactionEvent.getInstance().reloadPlanning();
                                    break;
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                            case "totem":
                                Totem totem = TotemManager.getTotem(args[1]);
                                if (totem == null) {
                                    player.sendMessage(prefix + new StrManager(msg.getString("totem.doesnt_exist")).reTotem(args[1]));
                                    return true;
                                }
                                List<String> totemList = new ArrayList<>();
                                try {
                                    String valeurJour = args[2];
                                    String totemPath = valeurJour + ".totem";

                                    if (fc.contains(totemPath + "." + totem.getName())) {
                                        totemList = fc.getStringList(totemPath + "." + totem.getName());
                                    }
                                    totemList.add(heure + "h" + minutes);
                                    fc.set(totemPath + "." + totem.getName(), totemList);
                                    fc.save(FileManager.getDataFile("planning.yml"));
                                    player.sendMessage(pPrefix + new StrManager(msg.getString("totem.setup")).reTotem(totem.getName()).reTime(valeurJour + " " + heure + "h" + minutes + "m").toString());

                                    FactionEvent.getInstance().reloadPlanning();
                                    break;
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                                break;
                            case "dtc":
                                DTC dtc = DTCManager.getDTCbyName(args[1]);
                                if (dtc == null) {
                                    player.sendMessage(prefix + new StrManager(msg.getString("dtc.doesnt_exist")).reDTC(args[1]));
                                    return true;
                                }
                                List<String> dtcList = new ArrayList<>();
                                try {
                                    String valeurJour = args[2];
                                    String dtcPath = valeurJour + ".dtc";

                                    if (fc.contains(dtcPath + "." + dtc.getName())) {
                                        dtcList = fc.getStringList(dtcPath + "." + dtc.getName());
                                    }

                                    dtcList.add(heure + "h" + minutes);
                                    fc.set(dtcPath + "." + dtc.getName(), dtcList);
                                    fc.save(FileManager.getDataFile("planning.yml"));
                                    player.sendMessage(pPrefix + new StrManager(msg.getString("dtc.setup")).reDTC(dtc.getName()).reTime(valeurJour + " " + heure + "h" + minutes + "m").toString());

                                    FactionEvent.getInstance().reloadPlanning();
                                    break;
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                                break;
                        }
                    } else {
                        player.sendMessage(msg.getString("planning.time_syntaxe"));
                    }
                } else {
                    player.sendMessage(msg.getString("planning.time_syntaxe"));
                }
                return true;
            } else {
                player.sendMessage(msg.getString("planning.usage"));
            }
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
            if (args[0].equalsIgnoreCase("koth")) {
                customs = KOTHManager.getListKothNames();
            } else if (args[0].equalsIgnoreCase("totem")) {
                customs = TotemManager.getListTotemNames();
            } else if (args[0].equalsIgnoreCase("dtc")) {
                customs = DTCManager.getDTCNames();
            } else if (args[0].equalsIgnoreCase("lms")) {
                customs = LMSManager.getListLMSNames();
            }
        } else if (args.length == 3) {
            customs = new ArrayList<>(Arrays.asList("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"));
        } else if (args.length == 4) {
            LocalDateTime now = LocalDateTime.now();
            int minutes = now.getMinute();
            if (minutes < 30) {
                now = now.plusMinutes(30 - minutes);
            } else {
                now = now.plusMinutes(60 - minutes);
            }
            String hours = String.valueOf(now.getHour()).length() == 1 ? "0" + now.getHour() : String.valueOf(now.getHour());
            String minute = String.valueOf(now.getMinute()).length() == 1 ? "0" + now.getMinute() : String.valueOf(now.getMinute());
            String formated = hours + "-" + minute;
            stringList.add(formated);
        }
        for (String str : customs) {
            if (str.toLowerCase().startsWith(args[args.length - 1])) {
                stringList.add(str);
            }
        }
        return stringList;
    }
}
