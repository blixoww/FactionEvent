package fr.blixow.factionevent;

import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.commands.classement.ClassementCommand;
import fr.blixow.factionevent.commands.dtc.DTCCommand;
import fr.blixow.factionevent.commands.dtc.DTCListCommand;
import fr.blixow.factionevent.commands.events.EventCommand;
import fr.blixow.factionevent.commands.koth.KothCommand;
import fr.blixow.factionevent.commands.koth.KothListCommand;
import fr.blixow.factionevent.commands.planning.PlanningAddCommand;
import fr.blixow.factionevent.commands.planning.PlanningCommand;
import fr.blixow.factionevent.commands.planning.PlanningRemoveCommand;
import fr.blixow.factionevent.commands.totem.TotemCommand;
import fr.blixow.factionevent.commands.totem.TotemListCommand;
import fr.blixow.factionevent.enumeration.DayEnum;
import fr.blixow.factionevent.events.CustomEvents;
import fr.blixow.factionevent.events.ManagerFactionEvent;
import fr.blixow.factionevent.events.InventoryEvent;
import fr.blixow.factionevent.manager.*;
import fr.blixow.factionevent.utils.PlanningScheduler;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemEditor;
import fr.blixow.factionevent.utils.totem.TotemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.LocalDateTime;
import java.util.*;

public final class FactionEvent extends JavaPlugin {
    private static FactionEvent instance;
    // Planning
    private Map<String, String> planning;
    // KOTH
    private ArrayList<KOTH> listKOTH;
    private ArrayList<Meteorite> listMeteorite;

    // Totem
    private ArrayList<Totem> listTotem;
    private HashMap<Player, TotemEditor> playerTotemEditorHashMap;

    // DTC
    private ArrayList<DTC> listDTC;

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
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Activation du plugin FactionEvent");
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
        getCommand("planningremove").setExecutor(new PlanningRemoveCommand());
        getCommand("planningremove").setTabCompleter(new PlanningRemoveCommand());
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
    }

    private void loadListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        // Events
        pluginManager.registerEvents(new CustomEvents(), this);
        // Disband faction  actions
        pluginManager.registerEvents(new ManagerFactionEvent(), this);
        // Inventory actions
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
        planning = loadPlanning();
    }

    public void reloadPlanning() {
        this.planning = loadPlanning();
    }

    private HashMap<String, String> loadPlanning() {
        HashMap<String, String> scheduleMap = new HashMap<>();
        LocalDateTime currentDateTime = LocalDateTime.now();
        int currentWeek = DateManager.getWeekOfYear(currentDateTime);
        String basePath = currentDateTime.getYear() + "." + currentWeek;

        for (DayEnum day : DayEnum.values()) {
            String value = day.getValeur();
            String dayPath = basePath + "." + value;
            String dayDate = PlanningManager.getDate(dayPath);
            HashMap<String, List<String>> dailyEvents = PlanningManager.getDailyEvents(dayPath);

            for (Map.Entry<String, List<String>> entry : dailyEvents.entrySet()) {
                String category = entry.getKey();
                List<String> events = entry.getValue();

                for (String event : events) {
                    try {
                        String[] eventDetails = event.split("\\|");
                        String time = eventDetails[0];
                        String eventName = eventDetails[1];

                        String formattedTime = DateManager.formatTime(time);
                        String eventKey = dayDate + "-" + formattedTime;

                        scheduleMap.put(eventKey, category + "-" + eventName);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
        return scheduleMap;
    }

    private void startSchedulerForPlanning() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimer(this, new PlanningScheduler(), 0, 20 * 60);
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

    public Map<String, String> getPlanning() {
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

    public ArrayList<Meteorite> getList() { return listMeteorite; }

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
