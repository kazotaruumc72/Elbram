package fr.kazotaruumc72.elbram.listeners;

import fr.kazotaruumc72.elbram.Elbram;
import fr.kazotaruumc72.elbram.gui.KnowledgeGUI;
import fr.kazotaruumc72.elbram.managers.KnowledgeManager;
import fr.kazotaruumc72.elbram.managers.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Gère tous les clics dans le KnowledgeGUI.
 */
public class GUIListener implements Listener {

    private final Elbram plugin;

    public GUIListener(Elbram plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof KnowledgeGUI gui)) return;

        // Annule tout déplacement d'item dans ce GUI
        event.setCancelled(true);

        // Ignore les clics hors de l'inventaire du plugin
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getInventory())) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();

        // Slot 4 : item de navigation configurable
        if (slot == 4) {
            handleNavigationClick(player, gui);
            return;
        }

        // Cherche l'item correspondant au slot cliqué dans le menu actuel
        MenuManager.MenuConfig config = plugin.getMenuManager().loadMenu(gui.getCurrentMenu());
        if (config == null) return;

        for (MenuManager.MenuItem item : config.getItems()) {
            if (item.getSlot() == slot) {
                handleMenuItemClick(player, gui, item);
                return;
            }
        }
    }

    // -------------------------------------------------------------------------

    private void handleNavigationClick(Player player, KnowledgeGUI gui) {
        if (player.hasPermission("elbram.informations.tours")
                || player.hasPermission("elbram.informations.construction")
                || player.hasPermission("elbram.informations.combat")
                || player.hasPermission("elbram.informations.exploration")) {
            gui.openMenu("connaissances_profil");
        } else {
            gui.openMenu("connaissances_informations");
        }
    }

    private void handleMenuItemClick(Player player, KnowledgeGUI gui, MenuManager.MenuItem item) {
        if (item.isKnowledgeItem()) {
            learnKnowledge(player, gui, item);
        } else if ("open_submenu".equals(item.getAction())) {
            openSubmenu(player, gui, item);
        } else if ("close".equals(item.getAction())) {
            player.closeInventory();
        }
    }

    private void learnKnowledge(Player player, KnowledgeGUI gui, MenuManager.MenuItem item) {
        KnowledgeManager km = plugin.getKnowledgeManager();
        String id = item.getKnowledgeId();

        if (km.hasLearned(player.getUniqueId(), id)) {
            player.sendMessage("§aVous connaissez déjà cette information !");
            return;
        }

        if (km.isBlocked(player.getUniqueId(), id)) {
            player.sendMessage("§cCette information vous est définitivement inaccessible.");
            return;
        }

        String skill = item.getSkillRequired();
        if (skill != null && !skill.isEmpty() && !km.hasLearned(player.getUniqueId(), skill)) {
            String skillName = plugin.getMenuManager().getKnowledgeName(skill);
            player.sendMessage("§cPrérequis manquant : §e" + skillName);
            return;
        }

        km.learn(player.getUniqueId(), id);
        player.sendMessage("§a✔ Vous avez appris : §e" + id);

        // Rafraîchit le GUI pour refléter l'état appris
        gui.openMenu(gui.getCurrentMenu());
    }

    private void openSubmenu(Player player, KnowledgeGUI gui, MenuManager.MenuItem item) {
        String perm = item.getPermission();
        if (perm != null && !perm.isEmpty() && !player.hasPermission(perm)) {
            player.sendMessage("§cVous n'avez pas accès à cette section.");
            return;
        }
        gui.openMenu(item.getSubmenu());
    }
}
