package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.block.BlockIgniteEvent;

public class PetAbilityTriggerListener implements Listener {
    private final SincePet plugin;

    public PetAbilityTriggerListener(SincePet plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        switch (e.getAction()) {
            case LEFT_CLICK_AIR -> triggerActive(e.getPlayer(), "LEFT_CLICK_AIR");
            case LEFT_CLICK_BLOCK -> triggerActive(e.getPlayer(), "LEFT_CLICK_BLOCK");
            case RIGHT_CLICK_AIR -> triggerActive(e.getPlayer(), "RIGHT_CLICK_AIR");
            case RIGHT_CLICK_BLOCK -> triggerActive(e.getPlayer(), "RIGHT_CLICK_BLOCK");
            case PHYSICAL -> triggerActive(e.getPlayer(), "PHYSICAL_INTERACT");
        }
        if (e.getAction().isLeftClick()) triggerActive(e.getPlayer(), "LEFT_CLICK");
        if (e.getAction().isRightClick()) triggerActive(e.getPlayer(), "RIGHT_CLICK");
        triggerActive(e.getPlayer(), "INTERACT");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (plugin.getPetManager().isPlayerPet(e.getRightClicked())) return;
        triggerActive(e.getPlayer(), "RIGHT_CLICK_ENTITY");
    }

    @EventHandler(ignoreCancelled = true)
    public void onAnimation(PlayerAnimationEvent e) {
        triggerActive(e.getPlayer(), "SWING");
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        triggerActive(e.getPlayer(), "SWAP_HAND");
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        triggerPassive(e.getPlayer(), e.isSneaking() ? "SNEAK_START" : "SNEAK_STOP", e.getPlayer());
        if (!e.isSneaking()) return;
        triggerActive(e.getPlayer(), "SNEAK");
        abilities().triggerDoubleSneak(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSprint(PlayerToggleSprintEvent e) {
        triggerPassive(e.getPlayer(), e.isSprinting() ? "SPRINT_START" : "SPRINT_STOP", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        triggerPassive(e.getPlayer(), e.isFlying() ? "FLIGHT_START" : "FLIGHT_STOP", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent e) {
        if (e.getEntity() instanceof Player p) {
            triggerPassive(p, e.isGliding() ? "GLIDE_START" : "GLIDE_STOP", p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSwim(EntityToggleSwimEvent e) {
        if (e.getEntity() instanceof Player p) {
            triggerPassive(p, e.isSwimming() ? "SWIM_START" : "SWIM_STOP", p);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        triggerPassive(p, "OWNER_DAMAGED", p);
        triggerPassive(p, "OWNER_DAMAGED_" + e.getCause().name(), p);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerDamageEntity(EntityDamageByEntityEvent e) {
        Player attacker = getPlayerDamager(e.getDamager());
        if (attacker != null) {
            triggerPassive(attacker, "OWNER_ATTACK", e.getEntity());
            if (e.getEntity() instanceof Player) triggerPassive(attacker, "OWNER_ATTACK_PLAYER", e.getEntity());
            if (e.getEntity() instanceof Monster) triggerPassive(attacker, "OWNER_ATTACK_MONSTER", e.getEntity());
        }

        if (e.getEntity() instanceof Player victim) {
            triggerPassive(victim, "OWNER_DAMAGED_BY_ENTITY", e.getDamager());
            if (attacker != null) triggerPassive(victim, "OWNER_DAMAGED_BY_PLAYER", e.getDamager());
            else if (e.getDamager() instanceof LivingEntity) triggerPassive(victim, "OWNER_DAMAGED_BY_MOB", e.getDamager());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer != null) {
            triggerPassive(killer, "OWNER_KILL", e.getEntity());
            if (e.getEntity() instanceof Player) triggerPassive(killer, "OWNER_KILL_PLAYER", e.getEntity());
            if (e.getEntity() instanceof Monster) triggerPassive(killer, "OWNER_KILL_MONSTER", e.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent e) {
        triggerPassive(e.getEntity(), "OWNER_DEATH", e.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent e) {
        triggerPassive(e.getPlayer(), "OWNER_RESPAWN", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent e) {
        if (e.getEntity() instanceof Player p) triggerPassive(p, "OWNER_RESURRECT", p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player p) triggerPassive(p, "SHOOT_BOW", e.getProjectile());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent e) {
        Player p = getProjectileOwner(e.getEntity());
        if (p != null) triggerPassive(p, "PROJECTILE_LAUNCH", e.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent e) {
        Player p = getProjectileOwner(e.getEntity());
        if (p != null) triggerPassive(p, "PROJECTILE_HIT", e.getHitEntity() == null ? e.getEntity() : e.getHitEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        triggerPassive(e.getPlayer(), "BLOCK_BREAK", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        triggerPassive(e.getPlayer(), "BLOCK_PLACE", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        triggerPassive(e.getPlayer(), "ITEM_CONSUME", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        triggerPassive(e.getPlayer(), "ITEM_DAMAGE", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemMend(PlayerItemMendEvent e) {
        triggerPassive(e.getPlayer(), "ITEM_MEND", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHotbarChange(PlayerItemHeldEvent e) {
        triggerActive(e.getPlayer(), "HOTBAR_CHANGE");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent e) {
        triggerActive(e.getPlayer(), "DROP_ITEM");
        triggerPassive(e.getPlayer(), "ITEM_DROP", e.getItemDrop());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) triggerPassive(p, "ITEM_PICKUP", e.getItem());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent e) {
        triggerPassive(e.getPlayer(), "FISH_" + e.getState().name(), e.getCaught() == null ? e.getPlayer() : e.getCaught());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        triggerPassive(e.getPlayer(), "BUCKET_FILL", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        triggerPassive(e.getPlayer(), "BUCKET_EMPTY", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent e) {
        triggerPassive(e.getPlayer(), "SHEAR_ENTITY", e.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        triggerPassive(e.getEnchanter(), "ENCHANT_ITEM", e.getEnchanter());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player p) triggerPassive(p, "OWNER_HEAL", p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p) triggerPassive(p, "FOOD_CHANGE", p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent e) {
        if (e.getEntity() instanceof Player p) triggerPassive(p, "POTION_EFFECT", p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAirChange(EntityAirChangeEvent e) {
        if (e.getEntity() instanceof Player p) triggerPassive(p, "AIR_CHANGE", p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExp(PlayerExpChangeEvent e) {
        if (e.getAmount() > 0) triggerPassive(e.getPlayer(), "EXP_GAIN", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLevelChange(PlayerLevelChangeEvent e) {
        triggerPassive(e.getPlayer(), e.getNewLevel() > e.getOldLevel() ? "LEVEL_UP" : "LEVEL_DOWN", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        triggerPassive(e.getPlayer(), "TELEPORT", e.getPlayer());
        triggerPassive(e.getPlayer(), "TELEPORT_" + e.getCause().name(), e.getPlayer());
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent e) {
        triggerPassive(e.getPlayer(), "WORLD_CHANGE", e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        triggerPassive(e.getPlayer(), "RESPAWN", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent e) {
        triggerPassive(e.getPlayer(), "BED_ENTER", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onRiptide(PlayerRiptideEvent e) {
        triggerPassive(e.getPlayer(), "RIPTIDE", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        triggerPassive(e.getPlayer(), "GAMEMODE_CHANGE", e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        triggerPassive(e.getPlayer(), "JOIN", e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        triggerPassive(e.getPlayer(), "QUIT", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.hasChangedBlock()) {
            triggerPassive(e.getPlayer(), "MOVE", e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        triggerPassive(e.getPlayer(), "COMMAND", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        triggerPassive(e.getPlayer(), "CHAT", e.getPlayer());
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent e) {
        triggerPassive(e.getPlayer(), "ITEM_BREAK", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        triggerPassive(e.getPlayer(), "ARMOR_STAND_MANIPULATE", e.getRightClicked());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEditBook(PlayerEditBookEvent e) {
        triggerPassive(e.getPlayer(), "EDIT_BOOK", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onStatisticIncrement(PlayerStatisticIncrementEvent e) {
        triggerPassive(e.getPlayer(), "STATISTIC_INCREMENT", e.getPlayer());
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent e) {
        triggerPassive(e.getPlayer(), "ADVANCEMENT_DONE", e.getPlayer());
    }

    @EventHandler
    public void onEggThrow(PlayerEggThrowEvent e) {
        triggerPassive(e.getPlayer(), "EGG_THROW", e.getEgg());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHarvestBlock(PlayerHarvestBlockEvent e) {
        triggerPassive(e.getPlayer(), "HARVEST_BLOCK", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTameEntity(EntityTameEvent e) {
        if (e.getOwner() instanceof Player p) {
            triggerPassive(p, "TAME_ENTITY", e.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (e.getEntered() instanceof Player p) {
            triggerPassive(p, "VEHICLE_ENTER", e.getVehicle());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent e) {
        if (e.getExited() instanceof Player p) {
            triggerPassive(p, "VEHICLE_EXIT", e.getVehicle());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        triggerPassive(e.getPlayer(), "PORTAL", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBedLeave(PlayerBedLeaveEvent e) {
        triggerPassive(e.getPlayer(), "BED_LEAVE", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTakeLecternBook(PlayerTakeLecternBookEvent e) {
        triggerPassive(e.getPlayer(), "TAKE_LECTERN_BOOK", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent e) {
        triggerPassive(e.getPlayer(), "RECIPE_DISCOVER", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignOpen(PlayerOpenSignEvent e) {
        triggerPassive(e.getPlayer(), "SIGN_OPEN", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent e) {
        triggerPassive(e.getPlayer(), "JUMP", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (e.getBreeder() instanceof Player p) {
            triggerPassive(p, "BREED_ENTITY", e.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent e) {
        if (e.getPlayer() != null) {
            triggerPassive(e.getPlayer(), "FERTILIZE_BLOCK", e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent e) {
        triggerPassive(e.getPlayer(), "BLOCK_DAMAGE", e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent e) {
        triggerPassive(e.getPlayer(), "LEASH_ENTITY", e.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onUnleash(PlayerUnleashEntityEvent e) {
        triggerPassive(e.getPlayer(), "UNLEASH_ENTITY", e.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEntity(PlayerBucketEntityEvent e) {
        triggerPassive(e.getPlayer(), "BUCKET_ENTITY", e.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p) {
            triggerPassive(p, "INVENTORY_OPEN", p);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            triggerPassive(p, "INVENTORY_CLOSE", p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player p) {
            triggerPassive(p, "TARGETED", e.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (e.getPlayer() != null) {
            triggerPassive(e.getPlayer(), "BLOCK_IGNITE", e.getPlayer());
        }
    }

    private void triggerActive(Player p, String trigger) {
        abilities().triggerActive(p, trigger);
    }

    private void triggerPassive(Player p, String trigger, Entity target) {
        abilities().triggerPassive(p, trigger, target);
    }

    private PetAbilityManager abilities() {
        return plugin.getPetManager().getAbilityManager();
    }

    private Player getProjectileOwner(Projectile projectile) {
        ProjectileSource source = projectile.getShooter();
        return source instanceof Player p ? p : null;
    }

    private Player getPlayerDamager(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile) return getProjectileOwner(projectile);
        return null;
    }
}
