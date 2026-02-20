package fr.kazotaruumc72.elbram.managers;

import fr.kazotaruumc72.elbram.Elbram;
import fr.kazotaruumc72.elbram.model.Rarity;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gère les connaissances apprises par chaque joueur ainsi que les connaissances
 * bloquées (définitivement non réacquérables).
 *
 * Les données sont stockées dans plugins/Elbram/players/&lt;uuid&gt;.yml.
 * Une connaissance bloquée ne peut jamais être réapprise.
 */
public class KnowledgeManager {

    private final Elbram plugin;
    private final Map<UUID, Set<String>> playerKnowledge = new HashMap<>();
    /** Connaissances définitivement verrouillées pour chaque joueur (ne peuvent être réapprises). */
    private final Map<UUID, Set<String>> playerBlocked   = new HashMap<>();
    private final File playersDir;

    public KnowledgeManager(Elbram plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists()) {
            playersDir.mkdirs();
        }
    }

    // -------------------------------------------------------------------------
    // Lecture
    // -------------------------------------------------------------------------

    /** Retourne true si le joueur a déjà appris cette connaissance. */
    public boolean hasLearned(UUID playerId, String knowledgeId) {
        return getKnowledge(playerId).contains(knowledgeId);
    }

    /** Retourne true si la connaissance est définitivement verrouillée pour ce joueur. */
    public boolean isBlocked(UUID playerId, String knowledgeId) {
        return getBlocked(playerId).contains(knowledgeId);
    }

    /** Retourne toutes les connaissances apprises par un joueur (vue immuable). */
    public Set<String> getAllLearned(UUID playerId) {
        return Collections.unmodifiableSet(getKnowledge(playerId));
    }

    /** Retourne toutes les connaissances bloquées pour un joueur (vue immuable). */
    public Set<String> getAllBlocked(UUID playerId) {
        return Collections.unmodifiableSet(getBlocked(playerId));
    }

    // -------------------------------------------------------------------------
    // Mutation simple
    // -------------------------------------------------------------------------

    /**
     * Marque une connaissance comme apprise si elle n'est pas bloquée.
     *
     * @return {@code true} si l'apprentissage a bien eu lieu,
     *         {@code false} si la connaissance était déjà apprise ou bloquée.
     */
    public boolean learn(UUID playerId, String knowledgeId) {
        if (isBlocked(playerId, knowledgeId)) return false;
        getKnowledge(playerId).add(knowledgeId);
        savePlayer(playerId);
        return true;
    }

    // -------------------------------------------------------------------------
    // Transfert asymétrique (/apprendre)
    // -------------------------------------------------------------------------

    /**
     * Transfère une connaissance du donneur ({@code giverId}) au receveur ({@code receiverId}).
     *
     * <p>Règles métier appliquées :</p>
     * <ol>
     *   <li>La rareté doit être transférable (COMMON → EPIC).</li>
     *   <li>Le donneur doit posséder la connaissance.</li>
     *   <li>Le receveur ne doit ni posséder ni avoir bloquée la connaissance.</li>
     *   <li>Le receveur apprend la connaissance.</li>
     *   <li>Le receveur subit une perte aléatoire : chaque Information vulnérable
     *       (COMMON → EPIC) a une probabilité configurable d'être définitivement perdue.</li>
     *   <li>Le donneur subit une perte en cascade (obsolescence descendante) :
     *       la connaissance transmise et toutes celles qui en dépendent (skill_required)
     *       sont retirées et définitivement bloquées pour lui.</li>
     * </ol>
     *
     * @param giverId    UUID du joueur qui enseigne
     * @param receiverId UUID du joueur qui reçoit
     * @param knowledgeId identifiant de la connaissance à transférer
     * @return un {@link TeachResult} décrivant le résultat de l'opération
     */
    public TeachResult teach(UUID giverId, UUID receiverId, String knowledgeId) {
        // 1. Rareté transférable ?
        Rarity rarity = plugin.getMenuManager().getKnowledgeRarity(knowledgeId);
        if (!rarity.isTransferable()) {
            return new TeachResult(TeachResult.Status.NOT_TRANSFERABLE, List.of(), List.of());
        }

        // 2. Le donneur possède la connaissance ?
        if (!hasLearned(giverId, knowledgeId)) {
            return new TeachResult(TeachResult.Status.GIVER_DOES_NOT_HAVE, List.of(), List.of());
        }

        // 3. Le receveur ne la possède pas et elle n'est pas bloquée pour lui ?
        if (hasLearned(receiverId, knowledgeId)) {
            return new TeachResult(TeachResult.Status.RECEIVER_ALREADY_HAS, List.of(), List.of());
        }
        if (isBlocked(receiverId, knowledgeId)) {
            return new TeachResult(TeachResult.Status.RECEIVER_BLOCKED, List.of(), List.of());
        }

        // 4. Transfert effectif
        getKnowledge(receiverId).add(knowledgeId);

        // 5. Perte aléatoire chez le receveur (avant sauvegarde)
        List<String> receiverLosses = applyRandomLoss(receiverId, knowledgeId);

        // 6. Perte en cascade chez le donneur
        List<String> giverLosses = applyCascadeLoss(giverId, knowledgeId);

        // Sauvegarde finale des deux joueurs
        savePlayer(receiverId);
        savePlayer(giverId);

        return new TeachResult(TeachResult.Status.SUCCESS, receiverLosses, giverLosses);
    }

    // -------------------------------------------------------------------------
    // Mécanismes de perte internes
    // -------------------------------------------------------------------------

    /**
     * Applique la perte aléatoire chez le receveur.
     * Chaque connaissance vulnérable (COMMON → EPIC) a une probabilité
     * {@code transfer.receiver-loss-chance} % d'être définitivement perdue.
     *
     * @param receiverId    UUID du receveur
     * @param justLearnedId identifiant de la connaissance fraîchement reçue (immunisée)
     * @return liste des identifiants définitivement perdus
     */
    private List<String> applyRandomLoss(UUID receiverId, String justLearnedId) {
        List<String> lost = new ArrayList<>();
        double lossChance = plugin.getConfig().getDouble("transfer.receiver-loss-chance", 30.0) / 100.0;
        if (lossChance <= 0) return lost;

        // Copie pour éviter la modification concurrente
        Set<String> snapshot = new HashSet<>(getKnowledge(receiverId));
        snapshot.remove(justLearnedId);

        MenuManager mm = plugin.getMenuManager();
        for (String id : snapshot) {
            Rarity r = mm.getKnowledgeRarity(id);
            if (r.isLossVulnerable() && ThreadLocalRandom.current().nextDouble() < lossChance) {
                getKnowledge(receiverId).remove(id);
                getBlocked(receiverId).add(id);
                lost.add(id);
            }
        }
        return lost;
    }

    /**
     * Applique l'obsolescence en cascade chez le donneur.
     * La connaissance transmise ET toutes celles qui en dépendent directement ou
     * indirectement (via {@code skill_required}) sont retirées et définitivement bloquées.
     *
     * @param giverId     UUID du donneur
     * @param knowledgeId identifiant de la connaissance transmise
     * @return liste des identifiants effectivement retirés du donneur
     */
    private List<String> applyCascadeLoss(UUID giverId, String knowledgeId) {
        Set<String> toBlock = new LinkedHashSet<>();
        collectCascade(knowledgeId, toBlock, new HashSet<>());

        List<String> actuallyLost = new ArrayList<>();
        for (String id : toBlock) {
            getBlocked(giverId).add(id);
            if (getKnowledge(giverId).remove(id)) {
                actuallyLost.add(id);
            }
        }
        return actuallyLost;
    }

    /**
     * Collecte récursivement tous les identifiants impactés par la cascade,
     * en suivant les liens {@code skill_required}.
     */
    private void collectCascade(String knowledgeId, Set<String> result, Set<String> visited) {
        if (!visited.add(knowledgeId)) return; // déjà visité
        result.add(knowledgeId);
        for (String dependent : plugin.getMenuManager().getDependentKnowledge(knowledgeId)) {
            collectCascade(dependent, result, visited);
        }
    }

    // -------------------------------------------------------------------------
    // Accès aux données internes
    // -------------------------------------------------------------------------

    private Set<String> getKnowledge(UUID playerId) {
        ensureLoaded(playerId);
        return playerKnowledge.get(playerId);
    }

    private Set<String> getBlocked(UUID playerId) {
        ensureLoaded(playerId);
        return playerBlocked.get(playerId);
    }

    /** Charge atomiquement knowledge et blocked depuis le disque si pas encore en mémoire. */
    private void ensureLoaded(UUID playerId) {
        if (!playerKnowledge.containsKey(playerId)) {
            PlayerData data = loadPlayerData(playerId);
            playerKnowledge.put(playerId, data.knowledge);
            playerBlocked.put(playerId, data.blocked);
        }
    }

    // -------------------------------------------------------------------------
    // Persistance
    // -------------------------------------------------------------------------

    private PlayerData loadPlayerData(UUID playerId) {
        File file = new File(playersDir, playerId + ".yml");
        if (!file.exists()) {
            return new PlayerData(new HashSet<>(), new HashSet<>());
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<String> knowledge = new HashSet<>(config.getStringList("knowledge"));
        Set<String> blocked   = new HashSet<>(config.getStringList("blocked"));
        return new PlayerData(knowledge, blocked);
    }

    private void savePlayer(UUID playerId) {
        File file = new File(playersDir, playerId + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("knowledge", new ArrayList<>(getKnowledge(playerId)));
        config.set("blocked",   new ArrayList<>(getBlocked(playerId)));
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder les données du joueur " + playerId + " : " + e.getMessage());
        }
    }

    /** Sauvegarde toutes les données en mémoire sur le disque. */
    public void saveAll() {
        Set<UUID> allPlayers = new HashSet<>(playerKnowledge.keySet());
        allPlayers.addAll(playerBlocked.keySet());
        for (UUID playerId : allPlayers) {
            savePlayer(playerId);
        }
    }

    // =========================================================================
    // Classes imbriquées
    // =========================================================================

    /** Données combinées pour un joueur (chargement atomique). */
    private record PlayerData(Set<String> knowledge, Set<String> blocked) {}

    /**
     * Résultat d'un appel à {@link KnowledgeManager#teach}.
     */
    public static class TeachResult {

        public enum Status {
            /** Transfert effectué avec succès. */
            SUCCESS,
            /** La rareté de l'Information interdit le transfert (LEGENDARY / TOP_SECRET). */
            NOT_TRANSFERABLE,
            /** Le donneur ne possède pas cette Information. */
            GIVER_DOES_NOT_HAVE,
            /** Le receveur possède déjà cette Information. */
            RECEIVER_ALREADY_HAS,
            /** Cette Information est définitivement bloquée pour le receveur. */
            RECEIVER_BLOCKED
        }

        private final Status status;
        private final List<String> receiverLosses;
        private final List<String> giverCascadeLosses;

        public TeachResult(Status status, List<String> receiverLosses, List<String> giverCascadeLosses) {
            this.status             = status;
            this.receiverLosses     = Collections.unmodifiableList(receiverLosses);
            this.giverCascadeLosses = Collections.unmodifiableList(giverCascadeLosses);
        }

        public Status getStatus()                    { return status; }
        public boolean isSuccess()                   { return status == Status.SUCCESS; }
        /** Connaissances définitivement perdues par le receveur (perte aléatoire). */
        public List<String> getReceiverLosses()      { return receiverLosses; }
        /** Connaissances définitivement perdues par le donneur (obsolescence cascade). */
        public List<String> getGiverCascadeLosses()  { return giverCascadeLosses; }
    }
}
