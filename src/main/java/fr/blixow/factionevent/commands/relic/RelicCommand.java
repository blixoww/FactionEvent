package fr.blixow.factionevent.commands.relic;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.relic.Relic;
import fr.blixow.factionevent.utils.relic.RelicManager;
import fr.blixow.factionevent.utils.relic.RelicSpawn;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelicCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String prefix = msg.getString("relic.prefix", "§8[§dRELIQUE§8]§7 ");

        if (!player.hasPermission("factionevent.admin.relic")) {
            player.sendMessage(msg.getString("prefix") + msg.getString("no-permissions"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, prefix, msg);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── /relic start ──────────────────────────────────────────────
            case "start": {
                if (RelicManager.isRelicStarted()) {
                    player.sendMessage(prefix + msg.getString("relic.already_started",
                        "§cUne Course à la Relique est déjà en cours."));
                    return true;
                }
                RelicSpawn spawn = RelicManager.getRandomSpawn();
                if (spawn == null || spawn.getLocation() == null) {
                    player.sendMessage(prefix + msg.getString("relic.no_spawns",
                        "§cAucun point de spawn défini. Ajoutez-en avec /relic addspawn <nom>."));
                    return true;
                }
                new Relic(spawn.getLocation()).start(player);
                break;
            }

            // ── /relic stop ───────────────────────────────────────────────
            case "stop": {
                new Relic(null).stop(player);
                break;
            }

            // ── /relic list ───────────────────────────────────────────────
            case "list": {
                List<RelicSpawn> spawns = FactionEvent.getInstance().getListRelicSpawns();
                if (spawns.isEmpty()) {
                    player.sendMessage(prefix + msg.getString("relic.empty_list",
                        "§7Aucun point de spawn créé."));
                    return true;
                }
                player.sendMessage("§8§m      §r §8[ §dSPAWNS RELIQUE §8] §m      ");
                for (RelicSpawn s : spawns) {
                    player.sendMessage("  §8» " + s.toString());
                }
                break;
            }

            // ── /relic addspawn <name> ────────────────────────────────────
            case "addspawn": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                String name = args[1];
                if (RelicManager.getSpawn(name) != null) {
                    player.sendMessage(prefix + msg.getString("relic.spawn_already_exists",
                        "§cLe spawn §7{spawn} §cexiste déjà.").replace("{spawn}", name));
                    return true;
                }
                RelicSpawn spawn = new RelicSpawn(name, player.getLocation());
                FactionEvent.getInstance().getListRelicSpawns().add(spawn);
                RelicManager.saveSpawn(spawn);
                Location l = spawn.getLocation();
                player.sendMessage(prefix + msg.getString("relic.spawn_created",
                    "§7Spawn §d{spawn} §7créé à §f{loc}§7.")
                    .replace("{spawn}", name)
                    .replace("{loc}", l.getBlockX() + "/" + l.getBlockY() + "/" + l.getBlockZ()));
                break;
            }

            // ── /relic delspawn <name> ────────────────────────────────────
            case "delspawn": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                if (RelicManager.getSpawn(args[1]) == null) {
                    player.sendMessage(prefix + spawnNotFound(args[1], msg));
                    return true;
                }
                RelicManager.deleteSpawn(args[1]);
                player.sendMessage(prefix + msg.getString("relic.spawn_deleted",
                    "§7Spawn §d{spawn} §7supprimé.").replace("{spawn}", args[1]));
                break;
            }

            // ── /relic tp <name> ──────────────────────────────────────────
            case "tp": {
                if (args.length < 2) { sendUsage(player, prefix, msg); return true; }
                RelicSpawn spawn = RelicManager.getSpawn(args[1]);
                if (spawn == null || spawn.getLocation() == null) {
                    player.sendMessage(prefix + spawnNotFound(args[1], msg));
                    return true;
                }
                player.teleport(spawn.getLocation());
                player.sendMessage(prefix + "§7Téléporté au spawn §d" + spawn.getName() + "§7.");
                break;
            }

            // ── /relic save ───────────────────────────────────────────────
            case "save": {
                boolean ok = true;
                for (RelicSpawn s : FactionEvent.getInstance().getListRelicSpawns()) {
                    ok &= RelicManager.saveSpawn(s);
                }
                player.sendMessage(prefix + (ok
                    ? msg.getString("relic.save_success", "§7Tous les spawns ont été sauvegardés.")
                    : msg.getString("relic.save_failed", "§cErreur lors de la sauvegarde.")));
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
            for (String a : Arrays.asList("start", "stop", "list", "addspawn", "delspawn", "tp", "save")) {
                if (a.startsWith(args[0].toLowerCase())) suggestions.add(a);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("delspawn", "tp").contains(sub)) {
                // Noms de spawns existants
                for (String name : RelicManager.getSpawnNames()) {
                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) suggestions.add(name);
                }
            } else if (sub.equals("addspawn") && args[1].isEmpty()) {
                // Indication de l'argument attendu
                suggestions.add("<nom>");
            }
        }
        return suggestions;
    }

    private void sendUsage(Player player, String prefix, FileConfiguration msg) {
        player.sendMessage("§8§m      §r §8[ §dCOURSE À LA RELIQUE §8] §m      ");
        player.sendMessage("§8» §d/relic start §8- §7Lance l'event (spawn aléatoire)");
        player.sendMessage("§8» §d/relic stop §8- §7Arrête / annule l'event en cours");
        player.sendMessage("§8» §d/relic list §8- §7Liste les points de spawn");
        player.sendMessage("§8» §d/relic addspawn §8<§7nom§8> §8- §7Crée un spawn à ta position");
        player.sendMessage("§8» §d/relic delspawn §8<§7nom§8> §8- §7Supprime un spawn");
        player.sendMessage("§8» §d/relic tp §8<§7nom§8> §8- §7Te téléporte à un spawn");
        player.sendMessage("§8» §d/relic save §8- §7Sauvegarde tous les spawns");
    }

    private String spawnNotFound(String name, FileConfiguration msg) {
        return msg.getString("relic.spawn_not_found",
            "§cLe spawn §7{spawn} §cn'existe pas.").replace("{spawn}", name);
    }
}
