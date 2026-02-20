package fr.kazotaruumc72.elbram.gui;

import fr.kazotaruumc72.elbram.Elbram;
import fr.kazotaruumc72.elbram.managers.KnowledgeManager;
import fr.kazotaruumc72.elbram.managers.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * GUI de 6 lignes (54 slots) du système de connaissances.
 *
 * Ligne 1 (slots 0-8) :
 *   - Slots 0-3 et 5-8 : vitres grises sans nom (décoration)
 *   - Slot 4            : item configurable (navigation / ouverture de menu)
 * Lignes 2-6 (slots 9-53) : boutons d'apprentissage ou de sélection de catégorie
 */
public class KnowledgeGUI implements InventoryHolder {

    private final Elbram plugin;
    private final Player player;
    private Inventory inventory;
    private String currentMenu;

    public KnowledgeGUI(Elbram plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Ouvre le GUI en déterminant automatiquement le menu initial
     * en fonction des permissions du joueur.
     */
    public void open() {
        openMenu(determineInitialMenu());
    }

    /**
     * Ouvre le GUI en affichant le menu spécifié.
     *
     * @param menuName chemin relatif (ex. "informations/tours" ou "connaissances_informations")
     */
    public void openMenu(String menuName) {
        this.currentMenu = menuName;

        MenuManager.MenuConfig config = plugin.getMenuManager().loadMenu(menuName);
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("gui.title", "&8Connaissances"));

        inventory = Bukkit.createInventory(this, 54, title);

        fillTopRow();
        if (config != null) {
            fillContent(config);
        }

        player.openInventory(inventory);
    }

    // -------------------------------------------------------------------------
    // Construction du contenu
    // -------------------------------------------------------------------------

    private void fillTopRow() {
        // Vitre grise sans nom pour les 8 slots de décoration
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = grayPane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.setDisplayName(" ");
            grayPane.setItemMeta(paneMeta);
        }

        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                inventory.setItem(i, grayPane.clone());
            }
        }

        // Item configurable au centre (slot 4)
        inventory.setItem(4, plugin.getMenuManager().createSpecialItem());
    }

    private void fillContent(MenuManager.MenuConfig config) {
        KnowledgeManager km = plugin.getKnowledgeManager();

        for (MenuManager.MenuItem item : config.getItems()) {
            ItemStack itemStack;
            if (item.isKnowledgeItem()) {
                boolean learned = km.hasLearned(player.getUniqueId(), item.getKnowledgeId());
                itemStack = learned ? item.createLearnedItem() : item.createUnlearnedItem();
            } else {
                itemStack = item.createCategoryItem();
            }
            inventory.setItem(item.getSlot(), itemStack);
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /**
     * Détermine le menu à afficher en priorité selon les permissions du joueur.
     * Si le joueur a une permission "elbram.informations.<type>", le menu
     * correspondant est ouvert directement.
     * Sinon, le menu de sélection général est affiché.
     */
    private String determineInitialMenu() {
        if (player.hasPermission("elbram.informations.tours")
                || player.hasPermission("elbram.informations.construction")
                || player.hasPermission("elbram.informations.combat")) {
            return "connaissances_profil";
        }
        return "connaissances_informations";
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCurrentMenu() {
        return currentMenu;
    }
}
