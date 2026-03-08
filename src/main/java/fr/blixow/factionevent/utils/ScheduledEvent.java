package fr.blixow.factionevent.utils;

import java.time.LocalDateTime;

public class ScheduledEvent {
    private final String type;
    private final String name;
    private final LocalDateTime time;
    private boolean warned5min;
    private boolean warned1min;
    private boolean started;

    public ScheduledEvent(String type, String name, LocalDateTime time) {
        this.type = type;
        this.name = name;
        this.time = time;
        this.warned5min = false;
        this.warned1min = false;
        this.started = false;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public LocalDateTime getTime() { return time; }

    public boolean isWarned5min() { return warned5min; }
    public void setWarned5min(boolean warned5min) { this.warned5min = warned5min; }

    public boolean isWarned1min() { return warned1min; }
    public void setWarned1min(boolean warned1min) { this.warned1min = warned1min; }

    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
}

