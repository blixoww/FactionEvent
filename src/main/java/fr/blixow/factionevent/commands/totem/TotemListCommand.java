package fr.blixow.factionevent.commands.totem;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class TotemListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String title = "§8§m----§r§8[ §cTOTEM §8]§m----";
        try { title = msg.getString("totemlist.title"); } catch (Exception exception){ exception.printStackTrace(); }
        String message, empty = "§7Aucun totem n'a été créé";
        sender.sendMessage(title);
        sender.sendMessage("");
        if(FactionEvent.getInstance().getListTotem().isEmpty()){
            try { empty = msg.getString("totemlist.empty"); } catch (Exception exception){ exception.printStackTrace(); }
            sender.sendMessage(empty);
        }
        for(Totem totem : FactionEvent.getInstance().getListTotem()){
            if(TotemManager.isTotemStarted(totem)){
                message = "§8» §7" + totem.getName() + " §8(§aEn cours§8)";
                try { message = new StrManager(msg.getString("totemlist.item-started")).reTotem(totem.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            } else {
                message = "§8» §7" + totem.getName();
                try { message = new StrManager(msg.getString("totemlist.item")).reTotem(totem.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            }
        }
        sender.sendMessage("");
        return true;
    }
}
