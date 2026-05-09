package fr.blixow.factionevent.utils.lms;

public enum LMSMode {
    SOLO, DUO;

    public static LMSMode fromString(String s) {
        if (s == null) return SOLO;
        switch (s.toUpperCase().trim()) {
            case "DUO": return DUO;
            default: return SOLO;
        }
    }

    public String getDisplayName() {
        return this == DUO ? "DUO" : "SOLO";
    }
}

