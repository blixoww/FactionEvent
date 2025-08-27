package fr.blixow.factionevent.utils.guess;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.RankingManager;
import fr.blixow.factionevent.manager.StrManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;

public class GuessEvent {
    private Guess guess;
    private boolean won = false;
    private long startTime;
    private int currentWordIndex = 0;
    private final long duration = 120 * 1000; // 2 minutes par mot
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.getString("guess.prefix");

    public GuessEvent(Guess guess) {
        this.guess = guess;
        this.startTime = System.currentTimeMillis();
        this.showCurrentWord();
    }

    public void checkGuess(Player player, String guess) {
        String correctAnswer = this.guess.getWords().get(currentWordIndex);

        if (guess.equalsIgnoreCase(correctAnswer)) {
            grantVictory(player);
        } else {
            player.sendMessage(prefix + msg.getString("guess.incorrect"));
        }
    }

    private void grantVictory(Player player) {
        won = true;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("guess.correct"))
                .rePlayer(player).reWord(String.valueOf(guess.getWords().get(currentWordIndex))).toString());
        RankingManager.addPoints(faction, 1);
        nextWord();
    }

    public void nextWord() {
        currentWordIndex++;
        won = false;
        if (currentWordIndex < guess.getWords().size()) {
            showCurrentWord();
            resetTimer();
        } else {
            finishingGuess();
        }
    }

    private void finishingGuess() {
        Bukkit.broadcastMessage(prefix + msg.getString("guess.ended"));
        guess.stop();
    }

    private void showCurrentWord() {
        String currentScrambledWord = guess.getScrambledWords().get(currentWordIndex);
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("guess.word_message"))
                .reWord(currentScrambledWord).toString());
    }

    private void resetTimer() {
        startTime = System.currentTimeMillis();
    }

    public boolean checkTimer() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - startTime) >= duration && !won) {
            Bukkit.broadcastMessage(prefix + msg.getString("guess.time_up"));
            nextWord();
        }
        return false;
    }

    public void setCurrentWordIndex(int i) {
        this.currentWordIndex = i;
    }

    public Guess getGuess() {
        return guess;
    }

    public int getCurrentWordIndex() {
        return currentWordIndex;
    }
}
