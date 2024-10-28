package fr.blixow.factionevent.utils;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class ScopedTask<T> extends BukkitRunnable {

  private final T param;
  private final Consumer<T> func;

  private ScopedTask(Consumer<T> func, final T param) {
    this.param = param;
    this.func = func;
  }

  @Override
  public void run() {
    this.func.accept(this.param);
  }

  public static <T> ScopedTask<T> of(Consumer<T> func, final T param) {
    return new ScopedTask<T>(func, param);
  }
}
