package net.danh.sincePet.pets;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.api.stat.provider.StatProvider;
import io.lumine.mythic.lib.damage.AttackMetadata;
import io.lumine.mythic.lib.damage.DamageMetadata;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.element.Element;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.player.modifier.ModifierType;
import io.papermc.paper.entity.TeleportFlag;
import net.danh.sincePet.SincePet;
import net.danh.sincePet.hooks.WorldGuardHook;
import net.danh.sincePet.utils.Calculator;
import net.danh.sincePet.utils.ColorUtils;
import net.danh.sincePet.utils.SchedulerUtils;
import net.kyori.adventure.text.Component;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the core physical logic, generation, attacking mechanics, and stat assignments of Pets.
 */
public class PetManager {

    private final SincePet plugin;
    private final PetConfig petConfig;
    private final PetAbilityManager abilityManager;
    private final Map<UUID, ItemDisplay> activePets = new ConcurrentHashMap<>();
    private final Map<UUID, TextDisplay> activePetNames = new ConcurrentHashMap<>();
    private final Map<UUID, PetData> activePetData = new ConcurrentHashMap<>();
    private final Map<UUID, float[]> playerInputs = new ConcurrentHashMap<>();
    private final Map<UUID, Vector> currentVelocity = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> petTasks = new ConcurrentHashMap<>();

    // TPS Optimizers
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTargetCheckTime = new ConcurrentHashMap<>();

    private final Map<UUID, Double> damageModifiers = new ConcurrentHashMap<>();
    private final Map<UUID, Map<StateFlag, Boolean>> lastFlagStates = new ConcurrentHashMap<>();
    private final Set<UUID> activeBuffs = ConcurrentHashMap.newKeySet();

    private final boolean hasMythicLib;
    private final boolean hasWorldGuard;
    private PetSettings settings;
    private double bobbingTick = 0;

    public PetManager(SincePet plugin) {
        this.plugin = plugin;
        this.petConfig = new PetConfig(plugin);
        this.settings = PetSettings.load(plugin);
        this.hasMythicLib = Bukkit.getPluginManager().isPluginEnabled("MythicLib");
        this.hasWorldGuard = plugin.getWorldGuardHook() != null;
        this.abilityManager = new PetAbilityManager(plugin, this);
    }

    public PetAbilityManager getAbilityManager() {
        return abilityManager;
    }

    public boolean canAccessPet(Player p, PetData data) {
        if (data == null) return false;
        return p.hasPermission("sincepet.pet." + data.id().toLowerCase());
    }

    public PetConfig getPetConfig() {
        return petConfig;
    }

    public Map<UUID, Double> getDamageModifiers() {
        return damageModifiers;
    }

    /**
     * Re-initializes a pet for a player connecting to the server based on their stored session.
     */
    public void onPlayerJoin(Player p) {
        plugin.getPlayerDataHandler().loadData(p);
        SchedulerUtils.runEntityDelayed(plugin, p, () -> {
            if (!p.isOnline()) return;
            var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
            if (s != null && s.getActivePetId() != null) {
                var data = petConfig.getPet(s.getActivePetId());
                if (data != null && canAccessPet(p, data)) {
                    spawnPet(p, data, s.getLevel(data.id()), false);
                } else {
                    s.setActivePetId(null);
                }
            }
        }, 20L);
    }

    /**
     * Cleans up tracking data when a player disconnects to prevent memory leaks.
     */
    public void onPlayerQuit(Player p) {
        removePetVisuals(p);
        lastFlagStates.remove(p.getUniqueId());
    }

    /**
     * Safely disables and removes all pets network-wide.
     */
    public void disable() {
        for (Player p : Bukkit.getOnlinePlayers()) removePetVisuals(p);
        for (UUID uuid : petTasks.keySet()) cancelPetTask(uuid);
        activePets.values().forEach(Entity::remove);
        activePetNames.values().forEach(Entity::remove);
        activePets.clear();
        activePetNames.clear();
        activePetData.clear();
        playerInputs.clear();
        currentVelocity.clear();
        lastAttackTime.clear();
        lastTargetCheckTime.clear();
        damageModifiers.clear();
        lastFlagStates.clear();
        activeBuffs.clear();
    }

    /**
     * Flushes current configurations and recreates pet entities.
     */
    public void reload() {
        disable();
        settings = PetSettings.load(plugin);
        petConfig.loadPets();
        for (Player p : Bukkit.getOnlinePlayers()) onPlayerJoin(p);
    }

    /**
     * Summons the visual ItemDisplay entity representing the pet.
     */
    public void spawnPet(Player p, PetData data, int level, boolean msg) {
        if (!checkFlag(p, WorldGuardHook.PET_SPAWN)) {
            if (msg) p.sendMessage(getComp("pet.worldguard.deny_spawn"));
            return;
        }

        removePetVisuals(p);
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s != null) s.setActivePetId(data.id());

        var spawnLoc = p.getLocation().add(0, settings.seatOffset(), 0);
        var pet = p.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(getSkull(data.texture()));
            d.setItemDisplayTransform(settings.itemTransform());
            setPetVisualOffset(d, settings.displayOffsetY());
            d.setBillboard(settings.billboard());
            d.setViewRange(settings.viewRange());
            d.setTeleportDuration(settings.teleportDuration());
            d.setInterpolationDuration(settings.teleportDuration());
            d.setInterpolationDelay(0);
            d.setPersistent(false);
            d.setCustomNameVisible(false);
            d.setRotation(p.getLocation().getYaw(), 0);
        });
        TextDisplay name = spawnNameDisplay(p, spawnLoc, data, level);

        spawnParticleSafe(p.getWorld(), spawnLoc, plugin.getConfigFile().getString("effects.spawn.particle", "CLOUD"), 10);
        playSoundSafe(p, spawnLoc, plugin.getConfigFile().getString("effects.spawn.sound", "ENTITY_CHICKEN_EGG"));

        activePets.put(p.getUniqueId(), pet);
        if (name != null) activePetNames.put(p.getUniqueId(), name);
        activePetData.put(p.getUniqueId(), data);
        currentVelocity.put(p.getUniqueId(), new Vector(0, 0, 0));
        updateStatStatus(p, data);
        startPetTask(p, pet);
        
        getAbilityManager().triggerActive(p, "EQUIP");
        getAbilityManager().triggerPassive(p, "EQUIP", p);

        if (msg) p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.spawn").replace("<name>", data.name())));
    }

    /**
     * Unregisters the visual components and stat modifiers associated with a pet.
     */
    public void removePetVisuals(Player p) {
        var id = p.getUniqueId();
        cancelPetTask(id);
        var pet = activePets.remove(id);
        var name = activePetNames.remove(id);
        if (pet != null) {
            spawnParticleSafe(p.getWorld(), pet.getLocation(), plugin.getConfigFile().getString("effects.despawn.particle", "POOF"), 5);
            pet.remove();
        }
        if (name != null) name.remove();
        var d = activePetData.remove(id);
        if (d != null) applyStat(p, d, false);

        playerInputs.remove(id);
        lastAttackTime.remove(id);
        lastTargetCheckTime.remove(id);
        damageModifiers.remove(id);
        currentVelocity.remove(id);
        activeBuffs.remove(id);
        lastFlagStates.remove(id);
    }

    /**
     * Executes the riding logic if permitted by the pet data and region.
     */
    public void ridePet(Player p) {
        if (!activePets.containsKey(p.getUniqueId())) {
            p.sendMessage(getComp("pet.command.ride_fail"));
            return;
        }
        var data = activePetData.get(p.getUniqueId());
        if (!checkFlag(p, WorldGuardHook.PET_RIDE)) {
            p.sendMessage(getComp("pet.worldguard.deny_ride"));
            return;
        }
        if (!p.hasPermission("sincepet.ride")) {
            p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.no_permission")));
            return;
        }
        if (data != null && !data.rideable()) {
            p.sendMessage(getComp("pet.command.ride_not_allowed"));
            return;
        }
        if (activePets.get(p.getUniqueId()).addPassenger(p)) {
            playerInputs.put(p.getUniqueId(), readCurrentInput(p));
            p.sendMessage(getComp("pet.command.ride_success"));
        }
    }

    /**
     * Fully de-registers and un-assigns the pet from the player's session.
     */
    public void despawnPet(Player p) {
        removePetVisuals(p);
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s != null) s.setActivePetId(null);
    }

    public int getPetLevel(Player p, String petId) {
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        return (s != null) ? s.getLevel(petId) : 1;
    }

    /**
     * Increases the pet's level and triggers relevant visual and audio cues.
     */
    public void levelUp(Player target, CommandSender sender) {
        var s = plugin.getPlayerDataHandler().getSession(target.getUniqueId());
        if (s == null || s.getActivePetId() == null) {
            sender.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.levelup_fail_target").replace("<target>", target.getName())));
            return;
        }
        var id = s.getActivePetId();
        int newLv = s.getLevel(id) + 1;
        s.setLevel(id, newLv);
        var data = activePetData.get(target.getUniqueId());
        if (data != null) {
            updateStatStatus(target, data);
            ItemDisplay pet = activePets.get(target.getUniqueId());
            TextDisplay name = activePetNames.get(target.getUniqueId());
            if (name != null) updatePetName(name, data, newLv);
        }
        var name = (data != null) ? data.name() : id;
        target.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.levelup_self").replace("<name>", name).replace("<level>", String.valueOf(newLv))));
        playSoundSafe(target, target.getLocation(), plugin.getConfigFile().getString("effects.levelup.sound", "ENTITY_PLAYER_LEVELUP"));

        if (!sender.equals(target))
            sender.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.levelup_other").replace("<target>", target.getName()).replace("<level>", String.valueOf(newLv))));
    }

    public void updateInput(Player p, float f, float s, boolean jump, boolean sneak) {
        if (activePets.containsKey(p.getUniqueId()))
            playerInputs.put(p.getUniqueId(), new float[]{f, s, jump ? 1.0f : 0.0f, sneak ? 1.0f : 0.0f});
    }

    private float[] readCurrentInput(Player p) {
        Input input = p.getCurrentInput();
        float forward = 0;
        if (input.isForward()) forward += 1;
        if (input.isBackward()) forward -= 1;
        float sideways = 0;
        if (input.isLeft()) sideways += 1;
        if (input.isRight()) sideways -= 1;
        return new float[]{forward, sideways, input.isJump() ? 1.0f : 0.0f, input.isSneak() ? 1.0f : 0.0f};
    }

    public boolean isPlayerPet(Entity entity) {
        if (entity == null) return false;
        for (ItemDisplay pet : activePets.values()) {
            if (pet.getUniqueId().equals(entity.getUniqueId())) return true;
        }
        return false;
    }

    public PetData getActivePetData(Player p) {
        return activePetData.get(p.getUniqueId());
    }

    public ItemDisplay getActivePetEntity(Player p) {
        if (p == null) return null;
        return activePets.get(p.getUniqueId());
    }

    public boolean getPetSetting(Player p, String petId, String settingId, boolean fallback) {
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        return s == null ? fallback : s.getSetting(petId, settingId, fallback);
    }

    public void togglePetSetting(Player p, String settingId) {
        PetData data = getActivePetData(p);
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (data == null || s == null) return;
        boolean next = !s.getSetting(data.id(), settingId, true);
        s.setSetting(data.id(), settingId, next);
        if ("show_name".equals(settingId)) refreshNameDisplay(p, data);
        if ("stat_buff".equals(settingId)) {
            applyStat(p, data, false);
            activeBuffs.remove(p.getUniqueId());
            updateStatStatus(p, data);
        }
        plugin.getPlayerDataHandler().saveData(p.getUniqueId(), false);
    }

    public int getUpgradeLevel(Player p, PetData data, PetUpgrade upgrade) {
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        return s == null ? 0 : s.getUpgradeLevel(data.id(), upgrade.id());
    }

    public double getUpgradeStatBonus(Player p, PetData data, PetUpgrade upgrade, int upgradeLevel) {
        return calculateUpgradeFormula(p, data, upgrade.statFormula(), upgradeLevel);
    }

    public double getUpgradeDamageBonus(Player p, PetData data, PetUpgrade upgrade, int upgradeLevel) {
        return calculateUpgradeFormula(p, data, upgrade.damageFormula(), upgradeLevel);
    }

    public String getResolvedUpgradeRequirement(Player p, PetUpgrade upgrade) {
        return resolvePlaceholders(p, upgrade.papi());
    }

    public boolean upgradePet(Player p, PetUpgrade upgrade) {
        PetData data = getActivePetData(p);
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (data == null || s == null) return false;
        int current = s.getUpgradeLevel(data.id(), upgrade.id());
        if (current >= upgrade.maxLevel()) {
            p.sendMessage(getComp("pet.upgrade.maxed"));
            return false;
        }
        if (!checkUpgradeRequirement(p, upgrade)) {
            p.sendMessage(getComp("pet.upgrade.requirement_failed"));
            return false;
        }
        s.setUpgradeLevel(data.id(), upgrade.id(), current + 1);
        runUpgradeCommands(p, data, upgrade, current + 1);
        applyStat(p, data, false);
        activeBuffs.remove(p.getUniqueId());
        updateStatStatus(p, data);
        plugin.getPlayerDataHandler().saveData(p.getUniqueId(), false);
        p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.upgrade.success")
                .replace("<upgrade>", upgrade.name())
                .replace("<level>", String.valueOf(current + 1))));
        return true;
    }

    // =================================================================
    //                      PHYSICS ENGINE (CORE)
    // =================================================================

    /**
     * Starts a cancellable per-pet tick task instead of one global entity loop.
     */
    private void startPetTask(Player owner, ItemDisplay pet) {
        UUID uuid = owner.getUniqueId();
        cancelPetTask(uuid);
        ScheduledTask task = SchedulerUtils.runEntityTimer(plugin, pet, () -> tickPet(uuid, pet), 1L, 1L);
        petTasks.put(uuid, task);
    }

    private void tickPet(UUID uuid, ItemDisplay pet) {
        bobbingTick += settings.bobSpeed();
        Player p = Bukkit.getPlayer(uuid);

        if (p == null || !p.isOnline() || pet == null || pet.isDead() || !pet.isValid()) {
            cleanupEntry(uuid, pet);
            return;
        }

        checkAndNotifyFlags(p);
        PetData data = activePetData.get(uuid);
        if (data == null) return;

        if (!checkFlag(p, WorldGuardHook.PET_SPAWN)) {
            p.sendMessage(getComp("pet.worldguard.auto_despawn"));
            cleanupEntry(uuid, pet);
            return;
        }

        updateStatStatus(p, data);
        handleAttack(p, pet, data);

        if (pet.getPassengers().contains(p)) {
            setPetVisualOffset(pet, settings.rideDisplayOffsetY());
            handleRideLogic(p, pet, uuid, data);
        } else {
            setPetVisualOffset(pet, settings.displayOffsetY());
            handleFollowLogic(p, pet, uuid.hashCode());
        }
    }

    private void handleRideLogic(Player p, ItemDisplay pet, UUID uuid, PetData data) {
        if (!checkFlag(p, WorldGuardHook.PET_RIDE)) {
            p.sendMessage(getComp("pet.worldguard.auto_dismount"));
            pet.removePassenger(p);
            return;
        }

        var input = readCurrentInput(p);
        playerInputs.put(uuid, input);
        boolean canFly = data.canFly() && checkFlag(p, WorldGuardHook.PET_FLY);
        var curLoc = pet.getLocation();
        curLoc.setYaw(p.getLocation().getYaw());
        curLoc.setPitch(0);

        var velocity = currentVelocity.getOrDefault(uuid, new Vector(0, 0, 0));

        if (canFly) {
            var desiredVel = new Vector(0, 0, 0);
            if (input[0] != 0 || input[1] != 0) {
                var dir = p.getLocation().getDirection();
                if (dir.lengthSquared() > 0) {
                    dir.normalize();
                    var flatDir = p.getLocation().getDirection().setY(0);
                    if (flatDir.lengthSquared() == 0) flatDir = new Vector(0, 0, 1);
                    else flatDir.normalize();
                    var left = new Vector(0, 1, 0).crossProduct(flatDir).normalize();
                    desiredVel = dir.multiply(input[0]).add(left.multiply(input[1])).normalize().multiply(settings.flySpeed());
                }
            }
            if (input[2] > 0) desiredVel.add(new Vector(0, settings.flyVerticalSpeed(), 0));
            if (input[3] > 0) desiredVel.add(new Vector(0, -settings.flyVerticalSpeed(), 0));

            velocity.multiply(settings.flyFriction());
            velocity.add(desiredVel.multiply(settings.flyAcceleration()));

            var target = curLoc.clone().add(velocity);
            if (!isCollision(target)) curLoc.add(velocity);
            else velocity.multiply(0.5);
        } else {
            var moveDir = new Vector(0, 0, 0);
            if (input[0] != 0 || input[1] != 0) {
                var dir = p.getLocation().getDirection().setY(0);
                if (dir.lengthSquared() > 0) {
                    dir.normalize();
                    var left = new Vector(0, 1, 0).crossProduct(dir).normalize();
                    moveDir = dir.multiply(input[0]).add(left.multiply(input[1])).normalize().multiply(settings.groundSpeed());
                }
            }

            double yVel = velocity.getY();
            boolean touchingGround = isTouchingGround(curLoc);
            if (touchingGround && yVel <= 0) {
                yVel = 0;
                alignToGround(curLoc);
                if (input[2] > 0) yVel = settings.jumpForce();
            } else {
                yVel -= settings.gravity();
                if (yVel < -1.5) yVel = -1.5;
            }
            if (!touchingGround && yVel > 0 && isCollision(curLoc.clone().add(0, yVel, 0))) yVel = 0;

            velocity.setX(moveDir.getX());
            velocity.setZ(moveDir.getZ());
            velocity.setY(yVel);

            curLoc.add(0, velocity.getY(), 0);
            moveAxis(curLoc, new Vector(velocity.getX(), 0, 0), touchingGround);
            moveAxis(curLoc, new Vector(0, 0, velocity.getZ()), touchingGround);
        }

        currentVelocity.put(uuid, velocity);
        teleportPet(pet, curLoc);
    }

    /**
     * Executes the procedural logic to keep the pet trailing behind the owner.
     */
    private void handleFollowLogic(Player p, ItemDisplay pet, int seed) {
        playerInputs.remove(p.getUniqueId());
        currentVelocity.remove(p.getUniqueId());

        var targetPos = getFollowTarget(p, seed);
        var currentPos = pet.getLocation();

        if (targetPos.getWorld() != currentPos.getWorld()) {
            teleportPet(pet, targetPos, false);
            return;
        }

        double dx = p.getLocation().getX() - currentPos.getX();
        double dz = p.getLocation().getZ() - currentPos.getZ();
        double distSq = (dx * dx) + (dz * dz);

        // Safe yaw rotation to prevent NaN exceptions during idle states
        if (distSq > 0.01) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float newYaw = lerpYaw(currentPos.getYaw(), targetYaw, 0.15f);
            if (Float.isFinite(newYaw)) currentPos.setYaw(newYaw);
        }
        currentPos.setPitch(0);

        if (currentPos.distanceSquared(targetPos) > 400) {
            teleportPet(pet, targetPos, false);
        } else if (currentPos.distanceSquared(targetPos) > 0.5) {
            var dir = targetPos.toVector().subtract(currentPos.toVector());
            currentPos.add(dir.multiply(settings.followLerp()));
            double hover = Math.sin(bobbingTick + seed) * settings.bobAmplitude();
            currentPos.add(0, hover, 0);

            if (isValidLocation(currentPos)) teleportPet(pet, currentPos);
        } else {
            float targetYaw = p.getLocation().getYaw();
            if (Math.abs(currentPos.getYaw() - targetYaw) > 1) {
                float nextYaw = lerpYaw(currentPos.getYaw(), targetYaw, settings.idleLerp());
                if (Float.isFinite(nextYaw)) currentPos.setYaw(nextYaw);
            }
            double hover = Math.sin(bobbingTick + seed) * settings.idleBobAmplitude();
            currentPos.add(0, hover, 0);

            if (isValidLocation(currentPos)) teleportPet(pet, currentPos);
        }
    }

    private boolean isValidLocation(Location loc) {
        return Double.isFinite(loc.getX()) && Double.isFinite(loc.getY()) && Double.isFinite(loc.getZ()) && Float.isFinite(loc.getYaw()) && Float.isFinite(loc.getPitch());
    }

    private void moveAxis(Location loc, Vector vel, boolean onGround) {
        if (Math.abs(vel.getX()) < 0.001 && Math.abs(vel.getZ()) < 0.001) return;
        var target = loc.clone().add(vel);
        if (!isCollision(target)) {
            loc.add(vel);
        } else if (onGround) {
            for (double h = 0.5; h <= settings.maxStepHeight(); h += 0.5) {
                var stepHigh = target.clone().add(0, h, 0);
                if (!isCollision(stepHigh) && !isCollision(stepHigh.clone().add(0, 1, 0))) {
                    loc.add(vel).add(0, h, 0);
                    break;
                }
            }
        }
    }

    private Location getFollowTarget(Player p, int seed) {
        var pLoc = p.getLocation();
        var pEye = p.getEyeLocation();
        var dir = pLoc.getDirection().setY(0);
        if (dir.lengthSquared() == 0) dir = new Vector(0, 0, 1);
        else dir.normalize();
        var left = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        double b = Math.sin(bobbingTick + seed) * settings.bobAmplitude();
        var target = pEye.clone()
                .add(left.multiply(settings.followSideOffset()))
                .subtract(dir.multiply(settings.followBackOffset()))
                .add(0, b + settings.followVerticalOffset(), 0);
        target.setYaw(pLoc.getYaw());
        target.setPitch(0);
        return target;
    }

    private float lerpYaw(float current, float target, float alpha) {
        float diff = target - current;
        while (diff <= -180) diff += 360;
        while (diff > 180) diff -= 360;
        return current + diff * alpha;
    }

    private boolean isTouchingGround(Location loc) {
        var box = BoundingBox.of(loc, settings.width() / 2, 0.1, settings.width() / 2);
        box.shift(0, -settings.seatOffset() - 0.05, 0);
        return checkBlockCollision(loc.getWorld(), box);
    }

    private boolean isCollision(Location loc) {
        var box = BoundingBox.of(loc, settings.width() / 2, settings.height() / 2, settings.width() / 2);
        box.shift(0, -settings.seatOffset() + (settings.height() / 2) + 0.05, 0);
        return checkBlockCollision(loc.getWorld(), box);
    }

    private boolean checkBlockCollision(World world, BoundingBox box) {
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.ceil(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY());
        int maxY = (int) Math.ceil(box.getMaxY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.ceil(box.getMaxZ());
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    var b = world.getBlockAt(x, y, z);
                    if (b.getType().isSolid() && !b.isPassable()) {
                        if (box.overlaps(b.getBoundingBox())) return true;
                    }
                }
            }
        }
        return false;
    }

    private void alignToGround(Location loc) {
        var res = loc.getWorld().rayTraceBlocks(loc.clone().add(0, 0.5, 0), new Vector(0, -1, 0), settings.seatOffset() + 1.0, FluidCollisionMode.NEVER, true);
        if (res != null && res.getHitPosition() != null) loc.setY(res.getHitPosition().getY() + settings.seatOffset());
    }

    private void cleanupEntry(UUID uuid, ItemDisplay pet) {
        cancelPetTask(uuid);
        if (pet != null) pet.remove();
        TextDisplay name = activePetNames.remove(uuid);
        if (name != null) name.remove();
        activePets.remove(uuid);
        activePetData.remove(uuid);
        playerInputs.remove(uuid);
        lastAttackTime.remove(uuid);
        lastTargetCheckTime.remove(uuid);
        damageModifiers.remove(uuid);
        currentVelocity.remove(uuid);
        activeBuffs.remove(uuid);
        lastFlagStates.remove(uuid);
    }

    private void cancelPetTask(UUID uuid) {
        ScheduledTask task = petTasks.remove(uuid);
        if (task != null && !task.isCancelled()) task.cancel();
    }

    private void teleportPet(ItemDisplay pet, Location location) {
        teleportPet(pet, location, true);
    }

    private void teleportPet(ItemDisplay pet, Location location, boolean smooth) {
        location.setPitch(0);
        boolean riding = !pet.getPassengers().isEmpty();
        pet.setTeleportDuration(smooth && !riding ? settings.teleportDuration() : 0);
        if (isValidLocation(location)) {
            if (riding) {
                pet.teleport(location, TeleportFlag.EntityState.RETAIN_PASSENGERS);
            } else {
                pet.teleport(location);
            }
            UUID owner = getOwnerId(pet);
            if (owner != null) {
                TextDisplay name = activePetNames.get(owner);
                if (name != null) teleportName(name, location, smooth && !riding, riding);
            }
        }
    }

    private void setPetVisualOffset(ItemDisplay pet, float offsetY) {
        pet.setTransformation(new Transformation(
                new Vector3f(0, offsetY, 0),
                new AxisAngle4f(),
                new Vector3f(settings.displayScale()),
                new AxisAngle4f()
        ));
    }

    private UUID getOwnerId(ItemDisplay pet) {
        for (var entry : activePets.entrySet()) {
            if (entry.getValue().equals(pet)) return entry.getKey();
        }
        return null;
    }

    private String getMsg(String path) {
        return plugin.getPetMessagesFile().getString(path);
    }

    private Component getComp(String path) {
        return ColorUtils.parseWithPrefix(getMsg(path));
    }

    private TextDisplay spawnNameDisplay(Player owner, Location base, PetData data, int lv) {
        if (!getPetSetting(owner, data.id(), "show_name", true)) return null;
        return owner.getWorld().spawn(getNameLocation(base, false), TextDisplay.class, display -> {
            display.text(getPetName(data, lv));
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setViewRange(settings.nameViewRange());
            display.setTeleportDuration(settings.teleportDuration());
            display.setInterpolationDuration(settings.teleportDuration());
            display.setInterpolationDelay(0);
            display.setPersistent(false);
        });
    }

    private void updatePetName(TextDisplay d, PetData data, int lv) {
        d.text(getPetName(data, lv));
    }

    private Component getPetName(PetData data, int lv) {
        return ColorUtils.parse(getMsg("pet.display.name_format").replace("<name>", data.name()).replace("<level>", String.valueOf(lv)));
    }

    private Location getNameLocation(Location base, boolean riding) {
        return base.clone().add(0, riding ? settings.rideNameOffsetY() : settings.nameOffsetY(), 0);
    }

    private void teleportName(TextDisplay display, Location petLocation, boolean smooth, boolean riding) {
        display.setTeleportDuration(smooth ? settings.teleportDuration() : 0);
        display.teleport(getNameLocation(petLocation, riding));
    }

    private void refreshNameDisplay(Player p, PetData data) {
        UUID uuid = p.getUniqueId();
        TextDisplay old = activePetNames.remove(uuid);
        if (old != null) old.remove();
        ItemDisplay pet = activePets.get(uuid);
        if (pet == null || !getPetSetting(p, data.id(), "show_name", true)) return;
        TextDisplay created = spawnNameDisplay(p, pet.getLocation(), data, getPetLevel(p, data.id()));
        if (created != null) activePetNames.put(uuid, created);
    }

    private double getUpgradeBonus(Player p, PetData data, String type) {
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s == null) return 0;
        double total = 0;
        for (PetUpgrade upgrade : data.upgrades()) {
            int level = s.getUpgradeLevel(data.id(), upgrade.id());
            if (level <= 0) continue;
            String formula = "damage".equals(type) ? upgrade.damageFormula() : upgrade.statFormula();
            try {
                total += Double.parseDouble(Calculator.calculator(formula
                        .replace("<level>", String.valueOf(getPetLevel(p, data.id())))
                        .replace("<upgrade_level>", String.valueOf(level)), 2));
            } catch (NumberFormatException ignored) {
            }
        }
        return total;
    }

    public boolean checkUpgradeRequirement(Player p, PetUpgrade upgrade) {
        if (upgrade.papi() == null || upgrade.papi().isBlank()) return true;
        String raw = resolvePlaceholders(p, upgrade.papi());
        String expected = resolvePlaceholders(p, upgrade.value());
        double left = parseDouble(raw);
        double right = parseDouble(expected);
        return switch (upgrade.compare()) {
            case ">", "greater" -> left > right;
            case "<", "less" -> left < right;
            case "<=", "less_or_equal" -> left <= right;
            case "=", "==", "equals" -> raw.equalsIgnoreCase(expected) || Double.compare(left, right) == 0;
            case "!=", "not_equals" -> !raw.equalsIgnoreCase(expected) && Double.compare(left, right) != 0;
            default -> left >= right;
        };
    }

    public String resolvePlaceholders(Player p, String text) {
        if (text == null) return "";
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) papi.getMethod("setPlaceholders", Player.class, String.class).invoke(null, p, text);
        } catch (ReflectiveOperationException ignored) {
            return text;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private double calculateUpgradeFormula(Player p, PetData data, String formula, int upgradeLevel) {
        if (formula == null || formula.isBlank()) return 0;
        try {
            return Double.parseDouble(Calculator.calculator(formula
                    .replace("<level>", String.valueOf(getPetLevel(p, data.id())))
                    .replace("<upgrade_level>", String.valueOf(upgradeLevel)), 2));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void runUpgradeCommands(Player p, PetData data, PetUpgrade upgrade, int level) {
        List<String> commands = upgrade.commands();
        if (commands == null || commands.isEmpty()) return;
        for (String command : commands) {
            String parsed = resolvePlaceholders(p, command)
                    .replace("<player>", p.getName())
                    .replace("<uuid>", p.getUniqueId().toString())
                    .replace("<pet>", data.id())
                    .replace("<upgrade>", upgrade.id())
                    .replace("<level>", String.valueOf(level));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    /**
     * Assigns and dynamically recalculates statistical modifiers applied to the player via MythicLib.
     */
    private void updateStatStatus(Player p, PetData d) {
        if (!hasMythicLib) return;
        var uuid = p.getUniqueId();
        boolean allowed = checkFlag(p, WorldGuardHook.PET_BUFF) && getPetSetting(p, d.id(), "stat_buff", true);
        boolean isActive = activeBuffs.contains(uuid);
        if (allowed && !isActive) {
            applyStat(p, d, true);
            activeBuffs.add(uuid);
        } else if (!allowed && isActive) {
            applyStat(p, d, false);
            activeBuffs.remove(uuid);
        }
    }

    private void applyStat(Player p, PetData d, boolean add) {
        if (!hasMythicLib) return;
        try {
            var pd = MMOPlayerData.get(p);
            var k = "sincepet_bonus";
            if (add) {
                int lv = getPetLevel(p, d.id());
                double v = Double.parseDouble(Calculator.calculator(d.formula().replace("<level>", String.valueOf(lv)), 2)) + getUpgradeBonus(p, d, "stat");
                new StatModifier(k, d.stat(), v, ModifierType.FLAT, EquipmentSlot.OTHER, ModifierSource.OTHER).register(pd);
            } else {
                var i = pd.getStatMap().getInstance(d.stat());
                if (i != null) i.removeIf(k::equals);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Executes the pet's auto-attack sequence targeting nearby hostile entities.
     * Incorporates TPS safeguarding buffers to prevent recursive entity-checking locks.
     */
    private void handleAttack(Player p, ItemDisplay pet, PetData data) {
        if (!hasMythicLib) return;
        if (p.getWorld() != pet.getWorld()) return;
        if (data.range() <= 0 || !checkFlag(p, WorldGuardHook.PET_ATTACK) || !getPetSetting(p, data.id(), "auto_attack", true)) return;

        long now = System.currentTimeMillis();

        // Verifying global cooldowns
        if (now - lastAttackTime.getOrDefault(p.getUniqueId(), 0L) < (long) (data.cooldown() * 1000)) return;

        if (now - lastTargetCheckTime.getOrDefault(p.getUniqueId(), 0L) < settings.targetCheckMillis()) return;
        lastTargetCheckTime.put(p.getUniqueId(), now);

        var target = p.getWorld().getNearbyEntities(p.getLocation(), data.range(), data.range(), data.range()).stream()
                .filter(e -> e instanceof Monster && !e.isDead())
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(p.getLocation())))
                .orElse(null);

        if (target != null) {
            var playerData = MMOPlayerData.get(p);
            var damager = StatProvider.get(p, EquipmentSlot.MAIN_HAND, true);

            int lv = getPetLevel(p, data.id());
            double petBaseDmg = Double.parseDouble(Calculator.calculator(data.getDamageFormula().replace("<level>", String.valueOf(lv)), 2)) + getUpgradeBonus(p, data, "damage");
            double playerPhysicalDmg = playerData.getStatMap().getInstance("ATTACK_DAMAGE").getTotal();
            double finalPhysicalDmg = petBaseDmg + (playerPhysicalDmg * data.inheritance());

            var physicalMeta = new DamageMetadata(finalPhysicalDmg, (Element) null, DamageType.SKILL, DamageType.PHYSICAL);

            target.setNoDamageTicks(0);
            MythicLib.plugin.getDamage().registerAttack(new AttackMetadata(physicalMeta, target, damager), true, false);

            getAbilityManager().triggerActive(p, "PET_ATTACK");
            getAbilityManager().triggerPassive(p, "PET_ATTACK", target);

            for (Element element : MythicLib.plugin.getElements().getAll()) {
                var statKey = UtilityMethods.enumName(element.getId() + "_DAMAGE");
                if (playerData.getStatMap().getInstance(statKey).isEmpty()) continue;

                double playerElementDmg = playerData.getStatMap().getInstance(statKey).getTotal();
                if (playerElementDmg > 0) {
                    double finalElementDmg = playerElementDmg * data.inheritance();
                    var elementMeta = new DamageMetadata(finalElementDmg, element, DamageType.SKILL, DamageType.MAGIC);
                    target.setNoDamageTicks(0);
                    MythicLib.plugin.getDamage().registerAttack(new AttackMetadata(elementMeta, target, damager), true, false);
                }
            }

            // Execute Graphical Effects natively grabbed from configurations
            var start = pet.getLocation().add(0, 0.5, 0);
            var end = target.getEyeLocation();
            var dir = end.toVector().subtract(start.toVector()).normalize();
            double dist = start.distance(end);

            var particleConfig = data.attackParticle();
            if (particleConfig == null) {
                particleConfig = plugin.getConfigFile().getString("effects.attack.particle", "CRIT");
            }
            spawnParticleSafe(start.getWorld(), start, particleConfig, dir, dist);
            playSoundSafe(p, start, plugin.getConfigFile().getString("effects.attack.sound", "ENTITY_PLAYER_ATTACK_SWEEP"));

            lastAttackTime.put(p.getUniqueId(), now);
        }
    }

    /**
     * Safely triggers a particle at a defined location avoiding console exceptions.
     */
    private void spawnParticleSafe(World world, Location loc, String particleName, int count) {
        try {
            world.spawnParticle(Particle.valueOf(particleName), loc, count, 0.2, 0.2, 0.2, 0.05);
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Safely executes a directional particle vector array.
     */
    private void spawnParticleSafe(World world, Location start, String particleName, Vector dir, double dist) {
        try {
            var p = Particle.valueOf(particleName);
            for (double i = 0; i < dist; i += 0.5) {
                world.spawnParticle(p, start.clone().add(dir.clone().multiply(i)), 1, 0, 0, 0, 0);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Safely triggers a sound packet sent to the specified player.
     */
    private void playSoundSafe(Player p, Location loc, String soundName) {
        Sound sound = resolveSound(soundName);
        if (sound != null) p.playSound(loc, sound, 1f, 1f);
    }

    private Sound resolveSound(String soundName) {
        if (soundName == null || soundName.isBlank()) return null;
        String normalized = soundName.toLowerCase().replace('_', '.');
        NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        return key == null ? null : Registry.SOUNDS.get(key);
    }

    /**
     * Resolves geographical restrictions dynamically querying the WorldGuard Region container.
     */
    private boolean checkFlag(Player p, StateFlag flag) {
        if (!hasWorldGuard) return true;
        var localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);
        var query = plugin.getWorldGuardHook().getQuery();
        var loc = BukkitAdapter.adapt(p.getLocation()); // Uses 'var' to prevent FQCN import overlap issues
        return query.testState(loc, localPlayer, flag);
    }

    private void checkAndNotifyFlags(Player p) {
        if (!hasWorldGuard) return;
        var uuid = p.getUniqueId();
        lastFlagStates.putIfAbsent(uuid, new HashMap<>());
        var states = lastFlagStates.get(uuid);
        checkSingleFlagNotify(p, states, WorldGuardHook.PET_SPAWN, "spawn");
        checkSingleFlagNotify(p, states, WorldGuardHook.PET_RIDE, "ride");
        checkSingleFlagNotify(p, states, WorldGuardHook.PET_FLY, "fly");
        checkSingleFlagNotify(p, states, WorldGuardHook.PET_ATTACK, "attack");
        checkSingleFlagNotify(p, states, WorldGuardHook.PET_BUFF, "buff");
    }

    private void checkSingleFlagNotify(Player p, Map<StateFlag, Boolean> states, StateFlag flag, String key) {
        boolean current = checkFlag(p, flag);
        if (!states.containsKey(flag)) {
            states.put(flag, current);
            return;
        }
        boolean last = states.get(flag);
        if (current != last) {
            if (current) p.sendMessage(getComp("pet.notifications.leave_deny_" + key));
            else p.sendMessage(getComp("pet.notifications.enter_deny_" + key));
            states.put(flag, current);
        }
    }

    private ItemStack getSkull(String base64) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        var profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Processes Experience Point injections modifying dynamic maximum level boundaries.
     */
    public void addExp(Player p, double amount) {
        var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s == null || s.getActivePetId() == null) return;

        var petId = s.getActivePetId();
        var data = activePetData.get(p.getUniqueId());
        if (data == null) return;

        int currentLevel = s.getLevel(petId);
        int maxLevel = s.getMaxPetLevel(petId);

        if (currentLevel >= maxLevel) {
            p.sendActionBar(ColorUtils.parse(getMsg("pet.level.max_level_actionbar")));
            return;
        }

        double currentXp = s.getXp(petId);
        double newXp = currentXp + amount;
        double maxXp = Double.parseDouble(Calculator.calculator(data.getMaxXpFormula().replace("<level>", String.valueOf(currentLevel)), 2));

        while (newXp >= maxXp) {
            if (currentLevel >= maxLevel) {
                newXp = 0;
                break;
            }
            newXp -= maxXp;
            levelUp(p, p);
            currentLevel++;
            if (currentLevel >= maxLevel) {
                newXp = 0;
                break;
            }
            maxXp = Double.parseDouble(Calculator.calculator(data.getMaxXpFormula().replace("<level>", String.valueOf(currentLevel)), 2));
        }

        s.setXp(petId, newXp);

        if (currentLevel >= maxLevel) {
            p.sendActionBar(ColorUtils.parse(getMsg("pet.level.max_level_actionbar")));
        } else {
            var xpMsg = getMsg("pet.level.xp_actionbar").replace("<amount>", String.format("%.1f", amount)).replace("<current_xp>", String.format("%.1f", newXp)).replace("<max_xp>", String.format("%.1f", maxXp));
            p.sendActionBar(ColorUtils.parse(xpMsg));
        }
    }
}
