package fr.blixow.factionevent.commands.relic;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.relic.RelicRewardManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * /relicreward — récupère les items en attente gagnés pendant la Course à la Relique.
 * Si l'inventaire est plein, n'en récupère qu'autant que possible et conserve le reste.
 */
public class RelicRewardCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }
        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("relic.prefix", "§8[§dRELIQUE§8]§7 ");

        if (!RelicRewardManager.hasRewards(player.getUniqueId())) {
            player.sendMessage(prefix + msg.getString("relic.no_rewards",
                "§cVous n'avez aucune récompense de Relique en attente."));
            return true;
        }

        int free = countFreeSlots(player);
        if (free <= 0) {
            player.sendMessage(prefix + msg.getString("relic.inventory_full",
                "§cVotre inventaire est plein. Libérez de la place puis réessayez."));
            return true;
        }

        int totalPending = RelicRewardManager.getRewards(player.getUniqueId()).size();
        List<ItemStack> claimed = RelicRewardManager.claimUpTo(player.getUniqueId(), free);
        for (ItemStack item : claimed) {
            player.getInventory().addItem(item.clone());
        }
        player.updateInventory();

        int remaining = totalPending - claimed.size();
        if (remaining > 0) {
            player.sendMessage(prefix + msg.getString("relic.rewards_partial",
                "§aRécupéré §e{got} §aitem(s). §7Reste §e{left} §7en attente §8(§7inventaire plein§8).")
                .replace("{got}", String.valueOf(claimed.size()))
                .replace("{left}", String.valueOf(remaining)));
        } else {
            player.sendMessage(prefix + msg.getString("relic.rewards_claimed",
                "§aVous avez récupéré §e{count} §arécompense(s) !")
                .replace("{count}", String.valueOf(claimed.size())));
        }
        return true;
    }

    private int countFreeSlots(Player player) {
        int free = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item == null || item.getType() == Material.AIR) free++;
        }
        return free;
    }
}
