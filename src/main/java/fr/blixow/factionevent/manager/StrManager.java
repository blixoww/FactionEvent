package fr.blixow.factionevent.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class StrManager {


    private String message;

    public StrManager(String message) {
        this.message = message;
    }

    public StrManager reTarget(Player target){
        this.message = this.message.replaceAll("\\{target}", target.getName());
        return this;
    }

    public StrManager rePlayer(Player player){
        try {
            this.message = this.message.replaceAll("\\{player}", player.getName());
        } catch (Exception exception){
            exception.printStackTrace();
        }

        return this;
    }

    public StrManager reTime(String time){
        try {
            this.message = this.message.replaceAll("\\{time}", time);
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }
    public StrManager reMaxTime(String time){
        this.message = this.message.replaceAll("\\{maxtime}", time);
        return this;
    }

    public StrManager reCurrentDTCLife(int vie, int max_vie){
        try {
            this.message = this.message.replaceAll("\\{life}", String.valueOf(vie));
            this.message = this.message.replaceAll("\\{max_life}", String.valueOf(max_vie));
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }

    public StrManager reDamageDealtDTC(int damageDealt){
        try {
            this.message = this.message.replaceAll("\\{damage}", String.valueOf(damageDealt));
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }

    public StrManager reCustom(String regex, String value){
        this.message = this.message.replaceAll(regex, value);
        return this;
    }

    public StrManager reKoth(String kothName){
        try {
            this.message = this.message.replaceAll("\\{koth}", kothName);
            this.message = this.message.replaceAll("\\{kothL}", kothName.toLowerCase());
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }


    public StrManager reTotem(String totemName){
        try {
            this.message = this.message.replaceAll("\\{totem}", totemName);
            this.message = this.message.replaceAll("\\{totemL}", totemName.toLowerCase());
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }


    public StrManager reBlocks(int blocks, int maxblocks){
        try {
            this.message = this.message.replaceAll("\\{blocks}", String.valueOf(blocks)).replaceAll("\\{maxblocks}", String.valueOf(maxblocks));
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }

    public StrManager reDTC(String DTCname){
        try {
            this.message = this.message.replaceAll("\\{dtc}", DTCname);
            this.message = this.message.replaceAll("\\{dtcL}", DTCname.toLowerCase());
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }

    public StrManager reMeteorite(String meteorite){
        try {
            this.message = this.message.replaceAll("\\{meteorite}", meteorite);
            this.message = this.message.replaceAll("\\{meteoriteL}", meteorite.toLowerCase());
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }

    public StrManager reMot(String mot){
        this.message = this.message.replaceAll("\\{mot}", mot);
        return this;
    }

    public StrManager rePlayerTarget(Player player, Player target){
        rePlayer(player);
        reTarget(target);
        return this;
    }

    public StrManager reLocation(Location location){
        try {
            String str = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
            this.message = this.message.replaceAll("\\{location}", str);
        } catch (Exception exception){ exception.printStackTrace(); }
        return this;
    }

    public StrManager reFaction(String faction){
        this.message = this.message.replaceAll("\\{faction}", faction);
        return this;
    }

    public StrManager rePoints(int points){
        this.message = this.message.replaceAll("\\{points}", String.valueOf(points));
        return this;
    }


    public String get(){ return this.message; }

    @Override
    public String toString(){ return this.message; }

}
