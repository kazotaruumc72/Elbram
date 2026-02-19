package fr.kazotaruumc72.elbram.managers;

import fr.kazotaruumc72.elbram.Elbram;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gère les connaissances apprises par chaque joueur.
 * Les données sont stockées dans plugins/Elbram/players/<uuid>.yml.
 * Une connaissance apprise ne peut jamais être oubliée.
 */
public class KnowledgeManager {

    private final Elbram plugin;
    private final Map<UUID, Set<String>> playerKnowledge = new HashMap<>();
    private final File playersDir;

    public KnowledgeManager(Elbram plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) {
            playersDir.mkdirs();
        }
    }

    /** Retourne true si le joueur a déjà appris cette connaissance. */
    public boolean hasLearned(UUID playerId, String knowledgeId) {
        return getKnowledge(playerId).contains(knowledgeId);
    }

    /** Marque une connaissance comme apprise (irréversible). */
    public void learn(UUID playerId, String knowledgeId) {
        getKnowledge(playerId).add(knowledgeId);
        savePlayer(playerId);
    }

    /** Retourne toutes les connaissances apprises par un joueur. */
    public Set<String> getAllLearned(UUID playerId) {
        return Collections.unmodifiableSet(getKnowledge(playerId));
    }

    private Set<String> getKnowledge(UUID playerId) {
        return playerKnowledge.computeIfAbsent(playerId, this::loadPlayer);
    }

    private Set<String> loadPlayer(UUID playerId) {
        File file = new File(playersDir, playerId + ".yml");
        if (!file.exists()) {
            return new HashSet<>();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return new HashSet<>(config.getStringList("knowledge"));
    }

    private void savePlayer(UUID playerId) {
        File file = new File(playersDir, playerId + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("knowledge", new ArrayList<>(getKnowledge(playerId)));
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder les données du joueur " + playerId + " : " + e.getMessage());
        }
    }

    /** Sauvegarde toutes les données en mémoire sur le disque. */
    public void saveAll() {
        for (UUID playerId : playerKnowledge.keySet()) {
            savePlayer(playerId);
        }
    }
}
