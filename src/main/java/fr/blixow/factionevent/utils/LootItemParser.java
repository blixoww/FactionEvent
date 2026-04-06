package fr.blixow.factionevent.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Parseur commun pour les listes d'items de loot dans config.yml.
 *
 * ─────────────────────────────────────────────────────────────────
 * FORMAT D'UNE ENTRÉE :
 *
 *   MATERIAL:quantité[,ENCHANTMENT:NOM,LEVEL:valeur[,...]][,WEIGHT:poids]
 *
 * Exemples :
 *   "DIAMOND:1"
 *   "DIAMOND:1,WEIGHT:3"
 *   "COBALT_SWORD:1,ENCHANTMENT:SHARPNESS,LEVEL:5"
 *   "COBALT_SWORD:1,ENCHANTMENT:SHARPNESS,LEVEL:5,ENCHANTMENT:FIRE_ASPECT,LEVEL:2,WEIGHT:4"
 *   "RUBY_HELMET:1,ENCHANTMENT:PROTECTION,LEVEL:3,WEIGHT:6"
 *
 * Règles :
 *   - Séparateur principal : virgule ","
 *   - Le premier token est toujours "MATERIAL:quantité"
 *   - ENCHANTMENT:NOM doit être suivi (immédiatement) de LEVEL:valeur
 *   - WEIGHT:valeur est optionnel (défaut : 10) ; peut apparaître n'importe où après le 1er token
 * ─────────────────────────────────────────────────────────────────
 */
public class LootItemParser {

    /** Résultat du parse d'une entrée : item + poids. */
    public static class LootEntry {
        public final ItemStack item;
        public final int weight;

        public LootEntry(ItemStack item, int weight) {
            this.item   = item;
            this.weight = weight;
        }
    }

    // ─── Table d'alias enchantements (noms communs → noms internes Bukkit 1.8) ───
    private static final Map<String, String> ENCHANT_ALIAS = new HashMap<>();
    static {
        ENCHANT_ALIAS.put("SHARPNESS",            "DAMAGE_ALL");
        ENCHANT_ALIAS.put("SMITE",                "DAMAGE_UNDEAD");
        ENCHANT_ALIAS.put("BANE_OF_ARTHROPODS",   "DAMAGE_ARTHROPODS");
        ENCHANT_ALIAS.put("PROTECTION",           "PROTECTION_ENVIRONMENTAL");
        ENCHANT_ALIAS.put("FIRE_PROTECTION",      "PROTECTION_FIRE");
        ENCHANT_ALIAS.put("PROJECTILE_PROTECTION","PROTECTION_PROJECTILE");
        ENCHANT_ALIAS.put("BLAST_PROTECTION",     "PROTECTION_EXPLOSIONS");
        ENCHANT_ALIAS.put("FEATHER_FALLING",      "PROTECTION_FALL");
        ENCHANT_ALIAS.put("UNBREAKING",           "DURABILITY");
        ENCHANT_ALIAS.put("POWER",                "ARROW_DAMAGE");
        ENCHANT_ALIAS.put("PUNCH",                "ARROW_KNOCKBACK");
        ENCHANT_ALIAS.put("FLAME",                "ARROW_FIRE");
        ENCHANT_ALIAS.put("INFINITY",             "ARROW_INFINITE");
        ENCHANT_ALIAS.put("LOOTING",              "LOOT_BONUS_MOBS");
        ENCHANT_ALIAS.put("FORTUNE",              "LOOT_BONUS_BLOCKS");
        ENCHANT_ALIAS.put("EFFICIENCY",           "DIG_SPEED");
        ENCHANT_ALIAS.put("FIRE_ASPECT",          "FIRE_ASPECT");
        ENCHANT_ALIAS.put("KNOCKBACK",            "KNOCKBACK");
        ENCHANT_ALIAS.put("THORNS",               "THORNS");
        ENCHANT_ALIAS.put("SILK_TOUCH",           "SILK_TOUCH");
        ENCHANT_ALIAS.put("RESPIRATION",          "OXYGEN");
        ENCHANT_ALIAS.put("AQUA_AFFINITY",        "WATER_WORKER");
        ENCHANT_ALIAS.put("DEPTH_STRIDER",        "DEPTH_STRIDER");
    }

    /**
     * Parse une liste de chaînes de config et retourne une liste de {@link LootEntry}.
     * Les entrées invalides (matériau inconnu, quantité nulle…) sont silencieusement ignorées.
     */
    public static List<LootEntry> parse(List<String> entries) {
        List<LootEntry> result = new ArrayList<>();
        if (entries == null) return result;

        for (String raw : entries) {
            if (raw == null || raw.trim().isEmpty()) continue;
            LootEntry entry = parseSingle(raw.trim());
            if (entry != null) result.add(entry);
        }
        return result;
    }

    /**
     * Parse une seule entrée.
     * Retourne null si l'entrée est invalide.
     */
    public static LootEntry parseSingle(String raw) {
        if (raw == null || raw.isEmpty()) return null;

        // Découpage sur les virgules
        String[] tokens = raw.split(",");
        if (tokens.length == 0) return null;

        // ── 1. Premier token : MATERIAL:quantité ──
        String[] matParts = tokens[0].trim().split(":", 2);
        if (matParts.length < 1) return null;

        String matName = matParts[0].trim().toUpperCase();
        Material mat = resolveMaterial(matName);
        if (mat == null || mat == Material.AIR) return null;

        int amount = 1;
        if (matParts.length >= 2 && !matParts[1].trim().isEmpty()) {
            try { amount = Integer.parseInt(matParts[1].trim()); } catch (NumberFormatException ignored) {}
        }
        if (amount <= 0) amount = 1;

        ItemStack stack = new ItemStack(mat, amount);

        // ── 2. Tokens suivants : ENCHANTMENT:NOM + LEVEL:valeur ou WEIGHT:valeur ──
        int weight = 10; // défaut

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) continue;

            String[] kv = token.split(":", 2);
            if (kv.length < 2) continue;

            String key   = kv[0].trim().toUpperCase();
            String value = kv[1].trim();

            switch (key) {
                case "WEIGHT":
                    try { weight = Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                    break;

                case "ENCHANTMENT":
                    // Le token suivant doit être LEVEL:valeur
                    int level = 1;
                    if (i + 1 < tokens.length) {
                        String next = tokens[i + 1].trim();
                        String[] nextKv = next.split(":", 2);
                        if (nextKv.length == 2 && nextKv[0].trim().equalsIgnoreCase("LEVEL")) {
                            try { level = Integer.parseInt(nextKv[1].trim()); } catch (NumberFormatException ignored) {}
                            i++; // consommer le token LEVEL
                        }
                    }
                    applyEnchantment(stack, value, level);
                    break;

                default:
                    // Token inconnu — on ignore
                    break;
            }
        }

        if (weight <= 0) weight = 1;
        return new LootEntry(stack, weight);
    }

    // ─── Tirage pondéré sans remise ──────────────────────────────────────────

    /**
     * Remplit un inventaire Minecraft avec un tirage pondéré sans remise.
     *
     * @param inventory   Inventaire cible
     * @param entries     Pool d'items parsés
     * @param countMin    Nombre minimum d'items à placer
     * @param countMax    Nombre maximum d'items à placer
     * @param rng         Instance Random à utiliser
     */
    public static void fillInventory(Inventory inventory, List<LootEntry> entries,
                                     int countMin, int countMax, Random rng) {
        if (entries.isEmpty()) return;

        int maxPossible = Math.min(Math.min(countMax, inventory.getSize()), entries.size());
        int minPossible = Math.min(countMin, maxPossible);
        if (minPossible < 1) minPossible = 1;

        int targetCount = minPossible;
        if (maxPossible > minPossible) {
            targetCount = minPossible + rng.nextInt(maxPossible - minPossible + 1);
        }

        // Copies mutables pour le tirage sans remise
        List<ItemStack> remainingItems   = new ArrayList<>();
        List<Integer>   remainingWeights = new ArrayList<>();
        for (LootEntry e : entries) {
            remainingItems.add(e.item);
            remainingWeights.add(e.weight);
        }

        List<ItemStack> chosen = new ArrayList<>();
        for (int i = 0; i < targetCount && !remainingItems.isEmpty(); i++) {
            int totalWeight = 0;
            for (int w : remainingWeights) totalWeight += w;

            int roll  = rng.nextInt(totalWeight);
            int cumul = 0;
            int picked = 0;
            for (int j = 0; j < remainingWeights.size(); j++) {
                cumul += remainingWeights.get(j);
                if (roll < cumul) { picked = j; break; }
            }
            chosen.add(remainingItems.get(picked));
            remainingItems.remove(picked);
            remainingWeights.remove(picked);
        }

        // Slots aléatoires distincts
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) slots.add(i);
        Collections.shuffle(slots, rng);
        for (int i = 0; i < chosen.size() && i < slots.size(); i++) {
            inventory.setItem(slots.get(i), chosen.get(i));
        }
    }

    // ─── Helpers privés ──────────────────────────────────────────────────────

    private static Material resolveMaterial(String name) {
        try {
            Material m = Material.getMaterial(name);
            if (m != null) return m;
        } catch (Exception ignored) {}
        try {
            return Material.valueOf(name);
        } catch (Exception ignored) {}
        return null;
    }

    private static void applyEnchantment(ItemStack stack, String enchName, int level) {
        if (enchName == null || enchName.isEmpty()) return;
        try {
            Enchantment ench = resolveEnchantment(enchName.toUpperCase().trim());
            if (ench == null) return;
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.addEnchant(ench, Math.max(1, level), true);
                stack.setItemMeta(meta);
            } else {
                stack.addUnsafeEnchantment(ench, Math.max(1, level));
            }
        } catch (Exception ignored) {}
    }

    private static Enchantment resolveEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        String n = name.trim().toUpperCase();

        // 1. Correspondance directe
        Enchantment e = Enchantment.getByName(n);
        if (e != null) return e;

        // 2. Alias courants
        String aliased = ENCHANT_ALIAS.get(n);
        if (aliased != null) {
            e = Enchantment.getByName(aliased);
            if (e != null) return e;
        }

        // 3. Parcours tolérant (sans tirets/espaces)
        String normalized = n.replaceAll("[^A-Z0-9]", "");
        for (Enchantment ench : Enchantment.values()) {
            try {
                if (ench.toString().replaceAll("[^A-Z0-9]", "").equalsIgnoreCase(normalized)) return ench;
            } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Method m = ench.getClass().getMethod("getName");
                Object obj = m.invoke(ench);
                if (obj instanceof String && ((String) obj).replaceAll("[^A-Z0-9]", "")
                        .equalsIgnoreCase(normalized)) return ench;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}


