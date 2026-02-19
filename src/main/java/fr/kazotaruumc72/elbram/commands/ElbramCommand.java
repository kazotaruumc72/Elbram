package fr.kazotaruumc72.elbram.commands;

import fr.kazotaruumc72.elbram.Elbram;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gère la commande principale /elbram.
 *
 * Sous-commandes :
 *   /elbram apprendre [joueur]  – Donne le Livre de Connaissances
 *   /elbram reload              – Recharge la configuration
 */
public class ElbramCommand implements CommandExecutor, TabCompleter {

    private final Elbram plugin;

    public ElbramCommand(Elbram plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "apprendre" -> handleApprendre(sender, args);
            case "reload"    -> handleReload(sender);
            default          -> sendHelp(sender);
        }
        return true;
    }

    // -------------------------------------------------------------------------

    private void handleApprendre(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elbram.apprendre")) {
            sender.sendMessage("§cVous n'avez pas la permission d'utiliser cette commande.");
            return;
        }

        Player target;
        if (args.length >= 2) {
            // Donner à un autre joueur (nécessite elbram.admin)
            if (!sender.hasPermission("elbram.admin")) {
                sender.sendMessage("§cVous n'avez pas la permission de donner à un autre joueur.");
                return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJoueur introuvable : §e" + args[1]);
                return;
            }
        } else {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cVous devez spécifier un joueur.");
                return;
            }
            target = p;
        }

        ItemStack book = plugin.getMenuManager().createKnowledgeBookItem();
        target.getInventory().addItem(book);
        target.sendMessage("§aVous avez reçu le §6Livre de Connaissances §a!");

        if (!target.equals(sender)) {
            sender.sendMessage("§aLe §6Livre de Connaissances §aa été donné à §e" + target.getName() + "§a !");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("elbram.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission de recharger le plugin.");
            return;
        }
        plugin.reloadConfig();
        sender.sendMessage("§aElbram rechargé avec succès !");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== Elbram ===");
        sender.sendMessage("§e/elbram apprendre §7— Recevoir le Livre de Connaissances");
        sender.sendMessage("§e/elbram apprendre <joueur> §7— Donner le livre à un joueur");
        sender.sendMessage("§e/elbram reload §7— Recharger la configuration");
    }

    // -------------------------------------------------------------------------
    // Tab completion
    // -------------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("apprendre", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && "apprendre".equalsIgnoreCase(args[0])
                && sender.hasPermission("elbram.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
