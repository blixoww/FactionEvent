package fr.blixow.factionevent.utils.guess;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.utils.event.EventOn;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Guess {
    private final List<String> words;
    private final List<String> scrambledWords;
    private final FileConfiguration msg = FactionEvent.getInstance().getMessageFileConfiguration();
    private final String prefix = msg.getString("guess.prefix");

    // Constructeur privé pour initialiser l'instance Guess
    public Guess(List<String> words) {
        this.words = words;
        this.scrambledWords = scrambleWords(words);
    }

    public List<String> getWords() {
        return words;
    }

    public List<String> getScrambledWords() {
        return scrambledWords;
    }

    private List<String> scrambleWords(List<String> inputWords) {
        List<String> scrambledList = new ArrayList<>();
        for (String word : inputWords) {
            List<Character> characters = new ArrayList<>();
            for (char c : word.toCharArray()) {
                characters.add(c);
            }
            Collections.shuffle(characters);
            StringBuilder scrambled = new StringBuilder();
            for (char c : characters) {
                scrambled.append(c);
            }
            scrambledList.add(scrambled.toString());
        }
        return scrambledList;
    }

    public void start(Player... players) {
        if (FactionEvent.getInstance().getEventOn().getGuessEvent() != null) {
            for (Player player : players) {
                player.sendMessage(prefix + msg.getString("guess.already_started"));
            }
            return;
        }
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        GuessEvent guessEvent = new GuessEvent(this);
        eventOn.setGuessEvent(guessEvent);
        eventOn.start(this, players);
        Bukkit.getScheduler().runTaskTimer(FactionEvent.getInstance(), () -> {
            GuessEvent currentEvent = FactionEvent.getInstance().getEventOn().getGuessEvent();
            if (currentEvent != null) {
                if (currentEvent.checkTimer()) {
                    currentEvent.nextWord();
                }
            }
        }, 20L, 20L);
    }

    public void stop() {
        EventOn eventOn = FactionEvent.getInstance().getEventOn();
        if (eventOn.getGuessEvent() != null) {
            eventOn.getGuessEvent().setCurrentWordIndex(0);
            eventOn.setGuessEvent(null);
        }
    }
}
