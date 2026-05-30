package fr.blixow.factionevent.commands.planning;

import fr.blixow.factionevent.enumeration.DayEnum;
import fr.blixow.factionevent.manager.PlanningManager;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlanningCommand implements CommandExecutor {

    // Correspondance DayEnum → DayOfWeek Java
    private static final DayOfWeek[] DAY_MAP = {
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
    };

    // Matériaux pour chaque jour (lundi → dimanche)
    private static final Material[] DAY_MATERIALS = {
            Material.WOOL,   // Lundi    - blanc
            Material.WOOL,   // Mardi    - jaune
            Material.WOOL,   // Mercredi - vert
            Material.WOOL,   // Jeudi    - bleu
            Material.WOOL,   // Vendredi - violet
            Material.WOOL,   // Samedi   - orange
            Material.WOOL    // Dimanche - rouge
    };

    // DyeColor pour chaque jour
    private static final DyeColor[] DAY_COLORS = {
            DyeColor.WHITE,
            DyeColor.YELLOW,
            DyeColor.LIME,
            DyeColor.LIGHT_BLUE,
            DyeColor.PURPLE,
            DyeColor.ORANGE,
            DyeColor.RED
    };

    // Emojis couleur pour chaque jour dans le nom
    private static final String[] DAY_PREFIX = {
            "§f⬛ ",   // Lundi
            "§e⬛ ",   // Mardi
            "§a⬛ ",   // Mercredi
            "§b⬛ ",   // Jeudi
            "§5⬛ ",   // Vendredi
            "§6⬛ ",   // Samedi
            "§c⬛ "    // Dimanche
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cVous devez être un joueur.");
            return true;
        }

        Player player = (Player) sender;

        // GUI de 54 slots (6 lignes)
        Inventory inv = Bukkit.createInventory(null, 54, "§8§lPlanning §8» §cÉvénements");

        // Remplissage fond en verre gris foncé
        ItemStack grayGlass = makeGlass(DyeColor.GRAY, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, grayGlass);
        }

        // Bordure haute et basse en verre noir
        ItemStack blackGlass = makeGlass(DyeColor.BLACK, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, blackGlass);
        for (int i = 45; i < 54; i++) inv.setItem(i, blackGlass);
        // Bordures latérales
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, blackGlass);
            inv.setItem(row * 9 + 8, blackGlass);
        }

        // Déterminer le jour actuel
        DayOfWeek todayDow = LocalDate.now().getDayOfWeek();

        // Item titre central en haut (slot 4)
        ItemStack titleItem = makeItem(Material.NETHER_STAR, "§6§lPlanning des Événements", Arrays.asList(
                "§8§m──────────────────────",
                "§7Consultez les événements",
                "§7programmés de la semaine.",
                "§8§m──────────────────────"
        ));
        inv.setItem(4, titleItem);

        // Placer les 7 jours dans les slots : 10, 11, 12, 13, 14, 15, 16 (ligne 2, colonnes 1-7)
        DayEnum[] days = DayEnum.values();
        int[] daySlots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < days.length; i++) {
            DayEnum day = days[i];
            String valeur = day.getValeur();
            List<String> dailyEvents = PlanningManager.getWeeklyEvents(valeur);

            boolean isToday = DAY_MAP[i].equals(todayDow);

            // Lore de l'item
            List<String> lore = new ArrayList<>();
            lore.add("§8§m──────────────────────");
            if (dailyEvents.isEmpty()) {
                lore.add("§7Aucun événement prévu.");
            } else {
                for (String event : dailyEvents) {
                    lore.add("§8› " + event);
                }
            }
            lore.add("§8§m──────────────────────");
            if (isToday) {
                lore.add("§a✦ Aujourd'hui");
            }

            // Nom de l'item
            String itemName = DAY_PREFIX[i] + (isToday ? "§e§l" : "§f§l") + valeur;

            // Matériau et couleur
            ItemStack item = new ItemStack(DAY_MATERIALS[i], 1, DAY_COLORS[i].getWoolData());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(itemName);
            meta.setLore(lore);
            item.setItemMeta(meta);

            // Si c'est aujourd'hui, on met aussi un cadre brillant (enchant glow trick ou bordure)
            if (isToday) {
                // Surbrillance : remplacer le verre autour avec du verre doré
                ItemStack goldGlass = makeGlass(DyeColor.YELLOW, "§e§lAujourd'hui");
                // Mettre en évidence en dessous (slot + 9) et au-dessus (déjà bordure)
                int slot = daySlots[i];
                if (slot + 9 < 54) inv.setItem(slot + 9, goldGlass);
            }

            inv.setItem(daySlots[i], item);
        }

        // Ligne 4 (slots 28-34) : résumé "Aujourd'hui"
        DayEnum todayEnum = getTodayEnum(todayDow);
        if (todayEnum != null) {
            List<String> todayEvents = PlanningManager.getWeeklyEvents(todayEnum.getValeur());
            List<String> todayLore = new ArrayList<>();
            todayLore.add("§8§m──────────────────────");
            if (todayEvents.isEmpty()) {
                todayLore.add("§7Aucun événement aujourd'hui.");
            } else {
                for (String ev : todayEvents) {
                    todayLore.add("§8› " + ev);
                }
            }
            todayLore.add("§8§m──────────────────────");

            ItemStack todayItem = makeItem(Material.WATCH, "§a§lÉvénements d'aujourd'hui §8(§e" + todayEnum.getValeur() + "§8)", todayLore);
            inv.setItem(31, todayItem);
        }

        player.openInventory(inv);
        return true;
    }

    private static ItemStack makeGlass(DyeColor color, String name) {
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, color.getData());
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(name);
        glass.setItemMeta(meta);
        return glass;
    }

    private static ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static DayEnum getTodayEnum(DayOfWeek dow) {
        DayEnum[] days = DayEnum.values();
        for (int i = 0; i < DAY_MAP.length; i++) {
            if (DAY_MAP[i].equals(dow)) return days[i];
        }
        return null;
    }
}
