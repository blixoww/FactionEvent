package fr.blixow.factionevent.commands.domination;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.domination.Domination;
import fr.blixow.factionevent.utils.domination.DominationManager;
import fr.blixow.factionevent.utils.domination.DominationZone;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DominationCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("domination.prefix", "§8[§cDOMINATION§8]§7 ");

        if (!player.hasPermission("factionevent.admin.domination")) {
            player.sendMessage(msg.getString("prefix") + msg.getString("no-permissions"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, prefix, msg);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── /domination start ──────────────────────────────────────────
            case "start": {
                if (DominationManager.isDominationStarted()) {
                    player.sendMessage(prefix + msg.getString("domination.already_started",
                        "§cUne Domination est déjà en cours."));
                    return true;
                }
                List<DominationZone> enabled = DominationManager.getEnabledZones();
                if (enabled.isEmpty()) {
                    player.sendMessage(prefix + msg.getString("domination.no_zones",
                        "§cAucune zone activée. Activez au moins une zone avec /domination enable <zone>."));
                    return true;
                }
                new Domination(enabled).start(player);
                break;
            }

            // ── /domination stop ──────────────────────────────────────────
            case "stop": {
                new Domination(DominationManager.getEnabledZones()).stop(player);
                break;
            }

            // ── /domination list ──────────────────────────────────────────
            case "list": {
                List<DominationZone> zones = FactionEvent.getInstance().getListDominationZones();
                if (zones.isEmpty()) {
                    player.sendMessage(prefix + msg.getString("domination.empty_list",
                        "§7Aucune zone créée."));
                    return true;
                }
                player.sendMessage("§8§m      §r §8[ §cZONES DOMINATION §8] §m      ");
                for (DominationZone z : zones) {
                    String status = z.isEnabled() ? "§aActivée" : "§cDésactivée";
                    player.sendMessage("  §8» §7" + z.getName() + " §8[" + status + "§8]");
                }
                break;
            }

            // ── /domination save ──────────────────────────────────────────
            case "save": {
                boolean ok = true;
                for (DominationZone z : FactionEvent.getInstance().getListDominationZones()) {
                    ok &= DominationManager.saveZone(z);
                }
                if (ok) {
                    player.sendMessage(prefix + msg.getString("domination.save_success",
                        "§7Toutes les zones ont été sauvegardées."));
                } else {
                    player.sendMessage(prefix + msg.getString("domination.save_failed",
                        "§cErreur lors de la sauvegarde."));
                }
                break;
            }

            // ── /domination createzone <name> ──────────────────────────────
            case "createzone": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                String name = args[1];
                if (DominationManager.getZone(name) != null) {
                    player.sendMessage(prefix + msg.getString("domination.zone_already_exists",
                        "§cLa zone §7" + name + " §cexiste déjà.").replace("{zone}", name));
                    return true;
                }
                DominationZone zone = new DominationZone(name, player.getLocation(), player.getLocation(), false);
                FactionEvent.getInstance().getListDominationZones().add(zone);
                player.sendMessage(prefix + msg.getString("domination.zone_created",
                    "§7Zone §c{zone} §7créée. Définissez les positions avec /domination setpos1/setpos2 <zone>.")
                    .replace("{zone}", name));
                break;
            }

            // ── /domination setpos1 <zone> ────────────────────────────────
            case "setpos1": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                zone.setPos1(player.getLocation());
                player.sendMessage(prefix + msg.getString("domination.pos1_updated",
                    "§7Position §c1 §7de la zone §c{zone} §7mise à jour.").replace("{zone}", args[1]));
                break;
            }

            // ── /domination setpos2 <zone> ────────────────────────────────
            case "setpos2": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                zone.setPos2(player.getLocation());
                player.sendMessage(prefix + msg.getString("domination.pos2_updated",
                    "§7Position §c2 §7de la zone §c{zone} §7mise à jour.").replace("{zone}", args[1]));
                break;
            }

            // ── /domination deletezone <zone> ─────────────────────────────
            case "deletezone": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                if (DominationManager.getZone(args[1]) == null) {
                    player.sendMessage(prefix + zoneNotFound(args[1], msg));
                    return true;
                }
                DominationManager.deleteZone(args[1]);
                player.sendMessage(prefix + msg.getString("domination.zone_deleted",
                    "§7Zone §c{zone} §7supprimée.").replace("{zone}", args[1]));
                break;
            }

            // ── /domination enable <zone> ─────────────────────────────────
            case "enable": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                zone.setEnabled(true);
                player.sendMessage(prefix + msg.getString("domination.zone_enabled",
                    "§7Zone §c{zone} §aactivée§7.").replace("{zone}", args[1]));
                break;
            }

            // ── /domination disable <zone> ────────────────────────────────
            case "disable": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                zone.setEnabled(false);
                player.sendMessage(prefix + msg.getString("domination.zone_disabled",
                    "§7Zone §c{zone} §cdésactivée§7.").replace("{zone}", args[1]));
                break;
            }

            // ── /domination info <zone> ───────────────────────────────────
            case "info": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                player.sendMessage(zone.toString());
                break;
            }

            default:
                sendUsage(player, prefix, msg);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            List<String> actions = Arrays.asList("start", "stop", "list", "save",
                "createzone", "setpos1", "setpos2", "deletezone", "enable", "disable", "info");
            for (String a : actions) {
                if (a.startsWith(args[0].toLowerCase())) suggestions.add(a);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            boolean needsZone = Arrays.asList("setpos1", "setpos2", "deletezone",
                "enable", "disable", "info").contains(sub);
            if (needsZone) {
                for (String name : DominationManager.getZoneNames()) {
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) suggestions.add(name);
                }
            }
        }
        return suggestions;
    }

    private void sendUsage(Player player, String prefix, FileConfiguration msg) {
        player.sendMessage(prefix + msg.getString("domination.usage",
            "§cUsage : §7/domination <action> §8| §7Actions : §fstart/stop/list/save/createzone/setpos1/setpos2/deletezone/enable/disable/info"));
    }

    private String zoneNotFound(String name, FileConfiguration msg) {
        return msg.getString("domination.zone_not_found",
            "§cLa zone §7{zone} §cn'existe pas.").replace("{zone}", name);
    }
}
