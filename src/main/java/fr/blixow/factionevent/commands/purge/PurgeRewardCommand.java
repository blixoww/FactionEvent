package fr.blixow.factionevent.commands.purge;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.purge.PurgeManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * /purgereward — récupère les items pending stockés dans PurgeManager.
 * Si l'inventaire n'a pas assez de place, n'en récupère qu'autant que possible
 * et conserve le reste pour une commande ultérieure.
 */
public class PurgeRewardCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Vous devez être un joueur.");
            return true;
        }
        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("purge.prefix", "§8[§4PURGE§8]§7 ");

        if (!PurgeManager.hasRewards(player.getUniqueId())) {
            player.sendMessage(prefix + msg.getString("purge.no_rewards",
                "§cVous n'avez aucune récompense en attente."));
            return true;
        }

        int free = countFreeSlots(player);
        if (free <= 0) {
            player.sendMessage(prefix + msg.getString("purge.inventory_full",
                "§cVotre inventaire est plein. Libérez de la place puis réessayez."));
            return true;
        }

        int totalPending = PurgeManager.getRewards(player.getUniqueId()).size();
        List<ItemStack> claimed = PurgeManager.claimUpTo(player.getUniqueId(), free);
        for (ItemStack item : claimed) {
            player.getInventory().addItem(item.clone());
        }

        int remaining = totalPending - claimed.size();
        if (remaining > 0) {
            player.sendMessage(prefix + msg.getString("purge.rewards_partial",
                "§aRécupéré §e{got} §aitem(s). §7Reste §e{left} §7en attente §8(§7inventaire plein§8).")
                .replace("{got}", String.valueOf(claimed.size()))
                .replace("{left}", String.valueOf(remaining)));
        } else {
            player.sendMessage(prefix + msg.getString("purge.rewards_claimed",
                "§aVous avez récupéré §e{count} §arécompense(s) !")
                .replace("{count}", String.valueOf(claimed.size())));
        }
        return true;
    }

    private int countFreeSlots(Player player) {
        int free = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) free++;
        }
        return free;
    }
}
