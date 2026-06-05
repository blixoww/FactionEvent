package fr.blixow.factionevent.utils.relic;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import fr.blixow.factionevent.FactionEvent;
import fr.blixow.factionevent.manager.DateManager;
import fr.blixow.factionevent.manager.EventManager;
import fr.blixow.factionevent.manager.FileManager;
import fr.blixow.factionevent.manager.RankingManager;
import fr.blixow.factionevent.manager.StrManager;
import fr.blixow.factionevent.utils.FactionMessageTitle;
import fr.blixow.factionevent.utils.LootItemParser;
import fr.blixow.factionevent.utils.Messages;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Runtime de la Course à la Relique.
 *
 * <p>La relique apparaît au sol (item flottant + colonne de particules + éclair + hologramme).
 * Pour la prendre, il faut <b>traverser les particules</b> après un court délai de cooldown.
 * Le porteur fait gagner des points à sa faction en continu et est ré-annoncé périodiquement
 * dans le chat (avec ses coordonnées). Chaque coup reçu a une probabilité de faire tomber la
 * relique. Aucune commande de téléportation n'est possible tant qu'on la porte.</p>
 *
 * <p>À la fin du timer, c'est le joueur ayant porté la relique <b>le plus longtemps</b> (temps
 * cumulé) qui l'emporte. Les 3 meilleurs reçoivent argent + items via {@code /relicreward}.
 * La relique (Nether Star par défaut) disparaît alors de tous les inventaires.</p>
 *
 * <p>Compatible Spigot 1.8.8 uniquement (aucune API 1.9+).</p>
 */
public class RelicEvent {

    /** Ligne de lore marqueur — identifie de façon unique l'item relique (pas de PersistentData en 1.8). */
    private static final String MARKER = "§8§o⟡ #fe-relic";

    private final Location spawnLocation;
    private final long startTime;
    private boolean ended;

    // Config
    private final int duration;
    private final int pointsPerSecond;
    private final double dropChancePercent;
    private final int pickupDelay;        // secondes avant qu'une relique au sol soit reprenable
    private final double pickupRadius;    // distance pour ramasser en traversant
    private final int winPoints;
    private final int announceInterval;   // secondes entre deux ré-annonces du porteur

    private final ItemStack relicItem;        // template de l'item relique
    private UUID carrier;                      // porteur actuel (null = relique au sol / aucune)
    private long carrierSince;                 // ms du début du portage actuel
    private Item groundItem;                   // entité au sol (null = portée)
    private ArmorStand groundHologram;         // libellé flottant au-dessus des particules
    private long groundPickableAt;             // ms à partir duquel la relique au sol est reprenable
    private long lastAnnounce;                 // ms de la dernière ré-annonce du porteur

    private final Map<UUID, Long> carryMillis;        // temps cumulé porté par joueur (ms)
    private final Map<UUID, String> playerNames;      // UUID → dernier pseudo connu
    private final Map<String, Integer> factionScores; // points accumulés par faction (id → pts)

    private final FileConfiguration msg;
    private final FileConfiguration config;
    private final String prefix;

    public RelicEvent(Relic relic) {
        this.spawnLocation = relic.getSpawnLocation();
        this.startTime = System.currentTimeMillis();
        this.ended = false;
        this.carrier = null;
        this.carrierSince = 0L;
        this.groundItem = null;
        this.groundHologram = null;
        this.lastAnnounce = System.currentTimeMillis();
        this.carryMillis = new LinkedHashMap<>();
        this.playerNames = new HashMap<>();
        this.factionScores = new LinkedHashMap<>();

        this.msg = FileManager.getMessageFileConfiguration();
        this.config = FileManager.getConfig();
        this.prefix = msg.getString("relic.prefix", "§8[§dRELIQUE§8]§7 ");

        this.duration          = config.getInt("relic.max_duration", 900);
        this.pointsPerSecond   = config.getInt("relic.points_per_second", 1);
        this.dropChancePercent = config.getDouble("relic.drop_chance", 15);
        this.pickupDelay       = config.getInt("relic.pickup_delay", 3);
        this.pickupRadius      = config.getDouble("relic.pickup_radius", 2.0);
        this.winPoints         = config.getInt("relic.win_points", 25);
        this.announceInterval  = Math.max(5, config.getInt("relic.announce_interval", 20));

        this.relicItem = buildRelicItem();

        // Apparition initiale de la relique au sol
        spawnGroundRelic(spawnLocation);
    }

    // ── Identité de la relique ───────────────────────────────────────────────

    private ItemStack buildRelicItem() {
        String itemStr = config.getString("relic.relic_item", "NETHER_STAR:1");
        LootItemParser.LootEntry entry = LootItemParser.parseSingle(itemStr);
        ItemStack stack = entry != null ? entry.item.clone() : new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(config.getString("relic.relic_name", "§d§l✦ Relique d'Event ✦"));
            List<String> lore = new ArrayList<>();
            lore.add("§7Relique de l'event §dCourse à la Relique§7.");
            lore.add(MARKER);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isRelic(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        if (!stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        List<String> lore = meta.getLore();
        return lore != null && lore.contains(MARKER);
    }

    public boolean isCarrier(Player p) {
        return p != null && carrier != null && carrier.equals(p.getUniqueId());
    }

    public Player getCarrier() {
        if (carrier == null) return null;
        Player p = Bukkit.getPlayer(carrier);
        return (p != null && p.isOnline()) ? p : null;
    }

    public boolean isEnded() { return ended; }

    // ── Relique au sol ───────────────────────────────────────────────────────

    public void spawnGroundRelic(Location loc) {
        if (ended) return;
        removeGroundItem();
        World w = (loc != null) ? loc.getWorld() : null;
        if (w == null) { loc = spawnLocation; w = spawnLocation.getWorld(); }
        if (w == null) return;
        Location dropLoc = loc.clone().add(0, 0.5, 0);
        Item item = w.dropItem(dropLoc, relicItem.clone());
        item.setVelocity(new Vector(0, 0, 0));
        // Pickup vanilla désactivé : on gère le ramassage manuellement (traversée des particules)
        item.setPickupDelay(Integer.MAX_VALUE);
        try {
            item.setMetadata("fe_relic", new FixedMetadataValue(FactionEvent.getInstance(), true));
        } catch (Exception ignored) {}
        this.groundItem = item;
        this.groundPickableAt = System.currentTimeMillis() + pickupDelay * 1000L;

        // Hologramme libellé au-dessus de la colonne de particules
        spawnHologram(dropLoc);

        // Éclair visuel (sans dégâts) + colonne de particules au point de chute
        w.strikeLightningEffect(dropLoc);
        particleColumn(dropLoc);
        updateCompassAll(dropLoc);
    }

    private void spawnHologram(Location base) {
        try {
            World w = base.getWorld();
            if (w == null) return;
            Location holoLoc = base.clone().add(0, 2.6, 0); // au-dessus des particules (≤ 2.5)
            ArmorStand as = w.spawn(holoLoc, ArmorStand.class);
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            try { as.setMarker(true); } catch (Throwable ignored) {}
            as.setCustomName(msg.getString("relic.hologram", "§d§l✦ Relique §7(traversez pour prendre)"));
            as.setCustomNameVisible(true);
            as.setMetadata("fe_relic", new FixedMetadataValue(FactionEvent.getInstance(), true));
            this.groundHologram = as;
        } catch (Exception ignored) {}
    }

    private void removeGroundItem() {
        if (groundItem != null) {
            try { groundItem.remove(); } catch (Exception ignored) {}
            groundItem = null;
        }
        if (groundHologram != null) {
            try { groundHologram.remove(); } catch (Exception ignored) {}
            groundHologram = null;
        }
    }

    // ── Changements de porteur ───────────────────────────────────────────────

    /** Un joueur traverse les particules et devient le nouveau porteur. */
    public void setCarrier(Player player) {
        if (ended || player == null) return;
        removeGroundItem();
        this.carrier = player.getUniqueId();
        this.carrierSince = System.currentTimeMillis();
        this.lastAnnounce = System.currentTimeMillis();
        this.playerNames.put(player.getUniqueId(), player.getName());
        this.carryMillis.putIfAbsent(player.getUniqueId(), 0L);
        player.getInventory().addItem(relicItem.clone());
        player.updateInventory();

        FPlayer fp = FPlayers.getInstance().getByPlayer(player);
        String fac = (fp == null || fp.getFaction().isWilderness())
            ? msg.getString("no-faction", "§7Sans faction") : fp.getFaction().getTag();

        // Annonce : récupération + invitation à le taper
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("relic.new_carrier",
            "§e{player} §7(§c{faction}§7) a récupéré la Relique ! §6Tapez-le pour tenter de la lui prendre !"))
            .rePlayer(player).reFaction(fac).toString());
        // Classement des meilleurs porteurs jusqu'ici
        broadcastStandings();

        // Notice de blocage des téléportations (au porteur uniquement)
        player.sendMessage(prefix + msg.getString("relic.tp_blocked_notice",
            "§cTant que vous portez la Relique, §laucune commande de téléportation §r§cn'est possible !"));

        FactionMessageTitle.sendPlayersTitle(5, 30, 10,
            msg.getString("relic.carrier_title", "§d✦ Relique"),
            new StrManager(msg.getString("relic.carrier_subtitle", "§7Portée par §c{player}")).rePlayer(player).toString());
        playSoundAll(Sound.LEVEL_UP);
        // Éclair unique au ramassage — aucune particule persistante sur le porteur
        try { player.getWorld().strikeLightningEffect(player.getLocation()); } catch (Exception ignored) {}
        updateCompassAll(player.getLocation());
    }

    /** Comptabilise le temps écoulé du portage courant dans le total du porteur. */
    private void accumulateCarry() {
        if (carrier != null && carrierSince > 0) {
            long elapsed = System.currentTimeMillis() - carrierSince;
            if (elapsed > 0) carryMillis.merge(carrier, elapsed, Long::sum);
        }
        carrierSince = 0L;
    }

    /** La relique tombe au sol (suite à un coup). */
    public void dropFromCarrier(Player player) {
        if (ended || !isCarrier(player)) return;
        accumulateCarry();
        removeRelicFromInventory(player);
        this.carrier = null;
        spawnGroundRelic(player.getLocation());
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("relic.dropped",
            "§c{player} §7a fait tomber la Relique !")).rePlayer(player).toString());
        broadcastStandings();
        playSoundAll(Sound.ITEM_BREAK);
    }

    /** Chaque coup porté au porteur : tirage de la probabilité fixe de drop. */
    public void handleHit(Player victim, Player attacker) {
        if (ended || !isCarrier(victim)) return;
        if (Math.random() * 100.0 < dropChancePercent) {
            dropFromCarrier(victim);
            if (attacker != null) {
                attacker.sendMessage(prefix + msg.getString("relic.you_knocked",
                    "§aVous avez fait tomber la Relique !"));
            }
        }
    }

    /** Le porteur meurt : la relique tombe à l'endroit de la mort (drops gérés par CustomEvents). */
    public void handleCarrierDeath(Player player) {
        if (ended || !isCarrier(player)) return;
        accumulateCarry();
        // Retire la relique de l'inventaire au cas où le serveur garde l'inventaire à la mort
        // (keepInventory) — sinon la relique du sol et celle de l'inventaire feraient doublon.
        removeRelicFromInventory(player);
        this.carrier = null;
        spawnGroundRelic(player.getLocation());
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("relic.carrier_died",
            "§c{player} §7est mort ! La Relique est tombée.")).rePlayer(player).toString());
        broadcastStandings();
    }

    /** Le porteur se déconnecte : la relique tombe à sa dernière position. */
    public void removePlayer(Player player) {
        if (ended || !isCarrier(player)) return;
        accumulateCarry();
        this.carrier = null;
        spawnGroundRelic(player.getLocation());
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("relic.carrier_quit",
            "§c{player} §7s'est déconnecté ! La Relique est tombée.")).rePlayer(player).toString());
        broadcastStandings();
    }

    private boolean removeRelicFromInventory(Player p) {
        boolean removed = false;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isRelic(contents[i])) {
                p.getInventory().setItem(i, null);
                removed = true;
            }
        }
        p.updateInventory();
        return removed;
    }

    // ── Ticks ────────────────────────────────────────────────────────────────

    /** Appelé toutes les secondes : score + boussole + action bar + ré-annonce périodique. */
    public void tick() {
        if (ended) return;
        Player carrierP = getCarrier();
        if (carrierP != null) {
            FPlayer fp = FPlayers.getInstance().getByPlayer(carrierP);
            if (fp != null && !fp.getFaction().isWilderness() && pointsPerSecond > 0) {
                factionScores.merge(fp.getFaction().getId(), pointsPerSecond, Integer::sum);
            }
            updateCompassAll(carrierP.getLocation());

            // Ré-annonce périodique : qui porte la relique + coordonnées actuelles
            if (System.currentTimeMillis() - lastAnnounce >= announceInterval * 1000L) {
                lastAnnounce = System.currentTimeMillis();
                announceCarrierLocation(carrierP, fp);
            }
        } else if (groundItem != null && groundItem.isValid()) {
            updateCompassAll(groundItem.getLocation());
        }

        String bar = buildActionBar(carrierP);
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                EventManager em = FactionEvent.getInstance().getEventScoreboardOff().get(p);
                if (em == null) {
                    em = EventManager.loadFromFile(p);
                    FactionEvent.getInstance().getEventScoreboardOff().put(p, em);
                }
                if (em.isActionbar()) Messages.sendActionBar(p, bar);
            } catch (Exception ignored) {}
        }
    }

    /** Appelé toutes les 5 ticks : particules au sol + anti-despawn + ramassage par traversée. */
    public void visualTick() {
        if (ended) return;
        Player carrierP = getCarrier();
        if (carrierP != null) {
            // Aucune particule sur le porteur (volonté de design)
            return;
        }
        if (groundItem != null && groundItem.isValid()) {
            try { groundItem.setTicksLived(1); } catch (Exception ignored) {} // empêche le despawn (5 min)
            particleColumn(groundItem.getLocation());
            tryProximityPickup();
        } else if (carrier == null) {
            // Relique perdue sans porteur (entité disparue) → réapparition au point de spawn
            spawnGroundRelic(spawnLocation);
        }
    }

    /** Cherche un joueur traversant les particules pour lui donner la relique. */
    private void tryProximityPickup() {
        if (System.currentTimeMillis() < groundPickableAt) return; // cooldown au sol pas écoulé
        if (groundItem == null) return;
        Location loc = groundItem.getLocation();
        World w = loc.getWorld();
        if (w == null) return;
        Player nearest = null;
        double best = pickupRadius * pickupRadius;
        for (Player p : w.getPlayers()) {
            if (p == null || !p.isOnline() || p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            double d2 = p.getLocation().distanceSquared(loc);
            if (d2 <= best) { best = d2; nearest = p; }
        }
        if (nearest != null) setCarrier(nearest);
    }

    /** Appelé périodiquement : termine l'event quand le timer est écoulé. */
    public boolean checkTimer() {
        if (ended) return true;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsed >= duration) { finalizeEvent(); return true; }
        return false;
    }

    // ── Classement / annonces ────────────────────────────────────────────────

    /** Liste triée (desc) des temps cumulés, en incluant le portage courant en cours. */
    private List<Map.Entry<UUID, Long>> sortedStandings() {
        Map<UUID, Long> snapshot = new LinkedHashMap<>(carryMillis);
        if (carrier != null && carrierSince > 0) {
            long running = System.currentTimeMillis() - carrierSince;
            snapshot.merge(carrier, Math.max(0, running), Long::sum);
        }
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(snapshot.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return sorted;
    }

    /** Diffuse le top 3 (ou moins) des porteurs par temps cumulé. */
    private void broadcastStandings() {
        List<Map.Entry<UUID, Long>> top = sortedStandings();
        if (top.isEmpty()) return;
        int n = Math.min(3, top.size());
        StringBuilder sb = new StringBuilder(prefix + msg.getString("relic.standings_header",
            "§7Top porteurs : "));
        for (int i = 0; i < n; i++) {
            UUID id = top.get(i).getKey();
            String name = playerNames.getOrDefault(id, "?");
            String time = DateManager.getFormattedTime((int) (top.get(i).getValue() / 1000));
            sb.append("§8").append(i + 1).append(".§e").append(name).append("§7(").append(time).append("§7)");
            if (i < n - 1) sb.append("§8, ");
        }
        Bukkit.broadcastMessage(sb.toString());
    }

    private void announceCarrierLocation(Player carrierP, FPlayer fp) {
        String fac = (fp == null || fp.getFaction().isWilderness())
            ? msg.getString("no-faction", "§7Sans faction") : fp.getFaction().getTag();
        Location l = carrierP.getLocation();
        String coords = l.getBlockX() + " / " + l.getBlockY() + " / " + l.getBlockZ();
        Bukkit.broadcastMessage(prefix + new StrManager(msg.getString("relic.carrier_locator",
            "§e{player} §7(§c{faction}§7) porte la Relique §8| §7Position : §f{coords}"))
            .rePlayer(carrierP).reFaction(fac).toString().replace("{coords}", coords));
    }

    // ── Fin d'event / récompenses ────────────────────────────────────────────

    private void finalizeEvent() {
        if (ended) return;
        accumulateCarry();
        ended = true;

        // Conversion des points faction accumulés (par seconde de portage)
        for (Map.Entry<String, Integer> e : factionScores.entrySet()) {
            Faction f = getFactionById(e.getKey());
            if (f != null && !f.isWilderness() && e.getValue() > 0) {
                RankingManager.addPoints(f, e.getValue());
            }
        }

        List<Map.Entry<UUID, Long>> top = sortedStandings();

        // Personne n'a jamais porté la relique → l'event est simplement annulé en fin de temps
        // (aucun vainqueur, aucune récompense, aucun point). La relique au sol est retirée par cleanup().
        if (top.isEmpty()) {
            Bukkit.broadcastMessage("\n§8§m-----------------------------------------------------\n"
                + "§r §8< §d§lCOURSE À LA RELIQUE §8> §8§m-----------------------------------------------------\n"
                + "§7Le temps est écoulé : §cpersonne n'a porté la Relique§7. Event annulé.\n"
                + "§8§m-----------------------------------------------------");
            cleanup();
            FactionEvent.getInstance().getEventOn().setRelicEvent(null);
            RankingManager.updateRanking(true);
            return;
        }

        // Vainqueur = plus long porteur cumulé
        UUID winnerId = top.get(0).getKey();
        String winnerName = playerNames.getOrDefault(winnerId, "?");
        Player winnerP = Bukkit.getPlayer(winnerId);

        Faction winnerFac = null;
        try {
            FPlayer fp = FPlayers.getInstance().getByOfflinePlayer(Bukkit.getOfflinePlayer(winnerId));
            if (fp != null) winnerFac = fp.getFaction();
        } catch (Exception ignored) {}
        String winFacTag = (winnerFac == null || winnerFac.isWilderness())
            ? msg.getString("no-faction", "§7Sans faction") : winnerFac.getTag();

        Bukkit.broadcastMessage("\n§8§m-----------------------------------------------------\n"
            + "§r §8< §d§lVICTOIRE — RELIQUE §8> §8§m-----------------------------------------------------\n"
            + "§7🏆 §e" + winnerName + " §7(§c" + winFacTag + "§7) a porté la Relique le plus longtemps !\n"
            + buildFinalLeaderboard(top)
            + "§8§m-----------------------------------------------------");
        FactionMessageTitle.sendPlayersTitle(20, 60, 20, "§6§l🏆 RELIQUE",
            "§e" + winnerName + " §7l'emporte !");
        playSoundAll(Sound.LEVEL_UP);

        if (winnerFac != null && !winnerFac.isWilderness()) {
            RankingManager.addRelicWins(winnerFac);
            RankingManager.addPoints(winnerFac, winPoints);
            FactionMessageTitle.sendFactionTitle(winnerFac, 20, 60, 20,
                "§6§l🏆 VICTOIRE !", "§a+" + winPoints + " pts classement");
        }

        // Téléport instantané du vainqueur au spawn (preuve de survie)
        if (winnerP != null) {
            String spawnCmd = config.getString("relic.rewards.spawn_command", "spawn %player%");
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    spawnCmd.replace("%player%", winnerP.getName()).replace("{player}", winnerP.getName()));
            } catch (Exception ignored) {}
        }

        // Récompenses argent + items aux 3 meilleurs (décroissant)
        distributeRewards(top);

        cleanup();
        FactionEvent.getInstance().getEventOn().setRelicEvent(null);
        RankingManager.updateRanking(true);
    }

    private void distributeRewards(List<Map.Entry<UUID, Long>> top) {
        boolean moneyEnabled = config.getBoolean("relic.rewards.money_enabled", true);
        Economy eco = FactionEvent.getEconomy();
        int ranks = Math.min(3, top.size());
        for (int i = 0; i < ranks; i++) {
            int rank = i + 1;
            UUID uuid = top.get(i).getKey();
            String name = playerNames.getOrDefault(uuid, "?");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) name = p.getName();

            String path = "relic.rewards.ranks." + rank;
            double money = config.getDouble(path + ".money", 0);
            int min = config.getInt(path + ".items_min", 0);
            int max = config.getInt(path + ".items_max", 0);
            List<String> itemDefs = config.getStringList(path + ".items");
            List<LootItemParser.LootEntry> pool = LootItemParser.parse(itemDefs);
            List<ItemStack> drops = pickItems(pool, min, max);

            if (moneyEnabled && eco != null && money > 0 && name != null) {
                try { eco.depositPlayer(name, money); } catch (Exception ignored) {}
            }
            for (ItemStack item : drops) {
                RelicRewardManager.addItemReward(uuid, item);
            }

            if (p != null && p.isOnline()) {
                String moneyPart = (moneyEnabled && money > 0) ? "§a+§e" + (int) money + "$ §8| " : "";
                p.sendMessage(prefix + "§6§l" + ordinal(rank) + " place §8» " + moneyPart
                    + "§a+§e" + drops.size() + " §aitems §7→ §e/relicreward");
                Messages.sendTitle(p, 10, 40, 10, "§6§l" + ordinal(rank),
                    moneyPart + "§7+§e" + drops.size() + " items");
            }
        }
    }

    /** Ordinal français : 1 → "1er", sinon "Nème". */
    private String ordinal(int rank) {
        return rank == 1 ? "1er" : rank + "ème";
    }

    private String buildFinalLeaderboard(List<Map.Entry<UUID, Long>> top) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(3, top.size());
        String[] medals = {"§6①", "§7②", "§c③"};
        for (int i = 0; i < n; i++) {
            String name = playerNames.getOrDefault(top.get(i).getKey(), "?");
            String time = DateManager.getFormattedTime((int) (top.get(i).getValue() / 1000));
            sb.append("§8» ").append(medals[i]).append(" §e").append(name)
              .append(" §8- §7porté §f").append(time).append(" §8→ §e/relicreward\n");
        }
        return sb.toString();
    }

    private List<ItemStack> pickItems(List<LootItemParser.LootEntry> pool, int min, int max) {
        List<ItemStack> result = new ArrayList<>();
        if (pool == null || pool.isEmpty() || max <= 0) return result;
        if (min < 0) min = 0;
        if (max < min) max = min;
        Random rng = new Random(System.nanoTime());
        int count = (max == min) ? min : (min + rng.nextInt(max - min + 1));
        if (count <= 0) return result;

        List<ItemStack> remainingItems = new ArrayList<>();
        List<Integer> remainingWeights = new ArrayList<>();
        for (LootItemParser.LootEntry e : pool) {
            remainingItems.add(e.item);
            remainingWeights.add(e.weight);
        }
        for (int i = 0; i < count && !remainingItems.isEmpty(); i++) {
            int totalWeight = 0;
            for (int wt : remainingWeights) totalWeight += wt;
            if (totalWeight <= 0) break;
            int roll = rng.nextInt(totalWeight);
            int cumul = 0;
            int picked = 0;
            for (int j = 0; j < remainingWeights.size(); j++) {
                cumul += remainingWeights.get(j);
                if (roll < cumul) { picked = j; break; }
            }
            result.add(remainingItems.get(picked).clone());
            remainingItems.remove(picked);
            remainingWeights.remove(picked);
        }
        return result;
    }

    /** Nettoyage complet (fin ou annulation) : entité au sol, hologramme, boussoles. */
    public void cleanup() {
        removeGroundItem();
        // La relique disparaît de tous les inventaires (fin d'event OU /relic stop)
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { removeRelicFromInventory(p); } catch (Exception ignored) {}
            try { p.setCompassTarget(p.getWorld().getSpawnLocation()); } catch (Exception ignored) {}
        }
        carrier = null;
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private void updateCompassAll(Location loc) {
        if (loc == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.setCompassTarget(loc); } catch (Exception ignored) {}
        }
    }

    private void particleColumn(Location base) {
        if (base == null) return;
        World w = base.getWorld();
        if (w == null) return;
        try {
            for (double dy = 0.0; dy <= 2.5; dy += 0.5) {
                Location l = base.clone().add(0, dy, 0);
                w.spigot().playEffect(l, Effect.FLAME, 0, 0, 0.15f, 0.15f, 0.15f, 0.02f, 8, 32);
            }
            w.spigot().playEffect(base.clone().add(0, 1.0, 0), Effect.HAPPY_VILLAGER,
                0, 0, 0.4f, 0.6f, 0.4f, 0.0f, 6, 32);
        } catch (Exception ignored) {}
    }

    private String buildActionBar(Player carrierP) {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        String timeStr = DateManager.getFormattedTime((int) Math.max(0, duration - elapsed));
        if (carrierP != null) {
            FPlayer fp = FPlayers.getInstance().getByPlayer(carrierP);
            String fac = (fp == null || fp.getFaction().isWilderness())
                ? "§7Sans faction" : "§c" + fp.getFaction().getTag();
            long heldMs = carryMillis.getOrDefault(carrierP.getUniqueId(), 0L)
                + (carrierSince > 0 ? System.currentTimeMillis() - carrierSince : 0);
            String held = DateManager.getFormattedTime((int) (heldMs / 1000));
            return "§8[§dRELIQUE§8] §7Porteur: §e" + carrierP.getName()
                + " §8(" + fac + "§8) §8| §7Porté: §e" + held + " §8| §7⏱§c" + timeStr;
        }
        return "§8[§dRELIQUE§8] §7La Relique est §cau sol §7! §8Traversez les particules §8| §7⏱§c" + timeStr;
    }

    private void playSoundAll(Sound sound) {
        try {
            for (Player p : Bukkit.getOnlinePlayers()) p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }

    private Faction getFactionById(String id) {
        try {
            for (Faction f : Factions.getInstance().getAllFactions()) {
                if (f.getId().equals(id)) return f;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
