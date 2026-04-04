package fr.blixow.factionevent.commands.domination;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.domination.Domination;
import fr.blixow.factionevent.utils.domination.DominationManager;
import fr.blixow.factionevent.utils.domination.DominationZone;
import org.bukkit.Location;
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

            // ── /domination setchest <zone> ───────────────────────────────
            // Définit la position du coffre de loot à votre position actuelle
            case "setchest": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                zone.setChestLocation(player.getLocation());
                DominationManager.saveZone(zone);
                Location cl = zone.getChestLocation();
                player.sendMessage(prefix + "§7Position du coffre de la zone §c" + args[1]
                    + " §7définie à §f" + cl.getBlockX() + "/" + cl.getBlockY() + "/" + cl.getBlockZ() + "§7.");
                break;
            }

            // ── /domination unsetchest <zone> ─────────────────────────────
            // Supprime la position manuelle du coffre (retour au placement automatique)
            case "unsetchest": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                zone.setChestLocation(null);
                DominationManager.saveZone(zone);
                player.sendMessage(prefix + "§7Position du coffre de la zone §c" + args[1]
                    + " §7supprimée. §8(placement automatique au centre)");
                break;
            }

            // ── /domination expand <zone> [up|down|all] [valeur] ──────────
            case "expand": {
                // Args: expand <zone> [direction] [valeur]
                // direction : up / down / all  (défaut : all)
                // valeur : entier positif       (défaut : config domination.expand.default_value)
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                DominationZone zone = DominationManager.getZone(args[1]);
                if (zone == null) { player.sendMessage(prefix + zoneNotFound(args[1], msg)); return true; }
                if (zone.getPos1() == null || zone.getPos2() == null) {
                    player.sendMessage(prefix + "§cLa zone §7" + args[1] + " §cn'a pas encore ses deux positions définies.");
                    return true;
                }

                org.bukkit.configuration.file.FileConfiguration cfg = FileManager.getConfig();
                int defaultExpand = cfg.getInt("domination.expand.default_value", 20);

                String direction = "all";
                int value = defaultExpand;

                if (args.length >= 3) {
                    String arg2 = args[2].toLowerCase();
                    if (arg2.equals("up") || arg2.equals("down") || arg2.equals("all")) {
                        direction = arg2;
                        if (args.length >= 4) {
                            try { value = Integer.parseInt(args[3]); }
                            catch (NumberFormatException e) {
                                player.sendMessage(prefix + "§cValeur invalide : §7" + args[3] + " §c(entier attendu).");
                                return true;
                            }
                        }
                    } else {
                        // 2ème arg est peut-être directement un nombre
                        try {
                            value = Integer.parseInt(arg2);
                        } catch (NumberFormatException e) {
                            player.sendMessage(prefix + "§cUsage : §7/domination expand <zone> [up|down|all] [valeur]");
                            return true;
                        }
                    }
                }

                if (value <= 0) {
                    player.sendMessage(prefix + "§cLa valeur d'expansion doit être positive (> 0).");
                    return true;
                }

                // Récupère les Y min/max actuels
                int minY = Math.min(zone.getPos1().getBlockY(), zone.getPos2().getBlockY());
                int maxY = Math.max(zone.getPos1().getBlockY(), zone.getPos2().getBlockY());
                org.bukkit.World w = zone.getPos1().getWorld();
                int worldMin = 0;
                int worldMax = w != null ? w.getMaxHeight() : 255;

                int newMinY = minY;
                int newMaxY = maxY;

                if (direction.equals("down") || direction.equals("all")) {
                    newMinY = Math.max(worldMin, minY - value);
                }
                if (direction.equals("up") || direction.equals("all")) {
                    newMaxY = Math.min(worldMax, maxY + value);
                }

                // Met à jour pos1 (Y min) et pos2 (Y max) en conservant X/Z
                Location p1 = zone.getPos1().clone();
                Location p2 = zone.getPos2().clone();

                // pos1 = coin Y-bas, pos2 = coin Y-haut (indépendamment de l'ordre original)
                if (zone.getPos1().getBlockY() <= zone.getPos2().getBlockY()) {
                    p1.setY(newMinY);
                    p2.setY(newMaxY);
                } else {
                    p1.setY(newMaxY);
                    p2.setY(newMinY);
                }

                zone.setPos1(p1);
                zone.setPos2(p2);
                DominationManager.saveZone(zone);

                String dirLabel;
                switch (direction) {
                    case "up":   dirLabel = "§7vers le §ahaut §7(+" + value + ")"; break;
                    case "down": dirLabel = "§7vers le §cbas §7(+" + value + ")"; break;
                    default:     dirLabel = "§7dans les §edeux directions §7(+" + value + " chaque)"; break;
                }
                player.sendMessage(prefix + "§7Zone §c" + zone.getName() + " §7étendue " + dirLabel + "§7.");
                player.sendMessage(prefix + "§8» §7Nouveau Y : §fde §c" + newMinY + " §fà §c" + newMaxY);
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
                "createzone", "setpos1", "setpos2", "setchest", "unsetchest",
                "expand", "deletezone", "enable", "disable", "info");
            for (String a : actions) {
                if (a.startsWith(args[0].toLowerCase())) suggestions.add(a);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            boolean needsZone = Arrays.asList("setpos1", "setpos2", "deletezone",
                "enable", "disable", "info", "expand", "setchest", "unsetchest").contains(sub);
            if (needsZone) {
                for (String name : DominationManager.getZoneNames()) {
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) suggestions.add(name);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("expand")) {
            // Directions
            for (String d : Arrays.asList("all", "up", "down")) {
                if (d.startsWith(args[2].toLowerCase())) suggestions.add(d);
            }
            // Valeurs numériques depuis la config
            for (int v : FileManager.getConfig().getIntegerList("domination.expand.presets")) {
                String sv = String.valueOf(v);
                if (sv.startsWith(args[2])) suggestions.add(sv);
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("expand")) {
            // Valeurs numériques depuis la config
            for (int v : FileManager.getConfig().getIntegerList("domination.expand.presets")) {
                String sv = String.valueOf(v);
                if (sv.startsWith(args[3])) suggestions.add(sv);
            }
        }
        return suggestions;
    }

    private void sendUsage(Player player, String prefix, FileConfiguration msg) {
        player.sendMessage(prefix + msg.getString("domination.usage",
            "§cUsage : §7/domination <action> §8| §7Actions : §fstart/stop/list/save/createzone/setpos1/setpos2/setchest/unsetchest/expand/deletezone/enable/disable/info"));
    }

    private String zoneNotFound(String name, FileConfiguration msg) {
        return msg.getString("domination.zone_not_found",
            "§cLa zone §7{zone} §cn'existe pas.").replace("{zone}", name);
    }
}
