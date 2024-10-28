package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.FactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

public class FileManager {

    private static final FactionEvent instance = FactionEvent.getInstance();

    public static void createFile(String filename) {
        File file = new File(FactionEvent.getInstance().getDataFolder(), filename);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createDataFile(String filename) {
        File file = new File(FactionEvent.getInstance().getDataFolder(), "data/" + filename);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createFile(String filename, boolean bool) {
        if (!FactionEvent.getInstance().getDataFolder().exists()) {
            FactionEvent.getInstance().getDataFolder().mkdirs();
        }
        File file = new File(FactionEvent.getInstance().getDataFolder(), filename);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                FactionEvent.getInstance().saveResource(filename, bool);
                Bukkit.getConsoleSender().sendMessage("§cDEBUG: §7Création du fichier : " + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void createDataFile(String filename, boolean bool) {
        if (!FactionEvent.getInstance().getDataFolder().exists()) {
            FactionEvent.getInstance().getDataFolder().mkdirs();
        }
        File file = new File(FactionEvent.getInstance().getDataFolder(), "data/" + filename);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                FactionEvent.getInstance().saveResource("data/" + filename, bool);
                Bukkit.getConsoleSender().sendMessage("§cDEBUG: §7Création du fichier : " + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static File getFile(String filename) {
        return new File(FactionEvent.getInstance().getDataFolder(), filename);
    }

    public static File getLogsFile() {
        LocalDateTime localDateTime = LocalDateTime.now();
        int day = localDateTime.getDayOfMonth(), months = localDateTime.getMonthValue(), year = localDateTime.getYear();
        String dayly_logs = "logs/logs-" + day + "-" + months + "-" + year + ".yml";
        return new File(FactionEvent.getInstance().getDataFolder(), dayly_logs);
    }

    public static File getDataFile(String filename) {
        return new File(FactionEvent.getInstance().getDataFolder(), "data/" + filename);
    }

    public static FileConfiguration getFileConfiguration(String filename) {
        return YamlConfiguration.loadConfiguration(new File(FactionEvent.getInstance().getDataFolder(), filename));
    }

    public static FileConfiguration getMessageFileConfiguration() {
        return FactionEvent.getInstance().getMessageFileConfiguration();
    }

    public static FileConfiguration getKothDataFC() {
        return FactionEvent.getInstance().getKothFileConfiguration();
    }

    public static FileConfiguration getDtcDataFC() {
        return FactionEvent.getInstance().getDtcFileConfiguration();
    }

    public static FileConfiguration getTotemDataFC() {
        return FactionEvent.getInstance().getTotemFileConfiguration();
    }

    public static FileConfiguration getMeteoriteDataFC() {
        return FactionEvent.getInstance().getMeteoriteFileConfiguration();
    }

    public static FileConfiguration getPlanningDataFC() {
        return FactionEvent.getInstance().getPlanningFileConfiguration();
    }

    public static FileConfiguration getEventManagerFC() {
        return FactionEvent.getInstance().getEventManagerFileConfiguration();
    }

    public static FileConfiguration getClassementFC() {
        return FactionEvent.getInstance().getClassementFileConfiguration();
    }

    public static FileConfiguration getChatEventDataFC() {
        return FactionEvent.getInstance().getChatEventFileConfiguration();
    }

    public static FileConfiguration getLogsFC() {
        return FactionEvent.getInstance().getLogsFileConfiguration();
    }

    public static FileConfiguration getConfig() {
        return FactionEvent.getInstance().getFileConfig();
    }

    public static void createNeededFiles() {
        FactionEvent.getInstance().saveDefaultConfig();
        FileManager.createFile("config.yml", true);
        FileManager.createFile("message.yml", true);
        FileManager.createDataFile("koth.yml");
        FileManager.createDataFile("totem.yml");
        FileManager.createDataFile("dtc.yml");
        FileManager.createDataFile("meteorite.yml");
        FileManager.createDataFile("chatEvent.yml");
        FileManager.createDataFile("planning.yml", false);
        FileManager.createDataFile("eventManager.yml");
        FileManager.createDataFile("classement.yml");
    }


    public static void loadNeededFiles() {
        try {
            createNeededFiles();
            instance.setConfig(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "config.yml")));
            instance.setMessageFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "message.yml")));
            instance.setKothFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/koth.yml")));
            instance.setTotemFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/totem.yml")));
            instance.setDtcFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/dtc.yml")));
            instance.setMeteoriteFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/meteorite.yml")));
            instance.setChatEventFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/chatEvent.yml")));
            instance.setPlanningFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/planning.yml")));
            instance.setEventManagerFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/eventManager.yml")));
            instance.setClassementFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), "data/classement.yml")));
            LocalDateTime localDateTime = LocalDateTime.now();
            int day = localDateTime.getDayOfMonth(), months = localDateTime.getMonthValue(), year = localDateTime.getYear();
            String dayly_logs = "logs/logs-" + day + "-" + months + "-" + year + ".yml";
            FileManager.createFile(dayly_logs);
            instance.setLogsFileConfiguration(YamlConfiguration.loadConfiguration(new File(instance.getDataFolder(), dayly_logs)));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void saveFiles() {
        try {
            instance.getFileConfig().save(getFile("config.yml"));
            instance.getMessageFileConfiguration().save(getFile("message.yml"));
            instance.getKothFileConfiguration().save(getDataFile("koth.yml"));
            instance.getTotemFileConfiguration().save(getDataFile("totem.yml"));
            instance.getDtcFileConfiguration().save(getDataFile("dtc.yml"));
            instance.getMeteoriteFileConfiguration().save(getDataFile("meteorite.yml"));
            instance.getChatEventFileConfiguration().save(getDataFile("chatEvent.yml"));
            instance.getPlanningFileConfiguration().save(getDataFile("planning.yml"));
            instance.getEventManagerFileConfiguration().save(getDataFile("eventManager.yml"));
            instance.getClassementFileConfiguration().save(getDataFile("classement.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
