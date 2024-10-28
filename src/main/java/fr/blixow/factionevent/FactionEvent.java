package fr.blixow.factionevent;

import fr.blixow.factionevent.commands.chat.ChatCommand;
import fr.blixow.factionevent.commands.classement.ClassementCommand;
import fr.blixow.factionevent.commands.dtc.DTCCommand;
import fr.blixow.factionevent.commands.dtc.DTCListCommand;
import fr.blixow.factionevent.commands.events.EventCommand;
import fr.blixow.factionevent.commands.koth.KothCommand;
import fr.blixow.factionevent.commands.koth.KothListCommand;
import fr.blixow.factionevent.commands.meteorite.MeteoriteCommand;
import fr.blixow.factionevent.commands.meteorite.MeteoriteListCommand;
import fr.blixow.factionevent.commands.meteorite.MeteoriteSetListCommand;
import fr.blixow.factionevent.commands.planning.PlanningAddCommand;
import fr.blixow.factionevent.commands.planning.PlanningCommand;
import fr.blixow.factionevent.commands.totem.TotemCommand;
import fr.blixow.factionevent.commands.totem.TotemListCommand;
import fr.blixow.factionevent.events.CustomEvents;
import fr.blixow.factionevent.events.DisbandFactionEvent;
import fr.blixow.factionevent.events.InventoryEvent;
import fr.blixow.factionevent.events.MeteoriteEventListener;
import fr.blixow.factionevent.utils.PlanningScheduler;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.meteorite.MeteoriteManager;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemEditor;
import fr.blixow.factionevent.utils.totem.TotemManager;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.manager.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public final class FactionEvent extends JavaPlugin {
    private static FactionEvent instance;
    // Planning
    private HashMap<String, String> planning;
    // KOTH
    private ArrayList<KOTH> listKOTH;

    // Totem
    private ArrayList<Totem> listTotem;
    private HashMap<Player, TotemEditor> playerTotemEditorHashMap;

    // DTC
    private ArrayList<DTC> listDTC;

    // Météorite
    private ArrayList<Meteorite> listMeteorite;

    // EventOn instance manager
    private EventOn eventOn;

    // Scoreboard and Title lists
    private HashMap<Player, EventManager> eventManagerMap;
    // Faction rankings
    private LinkedHashMap<Faction, Integer> factionRankings;
    // FileConfig
    private FileConfiguration config;
    private FileConfiguration messageFileConfiguration;
    private FileConfiguration kothFileConfiguration;
    private FileConfiguration totemFileConfiguration;
    private FileConfiguration dtcFileConfiguration;
    private FileConfiguration meteoriteFileConfiguration;
    private FileConfiguration chatEventFileConfiguration;
    private FileConfiguration planningFileConfiguration;
    private FileConfiguration eventManagerFileConfiguration;
    private FileConfiguration classementFileConfiguration;
    private FileConfiguration logsFileConfiguration;


    @Override
    public void onEnable() {
        instance = this;
        FileManager.createNeededFiles();
        FileManager.loadNeededFiles();
        instanceListMap();
        loadCommand();
        loadListeners();
        loadEvents();
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Activation du plugin");
        startSchedulerForPlanning();
        actionsForOnlinePlayers();
        RankingManager.runTaskUpdateRankings();
    }

    private void actionsForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            EventManager eventManager = EventManager.loadFromFile(player);
            getEventScoreboardOff().put(player, eventManager);
        }
    }

    private void loadCommand() {
        // Planning
        getCommand("planning").setExecutor(new PlanningCommand());
        getCommand("planningadd").setExecutor(new PlanningAddCommand());
        getCommand("planningadd").setTabCompleter(new PlanningAddCommand());
        // KOTH
        getCommand("koth").setExecutor(new KothCommand());
        getCommand("koth").setTabCompleter(new KothCommand());
        getCommand("kothlist").setExecutor(new KothListCommand());
        // Totem
        getCommand("totem").setExecutor(new TotemCommand());
        getCommand("totem").setTabCompleter(new TotemCommand());
        getCommand("totemlist").setExecutor(new TotemListCommand());
        // DTC
        getCommand("dtc").setExecutor(new DTCCommand());
        getCommand("dtc").setTabCompleter(new DTCCommand());
        getCommand("dtclist").setExecutor(new DTCListCommand());
        // Event
        getCommand("event").setExecutor(new EventCommand());
        getCommand("event").setTabCompleter(new EventCommand());
        // classement
        getCommand("classement").setExecutor(new ClassementCommand());
        getCommand("classement").setTabCompleter(new ClassementCommand());
        // Meteorite
        getCommand("meteorite").setExecutor(new MeteoriteCommand());
        getCommand("meteorite").setTabCompleter(new MeteoriteCommand());
        getCommand("meteoritelist").setExecutor(new MeteoriteListCommand());
        getCommand("meteoritesetlist").setExecutor(new MeteoriteSetListCommand());
        getCommand("meteoritesetlist").setTabCompleter(new MeteoriteSetListCommand());

        getCommand("chat").setExecutor(new ChatCommand());
        getCommand("chat").setTabCompleter(new ChatCommand());
    }

    private void loadListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        // Events
        pluginManager.registerEvents(new CustomEvents(), this);
        // Disband faction  actions
        pluginManager.registerEvents(new DisbandFactionEvent(), this);
        // MeteoriteEvents
        pluginManager.registerEvents(new MeteoriteEventListener(), this);

        pluginManager.registerEvents(new InventoryEvent(), this);
    }

    private void instanceListMap() {
        listKOTH = new ArrayList<>();
        listTotem = new ArrayList<>();
        listDTC = new ArrayList<>();
        listMeteorite = new ArrayList<>();
        playerTotemEditorHashMap = new HashMap<>();
        eventManagerMap = new HashMap<>();
        factionRankings = new LinkedHashMap<>();
        eventOn = new EventOn();
    }

    private void loadEvents() {
        KOTHManager.loadKOTH();
        TotemManager.loadTotems();
        DTCManager.loadDTCfromFile();
        MeteoriteManager.loadMeteoritesFromFile();
        planning = loadPlanning();
    }

    public void reloadPlanning() {
        this.planning = loadPlanning();
    }

    private HashMap<String, String> loadPlanning() {
        HashMap<String, String> planning_load = new HashMap<>();
        LocalDateTime localDateTime = LocalDateTime.now();
        int week = DateManager.getWeekOfYear(localDateTime);
        String base_path = localDateTime.getYear() + "." + week;
        for (String jour : DateManager.listJour) {
            String path_jour = base_path + "." + jour;
            String date = PlanningManager.getDate(path_jour);
            HashMap<String, List<String>> dailyEvents = PlanningManager.getDailyEvents(path_jour);
            dailyEvents.forEach((k, v) -> {
                for (String s : v) {
                    try {
                        String hours = s.split("\\|")[0].split("h")[0];
                        if (hours.length() == 1) {
                            hours = "0" + hours;
                        }
                        String mins = s.split("\\|")[0].split("h")[1];
                        if (mins.length() == 1) {
                            mins = "0" + mins;
                        }
                        String name = s.split("\\|")[1];
                        planning_load.put(date + "-" + hours + "-" + mins, k + "-" + name);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            });
        }
        return planning_load;
    }

    private void cancelAllEvent() {
        //EventOn.cancelEvents();
    }

    private void startSchedulerForPlanning() {
        new PlanningScheduler().runTaskTimer(this, 0L, 60 * 20L);
    }


    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§cDEBUG: §7Reloading new files");
        FileManager.loadNeededFiles();
        Bukkit.getConsoleSender().sendMessage("§cDEBUG: §7Saving new files");
        FileManager.saveFiles();
        eventOn.cancelEvent();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Désactivation du plugin");
    }

    public static FactionEvent getInstance() {
        return instance;
    }

    public HashMap<String, String> getPlanning() {
        return planning;
    }

    public void setListKOTH(ArrayList<KOTH> listKOTH) {
        this.listKOTH = listKOTH;
    }

    public ArrayList<KOTH> getListKOTH() {
        return listKOTH;
    }

    public ArrayList<Totem> getListTotem() {
        return listTotem;
    }

    public HashMap<Player, TotemEditor> getPlayerTotemEditorHashMap() {
        return playerTotemEditorHashMap;
    }


    public HashMap<Player, EventManager> getEventScoreboardOff() {
        return eventManagerMap;
    }

    public HashMap<Faction, Integer> getFactionRankings() {
        return factionRankings;
    }

    public void setFactionRankings(LinkedHashMap<Faction, Integer> factionRankings) {
        this.factionRankings = factionRankings;
    }

    public ArrayList<DTC> getListDTC() {
        return listDTC;
    }

    public void setListDTC(ArrayList<DTC> listDTC) {
        this.listDTC = listDTC;
    }

    public ArrayList<Meteorite> getListMeteorite() {
        return listMeteorite;
    }

    public void setListMeteorite(ArrayList<Meteorite> listMeteorite) {
        this.listMeteorite = listMeteorite;
    }

    public EventOn getEventOn() {
        return eventOn;
    }

    private void reloadFiles() {
        FileManager.saveFiles();
        FileManager.loadNeededFiles();
    }


    public FileConfiguration getFileConfig() {
        return config;
    }

    public FileConfiguration getMessageFileConfiguration() {
        return messageFileConfiguration;
    }

    public FileConfiguration getKothFileConfiguration() {
        return kothFileConfiguration;
    }

    public FileConfiguration getTotemFileConfiguration() {
        return totemFileConfiguration;
    }

    public FileConfiguration getDtcFileConfiguration() {
        return dtcFileConfiguration;
    }

    public FileConfiguration getMeteoriteFileConfiguration() {
        return meteoriteFileConfiguration;
    }

    public FileConfiguration getChatEventFileConfiguration() {
        return chatEventFileConfiguration;
    }

    public FileConfiguration getPlanningFileConfiguration() {
        return planningFileConfiguration;
    }

    public FileConfiguration getEventManagerFileConfiguration() {
        return eventManagerFileConfiguration;
    }

    public FileConfiguration getClassementFileConfiguration() {
        return classementFileConfiguration;
    }

    public FileConfiguration getLogsFileConfiguration() {
        return logsFileConfiguration;
    }

    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    public void setMessageFileConfiguration(FileConfiguration messageFileConfiguration) {
        this.messageFileConfiguration = messageFileConfiguration;
    }

    public void setKothFileConfiguration(FileConfiguration kothFileConfiguration) {
        this.kothFileConfiguration = kothFileConfiguration;
    }

    public void setTotemFileConfiguration(FileConfiguration totemFileConfiguration) {
        this.totemFileConfiguration = totemFileConfiguration;
    }

    public void setDtcFileConfiguration(FileConfiguration dtcFileConfiguration) {
        this.dtcFileConfiguration = dtcFileConfiguration;
    }

    public void setMeteoriteFileConfiguration(FileConfiguration meteoriteFileConfiguration) {
        this.meteoriteFileConfiguration = meteoriteFileConfiguration;
    }

    public void setChatEventFileConfiguration(FileConfiguration chatEventFileConfiguration) {
        this.chatEventFileConfiguration = chatEventFileConfiguration;
    }

    public void setPlanningFileConfiguration(FileConfiguration planningFileConfiguration) {
        this.planningFileConfiguration = planningFileConfiguration;
    }

    public void setEventManagerFileConfiguration(FileConfiguration eventManagerFileConfiguration) {
        this.eventManagerFileConfiguration = eventManagerFileConfiguration;
    }

    public void setClassementFileConfiguration(FileConfiguration classementFileConfiguration) {
        this.classementFileConfiguration = classementFileConfiguration;
    }

    public void setLogsFileConfiguration(FileConfiguration logsFileConfiguration) {
        this.logsFileConfiguration = logsFileConfiguration;
    }

}
