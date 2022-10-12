package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.dtc.DTCEvent;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.meteorite.MeteoriteEvent;
import fr.blixow.factionevent.utils.totem.TotemEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class EventManager {

    private final Player player;
    private boolean scoreboard;
    private boolean title;
    private boolean actionbar;


    public EventManager(Player player, boolean scoreboard, boolean title, boolean actionbar){
        this.player = player;
        this.scoreboard = scoreboard;
        this.title = title;
        this.actionbar = actionbar;
    }

    public void switchScoreboard(Player player){
        setScoreboard(!this.scoreboard);
        if(this.scoreboard){ player.sendMessage("§7Scoreboard §aactivé§7!");
        } else { player.sendMessage("§7Scoreboard §cdésactivé§7!"); }
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        KOTHEvent kothEvent = eventOn.getKothEvent();
        if(kothEvent != null){ kothEvent.getScoreBoardAPI().getObjective().unregister(); kothEvent.updateScoreboard(); }

        TotemEvent totemEvent = eventOn.getTotemEvent();
        if(totemEvent != null){ totemEvent.getScoreBoardAPI().getObjective().unregister(); totemEvent.updateScoreboard(); }

        DTCEvent dtcEvent = eventOn.getDtcEvent();
        if(dtcEvent != null){ dtcEvent.getScoreBoardAPI().getObjective().unregister(); dtcEvent.updateScoreboard(); }

        MeteoriteEvent meteoriteEvent = eventOn.getMeteoriteEvent();
        if(meteoriteEvent != null){ meteoriteEvent.getScoreBoardAPI().getObjective().unregister(); meteoriteEvent.updateScoreboard(); }


    }

    public void switchTitle(Player player){
        this.title = !this.title;
        if(this.title){ player.sendMessage("§7Les titres d'événement ont été §aactivé§7!");}
        else { player.sendMessage("§7Les titres d'événement ont été §cdésactivé§7!"); }
    }

    public void switchActionbar(Player player){
        this.actionbar = !this.actionbar;
        if(this.actionbar){ player.sendMessage("§7Les message dans la barre d'action on été §aactivé§7!");}
        else { player.sendMessage("\"§7Les message dans la barre d'action on été §cdésactivé§7!"); }
    }

    public void saveFile(){
        try {
            if(player == null){
                Bukkit.broadcastMessage("Player null");
            } else {
                FileConfiguration fc = FileManager.getEventManagerFC();
                fc.set(player.getName() + ".scoreboard", scoreboard);
                fc.set(player.getName() + ".title", scoreboard);
                fc.save(FileManager.getDataFile("eventManager.yml"));
            }
        } catch (Exception exception){
            exception.printStackTrace();
        }

    }

    public static EventManager loadFromFile(Player player){
        try {
            FileConfiguration fc = FileManager.getEventManagerFC();
            boolean scoreboard =  fc.contains(player.getName() + ".scoreboard") && fc.getBoolean(player.getName() + ".scoreboard");
            boolean title =  fc.contains(player.getName() + ".title") && fc.getBoolean(player.getName() + ".title");
            boolean actionbar = fc.contains(player.getName() + ".actionbar") && fc.getBoolean(player.getName() + ".actionbar");
            return new EventManager(player, scoreboard, title, actionbar);
        } catch (Exception exception){
            exception.printStackTrace();
        }
        return new EventManager(player, true, true, true);
    }

    public boolean isScoreboard() {
        return scoreboard;
    }
    public void setScoreboard(boolean scoreboard) {
        this.scoreboard = scoreboard;
    }
    public boolean isTitle() {
        return title;
    }
    public void setTitle(boolean title) {
        this.title = title;
    }
    public boolean isActionbar() {
        return title;
    }
    public void setActionbar(boolean title) {
        this.title = title;
    }
    @Override
    public String toString() {
        return "EventManager{" +
                "player=" + player.getName() +
                ", scoreboard=" + scoreboard +
                ", title=" + title +
                '}';
    }
}
