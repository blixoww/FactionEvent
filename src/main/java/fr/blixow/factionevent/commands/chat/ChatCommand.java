package fr.blixow.factionevent.commands.chat;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChatCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        if (args.length != 0) {
            switch (args[0]) {
                case "start":

            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if (args.length == 1) {
            for (Faction faction : Factions.getInstance().getAllFactions()) {
                if (faction.getTag().toLowerCase().startsWith(args[0].toLowerCase()) && !faction.isWarZone() && !faction.isSafeZone() && !faction.isWilderness()) {
                    stringList.add(faction.getTag());
                }
            }
        }
        //if(stringList.isEmpty()){ for(Player player : Bukkit.getOnlinePlayers()){ stringList.add(player.getName()); } }
        return stringList;
    }
}
