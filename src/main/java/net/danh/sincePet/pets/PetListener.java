package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.utils.ColorUtils;
import net.danh.sincePet.utils.ConfigUtils;
import org.bukkit.Input;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PetListener implements Listener {
    private final SincePet plugin;

    public PetListener(SincePet plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        UUID uuid = p.getUniqueId();
        Double multiplier = plugin.getPetManager().getDamageModifiers().get(uuid);
        if (multiplier != null) {
            e.setDamage(e.getDamage() * multiplier);
        }
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof PetGUI gui) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;

            Player p = (Player) e.getWhoClicked();
            int slot = e.getSlot();
            int currentPage = gui.getPage();
            ConfigUtils guiConfig = plugin.getPetGuiFile();

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

            List<PetData> allPets = new ArrayList<>(plugin.getPetManager().getPetConfig().getAllPets());
            List<PetData> viewablePets = new ArrayList<>();
            for (PetData pet : allPets) {
                if (p.hasPermission("pet." + pet.id().toLowerCase())) {
                    viewablePets.add(pet);
                }
            }

            int totalPages = (int) Math.ceil((double) viewablePets.size() / 45);
            if (slot == nextSlot && currentPage < totalPages) {
                new PetGUI(plugin, currentPage + 1).open(p);
                return;
            }

            if (slot < 45) {
                int realIndex = (currentPage - 1) * 45 + slot;
                if (realIndex >= 0 && realIndex < viewablePets.size()) {
                    PetData selected = viewablePets.get(realIndex);
                    int level = plugin.getPetManager().getPetLevel(p, selected.id());
                    plugin.getPetManager().spawnPet(p, selected, level, true);
                    p.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent e) {
        Player p = e.getPlayer();
        if (p.getVehicle() != null && plugin.getPetManager().isPlayerPet(p.getVehicle())) {
            Input i = e.getInput();
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