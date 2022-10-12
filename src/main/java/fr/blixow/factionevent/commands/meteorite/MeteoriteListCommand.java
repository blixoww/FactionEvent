package fr.blixow.factionevent.commands.meteorite;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.meteorite.MeteoriteManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class MeteoriteListCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        String title = "§8§m----§r§8[ §cMétéorite §8]§m----";
        String empty = "§7Aucune météorite n'a été créé";
        try { title = msg.getString("meteoritelist.title"); } catch (Exception exception){ exception.printStackTrace(); }
        String message;
        sender.sendMessage(title);
        sender.sendMessage("");
        if(FactionEvent.getInstance().getListMeteorite().isEmpty()){
            try { empty = msg.getString("meteoritelist.empty"); } catch (Exception exception){ exception.printStackTrace(); }
            sender.sendMessage(empty);
        }
        for(Meteorite meteorite : FactionEvent.getInstance().getListMeteorite()){
            if(MeteoriteManager.isMeteoriteStarted(meteorite)){
                message = "§8» §7" + meteorite.getName() + " §8(§aEn cours§8)";
                try { message = new StrManager(msg.getString("meteoritelist.item-started")).reMeteorite(meteorite.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            } else {
                message = "§8» §7" + meteorite.getName();
                try { message = new StrManager(msg.getString("meteoritelist.item")).reMeteorite(meteorite.getName()).toString();
                } catch (Exception exception){ exception.printStackTrace(); }
                sender.sendMessage(message);
            }
        }
        sender.sendMessage("");
        return true;
    }
}
