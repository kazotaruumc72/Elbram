package fr.kazotaruumc72.elbram.managers;

import fr.kazotaruumc72.elbram.Elbram;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Charge les configurations de menus depuis les fichiers YAML et crée les ItemStacks.
 */
public class MenuManager {

    /** Clé NBT identifiant le livre de connaissances physique. */
    public static final String KNOWLEDGE_BOOK_KEY = "knowledge_book";

    private final Elbram plugin;

    public MenuManager(Elbram plugin) {
        this.plugin = plugin;
        saveDefaultMenu("menus/connaissances_informations.yml");
        saveDefaultMenu("menus/informations/tours.yml");
    }

    private void saveDefaultMenu(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
    }

    // -------------------------------------------------------------------------
    // Chargement des menus
    // -------------------------------------------------------------------------

    /**
     * Charge et retourne la configuration d'un menu depuis le dossier du plugin
     * (ou depuis le JAR si le fichier n'existe pas encore sur le disque).
     *
     * @param menuName chemin relatif sans le préfixe "menus/" ni le suffixe ".yml"
     *                 (ex. : "informations/tours" ou "connaissances_informations")
     */
    public MenuConfig loadMenu(String menuName) {
        String resourcePath = "menus/" + menuName + ".yml";
        File file = new File(plugin.getDataFolder(), resourcePath);

        YamlConfiguration config;
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            InputStream stream = plugin.getResource(resourcePath);
            if (stream == null) return null;
            config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
        }

        String title = color(config.getString("title", "Menu"));
        List<MenuItem> items = new ArrayList<>();

        List<?> rawList = config.getList("items");
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> raw) {
                    @SuppressWarnings("unchecked")
                    MenuItem item = parseMenuItem((Map<String, Object>) raw);
                    if (item != null) items.add(item);
                }
            }
        }
        return new MenuConfig(title, items);
    }

    private MenuItem parseMenuItem(Map<String, Object> map) {
        try {
            int slot = toInt(map.getOrDefault("slot", 0));
            String knowledgeId = (String) map.get("knowledge_id");

            if (knowledgeId != null) {
                // --- Bouton de connaissance ---
                String nameUnlearned = color((String) map.getOrDefault("name_unlearned", "&7Inconnu"));
                String nameLearned   = color((String) map.getOrDefault("name_learned",   "&aAppris"));
                List<String> loreUnlearned = colorList(getStringList(map, "lore_unlearned"));
                List<String> loreLearned   = colorList(getStringList(map, "lore_learned"));
                Material matUnlearned = parseMaterial((String) map.getOrDefault("material_unlearned", "GRAY_STAINED_GLASS_PANE"));
                Material matLearned   = parseMaterial((String) map.getOrDefault("material_learned",   "GLOWSTONE"));
                int cmdUnlearned = toInt(map.getOrDefault("custom_model_data_unlearned", 0));
                int cmdLearned   = toInt(map.getOrDefault("custom_model_data_learned",   0));

                return new MenuItem(slot, knowledgeId,
                        nameUnlearned, nameLearned,
                        loreUnlearned, loreLearned,
                        matUnlearned, matLearned,
                        cmdUnlearned, cmdLearned);
            } else {
                // --- Bouton de catégorie / navigation ---
                String name   = color((String) map.getOrDefault("name",     "&fItem"));
                List<String> lore = colorList(getStringList(map, "lore"));
                Material material = parseMaterial((String) map.getOrDefault("material", "PAPER"));
                String action  = (String) map.get("action");
                String submenu = (String) map.get("submenu");
                String perm    = (String) map.get("permission");
                int customModelData = toInt(map.getOrDefault("custom_model_data", 0));

                return new MenuItem(slot, material, name, lore, action, submenu, perm, customModelData);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors du parsing d'un item de menu : " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Création des ItemStacks spéciaux
    // -------------------------------------------------------------------------

    /** Crée l'item configurable qui occupe le slot 4 (milieu de la ligne du haut). */
    public ItemStack createSpecialItem() {
        FileConfiguration cfg = plugin.getConfig();
        ItemStack item = buildItem(
                cfg.getString("gui.item.material", "BOOK"),
                cfg.getInt("gui.item.custom-model-data", 0),
                cfg.getString("gui.item.name", "&6Connaissances"),
                cfg.getStringList("gui.item.lore"));
        return item;
    }

    /**
     * Crée le livre de connaissances physique donné par /elbram apprendre.
     * L'item est marqué avec un tag NBT pour être reconnu par ItemListener.
     */
    public ItemStack createKnowledgeBookItem() {
        FileConfiguration cfg = plugin.getConfig();
        ItemStack item = buildItem(
                cfg.getString("knowledge-item.material", "BOOK"),
                cfg.getInt("knowledge-item.custom-model-data", 0),
                cfg.getString("knowledge-item.name", "&6Livre de Connaissances"),
                cfg.getStringList("knowledge-item.lore"));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        NamespacedKey key = new NamespacedKey(plugin, KNOWLEDGE_BOOK_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Utilitaires privés
    // -------------------------------------------------------------------------

    private ItemStack buildItem(String materialName, int customModelData, String rawName, List<String> rawLore) {
        Material material = parseMaterial(materialName);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(color(rawName));
        meta.setLore(colorList(rawLore));
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static List<String> colorList(List<String> list) {
        return list.stream().map(MenuManager::color).collect(Collectors.toList());
    }

    private static Material parseMaterial(String name) {
        if (name == null) return Material.PAPER;
        Material m = Material.matchMaterial(name);
        return m != null ? m : Material.PAPER;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List<?>) return (List<String>) val;
        return new ArrayList<>();
    }

    private static int toInt(Object val) {
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    // =========================================================================
    // Classes imbriquées
    // =========================================================================

    public static class MenuConfig {
        private final String title;
        private final List<MenuItem> items;

        public MenuConfig(String title, List<MenuItem> items) {
            this.title = title;
            this.items = items;
        }

        public String getTitle() { return title; }
        public List<MenuItem> getItems() { return items; }
    }

    public static class MenuItem {
        private final int slot;
        private final boolean knowledgeItem;

        // --- Bouton de catégorie ---
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final String action;
        private final String submenu;
        private final String permission;
        private final int customModelData;

        // --- Bouton de connaissance ---
        private final String knowledgeId;
        private final String nameUnlearned;
        private final String nameLearned;
        private final List<String> loreUnlearned;
        private final List<String> loreLearned;
        private final Material materialUnlearned;
        private final Material materialLearned;
        private final int customModelDataUnlearned;
        private final int customModelDataLearned;

        /** Constructeur pour un bouton de catégorie / navigation. */
        public MenuItem(int slot, Material material, String name, List<String> lore,
                        String action, String submenu, String permission, int customModelData) {
            this.slot = slot;
            this.knowledgeItem = false;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.action = action;
            this.submenu = submenu;
            this.permission = permission;
            this.customModelData = customModelData;
            // Champs connaissance inutilisés
            this.knowledgeId = null;
            this.nameUnlearned = null;
            this.nameLearned = null;
            this.loreUnlearned = null;
            this.loreLearned = null;
            this.materialUnlearned = null;
            this.materialLearned = null;
            this.customModelDataUnlearned = 0;
            this.customModelDataLearned = 0;
        }

        /** Constructeur pour un bouton de connaissance. */
        public MenuItem(int slot, String knowledgeId,
                        String nameUnlearned, String nameLearned,
                        List<String> loreUnlearned, List<String> loreLearned,
                        Material materialUnlearned, Material materialLearned,
                        int customModelDataUnlearned, int customModelDataLearned) {
            this.slot = slot;
            this.knowledgeItem = true;
            this.knowledgeId = knowledgeId;
            this.nameUnlearned = nameUnlearned;
            this.nameLearned = nameLearned;
            this.loreUnlearned = loreUnlearned;
            this.loreLearned = loreLearned;
            this.materialUnlearned = materialUnlearned;
            this.materialLearned = materialLearned;
            this.customModelDataUnlearned = customModelDataUnlearned;
            this.customModelDataLearned = customModelDataLearned;
            // Champs catégorie inutilisés
            this.material = null;
            this.name = null;
            this.lore = null;
            this.action = null;
            this.submenu = null;
            this.permission = null;
            this.customModelData = 0;
        }

        public ItemStack createCategoryItem() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);
                if (customModelData > 0) meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
            return item;
        }

        public ItemStack createUnlearnedItem() {
            ItemStack item = new ItemStack(materialUnlearned);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(nameUnlearned);
                meta.setLore(loreUnlearned);
                if (customModelDataUnlearned > 0) meta.setCustomModelData(customModelDataUnlearned);
                item.setItemMeta(meta);
            }
            return item;
        }

        public ItemStack createLearnedItem() {
            ItemStack item = new ItemStack(materialLearned);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(nameLearned);
                meta.setLore(loreLearned);
                if (customModelDataLearned > 0) meta.setCustomModelData(customModelDataLearned);
                item.setItemMeta(meta);
            }
            return item;
        }

        public int getSlot() { return slot; }
        public boolean isKnowledgeItem() { return knowledgeItem; }
        public String getAction() { return action; }
        public String getSubmenu() { return submenu; }
        public String getPermission() { return permission; }
        public String getKnowledgeId() { return knowledgeId; }
    }
}
