package fr.kazotaruumc72.elbram.managers;

import fr.kazotaruumc72.elbram.Elbram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gère les bonus accordés aux joueurs via les informations apprises.
 * Chaque information peut déclarer une liste de bonus (ex. "chest_loot:1").
 * Les bonus sont de la forme "identifiant" ou "identifiant:rang".
 */
public class BonusManager {

    private final Elbram plugin;
    /** knowledgeId → liste de bonus strings (ex. ["chest_loot:1"]) */
    private final Map<String, List<String>> knowledgeBonuses = new HashMap<>();

    public BonusManager(Elbram plugin) {
        this.plugin = plugin;
    }

    /**
     * Parcourt tous les menus enregistrés et enregistre les bonus déclarés
     * sur les items de connaissance. À appeler après l'initialisation du MenuManager.
     */
    public void init() {
        MenuManager mm = plugin.getMenuManager();
        for (String menuName : mm.getRegisteredMenus()) {
            MenuManager.MenuConfig config = mm.loadMenu(menuName);
            if (config == null) continue;
            for (MenuManager.MenuItem item : config.getItems()) {
                if (item.isKnowledgeItem() && !item.getBonuses().isEmpty()) {
                    registerBonus(item.getKnowledgeId(), item.getBonuses());
                }
            }
        }
    }

    /** Associe une liste de bonus à un knowledgeId. */
    public void registerBonus(String knowledgeId, List<String> bonuses) {
        knowledgeBonuses.put(knowledgeId, new ArrayList<>(bonuses));
    }

    /**
     * Retourne true si le joueur possède au moins une connaissance qui accorde
     * le bonus identifié par {@code bonusId}.
     */
    public boolean hasBonus(UUID playerId, String bonusId) {
        KnowledgeManager km = plugin.getKnowledgeManager();
        for (Map.Entry<String, List<String>> entry : knowledgeBonuses.entrySet()) {
            if (km.hasLearned(playerId, entry.getKey())) {
                for (String bonus : entry.getValue()) {
                    if (bonus.equals(bonusId) || bonus.startsWith(bonusId + ":")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Retourne le rang le plus élevé du bonus {@code bonusId} que possède le joueur.
     * Un bonus sans rang (ex. "chest_loot") vaut 1. Retourne 0 si le joueur n'a pas le bonus.
     */
    public int getBonusTier(UUID playerId, String bonusId) {
        KnowledgeManager km = plugin.getKnowledgeManager();
        int maxTier = 0;
        for (Map.Entry<String, List<String>> entry : knowledgeBonuses.entrySet()) {
            if (km.hasLearned(playerId, entry.getKey())) {
                for (String bonus : entry.getValue()) {
                    if (bonus.equals(bonusId)) {
                        maxTier = Math.max(maxTier, 1);
                    } else if (bonus.startsWith(bonusId + ":")) {
                        try {
                            int tier = Integer.parseInt(bonus.substring(bonusId.length() + 1));
                            maxTier = Math.max(maxTier, tier);
                        } catch (NumberFormatException ignored) {
                            plugin.getLogger().warning("Rang de bonus invalide pour '" + entry.getKey() + "' : " + bonus);
                        }
                    }
                }
            }
        }
        return maxTier;
    }
}
