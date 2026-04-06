package fr.blixow.factionevent.events;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.fallingchest.FallingChestManager;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public class FallingChestListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!FallingChestManager.isActive()) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Chest)) return;

        Location chestLoc = FallingChestManager.getChestLocation();
        if (chestLoc == null) return;

        Chest chest = (Chest) holder;
        if (chest.getLocation().getBlock().equals(chestLoc.getBlock())) {
            FallingChestManager.onOpen((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!FallingChestManager.isActive()) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Chest)) return;

        Location chestLoc = FallingChestManager.getChestLocation();
        if (chestLoc == null) return;

        Chest chest = (Chest) holder;
        if (chest.getLocation().getBlock().equals(chestLoc.getBlock())) {
            FallingChestManager.onClose((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!FallingChestManager.isActive()) return;
        Location chestLoc = FallingChestManager.getChestLocation();
        if (chestLoc == null) return;

        if (event.getBlock().getLocation().getBlock().equals(chestLoc.getBlock())) {
            event.setCancelled(true);
            String prefix = FileManager.getMessageFileConfiguration().getString("falling_chest.prefix", "§8[§6Coffre§8] §7");
            String noBreak = FileManager.getMessageFileConfiguration().getString("falling_chest.no_break", "§cVous ne pouvez pas casser ce coffre.");
            event.getPlayer().sendMessage(prefix + noBreak);
        }
    }
}
