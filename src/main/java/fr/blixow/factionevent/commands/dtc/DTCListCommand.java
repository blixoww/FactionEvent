package fr.blixow.factionevent.commands.dtc;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class DTCListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String title = "§8§m----§r§8[ §cDTC §8]§m----";
        String empty = "§7Aucun DTC n'a été créé";
        try { title = msg.getString("dtclist.title"); } catch (Exception exception){ exception.printStackTrace(); }
        String message;
        sender.sendMessage(title);
        sender.sendMessage("");
        if(FactionEvent.getInstance().getListDTC().isEmpty()){
            try { empty = msg.getString("dtclist.empty"); } catch (Exception exception){ exception.printStackTrace(); }
            sender.sendMessage(empty);
        }
        for(DTC dtc : FactionEvent.getInstance().getListDTC()){
            if(DTCManager.isDTCStarted()){
                message = "§8» §7" + dtc.getName() + " §8(§aEn cours§8)";
                try { message = new StrManager(msg.getString("dtclist.item-started")).reDTC(dtc.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            } else {
                message = "§8» §7" + dtc.getName();
                try { message = new StrManager(msg.getString("dtclist.item")).reDTC(dtc.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            }
        }
        sender.sendMessage("");
        return true;
    }
}
