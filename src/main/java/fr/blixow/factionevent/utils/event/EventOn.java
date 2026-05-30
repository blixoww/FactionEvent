package fr.blixow.factionevent.utils.event;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.domination.Domination;
import fr.blixow.factionevent.utils.domination.DominationEvent;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCEvent;
import fr.blixow.factionevent.utils.guess.Guess;
import fr.blixow.factionevent.utils.guess.GuessEvent;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.lms.LMS;
import fr.blixow.factionevent.utils.lms.LMSEvent;
import fr.blixow.factionevent.utils.purge.Purge;
import fr.blixow.factionevent.utils.purge.PurgeEvent;
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
    private DominationEvent dominationEvent;
    private PurgeEvent purgeEvent;
    private ArrayList<Object> queue;
    private final FileConfiguration msg;

    public EventOn() {
        this.msg = FileManager.getMessageFileConfiguration();
        this.kothEvent = null;
        this.totemEvent = null;
        this.dtcEvent = null;
        this.lmsEvent = null;
        this.guessEvent = null;
        this.dominationEvent = null;
        this.purgeEvent = null;
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
                            } else if (o instanceof Domination) {
                                Domination domination = (Domination) o;
                                domination.start();
                            } else if (o instanceof Purge) {
                                Purge purge = (Purge) o;
                                purge.start();
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
        return kothEvent == null && totemEvent == null && dtcEvent == null && lmsEvent == null && guessEvent == null && dominationEvent == null && purgeEvent == null;
    }

    public void start(KOTH koth, Player... players) {
        if (this.canStartAnEvent()) {
            this.kothEvent = new KOTHEvent(koth);
            String kothName = koth.getName() == null ? "KOTH NULL" : koth.getName();
            String message = new StrManager(msg.getString("koth.started")).reKoth(kothName).toString();
            Bukkit.broadcastMessage(addProportionalLines(message));

            // Title global au démarrage (comme la Domination)
            FactionMessageTitle.sendPlayersTitle(20, 40, 20, "§c§l♛ KOTH", "§7Prenez le contrôle de la zone !");

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

            // Title global au démarrage (comme la Domination)
            FactionMessageTitle.sendPlayersTitle(20, 40, 20, "§c§l⛏ TOTEM", "§7Détruisez la tour adverse !");

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
            // Le LMS gère ses propres broadcasts (registration_started, lms.started, etc.)
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

            // Title global au démarrage (comme la Domination)
            FactionMessageTitle.sendPlayersTitle(20, 40, 20, "§c§l❓ GUESS", "§7Trouvez le mot le plus vite possible !");

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

    public void start(Domination domination, Player... players) {
        if (this.canStartAnEvent()) {
            this.dominationEvent = new DominationEvent(domination);
            FileConfiguration config = FileManager.getConfig();
            // Lire msg fraîchement pour garantir que {score_to_win} est bien disponible
            FileConfiguration freshMsg = FileManager.getMessageFileConfiguration();
            int check_time = 2;
            try {
                if (config.contains("domination.check_time")) {
                    check_time = Math.max(1, config.getInt("domination.check_time"));
                }
            } catch (Exception ignored) {}
            final int finalCheckTime = check_time;

            String startedMsg = freshMsg.getString("domination.started",
                "§8§m-----------------------------------------------------\n"
                + "§r §8< §6§lDOMINATION §8> §8§m-----------------------------------------------------\n"
                + "§7⚔ La Domination commence ! Capturez les zones !\n"
                + "§7Zones actives : §c" + domination.getActiveZones().size() + "\n"
                + "§8§m-----------------------------------------------------");
            // Remplacer les placeholders
            try {
                int scoreToWinCfg = config.getInt("domination.score_to_win", 150);
                if (startedMsg != null) {
                    startedMsg = startedMsg
                        .replace("{score_to_win}", String.valueOf(scoreToWinCfg))
                        .replace("{zones}", String.valueOf(domination.getActiveZones().size()));
                }
            } catch (Exception ignored) {}
            Bukkit.broadcastMessage(addProportionalLines(startedMsg));

            FactionMessageTitle.sendPlayersTitle(20, 40, 20, "§c§l⚔ DOMINATION", "§7Capturez les zones !");

            // Runnable 1 sec : capture + action bar
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dominationEvent == null) { cancel(); return; }
                    dominationEvent.updateScoreboard();
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, 20L);

            // Runnable timer check
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dominationEvent == null) { cancel(); return; }
                    if (dominationEvent.checkTimer()) { cancel(); }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, finalCheckTime * 20L);
            return;
        }
        String queueMessage = msg.getString("domination.prefix", "§8[§cDOMINATION§8]§7 ")
            + msg.getString("domination.adding_to_queue", "§7Un événement est déjà en cours. La Domination sera lancée automatiquement ensuite.");
        FactionMessageTitle.sendPlayersMessage(addProportionalLines(queueMessage), players);
        queue.add(domination);
    }

    public void start(Purge purge, Player... players) {
        if (this.canStartAnEvent()) {
            this.purgeEvent = new PurgeEvent();
            FileConfiguration freshMsg = FileManager.getMessageFileConfiguration();
            FileConfiguration config = FileManager.getConfig();
            int check_time = 2;
            try {
                if (config.contains("purge.check_time")) {
                    check_time = Math.max(1, config.getInt("purge.check_time"));
                }
            } catch (Exception ignored) {}
            final int finalCheckTime = check_time;

            int duration = config.getInt("purge.max_duration", 1800);
            String startedMsg = freshMsg.getString("purge.started",
                "§8§m-----------------------------------------------------\n"
                + "§r §8< §c§lPURGE §8> §8§m-----------------------------------------------------\n"
                + "§7La Purge commence ! Toutes les portes sont ouvertes !\n"
                + "§7Chaque kill rapporte de l'argent et des items §8(§7/purgereward§8)\n"
                + "§7Durée : §c{duration} §8| §7Top 5 récompensé en fin d'event\n"
                + "§8§m-----------------------------------------------------");
            try {
                int minutes = duration / 60;
                String dur = (minutes > 0 ? minutes + "m" : duration + "s");
                if (startedMsg != null) {
                    startedMsg = startedMsg
                        .replace("{duration}", dur)
                        .replace("{seconds}", String.valueOf(duration));
                }
            } catch (Exception ignored) {}
            Bukkit.broadcastMessage(addProportionalLines(startedMsg));

            FactionMessageTitle.sendPlayersTitle(20, 40, 20,
                "§c§lPURGE", "§7Toutes les portes sont ouvertes !");

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (purgeEvent == null) { cancel(); return; }
                    purgeEvent.updateScoreboard();
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, 20L);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (purgeEvent == null) { cancel(); return; }
                    if (purgeEvent.checkTimer()) { cancel(); }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, finalCheckTime * 20L);
            return;
        }
        String queueMessage = msg.getString("purge.prefix", "§8[§cPURGE§8]§7 ")
            + msg.getString("purge.adding_to_queue",
                "§7Un événement est déjà en cours. La Purge sera lancée automatiquement ensuite.");
        FactionMessageTitle.sendPlayersMessage(addProportionalLines(queueMessage), players);
        queue.add(purge);
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
            if (dominationEvent != null) {
                new Domination(dominationEvent.getDomination().getActiveZones()).stop();
            }
            if (purgeEvent != null) {
                new Purge().stop();
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

    public DominationEvent getDominationEvent() {
        return dominationEvent;
    }

    public void setDominationEvent(DominationEvent dominationEvent) {
        this.dominationEvent = dominationEvent;
    }

    public PurgeEvent getPurgeEvent() {
        return purgeEvent;
    }

    public void setPurgeEvent(PurgeEvent purgeEvent) {
        this.purgeEvent = purgeEvent;
    }

    private String addProportionalLines(String message) {
        String lineSeparator = "\n";
        return lineSeparator + message + lineSeparator;
    }
}
