package fr.kazotaruumc72.elbram.listeners;

import fr.kazotaruumc72.elbram.Elbram;
import fr.kazotaruumc72.elbram.gui.KnowledgeGUI;
import fr.kazotaruumc72.elbram.managers.MenuManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Détecte le clic droit sur le Livre de Connaissances physique
 * et ouvre le KnowledgeGUI.
 */
public class ItemListener implements Listener {

    private final Elbram plugin;
    private final NamespacedKey bookKey;

    public ItemListener(Elbram plugin) {
        this.plugin = plugin;
        this.bookKey = new NamespacedKey(plugin, MenuManager.KNOWLEDGE_BOOK_KEY);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(bookKey, PersistentDataType.BYTE)) return;

        event.setCancelled(true);

        KnowledgeGUI gui = new KnowledgeGUI(plugin, player);
        gui.open();
    }
}
