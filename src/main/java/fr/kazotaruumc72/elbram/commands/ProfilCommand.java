package fr.kazotaruumc72.elbram.commands;

import fr.kazotaruumc72.elbram.Elbram;
import fr.kazotaruumc72.elbram.gui.KnowledgeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Gère la commande /profil.
 *
 * Ouvre le GUI de connaissances :
 *   - Première utilisation (aucune permission de catégorie) → menu des compétences
 *   - Sinon → menu des informations (skill tree)
 */
public class ProfilCommand implements CommandExecutor {

    private final Elbram plugin;

    public ProfilCommand(Elbram plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande est réservée aux joueurs.");
            return true;
        }

        KnowledgeGUI gui = new KnowledgeGUI(plugin, player);
        gui.open();
        return true;
    }
}
