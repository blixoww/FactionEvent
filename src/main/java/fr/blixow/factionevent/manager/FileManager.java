package fr.blixow.factionevent.manager;

import fr.blixow.factionevent.FactionEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                saveResourceUtf8(filename, file);
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
                saveResourceUtf8("data/" + filename, file);
                Bukkit.getConsoleSender().sendMessage("§cDEBUG: §7Création du fichier : " + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copie une ressource du JAR vers un fichier en forçant l'encodage UTF-8.
     */
    private static void saveResourceUtf8(String resourcePath, File destination) throws IOException {
        InputStream in = FactionEvent.getInstance().getResource(resourcePath);
        if (in == null) {
            // Ressource introuvable dans le JAR, créer fichier vide
            destination.createNewFile();
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destination), StandardCharsets.UTF_8)) {
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, len);
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
        File file = new File(FactionEvent.getInstance().getDataFolder(), filename);
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            e.printStackTrace();
            return YamlConfiguration.loadConfiguration(file);
        }
    }

    public static FileConfiguration getMessageFileConfiguration() {
        return getFileConfiguration("message.yml");
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

    public static FileConfiguration getLMSDataFC() {
        return FactionEvent.getInstance().getLMSFileConfiguration();
    }

    public static FileConfiguration getGuessDataFC() {
        return FactionEvent.getInstance().getGuessFileConfiguration();
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

    public static FileConfiguration getDominationDataFC() {
        return FactionEvent.getInstance().getDominationFileConfiguration();
    }

    public static FileConfiguration getPurgeRewardsDataFC() {
        return FactionEvent.getInstance().getPurgeRewardsFileConfiguration();
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
        FileManager.createDataFile("lms.yml");
        FileManager.createDataFile("guess.yml", true);
        FileManager.createDataFile("planning.yml", false);
        FileManager.createDataFile("eventManager.yml");
        FileManager.createDataFile("classement.yml");
        FileManager.createDataFile("domination.yml");
        FileManager.createDataFile("purgeRewards.yml");
    }


    public static void loadNeededFiles() {
        try {
            createNeededFiles();
            instance.setConfig(loadUtf8(getFile("config.yml")));
            instance.setMessageFileConfiguration(loadUtf8(getFile("message.yml")));
            instance.setKothFileConfiguration(loadUtf8(getDataFile("koth.yml")));
            instance.setTotemFileConfiguration(loadUtf8(getDataFile("totem.yml")));
            instance.setDtcFileConfiguration(loadUtf8(getDataFile("dtc.yml")));
            instance.setLMSFileConfiguration(loadUtf8(getDataFile("lms.yml")));
            instance.setGuessFileConfiguration(loadUtf8(getDataFile("guess.yml")));
            instance.setPlanningFileConfiguration(loadUtf8(getDataFile("planning.yml")));
            instance.setEventManagerFileConfiguration(loadUtf8(getDataFile("eventManager.yml")));
            instance.setClassementFileConfiguration(loadUtf8(getDataFile("classement.yml")));
            instance.setDominationFileConfiguration(loadUtf8(getDataFile("domination.yml")));
            instance.setPurgeRewardsFileConfiguration(loadUtf8(getDataFile("purgeRewards.yml")));
            LocalDateTime localDateTime = LocalDateTime.now();
            int day = localDateTime.getDayOfMonth(), months = localDateTime.getMonthValue(), year = localDateTime.getYear();
            String dayly_logs = "logs/logs-" + day + "-" + months + "-" + year + ".yml";
            FileManager.createFile(dayly_logs);
            instance.setLogsFileConfiguration(loadUtf8(getFile(dayly_logs)));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static FileConfiguration loadUtf8(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            e.printStackTrace();
            return YamlConfiguration.loadConfiguration(file);
        }
    }

    public static void saveFiles() {
        try {
            saveWithUtf8(instance.getMessageFileConfiguration(), getFile("message.yml"));
            saveWithUtf8(instance.getKothFileConfiguration(), getDataFile("koth.yml"));
            saveWithUtf8(instance.getTotemFileConfiguration(), getDataFile("totem.yml"));
            saveWithUtf8(instance.getDtcFileConfiguration(), getDataFile("dtc.yml"));
            saveWithUtf8(instance.getLMSFileConfiguration(), getDataFile("lms.yml"));
            saveWithUtf8(instance.getGuessFileConfiguration(), getDataFile("guess.yml"));
            saveWithUtf8(instance.getPlanningFileConfiguration(), getDataFile("planning.yml"));
            saveWithUtf8(instance.getEventManagerFileConfiguration(), getDataFile("eventManager.yml"));
            saveWithUtf8(instance.getClassementFileConfiguration(), getDataFile("classement.yml"));
            saveWithUtf8(instance.getDominationFileConfiguration(), getDataFile("domination.yml"));
            saveWithUtf8(instance.getPurgeRewardsFileConfiguration(), getDataFile("purgeRewards.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sauvegarde une FileConfiguration vers un fichier en UTF-8, en conservant les commentaires
     * du fichier existant à leur position d'origine.
     * Méthode publique pour pouvoir être appelée depuis d'autres classes (ex: GuessManager).
     */
    public static void saveConfigToFile(FileConfiguration config, File file) throws IOException {
        saveWithUtf8(config, file);
    }

    private static void saveWithUtf8(FileConfiguration config, File file) throws IOException {
        String newYaml = config.saveToString();

        if (!file.exists()) {
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                w.write(newYaml);
            }
            return;
        }

        // Lecture du fichier existant ligne par ligne
        List<String> existingLines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) existingLines.add(l);
        }

        String[] newLines = newYaml.split("\n", -1);

        // Associe chaque ligne de données existante (non-commentaire, non-vide)
        // à la liste des commentaires qui la précèdent
        LinkedHashMap<String, List<String>> commentsBefore = new LinkedHashMap<>();
        List<String> pending = new ArrayList<>();
        List<String> trailing = new ArrayList<>();

        for (String line : existingLines) {
            String t = line.trim();
            if (t.startsWith("#") || t.isEmpty()) {
                pending.add(line);
            } else {
                commentsBefore.put(line, new ArrayList<>(pending));
                pending.clear();
            }
        }
        trailing.addAll(pending); // commentaires en fin de fichier

        // Reconstruit le fichier en insérant les commentaires avant chaque ligne correspondante du nouveau YAML
        StringBuilder out = new StringBuilder();
        Set<String> usedKeys = new java.util.HashSet<>();

        for (String newLine : newLines) {
            String nt = newLine.trim();
            if (!nt.isEmpty()) {
                // Calcul d'indentation
                int newIndent = newLine.length() - newLine.replaceFirst("^\\s*", "").length();
                String newKey = nt.split(":")[0].trim();

                for (Map.Entry<String, List<String>> entry : commentsBefore.entrySet()) {
                    if (usedKeys.contains(entry.getKey())) continue;
                    String existLine = entry.getKey();
                    String et = existLine.trim();
                    int existIndent = existLine.length() - existLine.replaceFirst("^\\s*", "").length();
                    String existKey = et.split(":")[0].trim();
                    if (newKey.equals(existKey) && newIndent == existIndent) {
                        for (String c : entry.getValue()) out.append(c).append("\n");
                        usedKeys.add(entry.getKey());
                        break;
                    }
                }
            }
            out.append(newLine).append("\n");
        }

        // Commentaires de fin
        for (String c : trailing) out.append(c).append("\n");

        try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            w.write(out.toString());
        }
    }

    public static Plugin getInstance() {
        return instance;
    }
}
