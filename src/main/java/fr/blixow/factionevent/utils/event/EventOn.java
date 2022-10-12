package fr.blixow.factionevent.utils.event;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.dtc.DTC;
import fr.blixow.factionevent.utils.dtc.DTCEvent;
import fr.blixow.factionevent.utils.koth.KOTH;
import fr.blixow.factionevent.utils.koth.KOTHEvent;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.meteorite.Meteorite;
import fr.blixow.factionevent.utils.meteorite.MeteoriteEvent;
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
    private MeteoriteEvent meteoriteEvent;
    private ArrayList<Object> queue;
    private final FileConfiguration msg;

    public EventOn() {
        this.msg = FileManager.getMessageFileConfiguration();
        this.kothEvent = null;
        this.totemEvent = null;
        this.dtcEvent = null;
        this.meteoriteEvent = null;
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
                            } else if (o instanceof Meteorite) {
                                Meteorite meteorite = (Meteorite) o;
                                meteorite.start();
                            } else {
                                System.out.println("§cEvent started ?");
                            }
                            queue.remove(0);
                            System.out.println("§cEvent started ?");
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
        return kothEvent == null && totemEvent == null && dtcEvent == null && meteoriteEvent == null;
    }

    public void start(KOTH koth, Player... players) {
        if (this.canStartAnEvent()) {
            this.kothEvent = new KOTHEvent(koth);
            String kothName = koth.getName() == null ? "KOTH NULL" : koth.getName();
            Bukkit.broadcastMessage(msg.getString("koth.prefix") + new StrManager(msg.getString("koth.started")).reKoth(kothName));
            FileConfiguration configuration = FileManager.getConfig();
            int check_time = 2;
            try {
                if (configuration.contains("koth.check_time")) {
                    check_time = configuration.getInt("koth.check_time");
                }
            } catch (Exception ignored) {
            }
            check_time = check_time > 0 ? check_time : 1;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (getKothEvent() == null) {
                        koth.stop();
                        cancel();
                    } else if (kothEvent.checkTimer()) {
                        koth.stop();
                        cancel();
                    } else {
                        kothEvent.updateScoreboard();
                    }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, check_time * 20L);
            return;
        }
        FactionMessageTitle.sendPlayersMessage(msg.getString("koth.prefix") + new StrManager(msg.getString("koth.adding_to_queue")).reKoth(koth.getName()).toString(), players);
        queue.add(koth);
    }

    public void start(DTC dtc, Player... players) {
        FileConfiguration configuration = FileManager.getConfig();
        int check_time = 10;
        try {
            if (configuration.contains("dtc.check_time")) {
                check_time = configuration.getInt("dtc.check_time");
            }
        } catch (Exception ignored) {
        }
        check_time = check_time > 0 ? check_time : 1;
        if (canStartAnEvent()) {
            this.dtcEvent = new DTCEvent(dtc);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (dtcEvent == null) {
                        dtc.stop();
                        cancel();
                    } else if (dtcEvent.checkTimer()) {
                        dtc.stop();
                        cancel();
                    } else {
                        dtcEvent.updateScoreboard();
                    }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, check_time * 20L);
            return;
        }
        FactionMessageTitle.sendPlayersMessage(msg.getString("dtc.prefix") + new StrManager(msg.getString("dtc.adding_to_queue")).reDTC(dtc.getName()).toString(), players);
        queue.add(dtc);
    }

    public void start(Totem totem, Player... players) {
        if (canStartAnEvent()) {
            this.totemEvent = new TotemEvent(totem);
            FileConfiguration msg = FileManager.getMessageFileConfiguration();
            String str = msg.getString("totem.prefix") + new StrManager(msg.getString("totem.started")).reTotem(totem.getName()).toString();
            Bukkit.broadcastMessage(str);
            totemEvent.start();
            FileConfiguration configuration = FileManager.getConfig();
            int check_time = 10;
            try {
                if (configuration.contains("totem.check_time")) {
                    check_time = configuration.getInt("totem.check_time");
                }
            } catch (Exception ignored) {
            }
            check_time = check_time > 0 ? check_time : 1;
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (totemEvent == null) {
                            cancel();
                        } else if (totemEvent.checkTimer()) {
                            cancel();
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }.runTaskTimer(FactionEvent.getInstance(), 20L, check_time * 20L);
            return;
        }
        FactionMessageTitle.sendPlayersMessage(msg.getString("totem.prefix") + new StrManager(msg.getString("totem.adding_to_queue")).reTotem(totem.getName()).toString(), players);
        queue.add(totem);
    }

    public void start(Meteorite meteorite, Player... players) {
        if (canStartAnEvent()) {
            String message = msg.getString("meteorite.prefix") == null ? "§8[§cMétéorite§8]§7 " : msg.getString("meteorite.prefix");
            this.meteoriteEvent = new MeteoriteEvent(meteorite);
            message += new StrManager(msg.getString("meteorite.started")).reMeteorite(meteorite.getName()).toString();
            Bukkit.broadcastMessage(message);
            return;
        }
        FactionMessageTitle.sendPlayersMessage(msg.getString("meteorite.prefix") + new StrManager(msg.getString("meteorite.adding_to_queue")).reMeteorite(meteorite.getName()).toString(), players);
        queue.add(meteorite);
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
            if (meteoriteEvent != null) {
                meteoriteEvent.getMeteorite().stop();
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

    public MeteoriteEvent getMeteoriteEvent() {
        return meteoriteEvent;
    }

    public TotemEvent getTotemEvent() {
        return totemEvent;
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

    public void setMeteoriteEvent(MeteoriteEvent meteoriteEvent) {
        this.meteoriteEvent = meteoriteEvent;
    }

    public void setQueue(ArrayList<Object> queue) {
        this.queue = queue;
    }
}