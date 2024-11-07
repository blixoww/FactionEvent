package fr.blixow.factionevent.utils.guess;

import fr.blixow.factionevent.manager.FileManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

public class GuessManager {

    public static Guess loadWordsFromConfig() {
        FileConfiguration fc = FileManager.getGuessDataFC();
        if (fc.contains("guess.words")) {
            List<String> words = fc.getStringList("guess.words");
            return new Guess(selectRandomWords(words));
        } else {
            List<String> words = Arrays.asList("pomme", "voiture", "maison");
            saveWordsToConfig(words);
            return new Guess(words);
        }
    }

    private static List<String> selectRandomWords(List<String> words) {
        int maxWords = Math.min(5, words.size()); // Limite maximale de 5 mots
        int numberOfWords = 2 + (int)(Math.random() * (maxWords - 1)); // Nombre aléatoire de mots entre 2 et maxWords
        Collections.shuffle(words); // Mélange les mots
        return words.subList(0, numberOfWords); // Sélectionne le sous-ensemble
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
