package fr.blixow.factionevent.utils.chat;

import org.bukkit.entity.Player;

/**
 * WordGuesser event
 */
public class WordGuesser {

  private final WordGuesserSettings settings;

  // Timestamps in seconds
  private long startedAt;

  public WordGuesser(WordGuesserSettings settings) {
    this.settings = settings;
  }

  public void start() {
    this.startedAt = System.currentTimeMillis() / 1000L;
  }

  public void stop() {
  }

  public String targetWord() {
    return this.settings.word();
  }

  public boolean isEnded() {
    return ((System.currentTimeMillis() / 1000) - this.startedAt) >= this.settings.time();
  }

  /**
   * Call this method to handle answer from players.
   */
  public boolean checkAnswer(String answer, Player player) {
    if (answer.equalsIgnoreCase(this.targetWord())) {
      // Grant victory to the given player.
      return true;
    }

    return false;
  }
}
