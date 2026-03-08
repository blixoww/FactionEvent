package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.utils.guess.Guess;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class StrManager {

    private String message;

    public StrManager(String message) {
        this.message = message;
    }

    public StrManager reTarget(Player target){
        if (target != null) {
            this.message = this.message.replace("{target}", target.getName());
        }
        return this;
    }

    public StrManager rePlayer(Player player){
        if (player != null) {
            this.message = this.message.replace("{player}", player.getName());
        }
        return this;
    }

    public StrManager reTime(String time){
        if (time != null) {
            this.message = this.message.replace("{time}", time);
        }
        return this;
    }
    public StrManager reMaxTime(String time){
        if (time != null) {
            this.message = this.message.replace("{maxtime}", time);
        }
        return this;
    }

    public StrManager reCurrentDTCLife(int vie, int max_vie){
        this.message = this.message.replace("{life}", String.valueOf(vie));
        this.message = this.message.replace("{max_life}", String.valueOf(max_vie));
        return this;
    }

    public StrManager reDamageDealtDTC(int damageDealt){
        this.message = this.message.replace("{damage}", String.valueOf(damageDealt));
        return this;
    }

    public StrManager reCustom(String target, String value){
        if (target != null && value != null) {
            this.message = this.message.replace(target, value);
        }
        return this;
    }

    public StrManager reKoth(String kothName){
        if (kothName != null) {
            this.message = this.message.replace("{koth}", kothName);
            this.message = this.message.replace("{kothL}", kothName.toLowerCase());
        }
        return this;
    }


    public StrManager reTotem(String totemName){
        if (totemName != null) {
            this.message = this.message.replace("{totem}", totemName);
            this.message = this.message.replace("{totemL}", totemName.toLowerCase());
        }
        return this;
    }


    public StrManager reBlocks(int blocks, int maxblocks){
        this.message = this.message
                .replace("{blocks}", String.valueOf(blocks))
                .replace("{maxblocks}", String.valueOf(maxblocks));
        return this;
    }

    public StrManager reDTC(String DTCname){
        if (DTCname != null) {
            this.message = this.message.replace("{dtc}", DTCname);
            this.message = this.message.replace("{dtcL}", DTCname.toLowerCase());
        }
        return this;
    }

    public StrManager reLMS(String LMSname){
        if (LMSname != null) {
            this.message = this.message.replace("{lms}", LMSname);
            this.message = this.message.replace("{lmsL}", LMSname.toLowerCase());
        }
        return this;
    }

    public StrManager rePlayerTarget(Player player, Player target){
        rePlayer(player);
        reTarget(target);
        return this;
    }

    public StrManager reLocation(Location location){
        if (location != null) {
            String str = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
            this.message = this.message.replace("{location}", str);
        }
        return this;
    }

    public StrManager reFaction(String faction){
        if (faction != null) {
            this.message = this.message.replace("{faction}", faction);
        }
        return this;
    }

    public StrManager rePoints(int points){
        this.message = this.message.replace("{points}", String.valueOf(points));
        return this;
    }

    public StrManager reWord(String guess){
        if (guess != null) {
            this.message = this.message.replace("{word}", "§7" + guess);
        }
        return this;
    }

    public String get(){ return this.message; }

    @Override
    public String toString(){ return this.message; }

    public StrManager reType(String type) {
        if (type != null) {
            this.message = message.replace("{type}", type.toUpperCase());
        }
        return this;
    }

}
