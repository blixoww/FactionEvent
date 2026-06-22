package fr.blixow.factionevent.hooks;

import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.utils.FactionUtil;
import fr.redfaction.entity.FPlayer;
import fr.redfaction.entity.Faction;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Expansion PlaceholderAPI exposant le classement event d'une faction.
 *
 * <p>Indispensable depuis la migration vers RedFaction : son {@code ChatListener}
 * annule l'{@code AsyncPlayerChatEvent} et reconstruit lui-même chaque ligne du
 * chat public à partir de son {@code chat.global_format}. Le {@code setFormat()}
 * que FactionEvent appliquait auparavant n'a donc plus aucun effet. Le seul point
 * d'injection restant dans ce format est PlaceholderAPI (RedFaction y expand déjà
 * les placeholders du joueur émetteur).</p>
 *
 * <p>Placeholders (préfixe {@code %factionevent_}) :</p>
 * <ul>
 *   <li>{@code rank} — position de la faction dans le classement (vide hors classement)</li>
 *   <li>{@code rank_prefix} — préfixe formaté ({@code chat_format.faction_rank_prefix})
 *       suivi d'une espace, vide si le joueur n'a pas de faction / n'est pas classé</li>
 * </ul>
 */
public class FactionEventExpansion extends PlaceholderExpansion {

    private final FactionEvent plugin;

    public FactionEventExpansion(FactionEvent plugin) {
        this.plugin = plugin;
    }

    @Override public boolean persist()      { return true; } // survit aux reloads de PAPI
    @Override public boolean canRegister()  { return true; }
    @Override public String getIdentifier() { return "factionevent"; }
    @Override public String getAuthor()     { return "blixow"; }
    @Override public String getVersion()    { return plugin.getDescription().getVersion(); }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) return "";
        FPlayer fp = FactionUtil.fplayer(player.getUniqueId());
        Faction faction = fp == null ? null : fp.getFaction();
        if (FactionUtil.isWilderness(faction)) return "";

        int rank = rankOf(faction);

        switch (identifier.toLowerCase()) {
            case "rank":
                return rank > 0 ? String.valueOf(rank) : "";
            case "rank_prefix":
                if (rank <= 0) return "";
                String prefix = FileManager.getMessageFileConfiguration()
                        .getString("chat_format.faction_rank_prefix", "§7[§6%faction_rank%§7]");
                return prefix.replace("%faction_rank%", String.valueOf(rank)) + " ";
            default:
                return null; // placeholder inconnu
        }
    }

    /** Position (1-based) de la faction dans le classement, -1 si absente. */
    private int rankOf(Faction faction) {
        // Snapshot : onRequest peut être appelé depuis le thread async du chat.
        Map<Faction, Integer> rankings =
                new LinkedHashMap<>(FactionEvent.getInstance().getFactionRankings());
        int rank = 1;
        for (Faction f : rankings.keySet()) {
            if (f.equals(faction)) return rank;
            rank++;
        }
        return -1;
    }
}
