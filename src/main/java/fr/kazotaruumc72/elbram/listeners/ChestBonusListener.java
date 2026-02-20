package fr.kazotaruumc72.elbram.listeners;

import fr.kazotaruumc72.elbram.Elbram;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * Applique le bonus "chest_loot" (Chercheur de Trésor) lors de l'ouverture
 * d'un coffre rechargé. Ce listener sert de point d'intégration pour le
 * système de rechargement de coffres (ex. Quantum).
 *
 * Rang I  → +1 item, 20 % de chance par slot bonus
 * Rang II → +2 items, 40 % de chance par slot bonus
 * Rang III→ +3 items, 60 % de chance par slot bonus
 */
public class ChestBonusListener implements Listener {

    private final Elbram plugin;
    private final Random random = new Random();

    public ChestBonusListener(Elbram plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (inv.getType() != InventoryType.CHEST) return;

        int tier = plugin.getBonusManager().getBonusTier(player.getUniqueId(), "chest_loot");
        if (tier <= 0) return;

        // N'améliore que les coffres peu remplis (simulant un coffre fraîchement rechargé).
        long filled = Arrays.stream(inv.getContents()).filter(Objects::nonNull).count();
        if (filled > inv.getSize() / 3) return;

        int bonusSlots = tier;
        for (int i = 0; i < bonusSlots; i++) {
            if (random.nextInt(100) < (20 * tier)) {
                addBonusItem(inv, tier);
            }
        }
    }

    private void addBonusItem(Inventory inv, int tier) {
        int slot = inv.firstEmpty();
        if (slot < 0) return;
        Material mat = switch (tier) {
            case 1  -> random.nextBoolean() ? Material.IRON_INGOT   : Material.GOLD_INGOT;
            case 2  -> random.nextBoolean() ? Material.GOLD_INGOT   : Material.DIAMOND;
            default -> Material.DIAMOND;
        };
        inv.setItem(slot, new ItemStack(mat, 1 + random.nextInt(tier)));
    }
}
