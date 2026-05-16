package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
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

            int removeSlot = guiConfig.getInt("buttons.remove.slot", 49);
            int prevSlot = guiConfig.getInt("buttons.previous.slot", 45);
            int nextSlot = guiConfig.getInt("buttons.next.slot", 53);

            if (slot == removeSlot) {
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

            boolean hasAllPerm = p.hasPermission("pet.hasall");
            for (PetData pet : allPets) {
                if (hasAllPerm || p.hasPermission("pet." + pet.id().toLowerCase())) {
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
            plugin.getPetManager().updateInput(p, f, s, jump);
        }
    }

    @EventHandler
    public void onRightClickPet(PlayerInteractEntityEvent e) {
        if (plugin.getPetManager().isPlayerPet(e.getRightClicked())) {
            e.setCancelled(true);
            plugin.getPetManager().ridePet(e.getPlayer());
        }
    }
}
