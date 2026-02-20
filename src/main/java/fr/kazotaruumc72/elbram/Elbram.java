package fr.kazotaruumc72.elbram;

import fr.kazotaruumc72.elbram.commands.ElbramCommand;
import fr.kazotaruumc72.elbram.commands.ProfilCommand;
import fr.kazotaruumc72.elbram.listeners.GUIListener;
import fr.kazotaruumc72.elbram.listeners.ItemListener;
import fr.kazotaruumc72.elbram.managers.KnowledgeManager;
import fr.kazotaruumc72.elbram.managers.MenuManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Elbram extends JavaPlugin {

    private static Elbram instance;
    private KnowledgeManager knowledgeManager;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        knowledgeManager = new KnowledgeManager(this);
        menuManager = new MenuManager(this);

        ElbramCommand elbramCommand = new ElbramCommand(this);
        getCommand("elbram").setExecutor(elbramCommand);
        getCommand("elbram").setTabCompleter(elbramCommand);
        ProfilCommand profilCommand = new ProfilCommand(this);
        getCommand("profil").setExecutor(profilCommand);
        getCommand("profil").setTabCompleter(profilCommand);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);

        getLogger().info("Elbram activé !");
    }

    @Override
    public void onDisable() {
        if (knowledgeManager != null) {
            knowledgeManager.saveAll();
        }
        getLogger().info("Elbram désactivé !");
    }

    public static Elbram getInstance() {
        return instance;
    }

    public KnowledgeManager getKnowledgeManager() {
        return knowledgeManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }
}
