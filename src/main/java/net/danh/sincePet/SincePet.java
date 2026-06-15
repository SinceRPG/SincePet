package net.danh.sincePet;

import net.danh.sincePet.commands.PetCommandRegistrar;
import net.danh.sincePet.data.DatabaseManager;
import net.danh.sincePet.data.PlayerDataHandler;
import net.danh.sincePet.events.JoinQuit;
import net.danh.sincePet.hooks.WorldGuardHook;
import net.danh.sincePet.mythic.MythicDropRegister;
import net.danh.sincePet.pets.PetAbilityTriggerListener;
import net.danh.sincePet.pets.PetListener;
import net.danh.sincePet.pets.PetManager;
import net.danh.sincePet.utils.ConfigUtils;
import net.danh.sincePet.utils.SchedulerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main entry point for the SincePet plugin.
 * Handles the initialization of configurations, databases, managers, listeners, and commands.
 */
public final class SincePet extends JavaPlugin {
    private static SincePet plugin;
    private MiniMessage miniMessage;

    private ConfigUtils configFile;
    private ConfigUtils petGuiFile;
    private ConfigUtils petMessagesFile;

    private PetManager petManager;
    private WorldGuardHook worldGuardHook;
    private PlayerDataHandler playerDataHandler;
    private DatabaseManager databaseManager;

    /**
     * Retrieves the singleton instance of the plugin.
     *
     * @return The SincePet plugin instance.
     */
    public static SincePet getPlugin() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
            worldGuardHook.register();
            getLogger().info("Hooked into WorldGuard successfully!");
        }
    }

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();

        configFile = new ConfigUtils(this, "config.yml");
        petGuiFile = new ConfigUtils(this, "gui.yml");
        petMessagesFile = new ConfigUtils(this, "messages.yml");

        databaseManager = new DatabaseManager(this);
        playerDataHandler = new PlayerDataHandler(this);
        petManager = new PetManager(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PetListener(this), this);
        pm.registerEvents(new JoinQuit(this), this);
        pm.registerEvents(new PetAbilityTriggerListener(this), this);

        if (pm.isPluginEnabled("MythicMobs")) {
            pm.registerEvents(new MythicDropRegister(), this);
            getLogger().info("Hooked into MythicMobs Drops for Pet EXP!");
        }

        new PetCommandRegistrar(this).register();
        startAutoSaveTask();
    }

    @Override
    public void onDisable() {
        SchedulerUtils.cancelAsyncTasks(this);
        if (petManager != null) petManager.disable();

        if (playerDataHandler != null) {
            getLogger().info("Synchronizing and saving player data...");
            playerDataHandler.saveAllSync();
        }

        if (databaseManager != null) databaseManager.close();
    }

    /**
     * Reloads all YAML configurations and refreshes active systems.
     */
    public void reloadFiles() {
        configFile.reload();
        petGuiFile.reload();
        petMessagesFile.reload();

        if (databaseManager != null) {
            databaseManager.close();
            databaseManager = new DatabaseManager(this);
        }

        if (petManager != null) petManager.reload();
    }

    /**
     * Initializes the asynchronous data saving runnable based on config timing.
     */
    private void startAutoSaveTask() {
        long seconds = configFile.getConfig().getLong("auto-save", 300);
        if (seconds <= 0) return;
        SchedulerUtils.runAsyncTimer(this, () -> {
            if (playerDataHandler != null) playerDataHandler.saveAllAsync();
        }, seconds, seconds);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataHandler getPlayerDataHandler() {
        return playerDataHandler;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public ConfigUtils getConfigFile() {
        return configFile;
    }

    public ConfigUtils getPetGuiFile() {
        return petGuiFile;
    }

    public ConfigUtils getPetMessagesFile() {
        return petMessagesFile;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}
