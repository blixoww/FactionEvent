package fr.blixow.factionevent.commands.classement;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.RankingManager;
import fr.blixow.factionevent.manager.StrManager;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ClassementCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            Player player = (Player)sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
            Faction faction = fPlayer.getFaction();
            if(args.length == 0){
                if(msg.contains("classement.title") && msg.contains("classement.lines")){
                    String title = msg.getString("classement.title");
                    List<String> stringList = msg.getStringList("classement.lines");
                    player.sendMessage(title);
                    player.sendMessage("");
                    int nb_lines = stringList.size();
                    int indice = 1;
                    for(Faction faction1 : FactionEvent.getInstance().getFactionRankings().keySet()){
                        int points = FactionEvent.getInstance().getFactionRankings().get(faction1);
                        String line = new StrManager(stringList.get(indice -1)).reFaction(faction1.getTag()).rePoints(points).toString();
                        TextComponent text = new TextComponent(line);
                        text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clique pour afficher plus d'infos\n§7sur la faction §c" + faction1.getTag()).create()));
                        text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/classement " + faction1.getTag()));
                        player.spigot().sendMessage(text);
                        //player.sendMessage(line);
                        if(indice == nb_lines){
                            break;
                        }
                        indice++;
                    }
                } else {
                    int indice = 1;
                    for(Faction faction1 : FactionEvent.getInstance().getFactionRankings().keySet()){
                        int points = FactionEvent.getInstance().getFactionRankings().get(faction1);
                        switch(indice){
                            case 1:
                                player.sendMessage("§c" + indice + ") §7" + faction1.getTag() + " §8- §7" + points);
                                break;
                            case 2:
                                player.sendMessage("§6" + indice + ") §7" + faction1.getTag() + " §8- §7" + points);
                                break;
                            case 3:
                                player.sendMessage("§e" + indice + ") §7" + faction1.getTag() + " §8- §7" + points);
                                break;
                            default:
                                player.sendMessage("§8" + indice + ") §7" + faction1.getTag() + " §8- §7" + points);
                                break;
                        }

                        indice++;
                        if(indice == 10){ break; }
                    }
                }
            } else if(args.length == 1){
                if(args[0].startsWith("-")){
                    if(player.hasPermission("factionevent.admin.classement")){
                        switch (args[0]){
                            case "-update":
                            case "-reload":
                            case "-refresh":
                                RankingManager.updateRanking(true);
                                break;
                            default:
                                player.sendMessage("§cCommande: §7/classement [faction]");
                                break;
                        }
                    } else {
                        player.sendMessage("§7La faction §c" + args[0] + " §7n'existe pas");
                    }
                    return true;
                }
                FileConfiguration fc = FileManager.getClassementFC();
                Faction factions = Factions.getInstance().getByTag(args[0]);
                if(factions == null){
                    player.sendMessage("§7La faction §c" + args[0] + " §7n'existe pas");
                } else {
                    int points = 0, koth = 0, totem = 0, dtc = 0, lms = 0;
                    try {
                        String[] factionInformations = RankingManager.getFactionsInformations(fc, factions.getId()).split("-");
                        points = Integer.parseInt(factionInformations[0]);
                        koth = Integer.parseInt(factionInformations[1]);
                        totem = Integer.parseInt(factionInformations[2]);
                        dtc = Integer.parseInt(factionInformations[3]);
                        lms = Integer.parseInt(factionInformations[4]);
                        if(msg.contains("faction_classement.title") && msg.contains("faction_classement.footer") && msg.contains("faction_classement.lines")){
                            String title = new StrManager(msg.getString("faction_classement.title")).reFaction(factions.getTag()).toString();
                            String footer = new StrManager(msg.getString("faction_classement.footer")).reFaction(factions.getTag()).toString();
                            List<String> lines = msg.getStringList("faction_classement.lines");
                            player.sendMessage(title);
                            player.sendMessage("");
                            for(String  line : lines){
                                String line_custom = new StrManager(line)
                                        .rePoints(points)
                                        .reCustom("\\{nb_koth}", String.valueOf(koth))
                                        .reCustom("\\{nb_totem}", String.valueOf(totem))
                                        .reCustom("\\{nb_dtc}", String.valueOf(dtc))
                                        .reCustom("\\{nb_lms}", String.valueOf(lms))
                                        .toString();
                                player.sendMessage(line_custom);
                            }
                            player.sendMessage("");
                            player.sendMessage(footer);
                        } else {
                            player.sendMessage("§8§m-----§r§8[§e" + factions.getTag() + "§8]§m-----");
                            player.sendMessage("");
                            player.sendMessage("§8» §cPoints : §7" + points);
                            player.sendMessage("§8» §cKoth gagnés : §7" + koth);
                            player.sendMessage("§8» §cTotem gagnés : §7" + totem);
                            player.sendMessage("§8» §cDTC gagnés : §7" + dtc);
                            player.sendMessage("§8» §cLMS gagnés : §7" + lms);
                            player.sendMessage("");
                            player.sendMessage("§8§m-----§r§8[§e" + factions.getTag() + "§8]§m-----");
                        }
                    } catch (Exception exception){
                        exception.printStackTrace();
                    }


                }
            } else {
                player.sendMessage("§cCommande: §7/classement [faction]");
            }
            return true;
        }
        sender.sendMessage("Vous devez être un joueur pour éxécuter cette commande.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if(args.length == 1){
            for(Faction faction : Factions.getInstance().getAllFactions()){
                if(faction.getTag().toLowerCase().startsWith(args[0].toLowerCase()) && !faction.isWarZone() && !faction.isSafeZone() && !faction.isWilderness()){ stringList.add(faction.getTag()); }
            }
        }
        //if(stringList.isEmpty()){ for(Player player : Bukkit.getOnlinePlayers()){ stringList.add(player.getName()); } }
        return stringList;
    }
}
