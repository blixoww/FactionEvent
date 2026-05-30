package fr.blixow.factionevent.commands.lms;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.lms.LMSRewardManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class LMSRewardCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }
        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("lms.prefix", "§8[§cLMS§8]§7 ");

        if (!LMSRewardManager.hasRewards(player.getUniqueId())) {
            player.sendMessage(prefix + msg.getString("lms.no_rewards",
                "§cVous n'avez aucune récompense LMS en attente."));
            return true;
        }

        // Compter les emplacements libres
        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) freeSlots++;
        }

        List<ItemStack> rewards = LMSRewardManager.getRewards(player.getUniqueId());
        int total = rewards.size();

        if (freeSlots == 0) {
            player.sendMessage(prefix + msg.getString("lms.inventory_full",
                "§cVotre inventaire est plein. Libérez de la place puis réessayez."));
            return true;
        }

        List<ItemStack> claimed = LMSRewardManager.claimUpTo(player.getUniqueId(), freeSlots);
        for (ItemStack item : claimed) {
            player.getInventory().addItem(item);
        }
        player.updateInventory();

        int left = total - claimed.size();
        if (left > 0) {
            player.sendMessage(prefix + msg.getString("lms.rewards_partial",
                "§aRécupéré §e{got} §aitem(s). §7Reste §e{left} §7en attente §8(§7inventaire plein§8).")
                .replace("{got}", String.valueOf(claimed.size()))
                .replace("{left}", String.valueOf(left)));
        } else {
            player.sendMessage(prefix + msg.getString("lms.rewards_claimed",
                "§aVous avez récupéré §e{count} §arécompense(s) LMS !")
                .replace("{count}", String.valueOf(claimed.size())));
        }
        return true;
    }
}

