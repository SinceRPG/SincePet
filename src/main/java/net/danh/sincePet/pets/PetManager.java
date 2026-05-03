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
import net.danh.sincePet.SincePet;
import net.danh.sincePet.hooks.WorldGuardHook;
import net.danh.sincePet.utils.Calculator;
import net.danh.sincePet.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the core physical logic, generation, attacking mechanics, and stat assignments of Pets.
 */
public class PetManager {

    // Physics Engine Core Constants
    private static final float SEAT_OFFSET = 0.7f;
    private static final double PET_WIDTH = 0.6;
    private static final double PET_HEIGHT = 0.8;
    private static final double GRAVITY = 0.08;
    private static final double JUMP_FORCE = 0.6;
    private static final double MOVE_SPEED_GROUND = 0.45;
    private static final double MAX_STEP_HEIGHT = 1.1;
    private static final double FLY_SPEED = 0.8;
    private static final double FLY_ACCEL = 0.15;
    private static final double FLY_FRICTION = 0.85;
    private static final double FLY_VERTICAL_SPEED = 0.4;
    private static final int TELEPORT_DURATION = 2;

    private final SincePet plugin;
    private final PetConfig petConfig;
    private final Map<UUID, ItemDisplay> activePets = new ConcurrentHashMap<>();
    private final Map<UUID, PetData> activePetData = new ConcurrentHashMap<>();
    private final Map<UUID, float[]> playerInputs = new ConcurrentHashMap<>();
    private final Map<UUID, Vector> currentVelocity = new ConcurrentHashMap<>();

    // TPS Optimizers
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTargetCheckTime = new ConcurrentHashMap<>();

    private final Map<UUID, Double> damageModifiers = new ConcurrentHashMap<>();
    private final Map<UUID, Map<StateFlag, Boolean>> lastFlagStates = new ConcurrentHashMap<>();
    private final Set<UUID> activeBuffs = ConcurrentHashMap.newKeySet();

    private final boolean hasMythicLib;
    private final boolean hasWorldGuard;
    private double bobbingTick = 0;

    public PetManager(SincePet plugin) {
        this.plugin = plugin;
        this.petConfig = new PetConfig(plugin);
        this.hasMythicLib = Bukkit.getPluginManager().isPluginEnabled("MythicLib");
        this.hasWorldGuard = plugin.getWorldGuardHook() != null;
        startPetRunnable();
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
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                var s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
                if (s != null && s.getActivePetId() != null) {
                    var data = petConfig.getPet(s.getActivePetId());
                    if (data != null && p.hasPermission("pet." + data.id().toLowerCase()))
                        spawnPet(p, data, s.getLevel(data.id()), false);
                    else s.setActivePetId(null);
                }
            }
        }.runTaskLater(plugin, 20L);
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
    }

    /**
     * Flushes current configurations and recreates pet entities.
     */
    public void reload() {
        disable();
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

        var spawnLoc = p.getLocation().add(0, SEAT_OFFSET, 0);
        var pet = p.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
            d.setItemStack(getSkull(data.texture()));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setTransformation(new Transformation(new Vector3f(0, -SEAT_OFFSET, 0), new AxisAngle4f(), new Vector3f(1f), new AxisAngle4f()));
            d.setBillboard(Display.Billboard.FIXED);
            d.setViewRange(0.6f);
            d.setTeleportDuration(TELEPORT_DURATION);
            d.setPersistent(false);
            d.setCustomNameVisible(true);
            updatePetName(d, data, level);
        });

        spawnParticleSafe(p.getWorld(), spawnLoc, plugin.getConfigFile().getString("effects.spawn.particle", "CLOUD"), 10);
        playSoundSafe(p, spawnLoc, plugin.getConfigFile().getString("effects.spawn.sound", "ENTITY_CHICKEN_EGG"));

        activePets.put(p.getUniqueId(), pet);
        activePetData.put(p.getUniqueId(), data);
        currentVelocity.put(p.getUniqueId(), new Vector(0, 0, 0));
        updateStatStatus(p, data);

        if (msg) p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.spawn").replace("<name>", data.name())));
    }

    /**
     * Unregisters the visual components and stat modifiers associated with a pet.
     */
    public void removePetVisuals(Player p) {
        var id = p.getUniqueId();
        var pet = activePets.remove(id);
        if (pet != null) {
            spawnParticleSafe(p.getWorld(), pet.getLocation(), plugin.getConfigFile().getString("effects.despawn.particle", "POOF"), 5);
            pet.remove();
        }
        var d = activePetData.remove(id);
        if (d != null) applyStat(p, d, false);

        playerInputs.remove(id);
        lastAttackTime.remove(id);
        lastTargetCheckTime.remove(id);
        damageModifiers.remove(id);
        currentVelocity.remove(id);
        activeBuffs.remove(id);
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
        if (data != null && !data.rideable()) {
            p.sendMessage(getComp("pet.command.ride_not_allowed"));
            return;
        }
        activePets.get(p.getUniqueId()).addPassenger(p);
        p.sendMessage(getComp("pet.command.ride_success"));
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
            updatePetName(activePets.get(target.getUniqueId()), data, newLv);
        }
        var name = (data != null) ? data.name() : id;
        target.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.levelup_self").replace("<name>", name).replace("<level>", String.valueOf(newLv))));
        playSoundSafe(target, target.getLocation(), plugin.getConfigFile().getString("effects.levelup.sound", "ENTITY_PLAYER_LEVELUP"));

        if (!sender.equals(target))
            sender.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.levelup_other").replace("<target>", target.getName()).replace("<level>", String.valueOf(newLv))));
    }

    public void updateInput(Player p, float f, float s, boolean jump) {
        if (activePets.containsKey(p.getUniqueId()))
            playerInputs.put(p.getUniqueId(), new float[]{f, s, jump ? 1.0f : 0.0f});
    }

    public boolean isPlayerPet(Entity entity) {
        return activePets.containsValue(entity);
    }

    // =================================================================
    //                      PHYSICS ENGINE (CORE)
    // =================================================================

    /**
     * Master repeating task. Handles collision, input vectors, following algorithms, and combat intervals.
     */
    private void startPetRunnable() {
        new BukkitRunnable() {
            public void run() {
                bobbingTick += 0.15;
                var it = activePets.entrySet().iterator();

                while (it.hasNext()) {
                    var entry = it.next();
                    var uuid = entry.getKey();
                    var pet = entry.getValue();
                    var p = Bukkit.getPlayer(uuid);

                    if (p == null || !p.isOnline() || pet == null || pet.isDead()) {
                        cleanupEntry(it, uuid, pet);
                        continue;
                    }

                    checkAndNotifyFlags(p);
                    var data = activePetData.get(uuid);
                    if (data == null) continue;

                    if (!checkFlag(p, WorldGuardHook.PET_SPAWN)) {
                        p.sendMessage(getComp("pet.worldguard.auto_despawn"));
                        cleanupEntry(it, uuid, pet);
                        continue;
                    }

                    updateStatStatus(p, data);
                    handleAttack(p, pet, data);

                    // Riding mechanics execution
                    if (pet.getPassengers().contains(p)) {
                        if (!checkFlag(p, WorldGuardHook.PET_RIDE)) {
                            p.sendMessage(getComp("pet.worldguard.auto_dismount"));
                            pet.removePassenger(p);
                            continue;
                        }

                        var i = playerInputs.getOrDefault(uuid, new float[]{0, 0, 0});
                        boolean canFly = data.canFly() && checkFlag(p, WorldGuardHook.PET_FLY);
                        var curLoc = pet.getLocation();
                        curLoc.setYaw(p.getLocation().getYaw());

                        var velocity = currentVelocity.getOrDefault(uuid, new Vector(0, 0, 0));

                        if (canFly) {
                            curLoc.setPitch(p.getLocation().getPitch());
                            var desiredVel = new Vector(0, 0, 0);
                            if (i[0] != 0 || i[1] != 0) {
                                var dir = p.getLocation().getDirection().normalize();
                                var left = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                                desiredVel = dir.multiply(i[0]).add(left.multiply(i[1])).normalize().multiply(FLY_SPEED);
                            }
                            if (i[2] > 0) desiredVel.add(new Vector(0, FLY_VERTICAL_SPEED, 0));

                            velocity.multiply(FLY_FRICTION);
                            velocity.add(desiredVel.multiply(FLY_ACCEL));

                            var target = curLoc.clone().add(velocity);
                            if (!isCollision(target)) curLoc.add(velocity);
                            else velocity.multiply(0.5);

                        } else {
                            curLoc.setPitch(0);
                            var moveDir = new Vector(0, 0, 0);
                            if (i[0] != 0 || i[1] != 0) {
                                var dir = p.getLocation().getDirection().setY(0).normalize();
                                var left = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                                moveDir = dir.multiply(i[0]).add(left.multiply(i[1])).normalize().multiply(MOVE_SPEED_GROUND);
                            }

                            double yVel = velocity.getY();
                            boolean touchingGround = isTouchingGround(curLoc);
                            if (touchingGround && yVel <= 0) {
                                yVel = 0;
                                alignToGround(curLoc);
                                if (i[2] > 0) yVel = JUMP_FORCE;
                            } else {
                                yVel -= GRAVITY;
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
                        pet.setTeleportDuration(TELEPORT_DURATION);
                        pet.teleport(curLoc);

                    } else {
                        handleFollowLogic(p, pet, uuid.hashCode());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
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
            pet.teleport(targetPos);
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

        if (currentPos.distanceSquared(targetPos) > 400) {
            pet.setTeleportDuration(0);
            pet.teleport(targetPos);
        } else if (currentPos.distanceSquared(targetPos) > 0.5) {
            var dir = targetPos.toVector().subtract(currentPos.toVector());
            currentPos.add(dir.multiply(0.15));
            double hover = Math.sin(bobbingTick + seed) * 0.05;
            currentPos.add(0, hover, 0);

            pet.setTeleportDuration(TELEPORT_DURATION);
            if (isValidLocation(currentPos)) pet.teleport(currentPos);
        } else {
            float targetYaw = p.getLocation().getYaw();
            if (Math.abs(currentPos.getYaw() - targetYaw) > 1) {
                float nextYaw = lerpYaw(currentPos.getYaw(), targetYaw, 0.1f);
                if (Float.isFinite(nextYaw)) currentPos.setYaw(nextYaw);
            }
            double hover = Math.sin(bobbingTick + seed) * 0.03;
            currentPos.add(0, hover, 0);

            pet.setTeleportDuration(2);
            if (isValidLocation(currentPos)) pet.teleport(currentPos);
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
            for (double h = 0.5; h <= MAX_STEP_HEIGHT; h += 0.5) {
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
        var dir = pLoc.getDirection().setY(0).normalize();
        var left = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        double b = Math.sin(bobbingTick + seed) * 0.1;
        var target = pEye.clone().add(left.multiply(1.0)).subtract(dir.multiply(0.3)).add(0, b - 0.2, 0);
        target.setYaw(pLoc.getYaw());
        return target;
    }

    private float lerpYaw(float current, float target, float alpha) {
        float diff = target - current;
        while (diff <= -180) diff += 360;
        while (diff > 180) diff -= 360;
        return current + diff * alpha;
    }

    private boolean isTouchingGround(Location loc) {
        var box = BoundingBox.of(loc, PET_WIDTH / 2, 0.1, PET_WIDTH / 2);
        box.shift(0, -SEAT_OFFSET - 0.05, 0);
        return checkBlockCollision(loc.getWorld(), box);
    }

    private boolean isCollision(Location loc) {
        var box = BoundingBox.of(loc, PET_WIDTH / 2, PET_HEIGHT / 2, PET_WIDTH / 2);
        box.shift(0, -SEAT_OFFSET + (PET_HEIGHT / 2), 0);
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
        var res = loc.getWorld().rayTraceBlocks(loc.clone().add(0, 0.5, 0), new Vector(0, -1, 0), SEAT_OFFSET + 1.0, FluidCollisionMode.NEVER, true);
        if (res != null && res.getHitPosition() != null) loc.setY(res.getHitPosition().getY() + SEAT_OFFSET);
    }

    private void cleanupEntry(Iterator<Map.Entry<UUID, ItemDisplay>> it, UUID uuid, ItemDisplay pet) {
        if (pet != null) pet.remove();
        activePetData.remove(uuid);
        playerInputs.remove(uuid);
        lastAttackTime.remove(uuid);
        lastTargetCheckTime.remove(uuid);
        damageModifiers.remove(uuid);
        currentVelocity.remove(uuid);
        activeBuffs.remove(uuid);
        lastFlagStates.remove(uuid);
        it.remove();
    }

    private String getMsg(String path) {
        return plugin.getPetMessagesFile().getString(path);
    }

    private Component getComp(String path) {
        return ColorUtils.parseWithPrefix(getMsg(path));
    }

    private void updatePetName(ItemDisplay d, PetData data, int lv) {
        d.customName(ColorUtils.parse(getMsg("pet.display.name_format").replace("<name>", data.name()).replace("<level>", String.valueOf(lv))));
    }

    /**
     * Assigns and dynamically recalculates statistical modifiers applied to the player via MythicLib.
     */
    private void updateStatStatus(Player p, PetData d) {
        if (!hasMythicLib) return;
        var uuid = p.getUniqueId();
        boolean allowed = checkFlag(p, WorldGuardHook.PET_BUFF);
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
                double v = Double.parseDouble(Calculator.calculator(d.formula().replace("<level>", String.valueOf(lv)), 2));
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
        if (p.getWorld() != pet.getWorld()) return;
        if (data.range() <= 0 || !checkFlag(p, WorldGuardHook.PET_ATTACK)) return;

        long now = System.currentTimeMillis();

        // Verifying global cooldowns
        if (now - lastAttackTime.getOrDefault(p.getUniqueId(), 0L) < (long) (data.cooldown() * 1000)) return;

        // TPS Optimization: Throttle the heavy entity iteration query to once every 500ms
        if (now - lastTargetCheckTime.getOrDefault(p.getUniqueId(), 0L) < 500) return;
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
            double petBaseDmg = Double.parseDouble(Calculator.calculator(data.getDamageFormula().replace("<level>", String.valueOf(lv)), 2));
            double playerPhysicalDmg = playerData.getStatMap().getInstance("ATTACK_DAMAGE").getTotal();
            double finalPhysicalDmg = petBaseDmg + (playerPhysicalDmg * data.inheritance());

            var physicalMeta = new DamageMetadata(finalPhysicalDmg, (Element) null, DamageType.SKILL, DamageType.PHYSICAL);

            target.setNoDamageTicks(0);
            MythicLib.plugin.getDamage().registerAttack(new AttackMetadata(physicalMeta, target, damager), true, false);

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

            var particleConfig = plugin.getConfigFile().getString("effects.attack.particle", "CRIT");
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
        try {
            p.playSound(loc, Sound.valueOf(soundName), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
        }
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