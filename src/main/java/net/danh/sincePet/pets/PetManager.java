package net.danh.sincePet.pets;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
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
import net.danh.sincePet.data.PetConfig;
import net.danh.sincePet.data.PlayerDataHandler;
import net.danh.sincePet.hooks.WorldGuardHook;
import net.danh.sincePet.utils.Calculator;
import net.danh.sincePet.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetManager {

    // --- CẤU HÌNH VẬT LÝ ---
    private static final float SEAT_OFFSET = 0.7f;
    private static final double PET_WIDTH = 0.6;
    private static final double PET_HEIGHT = 0.8;

    // Physics Constants
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
    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
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

    public void onPlayerJoin(Player p) {
        plugin.getPlayerDataHandler().loadData(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                PlayerDataHandler.PlayerSession s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
                if (s != null && s.getActivePetId() != null) {
                    PetData data = petConfig.getPet(s.getActivePetId());
                    if (data != null && p.hasPermission("pet." + data.id().toLowerCase()))
                        spawnPet(p, data, s.getLevel(data.id()), false);
                    else s.setActivePetId(null);
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    public void onPlayerQuit(Player p) {
        removePetVisuals(p);
        lastFlagStates.remove(p.getUniqueId());
    }

    public void disable() {
        for (Player p : Bukkit.getOnlinePlayers()) removePetVisuals(p);
    }

    public void reload() {
        disable();
        petConfig.loadPets();
        for (Player p : Bukkit.getOnlinePlayers()) onPlayerJoin(p);
    }

    public void spawnPet(Player p, PetData data, int level, boolean msg) {
        if (!checkFlag(p, WorldGuardHook.PET_SPAWN)) {
            if (msg) p.sendMessage(getComp("pet.worldguard.deny_spawn"));
            return;
        }

        removePetVisuals(p);
        PlayerDataHandler.PlayerSession s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s != null) s.setActivePetId(data.id());

        Location spawnLoc = p.getLocation().add(0, SEAT_OFFSET, 0);
        ItemDisplay pet = p.getWorld().spawn(spawnLoc, ItemDisplay.class, d -> {
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

        // HIỆU ỨNG SPAWN
        p.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 10, 0.2, 0.2, 0.2, 0.05);
        p.playSound(spawnLoc, Sound.ENTITY_CHICKEN_EGG, 1f, 1f);

        activePets.put(p.getUniqueId(), pet);
        activePetData.put(p.getUniqueId(), data);
        currentVelocity.put(p.getUniqueId(), new Vector(0, 0, 0));
        updateStatStatus(p, data);

        if (msg) p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.spawn").replace("<name>", data.name())));
    }

    public void removePetVisuals(Player p) {
        UUID id = p.getUniqueId();
        ItemDisplay pet = activePets.remove(id);
        if (pet != null) {
            // HIỆU ỨNG DESPAWN
            p.getWorld().spawnParticle(Particle.POOF, pet.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
            pet.remove();
        }
        PetData d = activePetData.remove(id);
        if (d != null) applyStat(p, d, false);

        playerInputs.remove(id);
        lastAttackTime.remove(id);
        damageModifiers.remove(id);
        currentVelocity.remove(id);
        activeBuffs.remove(id);
    }

    public void ridePet(Player p) {
        if (!activePets.containsKey(p.getUniqueId())) {
            p.sendMessage(getComp("pet.command.ride_fail"));
            return;
        }
        PetData data = activePetData.get(p.getUniqueId());
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

    public void despawnPet(Player p) {
        removePetVisuals(p);
        PlayerDataHandler.PlayerSession s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s != null) s.setActivePetId(null);
    }

    public int getPetLevel(Player p, String petId) {
        PlayerDataHandler.PlayerSession s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        return (s != null) ? s.getLevel(petId) : 1;
    }

    public void levelUp(Player target, CommandSender sender) {
        PlayerDataHandler.PlayerSession s = plugin.getPlayerDataHandler().getSession(target.getUniqueId());
        if (s == null || s.getActivePetId() == null) {
            sender.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.levelup_fail_target").replace("<target>", target.getName())));
            return;
        }
        String id = s.getActivePetId();
        int newLv = s.getLevel(id) + 1;
        s.setLevel(id, newLv);
        PetData data = activePetData.get(target.getUniqueId());
        if (data != null) {
            updateStatStatus(target, data);
            updatePetName(activePets.get(target.getUniqueId()), data, newLv);
        }
        String name = (data != null) ? data.name() : id;
        target.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.levelup_self").replace("<name>", name).replace("<level>", String.valueOf(newLv))));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
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

    private void startPetRunnable() {
        new BukkitRunnable() {
            public void run() {
                bobbingTick += 0.15; // Tăng tốc độ bobbing nhẹ
                Iterator<Map.Entry<UUID, ItemDisplay>> it = activePets.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<UUID, ItemDisplay> entry = it.next();
                    UUID uuid = entry.getKey();
                    ItemDisplay pet = entry.getValue();
                    Player p = Bukkit.getPlayer(uuid);

                    if (p == null || !p.isOnline() || pet == null || pet.isDead()) {
                        cleanupEntry(it, uuid, pet);
                        continue;
                    }

                    checkAndNotifyFlags(p);
                    PetData data = activePetData.get(uuid);
                    if (data == null) continue;

                    if (!checkFlag(p, WorldGuardHook.PET_SPAWN)) {
                        p.sendMessage(getComp("pet.worldguard.auto_despawn"));
                        cleanupEntry(it, uuid, pet);
                        continue;
                    }

                    updateStatStatus(p, data);
                    handleAttack(p, pet, data);

                    // ================= LOGIC: RIDING =================
                    if (pet.getPassengers().contains(p)) {
                        if (!checkFlag(p, WorldGuardHook.PET_RIDE)) {
                            p.sendMessage(getComp("pet.worldguard.auto_dismount"));
                            pet.removePassenger(p);
                            continue;
                        }

                        float[] i = playerInputs.getOrDefault(uuid, new float[]{0, 0, 0});
                        boolean canFly = data.canFly() && checkFlag(p, WorldGuardHook.PET_FLY);
                        Location curLoc = pet.getLocation();
                        curLoc.setYaw(p.getLocation().getYaw()); // Đồng bộ Yaw

                        Vector velocity = currentVelocity.getOrDefault(uuid, new Vector(0, 0, 0));

                        if (canFly) {
                            curLoc.setPitch(p.getLocation().getPitch());
                            Vector desiredVel = new Vector(0, 0, 0);
                            if (i[0] != 0 || i[1] != 0) {
                                Vector dir = p.getLocation().getDirection().normalize();
                                Vector left = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
                                desiredVel = dir.multiply(i[0]).add(left.multiply(i[1])).normalize().multiply(FLY_SPEED);
                            }
                            if (i[2] > 0) desiredVel.add(new Vector(0, FLY_VERTICAL_SPEED, 0));

                            velocity.multiply(FLY_FRICTION);
                            velocity.add(desiredVel.multiply(FLY_ACCEL));

                            Location target = curLoc.clone().add(velocity);
                            if (!isCollision(target)) curLoc.add(velocity);
                            else velocity.multiply(0.5); // Giảm tốc khi va chạm

                        } else {
                            curLoc.setPitch(0);
                            Vector moveDir = new Vector(0, 0, 0);
                            if (i[0] != 0 || i[1] != 0) {
                                Vector dir = p.getLocation().getDirection().setY(0).normalize();
                                Vector left = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
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
                        // ================= LOGIC: FOLLOWING =================
                        handleFollowLogic(p, pet, uuid.hashCode());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void handleFollowLogic(Player p, ItemDisplay pet, int seed) {
        playerInputs.remove(p.getUniqueId());
        currentVelocity.remove(p.getUniqueId());

        Location targetPos = getFollowTarget(p, seed);
        Location currentPos = pet.getLocation();
        double distSq = currentPos.distanceSquared(targetPos);

        // Logic: Xoay đầu nhìn về phía chủ (hoặc hướng di chuyển)
        Location pLoc = p.getLocation();
        if (distSq > 0.1) {
            Vector dir = pLoc.toVector().subtract(currentPos.toVector()).normalize();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
            float newYaw = lerpYaw(currentPos.getYaw(), targetYaw, 0.15f);
            currentPos.setYaw(newYaw);
        }

        if (distSq > 400) {
            pet.setTeleportDuration(0);
            pet.teleport(targetPos);
        } else if (distSq > 0.5) {
            Vector dir = targetPos.toVector().subtract(currentPos.toVector());
            currentPos.add(dir.multiply(0.15));

            // Hiệu ứng nhấp nhô khi bay/đi
            double hover = Math.sin(bobbingTick + seed) * 0.05;
            currentPos.add(0, hover, 0);

            pet.setTeleportDuration(TELEPORT_DURATION);
            pet.teleport(currentPos);
        } else {
            // Đứng yên: Nhìn theo hướng chủ + Nhấp nhô
            float targetYaw = p.getLocation().getYaw();
            if (Math.abs(currentPos.getYaw() - targetYaw) > 1) {
                currentPos.setYaw(lerpYaw(currentPos.getYaw(), targetYaw, 0.1f));
            }
            double hover = Math.sin(bobbingTick + seed) * 0.03; // Nhấp nhô nhẹ hơn khi đứng
            currentPos.add(0, hover, 0);

            pet.setTeleportDuration(2);
            pet.teleport(currentPos);
        }
    }

    // Các helper function khác (moveAxis, getFollowTarget, lerpYaw, collision...) GIỮ NGUYÊN
    private void moveAxis(Location loc, Vector vel, boolean onGround) {
        if (Math.abs(vel.getX()) < 0.001 && Math.abs(vel.getZ()) < 0.001) return;
        Location target = loc.clone().add(vel);
        if (!isCollision(target)) {
            loc.add(vel);
        } else if (onGround) {
            for (double h = 0.5; h <= MAX_STEP_HEIGHT; h += 0.5) {
                Location stepHigh = target.clone().add(0, h, 0);
                if (!isCollision(stepHigh) && !isCollision(stepHigh.clone().add(0, 1, 0))) {
                    loc.add(vel).add(0, h, 0);
                    break;
                }
            }
        }
    }

    private Location getFollowTarget(Player p, int seed) {
        Location pLoc = p.getLocation();
        Location pEye = p.getEyeLocation();
        Vector dir = pLoc.getDirection().setY(0).normalize();
        Vector left = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        double b = Math.sin(bobbingTick + seed) * 0.1;
        Location target = pEye.clone().add(left.multiply(1.0)).subtract(dir.multiply(0.3)).add(0, b - 0.2, 0);
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
        BoundingBox box = BoundingBox.of(loc, PET_WIDTH / 2, 0.1, PET_WIDTH / 2);
        box.shift(0, -SEAT_OFFSET - 0.05, 0);
        return checkBlockCollision(loc.getWorld(), box);
    }

    private boolean isCollision(Location loc) {
        BoundingBox box = BoundingBox.of(loc, PET_WIDTH / 2, PET_HEIGHT / 2, PET_WIDTH / 2);
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
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType().isSolid() && !b.isPassable()) {
                        if (box.overlaps(b.getBoundingBox())) return true;
                    }
                }
            }
        }
        return false;
    }

    private void alignToGround(Location loc) {
        RayTraceResult res = loc.getWorld().rayTraceBlocks(loc.clone().add(0, 0.5, 0), new Vector(0, -1, 0), SEAT_OFFSET + 1.0, FluidCollisionMode.NEVER, true);
        if (res != null && res.getHitPosition() != null) loc.setY(res.getHitPosition().getY() + SEAT_OFFSET);
    }

    private void cleanupEntry(Iterator<Map.Entry<UUID, ItemDisplay>> it, UUID uuid, ItemDisplay pet) {
        if (pet != null) pet.remove();
        activePetData.remove(uuid);
        playerInputs.remove(uuid);
        lastAttackTime.remove(uuid);
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
        // CHỈNH SỬA: Dùng key từ messages.yml
        d.customName(ColorUtils.parse(getMsg("pet.display.name_format").replace("<name>", data.name()).replace("<level>", String.valueOf(lv))));
    }

    private void updateStatStatus(Player p, PetData d) {
        if (!hasMythicLib) return;
        UUID uuid = p.getUniqueId();
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
            MMOPlayerData pd = MMOPlayerData.get(p);
            String k = "sincepet_bonus";
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

    private void handleAttack(Player p, ItemDisplay pet, PetData data) {
        if (!checkFlag(p, WorldGuardHook.PET_ATTACK) || data.range() <= 0) return;
        long now = System.currentTimeMillis();
        if (now - lastAttackTime.getOrDefault(p.getUniqueId(), 0L) < (long) (data.cooldown() * 1000)) return;
        LivingEntity target = p.getWorld().getNearbyEntities(p.getLocation(), data.range(), data.range(), data.range())
                .stream()
                .filter(e -> e instanceof Monster && !e.isDead())
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(p.getLocation())))
                .orElse(null);
        if (target != null) {
            int lv = getPetLevel(p, data.id());
            double dmg = Double.parseDouble(Calculator.calculator(data.getDamageFormula().replace("<level>", String.valueOf(lv)), 2));
            Element attackElement = null;
            MMOPlayerData playerData = MMOPlayerData.get(p);
            String stat = null;
            for (Element element : MythicLib.plugin.getElements().getAll()) {
                stat = UtilityMethods.enumName(element.getId() + "_DAMAGE");
                if (playerData.getStatMap().getInstance(stat).getTotal() > 0) {
                    attackElement = element;
                    break;
                }
            }
            DamageMetadata damageMeta = new DamageMetadata(dmg + (playerData.getStatMap().getInstance(stat != null ? stat : "ATTACK_DAMAGE").getTotal() * data.inheritance()), attackElement, DamageType.SKILL, DamageType.PHYSICAL);
            final @Nullable StatProvider damager = StatProvider.get(p, EquipmentSlot.MAIN_HAND, true);
            MythicLib.plugin.getDamage().registerAttack(new AttackMetadata(damageMeta, target, damager), true);
            Location start = pet.getLocation().add(0, 0.5, 0);
            Location end = target.getEyeLocation();
            Vector dir = end.toVector().subtract(start.toVector()).normalize();
            double dist = start.distance(end);
            for (double i = 0; i < dist; i += 0.5) {
                start.getWorld().spawnParticle(Particle.CRIT, start.clone().add(dir.clone().multiply(i)), 1, 0, 0, 0, 0);
            }
            p.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 2f);
            lastAttackTime.put(p.getUniqueId(), now);
        }
    }

    private boolean checkFlag(Player p, StateFlag flag) {
        if (!hasWorldGuard) return true;
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);
        RegionQuery query = plugin.getWorldGuardHook().getQuery();
        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(p.getLocation());
        return query.testState(loc, localPlayer, flag);
    }

    private void checkAndNotifyFlags(Player p) {
        if (!hasWorldGuard) return;
        UUID uuid = p.getUniqueId();
        lastFlagStates.putIfAbsent(uuid, new HashMap<>());
        Map<StateFlag, Boolean> states = lastFlagStates.get(uuid);
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
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    public void addExp(Player p, double amount) {
        PlayerDataHandler.PlayerSession s = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (s == null || s.getActivePetId() == null) return;

        String petId = s.getActivePetId();
        PetData data = activePetData.get(p.getUniqueId());
        if (data == null) return;

        double currentXp = s.getXp(petId);
        double newXp = currentXp + amount;
        int currentLevel = s.getLevel(petId);

        // Tính Max XP
        double maxXp = Double.parseDouble(Calculator.calculator(data.getMaxXpFormula().replace("<level>", String.valueOf(currentLevel)), 2));

        // Level Up Logic (Dùng WHILE để hỗ trợ thăng nhiều cấp)
        while (newXp >= maxXp) {
            newXp -= maxXp;
            levelUp(p, p);

            // Cập nhật lại Level và MaxXP cho vòng lặp tiếp theo
            currentLevel++;
            maxXp = Double.parseDouble(Calculator.calculator(data.getMaxXpFormula().replace("<level>", String.valueOf(currentLevel)), 2));
        }

        s.setXp(petId, newXp);

        // CHỈNH SỬA: Sử dụng messages.yml cho Actionbar
        String xpMsg = getMsg("pet.level.xp_actionbar")
                .replace("<amount>", String.format("%.1f", amount))
                .replace("<current_xp>", String.format("%.1f", newXp))
                .replace("<max_xp>", String.format("%.1f", maxXp));

        p.sendActionBar(ColorUtils.parse(xpMsg));
    }
}