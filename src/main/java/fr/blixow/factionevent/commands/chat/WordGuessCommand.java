package fr.blixow.factionevent.commands.chat;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class WordGuessCommand implements TabExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length > 0) {
      switch (args[0]) {
        case "start":

          break;
        case "stop":

          break;
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

  public static class Guess implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 1) {
        // WorldGuesser::checkAnswer(sender as Player, args[0])
        return true;
      }

      return false;
    }
  }
}
