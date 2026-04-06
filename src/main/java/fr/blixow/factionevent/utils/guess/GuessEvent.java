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
    private boolean hintGiven = false; // indice déjà envoyé pour le mot en cours
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
        hintGiven = false; // réinitialiser l'indice pour le mot suivant
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
        // Affiche maintenant également la commande pour répondre (/answer)
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("guess.word_message", "§7Mot §e{word} §7({current}/{total})"))
                .reWord(currentScrambledWord)
                .reCustom("{current}", String.valueOf(wordNumber))
                .reCustom("{total}", String.valueOf(totalWords))
                .toString());
        String answerHint = msg.getString("guess.answer_hint", "§7Répondez avec §e/answer <mot>");
        Bukkit.broadcastMessage(prefix + answerHint);
    }

    private void resetTimer() {
        startTime = System.currentTimeMillis();
    }

    public boolean checkTimer() {
        if (finished || won) return false;
        long elapsed = System.currentTimeMillis() - startTime;

        // Envoyer l'indice à la moitié du temps si personne n'a trouvé
        if (!hintGiven && elapsed >= duration / 2) {
            hintGiven = true;
            showHint();
        }

        if (elapsed >= duration) {
            String correctWord = guess.getWords().get(currentWordIndex);
            String timeUpMsg = new StrManager(msg.getString("guess.time_up", "§cTemps écoulé ! Le mot était : §e{word}"))
                    .reWord(correctWord).toString();
            Bukkit.broadcastMessage(prefix + timeUpMsg);
            nextWord();
            return true;
        }
        return false;
    }

    /**
     * Envoie un indice global : les 2 premières lettres du mot correct dans l'ordre,
     * le reste masqué par des tirets.
     */
    private void showHint() {
        String correctWord = guess.getWords().get(currentWordIndex);
        int len = correctWord.length();

        // Construire l'indice : 2 premières lettres + tirets pour le reste
        StringBuilder hint = new StringBuilder();
        int revealCount = Math.min(2, len);
        for (int i = 0; i < len; i++) {
            if (i < revealCount) {
                hint.append("§e").append(correctWord.charAt(i)).append("§7");
            } else {
                hint.append("_");
            }
            if (i < len - 1) hint.append(" ");
        }

        String hintMsg = new StrManager(
                msg.getString("guess.hint",
                    "§6💡 Indice : §7le mot commence par §e{hint} §7({length} lettres)"))
                .reCustom("{hint}", hint.toString())
                .reCustom("{length}", String.valueOf(len))
                .toString();
        Bukkit.broadcastMessage(prefix + hintMsg);
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
