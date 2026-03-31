package fr.blixow.factionevent.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;

/**
 * ScoreBoardAPI — Un scoreboard PARTAGÉ entre tous les joueurs.
 * Un seul Scoreboard/Objective par instance. On l'assigne à chaque joueur via applyToPlayer().
 * Compatible Bukkit 1.8.
 */
public class ScoreBoardAPI {

    private static final String[] ENTRIES = {
        "§0","§1","§2","§3","§4","§5","§6","§7",
        "§8","§9","§a","§b","§c","§d","§e","§f"
    };

    private final String objectiveName;
    private Scoreboard sb;
    private Objective obj;
    private final HashMap<Integer, Team> lineTeams = new HashMap<>();

    /**
     * Crée un scoreboard partagé (utilisé par tous les joueurs d'un event).
     */
    public ScoreBoardAPI(String objectiveName) {
        this.objectiveName = objectiveName;
        build();
    }

    // Constructeur legacy par joueur (conservé pour compatibilité, redirige vers le partagé)
    public ScoreBoardAPI(Player player, String objectiveName) {
        this(objectiveName);
    }

    private void build() {
        try {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
            obj = sb.registerNewObjective(objectiveName, "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ScoreBoardAPI] build() error: " + e.getMessage());
        }
    }

    public void setDisplayName(String title) {
        if (obj == null) return;
        try {
            if (title == null) title = "";
            if (title.length() > 32) title = title.substring(0, 32);
            obj.setDisplayName(title);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("deprecation")
    public void setLine(int score, String text) {
        if (sb == null || obj == null) return;
        if (text == null) text = "";
        if (score < 0 || score >= ENTRIES.length) return;

        try {
            Team team = lineTeams.get(score);
            if (team == null) {
                String entry = ENTRIES[score];
                String teamName = "fe" + score;
                team = sb.getTeam(teamName);
                if (team == null) team = sb.registerNewTeam(teamName);
                try {
                    team.addEntry(entry);
                } catch (NoSuchMethodError e) {
                    team.addPlayer(Bukkit.getOfflinePlayer(entry));
                }
                obj.getScore(entry).setScore(score);
                lineTeams.put(score, team);
            }
            applyText(team, text);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ScoreBoardAPI] setLine(" + score + "): " + e.getMessage());
        }
    }

    private void applyText(Team team, String text) {
        String prefix, suffix;
        if (text.length() <= 16) {
            prefix = text;
            suffix = "";
        } else {
            int cut = (text.length() > 15 && text.charAt(15) == '§') ? 15 : 16;
            prefix = text.substring(0, cut);
            String color = lastColor(prefix);
            int maxSuf = 16 - color.length();
            suffix = color + text.substring(cut, Math.min(text.length(), cut + maxSuf));
        }
        if (!prefix.equals(team.getPrefix())) team.setPrefix(prefix);
        if (!suffix.equals(team.getSuffix())) team.setSuffix(suffix);
    }

    private String lastColor(String s) {
        for (int i = s.length() - 2; i >= 0; i--) {
            if (s.charAt(i) == '§') return s.substring(i, i + 2);
        }
        return "";
    }

    /**
     * Assigne ce scoreboard partagé au joueur donné.
     * À appeler à chaque update pour s'assurer qu'aucun autre plugin n'a pris la main.
     */
    public void applyToPlayer(Player player) {
        if (sb == null || player == null || !player.isOnline()) return;
        try {
            if (!sb.equals(player.getScoreboard())) {
                player.setScoreboard(sb);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ScoreBoardAPI] applyToPlayer(): " + e.getMessage());
        }
    }

    /**
     * Retire ce scoreboard du joueur et lui remet le scoreboard principal.
     */
    public void removeFromPlayer(Player player) {
        try {
            if (player != null && player.isOnline()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        } catch (Exception ignored) {}
    }

    /**
     * Retire ce scoreboard de tous les joueurs en ligne.
     */
    public void removeFromAll() {
        try {
            Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    if (sb != null && sb.equals(p.getScoreboard())) {
                        p.setScoreboard(main);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // ── Méthodes de compatibilité legacy ─────────────────────────────────────

    /** @deprecated Utiliser applyToPlayer(player) */
    @Deprecated
    public void apply() {
        // no-op dans le nouveau modèle partagé
    }

    /** @deprecated Utiliser removeFromPlayer(player) ou removeFromAll() */
    @Deprecated
    public void remove() {
        removeFromAll();
    }

    public Scoreboard getScoreboard() { return sb; }
    public Objective getObjective() { return obj; }
    public boolean ensureValid() { return sb != null && obj != null; }

    @Deprecated
    public ScoreBoardAPI(Scoreboard sb, String objectiveName, boolean init) {
        this.objectiveName = objectiveName;
        this.sb = sb;
        if (init) {
            try { this.obj = sb.registerNewObjective(objectiveName, "dummy"); }
            catch (Exception e) { try { this.obj = sb.getObjective(objectiveName); } catch (Exception ignored) {} }
        } else {
            try { this.obj = sb.getObjective(objectiveName); } catch (Exception ignored) {}
        }
    }

    public void setScoreboardOf(Player... players) {
        for (Player p : players) { try { p.setScoreboard(this.sb); } catch (Exception ignored) {} }
    }
}
