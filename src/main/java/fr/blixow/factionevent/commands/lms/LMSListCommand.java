package fr.blixow.factionevent.commands.lms;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class LMSListCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String title = "§8§m----§r§8[ §cLMS §8]§m----";
        try { title = msg.getString("lmslist.title"); } catch (Exception exception){ exception.printStackTrace(); }
        String message, empty = "§7Aucun LMS n'a été créé";
        sender.sendMessage(title);
        sender.sendMessage("");
        if(FactionEvent.getInstance().getListLMS().isEmpty()){
            try { empty = msg.getString("lmslist.empty"); } catch (Exception exception){ exception.printStackTrace(); }
            sender.sendMessage(empty);
        }
        for(LMS lms : FactionEvent.getInstance().getListLMS()){
            if(LMSManager.isLMSStarted(lms)){
                message = "§8» §7" + lms.getName() + " §8(§aEn cours§8)";
                try { message = new StrManager(msg.getString("lmslist.item-started")).reLMS(lms.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            } else {
                message = "§8» §7" + lms.getName();
                try { message = new StrManager(msg.getString("lmslist.item")).reLMS(lms.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            }
        }
        sender.sendMessage("");
        return true;
    }
}
