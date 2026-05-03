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
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of SincePet. Initializes configs, managers, listeners, and commands.
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

    public static SincePet getPlugin() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        // Hook into WorldGuard early to register flags safely
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
        petManager = new PetManager(this);

        // 3. Register Listeners
        getServer().getPluginManager().registerEvents(new PetListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinQuit(this), this);
        if (getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            getServer().getPluginManager().registerEvents(new MythicDropRegister(), this);
            getLogger().info("Hooked into MythicMobs Drops (pet-xp)!");
        }

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

        if (databaseManager != null) {
            databaseManager.close();
            databaseManager = new DatabaseManager(this);
        }

        if (petManager != null) petManager.reload();
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            // /pet command (Open GUI)
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
                    .build(), "Open Pet GUI");

            // /sincepet command (Admin)
            event.registrar().register(Commands.literal("sincepet")
                    .requires(s -> s.getSender().hasPermission("sincepet.admin"))

                    // 1. RELOAD
                    .then(Commands.literal("reload")
                            .executes(ctx -> {
                                reloadFiles();
                                ctx.getSource().getSender().sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("admin.reload")));
                                return 1;
                            }))

                    // 2. LEVEL UP
                    .then(Commands.literal("levelup")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        Player t = Bukkit.getPlayer(StringArgumentType.getString(ctx, "target"));
                                        if (t != null) petManager.levelUp(t, ctx.getSource().getSender());
                                        else
                                            ctx.getSource().getSender().sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.player_not_found")));
                                        return 1;
                                    })))
                    // 3. MAX LEVEL
                    .then(Commands.literal("max_level")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        String input = builder.getRemaining().toLowerCase();
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            if (p.getName().toLowerCase().startsWith(input))
                                                builder.suggest(p.getName());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(Commands.argument("pet", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                String input = builder.getRemaining().toLowerCase();
                                                if ("petall".startsWith(input)) builder.suggest("petall");
                                                for (String id : petManager.getPetConfig().getPets().keySet()) {
                                                    if (id.toLowerCase().startsWith(input)) builder.suggest(id);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> {
                                                        String targetName = StringArgumentType.getString(ctx, "target");
                                                        String petId = StringArgumentType.getString(ctx, "pet");
                                                        int newMax = IntegerArgumentType.getInteger(ctx, "level");

                                                        Player t = Bukkit.getPlayer(targetName);
                                                        var sender = ctx.getSource().getSender();

                                                        if (t == null) {
                                                            sender.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.player_not_found", "&cPlayer not found!")));
                                                            return 0;
                                                        }

                                                        PlayerDataHandler.PlayerSession s = playerDataHandler.getSession(t.getUniqueId());
                                                        if (s != null) {
                                                            String allString = petMessagesFile.getString("pet.command.all_pets", "All");

                                                            // Handle 'petall' keyword
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
                                                                sender.sendMessage(ColorUtils.parseWithPrefix(petMessagesFile.getString("pet.command.pet_not_found", "&cPet ID not found!")
                                                                        .replace("<pet>", petId)));
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

    private void startAutoSaveTask() {
        long seconds = configFile.getConfig().getLong("auto-save", 300);
        if (seconds <= 0) return;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerDataHandler != null) playerDataHandler.saveAllAsync();
        }, seconds * 20L, seconds * 20L);
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