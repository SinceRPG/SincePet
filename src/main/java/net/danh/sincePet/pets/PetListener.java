package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.data.PlayerDataHandler;
import net.danh.sincePet.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.ArrayList;

/**
 * Global listener capturing interactive events associated with Pet mechanics.
 */
public class PetListener implements Listener {
    private final SincePet plugin;

    public PetListener(SincePet plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        var uuid = p.getUniqueId();
        Double multiplier = plugin.getPetManager().getDamageModifiers().get(uuid);
        if (multiplier != null) {
            e.setDamage(e.getDamage() * multiplier);
        }
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent e) {
        // Warning Note: e.getInventory().getHolder() triggers a known Paper 1.21.x NBT deserialization lag issue
        // if interacting with highly bloated physically placed Chest blocks (MMOItems).
        // This is safe natively inside our GUI parameters.
        if (e.getInventory().getHolder(false) instanceof PetGUI gui) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;

            var p = (Player) e.getWhoClicked();
            int slot = e.getSlot();
            int currentPage = gui.getPage();
            var guiConfig = plugin.getPetGuiFile();

            if (gui.getView() == PetGUI.View.DETAIL) {
                if (slot == guiConfig.getInt("detail.buttons.back.slot", 18)) {
                    new PetGUI(plugin, 1).open(p);
                    return;
                }
                if (slot == guiConfig.getInt("detail.buttons.ride.slot", 11)) {
                    if (!gui.isActionVisible(p, "detail.buttons.ride")) return;
                    plugin.getPetManager().ridePet(p);
                    p.closeInventory();
                    return;
                }
                if (slot == guiConfig.getInt("detail.buttons.settings.slot", 13)) {
                    if (!gui.isActionVisible(p, "detail.buttons.settings")) return;
                    new PetGUI(plugin, 1, PetGUI.View.SETTINGS).open(p);
                    return;
                }
                if (slot == guiConfig.getInt("detail.buttons.upgrades.slot", 15)) {
                    if (!gui.isActionVisible(p, "detail.buttons.upgrades")) return;
                    new PetGUI(plugin, 1, PetGUI.View.UPGRADES).open(p);
                    return;
                }

                org.bukkit.configuration.ConfigurationSection expSection = guiConfig.getConfig().getConfigurationSection("pet-experience");
                if (expSection != null) {
                    for (String key : expSection.getKeys(false)) {
                        int expSlot = expSection.getInt(key + ".slot", -1);
                        if (slot == expSlot) {
                            handleExpItemConsume(p, guiConfig, key);
                            return;
                        }
                    }
                }
                return;
            }

            if (gui.getView() == PetGUI.View.SETTINGS) {
                if (slot == guiConfig.getInt("detail.buttons.back_detail.slot", 18)) {
                    new PetGUI(plugin, 1, PetGUI.View.DETAIL).open(p);
                    return;
                }
                if (slot == guiConfig.getInt("settings.show_name.slot", 10))
                    plugin.getPetManager().togglePetSetting(p, "show_name");
                if (slot == guiConfig.getInt("settings.auto_attack.slot", 12))
                    plugin.getPetManager().togglePetSetting(p, "auto_attack");
                if (slot == guiConfig.getInt("settings.stat_buff.slot", 14))
                    plugin.getPetManager().togglePetSetting(p, "stat_buff");
                new PetGUI(plugin, 1, PetGUI.View.SETTINGS).open(p);
                return;
            }

            if (gui.getView() == PetGUI.View.UPGRADES) {
                if (slot == guiConfig.getInt("detail.buttons.back_detail.slot", 18) || slot == guiConfig.getInt("detail.buttons.back_detail.slot", 49)) {
                    new PetGUI(plugin, 1, PetGUI.View.DETAIL).open(p);
                    return;
                }
                if (slot == guiConfig.getInt("buttons.reset_upgrades.slot", 50)) {
                    if (!gui.isActionVisible(p, "buttons.reset_upgrades")) return;
                    PetData active = plugin.getPetManager().getActivePetData(p);
                    if (active != null) {
                        plugin.getPetManager().resetUpgradePoints(p, active);
                        new PetGUI(plugin, 1, PetGUI.View.UPGRADES).open(p);
                    }
                    return;
                }
                PetData active = plugin.getPetManager().getActivePetData(p);
                if (active == null) return;
                for (PetUpgrade upgrade : active.upgrades()) {
                    if (slot == upgrade.slot()) {
                        plugin.getPetManager().upgradePet(p, upgrade);
                        new PetGUI(plugin, 1, PetGUI.View.UPGRADES).open(p);
                        return;
                    }
                }
                return;
            }

            int removeSlot = guiConfig.getInt("buttons.remove.slot", 49);
            int detailSlot = guiConfig.getInt("buttons.detail.slot", 48);
            int prevSlot = guiConfig.getInt("buttons.previous.slot", 45);
            int nextSlot = guiConfig.getInt("buttons.next.slot", 53);

            if (slot == detailSlot) {
                if (!gui.isActionVisible(p, "buttons.detail")) return;
                new PetGUI(plugin, 1, PetGUI.View.DETAIL).open(p);
                return;
            }

            if (slot == removeSlot) {
                if (!gui.isActionVisible(p, "buttons.remove")) return;
                plugin.getPetManager().despawnPet(p);
                p.closeInventory();
                p.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.command.despawn", "Pet despawned.")));
                return;
            }

            if (slot == prevSlot && currentPage > 1) {
                new PetGUI(plugin, currentPage - 1).open(p);
                return;
            }

            var allPets = new ArrayList<>(plugin.getPetManager().getPetConfig().getAllPets());
            var viewablePets = new ArrayList<PetData>();

            for (PetData pet : allPets) {
                if (plugin.getPetManager().canAccessPet(p, pet)) {
                    viewablePets.add(pet);
                }
            }

            int totalPages = Math.max(1, (int) Math.ceil((double) viewablePets.size() / 45));
            if (slot == nextSlot && currentPage < totalPages) {
                new PetGUI(plugin, currentPage + 1).open(p);
                return;
            }

            if (slot < 45) {
                int realIndex = (currentPage - 1) * 45 + slot;
                if (realIndex >= 0 && realIndex < viewablePets.size()) {
                    var selected = viewablePets.get(realIndex);
                    int level = plugin.getPetManager().getPetLevel(p, selected.id());
                    plugin.getPetManager().spawnPet(p, selected, level, true);
                    p.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent e) {
        var p = e.getPlayer();
        if (p.getVehicle() != null && plugin.getPetManager().isPlayerPet(p.getVehicle())) {
            var i = e.getInput();
            float f = 0;
            if (i.isForward()) f += 1;
            if (i.isBackward()) f -= 1;
            float s = 0;
            if (i.isLeft()) s += 1;
            if (i.isRight()) s -= 1;
            boolean jump = i.isJump();
            boolean sneak = i.isSneak();
            plugin.getPetManager().updateInput(p, f, s, jump, sneak);
        }
    }

    @EventHandler
    public void onRightClickPet(PlayerInteractEntityEvent e) {
        if (plugin.getPetManager().isPlayerPet(e.getRightClicked())) {
            e.setCancelled(true);
            plugin.getPetManager().ridePet(e.getPlayer());
        }
    }

    private void handleExpItemConsume(Player p, net.danh.sincePet.utils.ConfigUtils guiConfig, String key) {
        String path = "pet-experience." + key;
        String mmoitemId = guiConfig.getString(path + ".consumable-item.id", "");
        String mmoitemType = guiConfig.getString(path + ".consumable-item.type", "");
        int quantity = guiConfig.getInt(path + ".quantity", 1);

        if (guiConfig.getConfig().isConfigurationSection(path + ".consumable-item")) {
            mmoitemId = guiConfig.getString(path + ".consumable-item.id", "");
            mmoitemType = guiConfig.getString(path + ".consumable-item.type", "");
            quantity = guiConfig.getInt(path + ".consumable-item.quantity", 1);
        } else if (guiConfig.getConfig().isList(path + ".consumable-item")) {
            java.util.List<String> list = guiConfig.getConfig().getStringList(path + ".consumable-item");
            if (!list.isEmpty()) mmoitemId = list.get(0);
        }

        double expPoints = guiConfig.getDouble(path + ".exp-points", 0);
        int expLevel = guiConfig.getInt(path + ".exp-level", 0);

        if (mmoitemId.isBlank()) return;

        PetData active = plugin.getPetManager().getActivePetData(p);
        if (active == null) return;
        
        PlayerDataHandler.PlayerSession s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s == null) return;

        int currentLv = s.getLevel(active.id());
        int max = s.getMaxPetLevel(active.id());
        if (currentLv >= max) {
            p.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.experience.max_level")));
            return;
        }

        org.bukkit.inventory.ItemStack cursor = p.getItemOnCursor();
        if (cursor == null || cursor.getType().isAir()) {
            int found = 0;
            for (org.bukkit.inventory.ItemStack item : p.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) continue;
                io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                boolean matchType = mmoitemType.isBlank() || mmoitemType.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_TYPE"));
                if (nbt.hasType() && mmoitemId.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_ID")) && matchType) {
                    found += item.getAmount();
                }
            }
            if (found < quantity) {
                String itemName = getMMOItemName(mmoitemType, mmoitemId);
                p.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.experience.not_enough_inventory").replace("{item_name}", itemName)));
                return;
            }
            int remaining = quantity;
            for (int i = 0; i < p.getInventory().getSize(); i++) {
                org.bukkit.inventory.ItemStack item = p.getInventory().getItem(i);
                if (item == null || item.getType().isAir()) continue;
                io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                boolean matchType = mmoitemType.isBlank() || mmoitemType.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_TYPE"));
                if (nbt.hasType() && mmoitemId.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_ID")) && matchType) {
                    if (item.getAmount() <= remaining) {
                        remaining -= item.getAmount();
                        p.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - remaining);
                        remaining = 0;
                    }
                    if (remaining <= 0) break;
                }
            }
        } else {
            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(cursor);
            boolean matchType = mmoitemType.isBlank() || mmoitemType.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_TYPE"));
            if (!nbt.hasType() || !mmoitemId.equalsIgnoreCase(nbt.getString("MMOITEMS_ITEM_ID")) || !matchType) {
                String itemName = getMMOItemName(mmoitemType, mmoitemId);
                p.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.experience.wrong_item_cursor").replace("{item_name}", itemName)));
                return;
            }
            if (cursor.getAmount() < quantity) {
                p.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.experience.not_enough_cursor")));
                return;
            }
            cursor.setAmount(cursor.getAmount() - quantity);
        }

        boolean updated = false;
        if (expLevel > 0) {
            for (int i = 0; i < expLevel; i++) {
                if (s.getLevel(active.id()) < max) {
                    plugin.getPetManager().levelUp(p, p);
                }
            }
            updated = true;
        }
        if (expPoints > 0) {
            double currentXp = s.getXp(active.id());
            s.setXp(active.id(), currentXp + expPoints);
            updated = true;
            
            int loopLv = s.getLevel(active.id());
            while (loopLv < max) {
                double required = 0;
                try {
                    required = Double.parseDouble(net.danh.sincePet.utils.Calculator.calculator(active.maxXpFormula().replaceAll("(?i)<?\\blevel\\b>?", String.valueOf(loopLv)), 2));
                } catch (Exception ignored) {}
                if (required <= 0 || s.getXp(active.id()) < required) break;
                
                s.setXp(active.id(), s.getXp(active.id()) - required);
                plugin.getPetManager().levelUp(p, p);
                loopLv = s.getLevel(active.id());
            }
        }

        if (updated) {
            p.sendMessage(ColorUtils.parseWithPrefix(plugin.getPetMessagesFile().getString("pet.experience.success")));
            new PetGUI(plugin, 1, PetGUI.View.DETAIL).open(p);
        }
    }

    private String getMMOItemName(String type, String id) {
        try {
            net.Indyuce.mmoitems.api.Type mmoType = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(type.toUpperCase(java.util.Locale.ROOT));
            if (mmoType != null) {
                org.bukkit.inventory.ItemStack itemStack = net.Indyuce.mmoitems.MMOItems.plugin.getItem(mmoType, id);
                if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
                    String name = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(itemStack.getItemMeta().displayName());
                    return name + "<reset>";
                }
            }
        } catch (Exception ignored) {}
        return (type.isEmpty() ? "" : type + ":") + id;
    }
}
