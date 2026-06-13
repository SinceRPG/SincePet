package net.danh.sincePet.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.danh.sincePet.SincePet;
import net.danh.sincePet.pets.PetGUI;
import net.danh.sincePet.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PetCommandRegistrar {
    private final SincePet plugin;

    public PetCommandRegistrar(SincePet plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(Commands.literal("pet")
                    .executes(ctx -> {
                        if (ctx.getSource().getExecutor() instanceof Player p)
                            new PetGUI(plugin, 1).open(p);
                        return 1;
                    })
                    .then(Commands.literal("ride").executes(ctx -> {
                        if (ctx.getSource().getExecutor() instanceof Player p)
                            plugin.getPetManager().ridePet(p);
                        return 1;
                    }))
                    .then(Commands.literal("skill")
                            .executes(ctx -> {
                                if (ctx.getSource().getExecutor() instanceof Player p)
                                    plugin.getPetManager().getAbilityManager().useCommandAbility(p, "");
                                return 1;
                            })
                            .then(Commands.argument("ability", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        if (ctx.getSource().getExecutor() instanceof Player p) {
                                            var input = builder.getRemaining().toLowerCase();
                                            for (String id : plugin.getPetManager().getAbilityManager().getActiveAbilityIds(p)) {
                                                if (id.toLowerCase().startsWith(input)) builder.suggest(id);
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        if (ctx.getSource().getExecutor() instanceof Player p) {
                                            plugin.getPetManager().getAbilityManager().useCommandAbility(p, StringArgumentType.getString(ctx, "ability"));
                                        }
                                        return 1;
                                    })))
                    .build(), "Open the Pet GUI");

            event.registrar().register(Commands.literal("sincepet")
                    .requires(s -> s.getSender().hasPermission("sincepet.admin"))
                    .then(Commands.literal("reload")
                            .executes(ctx -> {
                                plugin.reloadFiles();
                                ctx.getSource().getSender().sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("admin.reload")));
                                return 1;
                            }))
                    .then(Commands.literal("levelup")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        var target = Bukkit.getPlayer(StringArgumentType.getString(ctx, "target"));
                                        if (target != null) plugin.getPetManager().levelUp(target, ctx.getSource().getSender());
                                        else ctx.getSource().getSender().sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.command.player_not_found")));
                                        return 1;
                                    })))
                    .then(Commands.literal("max_level")
                            .then(Commands.argument("target", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        var input = builder.getRemaining().toLowerCase();
                                        for (Player p : Bukkit.getOnlinePlayers()) {
                                            if (p.getName().toLowerCase().startsWith(input)) builder.suggest(p.getName());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(Commands.argument("pet", StringArgumentType.word())
                                            .suggests((ctx, builder) -> {
                                                var input = builder.getRemaining().toLowerCase();
                                                if ("petall".startsWith(input)) builder.suggest("petall");
                                                for (String id : plugin.getPetManager().getPetConfig().getPets().keySet()) {
                                                    if (id.toLowerCase().startsWith(input)) builder.suggest(id);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> setMaxLevel(
                                                            StringArgumentType.getString(ctx, "target"),
                                                            StringArgumentType.getString(ctx, "pet"),
                                                            IntegerArgumentType.getInteger(ctx, "level"),
                                                            ctx.getSource().getSender()
                                                    ))))))
                    .build(), "Admin commands");
        });
    }

    private int setMaxLevel(String targetName, String petId, int newMax, org.bukkit.command.CommandSender sender) {
        var target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.command.player_not_found", "&cPlayer not found!")));
            return 0;
        }

        var session = plugin.getPlayerDataHandler().getSession(target.getUniqueId());
        if (session == null) return 1;

        var allString = plugin.getPetMessagesFile().getString("pet.command.all_pets", "All");
        if (petId.equalsIgnoreCase("petall")) {
            for (String id : plugin.getPetManager().getPetConfig().getPets().keySet()) {
                session.setMaxPetLevel(id, newMax);
            }
            plugin.getPlayerDataHandler().saveData(target.getUniqueId(), false);
            sendMaxLevelMessages(sender, target, allString, newMax);
            return 1;
        }

        if (plugin.getPetManager().getPetConfig().getPet(petId) == null) {
            sender.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.command.pet_not_found", "&cPet ID not found!").replace("<pet>", petId)));
            return 0;
        }

        session.setMaxPetLevel(petId, newMax);
        plugin.getPlayerDataHandler().saveData(target.getUniqueId(), false);
        sendMaxLevelMessages(sender, target, petId, newMax);
        return 1;
    }

    private void sendMaxLevelMessages(org.bukkit.command.CommandSender sender, Player target, String pet, int level) {
        sender.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.command.max_level_pet_success")
                .replace("<target>", target.getName())
                .replace("<pet>", pet)
                .replace("<level>", String.valueOf(level))));

        target.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.command.max_level_pet_receive")
                .replace("<pet>", pet)
                .replace("<level>", String.valueOf(level))));
    }
}
