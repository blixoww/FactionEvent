package fr.blixow.factionevent.utils.totem;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.HashMap;

public class TotemEditor {

    private final Totem totem;
    private final HashMap<Location, Material> blocks;

    public TotemEditor(Totem totem){
        this.totem = totem;
        blocks = new HashMap<>();
    }

    public Totem getTotem() { return totem; }

    public static boolean isAlreadyEdited(Player player, Totem totem){
        HashMap<Player, TotemEditor> plEditor = FactionEvent.getInstance().getPlayerTotemEditorHashMap();
        FileConfiguration msg = FileManager.getMessageFileConfiguration();
        if(plEditor.containsKey(player)){
            player.sendMessage(msg.getString("totem.prefix") + new StrManager(msg.getString("totem.already_editing")).rePlayer(player));
            return true;
        }
        for(Player p : plEditor.keySet()){
            TotemEditor v = plEditor.get(p); if(v.getTotem().equals(totem)){
                player.sendMessage(msg.getString("totem.prefix") + new StrManager(msg.getString("totem.already_edited")).reTotem(totem.getName()));
                return true;
            }
        }
        return false;
    }

    public void save(){
        this.totem.setBlocks(this.blocks);
        this.totem.saveTotem();
        for(Location loc : blocks.keySet()){ loc.getBlock().setType(Material.AIR); }
        Player p = getPlayerByTotem(this.totem);
        this.remove(p);
    }


    public void remove(Player player){
        if(FactionEvent.getInstance().getPlayerTotemEditorHashMap().containsKey(player)){
            if(FactionEvent.getInstance().getPlayerTotemEditorHashMap().get(player).equals(this)){
                FactionEvent.getInstance().getPlayerTotemEditorHashMap().remove(player);
            }
        }
    }

    public void addBlocks(Block block){
        blocks.put(block.getLocation(), block.getType());
    }

    public static TotemEditor getTotemEditorByPlayer(Player player){
        if(FactionEvent.getInstance().getPlayerTotemEditorHashMap().containsKey(player)){
            return FactionEvent.getInstance().getPlayerTotemEditorHashMap().get(player);
        }
        return null;
    }

    public static Player getPlayerByTotem(Totem totem){
        for(Player p : FactionEvent.getInstance().getPlayerTotemEditorHashMap().keySet()){
            TotemEditor v = FactionEvent.getInstance().getPlayerTotemEditorHashMap().get(p);
            if(v.getTotem().equals(totem)){ return p; }
        }
        return null;
    }

}
