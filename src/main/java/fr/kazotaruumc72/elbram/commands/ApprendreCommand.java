package fr.kazotaruumc72.elbram.commands;

import fr.kazotaruumc72.elbram.Elbram;
import fr.kazotaruumc72.elbram.managers.KnowledgeManager;
import fr.kazotaruumc72.elbram.model.Rarity;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Commande {@code /apprendre <joueur> <connaissance>}
 *
 * <p>Permet à un joueur de transmettre une de ses Informations à un autre joueur.
 * La transmission suit le modèle d'échange asymétrique :</p>
 * <ul>
 *   <li>Le receveur acquiert l'Information.</li>
 *   <li>Le receveur subit une perte aléatoire de certaines de ses Informations
 *       vulnérables (COMMON → EPIC), définitivement verrouillées.</li>
 *   <li>Le donneur perd l'Information transmise ainsi que toutes celles qui en
 *       dépendent (obsolescence en cascade), définitivement verrouillées.</li>
 * </ul>
 * <p>Les Informations de rareté LEGENDARY et TOP_SECRET ne peuvent pas être transmises.</p>
 */
public class ApprendreCommand implements CommandExecutor, TabCompleter {

    private final Elbram plugin;

    public ApprendreCommand(Elbram plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player giver)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        if (!giver.hasPermission("elbram.teach")) {
            giver.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 2) {
            giver.sendMessage("§cUsage : /apprendre <joueur> <connaissance>");
            return true;
        }

        // --- Résolution de la cible ---
        Player receiver = Bukkit.getPlayer(args[0]);
        if (receiver == null) {
            giver.sendMessage("§cJoueur introuvable ou hors ligne : §e" + args[0]);
            return true;
        }
        if (receiver.equals(giver)) {
            giver.sendMessage("§cVous ne pouvez pas vous enseigner une connaissance à vous-même.");
            return true;
        }

        String knowledgeId = args[1];

        // --- Vérification rapide de la rareté avant d'appeler teach() ---
        Rarity rarity = plugin.getMenuManager().getKnowledgeRarity(knowledgeId);
        if (!rarity.isTransferable()) {
            giver.sendMessage("§cCette Information est de rareté " + rarity.getDisplayName()
                    + " §cet ne peut pas être transmise.");
            return true;
        }

        // --- Transfert asymétrique ---
        KnowledgeManager km = plugin.getKnowledgeManager();
        KnowledgeManager.TeachResult result = km.teach(
                giver.getUniqueId(), receiver.getUniqueId(), knowledgeId);

        switch (result.getStatus()) {
            case SUCCESS -> handleSuccess(giver, receiver, knowledgeId, result);
            case NOT_TRANSFERABLE ->
                giver.sendMessage("§cCette Information ne peut pas être transmise.");
            case GIVER_DOES_NOT_HAVE ->
                giver.sendMessage("§cVous ne possédez pas l'Information §e" + knowledgeId + "§c.");
            case RECEIVER_ALREADY_HAS ->
                giver.sendMessage("§e" + receiver.getName() + " §cpossède déjà cette Information.");
            case RECEIVER_BLOCKED ->
                giver.sendMessage("§e" + receiver.getName()
                        + " §cne peut plus acquérir cette Information (verrouillée définitivement).");
        }
        return true;
    }

    // -------------------------------------------------------------------------

    private void handleSuccess(Player giver, Player receiver, String knowledgeId,
                               KnowledgeManager.TeachResult result) {
        String knowledgeName = plugin.getMenuManager().getKnowledgeName(knowledgeId);

        // Message au donneur
        giver.sendMessage("§aVous avez transmis §e" + knowledgeName + " §aà §b" + receiver.getName() + "§a.");
        giver.sendMessage("§7Votre savoir se dilue... les connaissances liées disparaissent.");
        if (!result.getGiverCascadeLosses().isEmpty()) {
            String lost = result.getGiverCascadeLosses().stream()
                    .map(id -> plugin.getMenuManager().getKnowledgeName(id))
                    .collect(Collectors.joining("§7, §c"));
            giver.sendMessage("§cInformations perdues (cascade) : §c" + lost);
        }

        // Message au receveur
        receiver.sendMessage("§b" + giver.getName() + " §avous a transmis l'Information §e" + knowledgeName + "§a.");
        if (!result.getReceiverLosses().isEmpty()) {
            String lost = result.getReceiverLosses().stream()
                    .map(id -> plugin.getMenuManager().getKnowledgeName(id))
                    .collect(Collectors.joining("§7, §c"));
            receiver.sendMessage("§cLa réception a perturbé vos connaissances. Perdues : §c" + lost);
        }
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player giver)) return List.of();

        if (args.length == 1) {
            // Proposer les joueurs en ligne (sauf soi-même)
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(giver))
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            // Proposer les connaissances que le donneur possède et qui sont transférables
            Set<String> learned = plugin.getKnowledgeManager().getAllLearned(giver.getUniqueId());
            return learned.stream()
                    .filter(id -> plugin.getMenuManager().getKnowledgeRarity(id).isTransferable())
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
