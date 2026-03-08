package fr.blixow.factionevent.utils.guess;

import fr.blixow.factionevent.manager.FileManager;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

public class GuessManager {

    // Dernière sélection préparée pour le prochain Guess (peut être vide)
    private static List<String> lastSelectedWords = new ArrayList<>();

    public static Guess loadWordsFromConfig() {
        FileConfiguration fc = FileManager.getGuessDataFC();
        if (fc.contains("guess.words")) {
            List<String> words = fc.getStringList("guess.words");
            return new Guess(selectRandomWords(new ArrayList<>(words)));
        } else {
            List<String> words = Arrays.asList("pomme", "voiture", "maison");
            saveWordsToConfig(words);
            return new Guess(words);
        }
    }

    /**
     * Prépare une sélection aléatoire et la stocke dans lastSelectedWords.
     * Doit être appelée au moment du scheduling pour que la sélection soit visible avant le lancement.
     */
    public static List<String> prepareNextSelection() {
        FileConfiguration fc = FileManager.getGuessDataFC();
        List<String> words = new ArrayList<>();
        if (fc != null && fc.contains("guess.words")) {
            words = new ArrayList<>(fc.getStringList("guess.words"));
        } else {
            words = Arrays.asList("pomme", "voiture", "maison");
            saveWordsToConfig(words);
        }
        List<String> selection = selectRandomWords(words);
        lastSelectedWords = new ArrayList<>(selection);
        return new ArrayList<>(lastSelectedWords);
    }

    /**
     * Retourne la sélection préparée (non destructrice). Peut retourner une liste vide.
     */
    public static List<String> getLastSelectedWords() {
        return new ArrayList<>(lastSelectedWords);
    }

    /**
     * Crée et retourne une instance Guess à partir de la sélection préparée, ou null si aucune préparation.
     * Après la création, vide la sélection préparée.
     */
    public static Guess createGuessFromPrepared() {
        if (lastSelectedWords == null || lastSelectedWords.isEmpty()) return null;
        Guess g = new Guess(new ArrayList<>(lastSelectedWords));
        lastSelectedWords.clear();
        return g;
    }

    private static List<String> selectRandomWords(List<String> words) {
        if (words.size() < 2) {
            return new ArrayList<>(words); // Retourne tous les mots si la liste contient moins de 2 éléments
        }

        int maxWords = Math.min(5, words.size());
        int numberOfWords = 2 + (int)(Math.random() * (maxWords - 2 + 1)); // Nombre aléatoire de mots entre 2 et maxWords
        Collections.shuffle(words); // Mélange les mots
        return new ArrayList<>(words.subList(0, numberOfWords));
    }

    public static void saveWordsToConfig(List<String> words) {
        File file = FileManager.getFile("data/guess.yml");
        FileConfiguration fc = FileManager.getGuessDataFC();
        fc.set("guess.words", words);
        try {
            fc.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
