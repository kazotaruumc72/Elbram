package fr.kazotaruumc72.elbram.commands;

import fr.kazotaruumc72.elbram.Elbram;
import fr.kazotaruumc72.elbram.gui.KnowledgeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Gère la commande /profil.
 *
 * Ouvre le GUI de connaissances :
 *   - Première utilisation (aucune permission de catégorie) → menu des compétences
 *   - Sinon → menu des informations (skill tree)
 */
public class ProfilCommand implements CommandExecutor, TabCompleter {

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
