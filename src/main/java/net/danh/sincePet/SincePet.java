package net.danh.sincePet;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.danh.sincePet.data.DatabaseManager;
import net.danh.sincePet.data.PlayerDataHandler;
import net.danh.sincePet.events.JoinQuit;
import net.danh.sincePet.hooks.WorldGuardHook;
import net.danh.sincePet.pets.PetGUI;
import net.danh.sincePet.pets.PetListener;
import net.danh.sincePet.pets.PetManager;
import net.danh.sincePet.utils.ColorUtils;
import net.danh.sincePet.utils.ConfigUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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

    public static SincePet getPlugin() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        // Hooks
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
            worldGuardHook.register();
            getLogger().info("Hooked into WorldGuard!");
        }
    }

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();

        // 1. Load Configs
        configFile = new ConfigUtils(this, "config.yml");
        petGuiFile = new ConfigUtils(this, "gui.yml");
        petMessagesFile = new ConfigUtils(this, "messages.yml");

        // 2. Initialize Data & Managers
        databaseManager = new DatabaseManager(this);
        playerDataHandler = new PlayerDataHandler(this);

        // Init Manager (Starts tasks)
        petManager = new PetManager(this);

        // 3. Register Listeners
        getServer().getPluginManager().registerEvents(new PetListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinQuit(this), this);

        // 4. Register Commands
        registerCommands();

        // 5. Auto Save Task
        startAutoSaveTask();
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if (petManager != null) petManager.disable();
        if (playerDataHandler != null) {
            getLogger().info("Saving player data...");
            playerDataHandler.saveAllSync();
        }
        if (databaseManager != null) databaseManager.close();
    }

    public void reloadFiles() {
        configFile.reload();
        petGuiFile.reload();
        petMessagesFile.reload();
        if (petManager != null) petManager.reload();
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {

            // Lệnh /pet (Mở GUI)
            event.registrar().register(Commands.literal("pet")
                    .executes(ctx -> {
                        if (ctx.getSource().getExecutor() instanceof Player p)
                            new PetGUI(this, 1).open(p);
                        return 1;
                    })
                    .then(Commands.literal("ride").executes(ctx -> {
                        if (ctx.getSource().getExecutor() instanceof Player p) petManager.ridePet(p);
                        return 1;
                    }))
                    .build(), "Open Pet GUI");

            // Lệnh /sincepet (Admin)
            event.registrar().register(Commands.literal("sincepet")
                    .requires(s -> s.getSender().hasPermission("sincepet.admin"))
                    .then(Commands.literal("reload")
                            .executes(ctx -> {
                                reloadFiles();
                                ctx.getSource().getSender().sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("admin.reload", "<green>Reloaded successfully.")));
                                return 1;
                            }))
                    .then(Commands.literal("levelup")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .executes(ctx -> {
                                        Player t = Bukkit.getPlayer(StringArgumentType.getString(ctx, "target"));
                                        if (t != null) petManager.levelUp(t, ctx.getSource().getSender());
                                        return 1;
                                    })))
                    .build(), "Admin commands");
        });
    }

    private void startAutoSaveTask() {
        long seconds = configFile.getConfig().getLong("auto-save", 300);
        if (seconds <= 0) return;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerDataHandler != null) playerDataHandler.saveAllAsync();
        }, seconds * 20L, seconds * 20L);
    }

    // Getters
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