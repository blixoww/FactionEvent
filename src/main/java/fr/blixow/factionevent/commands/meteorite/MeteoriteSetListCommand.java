package fr.blixow.factionevent.commands.meteorite;

import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.meteorite.MeteoriteManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MeteoriteSetListCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            if(!player.hasPermission("factionevent.admin.meteoritesetlist")){ player.sendMessage(msg.getString("no-permissions")); }
            if(args.length == 4){
                Meteorite meteoriteObject = MeteoriteManager.getMeteoriteByName(args[3]);
                if(meteoriteObject == null){
                    player.sendMessage(msg.getString("meteorite.prefix") + new StrManager(msg.getString("meteorite.doesnt_exist")).reMeteorite(args[3]).toString());
                    return true;
                }
                int x_arg = 0, y_arg = 0, z_arg = 0;
                try {
                    x_arg = Integer.parseInt(args[0]);
                    y_arg = Integer.parseInt(args[1]);
                    z_arg = Integer.parseInt(args[2]);
                } catch (Exception exception){ exception.printStackTrace(); }
                Location loc = player.getLocation();
                World world = player.getWorld();
                FileConfiguration meteorite = FileManager.getMeteoriteDataFC();
                File file = FileManager.getDataFile("meteorite.yml");
                int nb_blocks = 0;
                List<String> stringList = new ArrayList<>();
                String blockPos = "";
                int locx = (int) loc.getX(), minx = locx, maxx = locx + x_arg;
                int locy = (int) loc.getY(), miny = locy, maxy = locy + y_arg;
                int locz = (int) loc.getZ(), minz = locz, maxz = locz + z_arg;
                int x, y, z;
                player.sendMessage("LOCX = " + locx + ", MINX = " + minx + ", MAXX = " + maxx);
                player.sendMessage("LOCY = " + locy + ", MINY = " + miny + ", MAXY = " + maxy);
                player.sendMessage("LOCZ = " + locz + ", MINZ = " + minz + ", MAXZ = " + maxz);
                for(x = minx; x < maxx; x++){
                    for(z = minz; z < maxz; z++){
                        for(y = miny; y < maxy; y++){
                            try {
                                Location location = new Location(world, x, y, z);
                                if(location.getBlock().getType().equals(Material.OBSIDIAN)){
                                    blockPos = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
                                    stringList.add(blockPos);
                                }
                            } catch (Exception exception){
                                exception.printStackTrace();
                            }

                        }
                    }
                }
                player.sendMessage("§cMise à jour des blocks faites");
                try {
                    meteorite.set(meteoriteObject.getName() + ".block_position", stringList);
                    meteorite.save(file);
                } catch (Exception exception){ exception.printStackTrace(); }
            } else {
                player.sendMessage("§c/" + label + " <xmax> <ymax> <zmax> <nomk>");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> stringList = new ArrayList<>();
        if(args.length < 4){
            ArrayList<String> stringArrayList = new ArrayList<>(100);
            for(int i = 1; i <= 100; i++){ stringArrayList.add(String.valueOf(i)); }
            for(String str : stringArrayList){
                if(str.toLowerCase().startsWith(args[args.length - 1].toLowerCase())){ stringList.add(str); }
            }
        } else if(args.length == 4){
            for(String str : MeteoriteManager.getMeteoriteListNames()){
                if(str.toLowerCase().startsWith(args[0].toLowerCase())){
                    stringList.add(str);
                }
            }
        }
        return stringList;
    }
}
