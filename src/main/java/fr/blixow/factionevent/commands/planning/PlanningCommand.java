package fr.blixow.factionevent.commands.planning;

import fr.blixow.factionevent.enumeration.DayEnum;
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
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PlanningCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est uniquement disponible pour les joueurs en ligne.");
            return true;
        }

        Player player = (Player) sender;

        Inventory inv = Bukkit.createInventory(null, 9, "§cPlanning des event faction");
        int i = 1;
        for (DayEnum day : DayEnum.values()) {
            String value = day.getValeur();
            List<String> dailyEvents = PlanningManager.getWeeklyEvents(value);
            ItemStack item = GuiItem.addItem(Material.PAPER, "§6" + value, 0, dailyEvents.toArray(new String[dailyEvents.size()]));
            inv.setItem(i, item);
            i++;
        }
        player.openInventory(inv);
        return true;
    }
}
