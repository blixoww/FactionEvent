package fr.blixow.factionevent.utils.event;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCEvent;
import fr.blixow.factionevent.utils.guess.Guess;
import fr.blixow.factionevent.utils.guess.GuessEvent;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSEvent;
import fr.blixow.factionevent.utils.totem.Totem;
import fr.blixow.factionevent.utils.totem.TotemEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class EventOn {
    private KOTHEvent kothEvent;
    private TotemEvent totemEvent;
    private DTCEvent dtcEvent;
    private LMSEvent lmsEvent;
    private GuessEvent guessEvent;
    private ArrayList<Object> queue;
    private final FileConfiguration msg;

    public EventOn() {
        this.msg = FileManager.getMessageFileConfiguration();
        this.kothEvent = null;
        this.totemEvent = null;
        this.dtcEvent = null;
        this.lmsEvent = null;
        this.guessEvent = null;
        this.queue = new ArrayList<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!queue.isEmpty()) {
                    try {
                        Object o = queue.get(0);
                        if (canStartAnEvent()) {
                            if (o instanceof KOTH) {
                                KOTH koth = (KOTH) o;
                                koth.start();
                            } else if (o instanceof Totem) {
                                Totem totem = (Totem) o;
                                totem.start();
                            } else if (o instanceof DTC) {
                                DTC dtc = (DTC) o;
                                dtc.start();
                            } else if (o instanceof LMS) {
                                LMS lms = (LMS) o;
                                lms.startRegistration();
                            } else if (o instanceof Guess) {
                                Guess guess = (Guess) o;
                                guess.start();
                            }
                            queue.remove(0);
                        }
                    } catch (Exception exception) {
                        queue.remove(0);
                        exception.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(FactionEvent.getInstance(), 0L, 2 * 20L);
    }

    public boolean canStartAnEvent() {
        return kothEvent == null && totemEvent == null && dtcEvent == null && lmsEvent == null && guessEvent == null;
    }

    public void start(KOTH koth, Player... players) {
        if (this.canStartAnEvent()) {
            this.kothEvent = new KOTHEvent(koth);
            String kothName = koth.getName() == null ? "KOTH NULL" : koth.getName();
            String message = new StrManager(msg.getString("koth.started")).reKoth(kothName).toString();
            Bukkit.broadcastMessage(addProportionalLines(message));
            FileConfiguration configuration = FileManager.getConfig();
            int check_time = 2;
            try {
                if (configuration.contains("koth.check_time")) {
                    check_time = configuration.getInt("koth.check_time");
                }
            } catch (Exception ignored) {}
            check_time = check_time > 0 ? check_time : 1;
            final int finalCheckTime = check_time;

            // Runnable action bar : toutes les secondes
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (kothEvent == null) { cancel(); return; }
                    kothEvent.updateScoreboard();
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, 20L);

            // Runnable vérification fin d'event
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (kothEvent == null) { cancel(); return; }
                    if (kothEvent.checkTimer()) { koth.stop(); cancel(); }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, finalCheckTime * 20L);
            return;
        }
        String queueMessage = msg.getString("koth.prefix") + new StrManager(msg.getString("koth.adding_to_queue")).reKoth(koth.getName()).toString();
        FactionMessageTitle.sendPlayersMessage(addProportionalLines(queueMessage), players);
        queue.add(koth);
    }

    public void start(DTC dtc, Player... players) {
        FileConfiguration configuration = FileManager.getConfig();
        int check_time = 10;
        try {
            if (configuration.contains("dtc.check_time")) {
                check_time = configuration.getInt("dtc.check_time");
            }
        } catch (Exception ignored) {}
        check_time = check_time > 0 ? check_time : 1;
        final int finalCheckTime = check_time;
        if (canStartAnEvent()) {
            this.dtcEvent = new DTCEvent(dtc);

            String message = new StrManager(msg.getString("dtc.started")).reDTC(dtc.getName()).toString();
            Bukkit.broadcastMessage(addProportionalLines(message));

            // Envoyer un Title global configuré (si activé dans message.yml)
            try {
                String title = new StrManager(msg.getString("dtc.title.title", "§aNexus en cours")).reDTC(dtc.getName()).toString();
                String subtitle = new StrManager(msg.getString("dtc.title.subtitle", "§7Préparez-vous au combat")).reDTC(dtc.getName()).toString();
                FactionMessageTitle.sendPlayersTitle(20, 40, 20, title, subtitle);
            } catch (Exception ignored) {}

            // Runnable action bar : toutes les secondes
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dtcEvent == null) { cancel(); return; }
                    dtcEvent.updateScoreboard();
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, 20L);

            // Runnable vérification fin d'event
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dtcEvent == null) { cancel(); return; }
                    if (dtcEvent.checkTimer()) { dtc.stop(); cancel(); }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, finalCheckTime * 20L);
            return;
        }
        String queueMessage = msg.getString("dtc.prefix") + new StrManager(msg.getString("dtc.adding_to_queue")).reDTC(dtc.getName()).toString();
        FactionMessageTitle.sendPlayersMessage(addProportionalLines(queueMessage), players);
        queue.add(dtc);
    }

    public void start(Totem totem, Player... players) {
        if (canStartAnEvent()) {
            this.totemEvent = new TotemEvent(totem);
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String str = new StrManager(msg.getString("totem.started")).reTotem(totem.getName()).toString();
            Bukkit.broadcastMessage(addProportionalLines(str));
            totemEvent.start();
            FileConfiguration configuration = FileManager.getConfig();
            int check_time = 10;
            try {
                if (configuration.contains("totem.check_time")) {
                    check_time = configuration.getInt("totem.check_time");
                }
            } catch (Exception ignored) {}
            check_time = check_time > 0 ? check_time : 1;
            final int finalCheckTime = check_time;

            // Runnable action bar : toutes les secondes
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (totemEvent == null) { cancel(); return; }
                    totemEvent.updateScoreboard();
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, 20L);

            // Runnable vérification fin d'event
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (totemEvent == null) { cancel(); return; }
                        if (totemEvent.checkTimer()) { cancel(); }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, finalCheckTime * 20L);
            return;
        }
        String queueMessage = msg.getString("totem.prefix") + new StrManager(msg.getString("totem.adding_to_queue")).reTotem(totem.getName()).toString();
        FactionMessageTitle.sendPlayersMessage(addProportionalLines(queueMessage), players);
        queue.add(totem);
    }

    public void start(LMS lms, Player... players) {
        if (this.canStartAnEvent()) {
            this.lmsEvent = new LMSEvent(lms);
            String message = new StrManager(msg.getString("lms.started")).reLMS(lms.getName()).toString();
            Bukkit.broadcastMessage(addProportionalLines(message));
            int check_time = 10;
            try {
                FileConfiguration configuration = FileManager.getConfig();
                if (configuration.contains("lms.check_time")) {
                    check_time = Math.max(1, configuration.getInt("lms.check_time"));
                }
            } catch (Exception ignored) {}
            final int finalCheckTime = check_time;

            // Runnable action bar : toutes les secondes
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (lmsEvent == null) { cancel(); return; }
                    lmsEvent.updateScoreboard();
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, 20L);

            // Runnable vérification fin d'event
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (lmsEvent == null) { cancel(); return; }
                    if (lmsEvent.isEventActive() && lmsEvent.checkTimer()) { lms.stop(); cancel(); }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, finalCheckTime * 20L);
            return;
        }
        String queueMessage = msg.getString("lms.prefix") + new StrManager(msg.getString("lms.adding_to_queue")).reLMS(lms.getName()).toString();
        FactionMessageTitle.sendPlayersMessage(addProportionalLines(queueMessage), players);
        queue.add(lms);
    }

    public void start(Guess guess, Player... players) {
        if (this.canStartAnEvent()) {
            // Vérifie que les mots ont été correctement chargés
            if (guess.getWords().isEmpty()) {
                Bukkit.broadcastMessage(msg.getString("guess.prefix", "") + new StrManager(msg.getString("guess.no_words", "§cAucun mot disponible.")).toString());
                return;
            }

            String message = new StrManager(msg.getString("guess.started", "§aUn Guess démarre !")).toString();
            Bukkit.broadcastMessage(addProportionalLines(message));
            // Créer le GuessEvent ici — source unique de création
            this.guessEvent = new GuessEvent(guess);

            // Tâche qui vérifie le timer toutes les secondes
            int taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    if (guessEvent == null) {
                        cancel();
                    } else {
                        guessEvent.checkTimer();
                    }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, 20L).getTaskId();

            // Stocker l'ID de tâche dans Guess pour pouvoir l'annuler au stop()
            guess.setTaskId(taskId);
        } else {
            queue.add(guess);
            String queueMessage = msg.getString("guess.prefix", "") + new StrManager(msg.getString("guess.adding_to_queue", "§7Ajouté à la file.")).toString();
            FactionMessageTitle.sendPlayersMessage(addProportionalLines(queueMessage), players);
        }
    }

    public void stopCurrentEvent() {
        if (!canStartAnEvent()) {
            if (kothEvent != null) {
                kothEvent.getKoth().stop();
            }
            if (totemEvent != null) {
                totemEvent.getTotem().stop();
            }
            if (dtcEvent != null) {
                dtcEvent.getDtc().stop();
            }
            if (lmsEvent != null) {
                lmsEvent.getLMS().stop();
            }
            if (guessEvent != null) {
                guessEvent.getGuess().stop();
            }
        }
    }

    public void cancelEvent() {
        try {
            queue = new ArrayList<>();
            stopCurrentEvent();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public ArrayList<Object> getQueue() {
        return queue;
    }

    public DTCEvent getDtcEvent() {
        return dtcEvent;
    }

    public KOTHEvent getKothEvent() {
        return kothEvent;
    }

    public TotemEvent getTotemEvent() {
        return totemEvent;
    }

    public LMSEvent getLMSEvent() {
        return lmsEvent;
    }

    public GuessEvent getGuessEvent() {
        return guessEvent;
    }

    public void setKothEvent(KOTHEvent kothEvent) {
        this.kothEvent = kothEvent;
    }

    public void setTotemEvent(TotemEvent totemEvent) {
        this.totemEvent = totemEvent;
    }

    public void setDtcEvent(DTCEvent dtcEvent) {
        this.dtcEvent = dtcEvent;
    }

    public void setGuessEvent(GuessEvent guessEvent) {
        this.guessEvent = guessEvent;
    }

    public void setLMSEvent(LMSEvent lmsEvent) {
        this.lmsEvent = lmsEvent;
    }

    public void setQueue(ArrayList<Object> queue) {
        this.queue = queue;
    }

    private String addProportionalLines(String message) {
        String lineSeparator = "\n";
        return lineSeparator + message + lineSeparator;
    }
}
