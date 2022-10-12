package fr.blixow.factionevent.commands.koth;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class KothListCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String title = "§8§m----§r§8[ §cKOTH §8]§m----";
        try { title = msg.getString("kothlist.title"); } catch (Exception exception){ exception.printStackTrace(); }
        String message, empty = "§7Aucun totem n'a été créé";
        sender.sendMessage(title);
        sender.sendMessage("");
        if(FactionEvent.getInstance().getListKOTH().isEmpty()){
            try { empty = msg.getString("kothlist.empty"); } catch (Exception exception){ exception.printStackTrace(); }
            sender.sendMessage(empty);
        }
        for(KOTH koth : FactionEvent.getInstance().getListKOTH()){
            if(KOTHManager.isKOTHStarted(koth)){
                message = "§8» §7" + koth.getName() + " §8(§aEn cours§8)";
                try { message = new StrManager(msg.getString("kothlist.item-started")).reKoth(koth.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            } else {
                message = "§8» §7" + koth.getName();
                try { message = new StrManager(msg.getString("kothlist.item")).reKoth(koth.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            }
        }
        sender.sendMessage("");
        return true;
    }
}
