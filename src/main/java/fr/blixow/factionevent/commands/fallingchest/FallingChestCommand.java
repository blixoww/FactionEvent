package fr.blixow.factionevent.commands.fallingchest;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.fallingchest.FallingChestManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class FallingChestCommand implements TabExecutor {

    private static final String PERM = "factionevent.admin.fallingchest";

    private String prefix() {
        return FileManager.getMessageFileConfiguration().getString("falling_chest.prefix", "§8[§6Coffre§8] §7");
    }

    private String msg(String key) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String val = msg.getString("falling_chest." + key);
        return val != null ? val : "§c[MSG MANQUANT: falling_chest." + key + "]";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        // /fallingchest next — accessible à tous
        if (action.equals("next")) {
            if (FallingChestManager.isActive()) {
                Location loc = FallingChestManager.getChestLocation();
                String activeMsg = new StrManager(msg("next_active"))
                        .reCustom("{x}", String.valueOf(loc.getBlockX()))
                        .reCustom("{y}", String.valueOf(loc.getBlockY()))
                        .reCustom("{z}", String.valueOf(loc.getBlockZ()))
                        .toString();
                sender.sendMessage(prefix() + activeMsg);
                return true;
            }
            if (FallingChestManager.isSpawning()) {
                sender.sendMessage(prefix() + msg("spawning"));
                return true;
            }
            long nextTs = FallingChestManager.getNextSpawnTimestampMs();
            if (nextTs > 0) {
                long remaining = nextTs - System.currentTimeMillis();
                if (remaining > 0) {
                    long mins = remaining / 60000;
                    long secs = (remaining % 60000) / 1000;
                    String nextMsg = new StrManager(msg("next_in"))
                            .reCustom("{minutes}", String.valueOf(mins))
                            .reCustom("{seconds}", String.valueOf(secs))
                            .toString();
                    sender.sendMessage(prefix() + nextMsg);
                } else {
                    sender.sendMessage(prefix() + msg("next_soon"));
                }
            } else {
                sender.sendMessage(prefix() + msg("next_none"));
            }
            return true;
        }

        // Toutes les autres actions requièrent la permission admin
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(FileManager.getMessageFileConfiguration().getString("prefix", "§8[§cFactionEvent§8]§7 ")
                    + FileManager.getMessageFileConfiguration().getString("no-permissions", "§cVous n'avez pas la permission d'effectuer cette commande."));
            return true;
        }

        switch (action) {
            case "spawn":
            case "force":
                if (FallingChestManager.isActive()) {
                    sender.sendMessage(prefix() + msg("already_active"));
                } else if (FallingChestManager.isSpawning()) {
                    sender.sendMessage(prefix() + msg("already_spawning"));
                } else {
                    sender.sendMessage(prefix() + msg("spawn_forced"));
                    FallingChestManager.spawn();
                }
                break;

            case "stop":
            case "remove":
                if (!FallingChestManager.isActive() && !FallingChestManager.isSpawning()) {
                    sender.sendMessage(prefix() + msg("not_active"));
                } else {
                    FallingChestManager.forceRemove();
                    sender.sendMessage(prefix() + msg("removed"));
                }
                break;

            case "status":
            case "info":
                if (FallingChestManager.isActive()) {
                    Location loc = FallingChestManager.getChestLocation();
                    String statusMsg = new StrManager(msg("status_active"))
                            .reCustom("{x}", String.valueOf(loc.getBlockX()))
                            .reCustom("{y}", String.valueOf(loc.getBlockY()))
                            .reCustom("{z}", String.valueOf(loc.getBlockZ()))
                            .reCustom("{world}", loc.getWorld().getName())
                            .toString();
                    sender.sendMessage(prefix() + statusMsg);
                } else if (FallingChestManager.isSpawning()) {
                    sender.sendMessage(prefix() + msg("status_spawning"));
                } else {
                    long nextTs = FallingChestManager.getNextSpawnTimestampMs();
                    if (nextTs > 0) {
                        long remaining = Math.max(0, nextTs - System.currentTimeMillis());
                        long mins = remaining / 60000;
                        long secs = (remaining % 60000) / 1000;
                        String statusMsg = new StrManager(msg("status_inactive"))
                                .reCustom("{minutes}", String.valueOf(mins))
                                .reCustom("{seconds}", String.valueOf(secs))
                                .toString();
                        sender.sendMessage(prefix() + statusMsg);
                    } else {
                        sender.sendMessage(prefix() + msg("status_none"));
                    }
                }
                break;

            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> actions = sender.hasPermission(PERM)
                ? Arrays.asList("next", "spawn", "stop", "status")
                : Arrays.asList("next");
            for (String a : actions) {
                if (a.startsWith(args[0].toLowerCase())) completions.add(a);
            }
        }
        return completions;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(prefix() + msg("usage"));
    }
}
