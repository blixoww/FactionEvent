package fr.blixow.factionevent.commands.planning;

import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.PlanningManager;
import fr.blixow.factionevent.utils.GuiItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class PlanningCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 0) {
                Inventory inv = Bukkit.createInventory(null, 9, "§cPlanning des event faction");
                int i = 1;
                for (String jour : DateManager.listJour) {
                    String[] eventArray;
                    ArrayList<String> dailyEvents = PlanningManager.getWeeklyEvents(jour);
                    StringBuilder eventString = new StringBuilder();
                    dailyEvents.forEach(array -> eventString.append(array).append("\n"));
                    eventArray = eventString.toString().split("\n");
                    inv.setItem(i, GuiItem.addItem(Material.PAPER, "§6" + jour, 0, eventArray));
                    i++;
                }
                player.openInventory(inv);
            }
        }
        return false;
    }
}
