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

public class GuessEvent {
    private final Guess guess;
    private boolean won = false;
    private long startTime;
    private int currentWordIndex = 0;
    private boolean finished = false;
    private final long duration = 120 * 1000; // 2 minutes par mot
    private final FileConfiguration msg = FileManager.getMessageFileConfiguration();
    private final String prefix = msg.getString("guess.prefix", "§8[§eGuess§8] §7");

    public GuessEvent(Guess guess) {
        this.guess = guess;
        this.startTime = System.currentTimeMillis();
        this.showCurrentWord();
    }

    public void checkGuess(Player player, String answer) {
        if (finished) return;
        String correctAnswer = this.guess.getWords().get(currentWordIndex);

        if (answer.equalsIgnoreCase(correctAnswer)) {
            grantVictory(player);
        } else {
            player.sendMessage(prefix + msg.getString("guess.incorrect", "§cMauvaise réponse !"));
        }
    }

    private void grantVictory(Player player) {
        won = true;
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        String correctWord = guess.getWords().get(currentWordIndex);
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("guess.correct", "§a{player} a trouvé le mot §e{word}§a !"))
                .rePlayer(player).reWord(correctWord).toString());
        if (!faction.isWilderness()) {
            RankingManager.addPoints(faction, 1);
        }
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
        if (finished) return;
        finished = true;
        Bukkit.broadcastMessage(prefix + msg.getString("guess.ended", "§6Le Guess est terminé !"));
        guess.stop();
    }

    private void showCurrentWord() {
        String currentScrambledWord = guess.getScrambledWords().get(currentWordIndex);
        int wordNumber = currentWordIndex + 1;
        int totalWords = guess.getWords().size();
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("guess.word_message", "§7Mot §e{word} §7({current}/{total})"))
                .reWord(currentScrambledWord)
                .reCustom("{current}", String.valueOf(wordNumber))
                .reCustom("{total}", String.valueOf(totalWords))
                .toString());
    }

    private void resetTimer() {
        startTime = System.currentTimeMillis();
    }

    public boolean checkTimer() {
        if (finished || won) return false;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - startTime) >= duration) {
            String correctWord = guess.getWords().get(currentWordIndex);
            String timeUpMsg = new StrManager(msg.getString("guess.time_up", "§cTemps écoulé ! Le mot était : §e{word}"))
                    .reWord(correctWord).toString();
            Bukkit.broadcastMessage(prefix + timeUpMsg);
            nextWord();
            return true;
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

    public boolean isFinished() {
        return finished;
    }
}
