package fr.blixow.factionevent;

import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.commands.classement.ClassementCommand;
import fr.blixow.factionevent.commands.dtc.DTCCommand;
import fr.blixow.factionevent.commands.dtc.DTCListCommand;
import fr.blixow.factionevent.commands.events.EventCommand;
import fr.blixow.factionevent.commands.guess.AnswerCommand;
import fr.blixow.factionevent.commands.guess.GuessCommand;
import fr.blixow.factionevent.commands.koth.KothCommand;
import fr.blixow.factionevent.commands.koth.KothListCommand;
import fr.blixow.factionevent.commands.lms.LMSCommand;
import fr.blixow.factionevent.commands.lms.LMSListCommand;
import fr.blixow.factionevent.commands.lms.LMSRCommand;
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
import fr.blixow.factionevent.utils.ScheduledEvent;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCManager;
import fr.blixow.factionevent.utils.event.EventOn;
import fr.blixow.factionevent.utils.guess.GuessManager;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHManager;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSManager;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemEditor;
import fr.blixow.factionevent.utils.totem.TotemManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

public final class FactionEvent extends JavaPlugin {
    private static FactionEvent instance;
    // Planning
    private List<ScheduledEvent> planningEvents;
    // KOTH
    private ArrayList<KOTH> listKOTH;

    // Totem
    private ArrayList<Totem> listTotem;
    private HashMap<Player, TotemEditor> playerTotemEditorHashMap;

    // DTC
    private ArrayList<DTC> listDTC;

    // LMS
    private ArrayList<LMS> listLMS;

    // EventOn instance manager
    private EventOn eventOn;

    // Scoreboard and Title lists
    private HashMap<Player, EventManager> eventManagerMap;
    // Timestamp (ms) du prochain Guess aléatoire planifié (-1 si non planifié)
    private volatile long nextGuessTimestampMillis = -1;
    // Faction rankings
    private LinkedHashMap<Faction, Integer> factionRankings;
    // FileConfig
    private FileConfiguration config;
    private FileConfiguration messageFileConfiguration;
    private FileConfiguration kothFileConfiguration;
    private FileConfiguration totemFileConfiguration;
    private FileConfiguration dtcFileConfiguration;
    private FileConfiguration lmsFileConfiguration;
    private FileConfiguration guessFileConfiguration;
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
        startRandomGuessScheduler();
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
        // LMS
        getCommand("lms").setExecutor(new LMSCommand());
        getCommand("lms").setTabCompleter(new LMSCommand());
        getCommand("lmsr").setExecutor(new LMSRCommand());
        getCommand("lmsr").setTabCompleter(new LMSRCommand());
        getCommand("lmslist").setExecutor(new LMSListCommand());

        // Guess
        getCommand("guess").setExecutor(new GuessCommand());
        getCommand("guess").setTabCompleter(new GuessCommand());
        getCommand("answer").setExecutor(new AnswerCommand());

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
        listLMS = new ArrayList<>();
        playerTotemEditorHashMap = new HashMap<>();
        eventManagerMap = new HashMap<>();
        factionRankings = new LinkedHashMap<>();
        eventOn = new EventOn();
        planningEvents = new ArrayList<>();
    }

    private void loadEvents() {
        KOTHManager.loadKOTH();
        TotemManager.loadTotems();
        DTCManager.loadDTCfromFile();
        LMSManager.loadLMSfromFile();
        GuessManager.loadWordsFromConfig();
        planningEvents = loadPlanning();
    }

    public void reloadPlanning() {
        this.planningEvents = loadPlanning();
    }

    private List<ScheduledEvent> loadPlanning() {
        List<ScheduledEvent> eventsList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Map jour (français) → DayOfWeek ISO (Lundi=1 ... Dimanche=7)
        Map<String, Integer> dayOfWeekMap = new LinkedHashMap<>();
        dayOfWeekMap.put("Lundi",    1);
        dayOfWeekMap.put("Mardi",    2);
        dayOfWeekMap.put("Mercredi", 3);
        dayOfWeekMap.put("Jeudi",    4);
        dayOfWeekMap.put("Vendredi", 5);
        dayOfWeekMap.put("Samedi",   6);
        dayOfWeekMap.put("Dimanche", 7);

        int todayDow = now.getDayOfWeek().getValue(); // 1=Lundi, 7=Dimanche

        for (DayEnum day : DayEnum.values()) {
            String value = day.getValeur();
            Integer dow = dayOfWeekMap.get(value);
            if (dow == null) continue;

            // Calcule la date du prochain (ou actuel) jour de la semaine, dans les 7 prochains jours
            int diff = dow - todayDow;
            if (diff < 0) diff += 7;
            LocalDate date = now.toLocalDate().plusDays(diff);

            HashMap<String, List<String>> dailyEvents = PlanningManager.getDailyEvents(value);

            for (Map.Entry<String, List<String>> entry : dailyEvents.entrySet()) {
                String category = entry.getKey();
                List<String> events = entry.getValue();

                for (String event : events) {
                    try {
                        // format : "16h00|NomEvent"
                        String[] eventDetails = event.split("\\|");
                        String time = eventDetails[0];
                        String eventName = eventDetails[1];

                        String[] parts = time.split("h");
                        int hour   = Integer.parseInt(parts[0]);
                        int minute = Integer.parseInt(parts[1]);

                        LocalDateTime eventTime = LocalDateTime.of(date, java.time.LocalTime.of(hour, minute));
                        eventsList.add(new ScheduledEvent(category, eventName, eventTime));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
        Bukkit.getConsoleSender().sendMessage("[FactionEvent] Planning rechargé : " + eventsList.size() + " événement(s) chargé(s).");
        return eventsList;
    }

    public void startSchedulerForPlanning() {
        new PlanningScheduler().runTaskTimer(this, 0L, 20L * 20L); // toutes les 20 secondes
    }

    private void startRandomGuessScheduler() {
        scheduleNextGuess();
    }

    private void scheduleNextGuess() {
        FileConfiguration guessConfig = FileManager.getGuessDataFC();
        int minDelay = guessConfig.getInt("guess.random_min_delay", 30);
        int maxDelay = guessConfig.getInt("guess.random_max_delay", 90);
        if (minDelay < 1) minDelay = 1;
        if (maxDelay < minDelay) maxDelay = minDelay;
        // Délai aléatoire en ticks (20 ticks = 1 seconde)
        long delayMinutes = minDelay + (long)(Math.random() * (maxDelay - minDelay + 1));
        long delayTicks = delayMinutes * 60L * 20L;
        // Préparer la sélection maintenant et la rendre visible via GuessManager.getLastSelectedWords()
        fr.blixow.factionevent.utils.guess.GuessManager.prepareNextSelection();
        // Enregistrer le timestamp (ms) du prochain lancement
        nextGuessTimestampMillis = System.currentTimeMillis() + (delayMinutes * 60L * 1000L);
        Bukkit.getConsoleSender().sendMessage("[FactionEvent] Prochain Guess aléatoire dans " + delayMinutes + " minutes.");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                // Ne pas lancer si un event est déjà en cours
                if (!eventOn.canStartAnEvent()) {
                    Bukkit.getConsoleSender().sendMessage("[FactionEvent] Guess aléatoire annulé : un event est en cours.");
                    scheduleNextGuess();
                    return;
                }
                // Ne pas lancer si aucun joueur en ligne
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    scheduleNextGuess();
                    return;
                }
                fr.blixow.factionevent.utils.guess.Guess guess = fr.blixow.factionevent.utils.guess.GuessManager.createGuessFromPrepared();
                if (guess == null) {
                    // Fallback
                    guess = fr.blixow.factionevent.utils.guess.GuessManager.loadWordsFromConfig();
                }
                if (guess.getWords().isEmpty()) {
                    Bukkit.getConsoleSender().sendMessage("[FactionEvent] Guess aléatoire annulé : aucun mot disponible.");
                    scheduleNextGuess();
                    return;
                }
                guess.start();
                Bukkit.getConsoleSender().sendMessage("[FactionEvent] Guess aléatoire lancé !");
            } catch (Exception e) {
                e.printStackTrace();
            }
            scheduleNextGuess();
        }, delayTicks);
    }

    // Retourne le timestamp (ms) du prochain Guess planifié, ou -1 si aucun
    public long getNextGuessTimestampMillis() {
        return nextGuessTimestampMillis;
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§cDEBUG: §7Reloading new files");
        FileManager.loadNeededFiles();
        Bukkit.getConsoleSender().sendMessage("§cDEBUG: §7Saving new files");
        GuessManager.saveWordsToConfig(new ArrayList<>());
        FileManager.saveFiles();
        eventOn.cancelEvent();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Désactivation du plugin");
    }

    public static FactionEvent getInstance() {
        return instance;
    }

    public List<ScheduledEvent> getPlanning() {
        return planningEvents;
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

    public void setListLMS(ArrayList<LMS> listLMS) {
        this.listLMS = listLMS;
    }

    public ArrayList<LMS> getListLMS() {
        return listLMS;
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

    public FileConfiguration getLMSFileConfiguration() {
        return lmsFileConfiguration;
    }
    public FileConfiguration getGuessFileConfiguration() {
        return guessFileConfiguration;
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

    public void setLMSFileConfiguration(FileConfiguration lmsFileConfiguration) {
        this.lmsFileConfiguration = lmsFileConfiguration;
    }

    public void setGuessFileConfiguration(FileConfiguration guessFileConfiguration) {
        this.guessFileConfiguration = guessFileConfiguration;
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
