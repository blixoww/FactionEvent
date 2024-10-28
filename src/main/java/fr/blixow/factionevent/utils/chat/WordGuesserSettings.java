package fr.blixow.factionevent.utils.chat;

public class WordGuesserSettings {

  // 120 seconds by default.
  private int time = 120;
  private final String word;

  private WordGuesserSettings(String word) {
    if (word == null || word.isEmpty())
      throw new IllegalArgumentException("Word mustn't be null or empty.");

    this.word = word;
  }

  public WordGuesserSettings time(int seconds) {
    this.time = seconds;
    return this;
  }

  public int time() {
    return this.time;
  }

  public String word() {
    return this.word;
  }

  public static WordGuesserSettings create(String word) {
    return new WordGuesserSettings(word);
  }
}
