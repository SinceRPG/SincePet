package net.danh.sincePet;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.danh.sincePet.data.DatabaseManager;
import net.danh.sincePet.data.PlayerDataHandler;
import net.danh.sincePet.events.JoinQuit;
import net.danh.sincePet.hooks.WorldGuardHook;
import net.danh.sincePet.mythic.MythicDropRegister;
import net.danh.sincePet.pets.PetGUI;
import net.danh.sincePet.pets.PetListener;
import net.danh.sincePet.pets.PetManager;
import net.danh.sincePet.utils.ColorUtils;
import net.danh.sincePet.utils.ConfigUtils;
import net.danh.sincePet.utils.SchedulerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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

        if (pm.isPluginEnabled("MythicMobs")) {
            pm.registerEvents(new MythicDropRegister(), this);
            getLogger().info("Hooked into MythicMobs Drops for Pet EXP!");
        }

        registerCommands();
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
     * Registers the Brigadier commands utilizing the Paper Lifecycle Events API.
     */
    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {

            // Standard User Command
            event.registrar().register(Commands.literal("pet")
                    .executes(ctx -> {
                        if (ctx.getSource().getExecutor() instanceof Player p)
                            new PetGUI(this, 1).open(p);
                        return 1;
                    })
                    .then(Commands.literal("ride").executes(ctx -> {
                        if (ctx.getSource().getExecutor() instanceof Player p)
                            if (p.hasPermission("pets.ride"))
                                petManager.ridePet(p);
                        return 1;
                    }))
                    .build(), "Open the Pet GUI");

            // Administrative Command
            event.registrar().register(Commands.literal("sincepet")
                    .requires(s -> s.getSender().hasPermission("sincepet.admin"))
                    .then(Commands.literal("reload")
                            .executes(ctx -> {
                                reloadFiles();
                                ctx.getSource().getSender().sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("admin.reload")));
                                return 1;
                            }))
                    .then(Commands.literal("levelup")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        var t = Bukkit.getPlayer(StringArgumentType.getString(ctx, "target"));
                                        if (t != null) petManager.levelUp(t, ctx.getSource().getSender());
                                        else
                                            ctx.getSource().getSender().sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.player_not_found")));
                                        return 1;
                                    })))
                    .then(Commands.literal("max_level")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        var input = builder.getRemaining().toLowerCase();
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            if (p.getName().toLowerCase().startsWith(input))
                                                builder.suggest(p.getName());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(Commands.argument("pet", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                var input = builder.getRemaining().toLowerCase();
                                                if ("petall".startsWith(input)) builder.suggest("petall");
                                                for (String id : petManager.getPetConfig().getPets().keySet()) {
                                                    if (id.toLowerCase().startsWith(input)) builder.suggest(id);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> {
                                                        var targetName = StringArgumentType.getString(ctx, "target");
                                                        var petId = StringArgumentType.getString(ctx, "pet");
                                                        var newMax = IntegerArgumentType.getInteger(ctx, "level");

                                                        var t = Bukkit.getPlayer(targetName);
                                                        var sender = ctx.getSource().getSender();

                                                        if (t == null) {
                                                            sender.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.player_not_found", "&cPlayer not found!")));
                                                            return 0;
                                                        }

                                                        var s = playerDataHandler.getSession(t.getUniqueId());
                                                        if (s != null) {
                                                            var allString = petMessagesFile.getString("pet.command.all_pets", "All");

                                                            if (petId.equalsIgnoreCase("petall")) {
                                                                for (String id : petManager.getPetConfig().getPets().keySet()) {
                                                                    s.setMaxPetLevel(id, newMax);
                                                                }
                                                                playerDataHandler.saveData(t.getUniqueId(), false);

                                                                sender.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.max_level_pet_success")
                                                                        .replace("<target>", t.getName())
                                                                        .replace("<pet>", allString)
                                                                        .replace("<level>", String.valueOf(newMax))));

                                                                t.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.max_level_pet_receive")
                                                                        .replace("<pet>", allString)
                                                                        .replace("<level>", String.valueOf(newMax))));
                                                                return 1;
                                                            }

                                                            if (petManager.getPetConfig().getPet(petId) == null) {
                                                                sender.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.pet_not_found", "&cPet ID not found!").replace("<pet>", petId)));
                                                                return 0;
                                                            }

                                                            s.setMaxPetLevel(petId, newMax);
                                                            playerDataHandler.saveData(t.getUniqueId(), false);

                                                            sender.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.max_level_pet_success")
                                                                    .replace("<target>", t.getName())
                                                                    .replace("<pet>", petId)
                                                                    .replace("<level>", String.valueOf(newMax))));

                                                            t.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.max_level_pet_receive")
                                                                    .replace("<pet>", petId)
                                                                    .replace("<level>", String.valueOf(newMax))));
                                                        }
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
                    .build(), "Admin commands");
        });
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
