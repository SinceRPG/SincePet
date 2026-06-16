package net.danh.sincePet.pets;

import io.lumine.mythic.bukkit.MythicBukkit;
import net.danh.sincePet.SincePet;
import net.danh.sincePet.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PetAbilityManager {
    private final SincePet plugin;
    private final PetManager petManager;
    private final boolean hasMythicMobs;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSneak = new ConcurrentHashMap<>();

    public PetAbilityManager(SincePet plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.hasMythicMobs = Bukkit.getPluginManager().isPluginEnabled("MythicMobs");
    }

    public void clear() {
        cooldowns.clear();
        lastSneak.clear();
    }

    public void clear(Player p) {
        cooldowns.remove(p.getUniqueId());
        lastSneak.remove(p.getUniqueId());
    }

    public boolean useCommandAbility(Player p, String abilityId) {
        return useActiveAbility(p, abilityId, "ACTIVE", true);
    }

    public void triggerActive(Player p, String trigger) {
        useActiveAbility(p, "", trigger, false);
    }

    public void triggerDoubleSneak(Player p) {
        long now = System.currentTimeMillis();
        long previous = lastSneak.getOrDefault(p.getUniqueId(), 0L);
        lastSneak.put(p.getUniqueId(), now);
        if (now - previous <= 500L) {
            triggerActive(p, "DOUBLE_SNEAK");
        }
    }

    public void triggerPassive(Player p, String trigger, Entity target) {
        if (p == null || trigger == null) return;
        PetData data = petManager.getActivePetData(p);
        if (data == null || data.skills().isEmpty()) return;

        String normalizedTrigger = trigger.toUpperCase();
        // Trace log to ensure trigger is firing
        if (normalizedTrigger.equals("RIGHT_CLICK_AIR") || normalizedTrigger.equals("SNEAK")) {
            debug("[DEBUG] Trace: Trigger '" + normalizedTrigger + "' received for pet " + data.id());
        }
        for (PetSkill ability : data.skills()) {
            if (!ability.matchesTrigger(normalizedTrigger) || ability.skillId().isBlank()) continue;
            if (!canUse(p, data, ability, false)) {
                continue;
            }
            execute(p, target, normalizedTrigger, ability.skillId());
            markUsed(p, data, ability);
        }
    }

    public List<String> getActiveAbilityIds(Player p) {
        PetData data = petManager.getActivePetData(p);
        if (data == null) return List.of();
        return data.skills().stream()
                .filter(PetSkill::enabled)
                .filter(ability -> ability.type().equalsIgnoreCase("active"))
                .filter(ability -> !ability.skillId().isBlank())
                .map(PetSkill::id)
                .toList();
    }

    private boolean useActiveAbility(Player p, String abilityId, String trigger, boolean notify) {
        if (!p.hasPermission("sincepet.skill")) {
            if (notify) p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.no_permission")));
            return false;
        }

        PetData data = petManager.getActivePetData(p);
        if (data == null) {
            if (notify) p.sendMessage(getComp("pet.command.skill_no_active"));
            return false;
        }

        List<PetSkill> activeAbilities = data.skills().stream()
                .filter(PetSkill::enabled)
                .filter(ability -> ability.type().equalsIgnoreCase("active"))
                .filter(ability -> !ability.skillId().isBlank())
                .filter(ability -> ability.matchesTrigger(trigger))
                .toList();

        if (activeAbilities.isEmpty()) {
            if (notify) p.sendMessage(getComp("pet.command.skill_none"));
            return false;
        }

        if (!notify && (abilityId == null || abilityId.isBlank())) {
            boolean used = false;
            for (PetSkill ability : activeAbilities) {
                debug("[DEBUG] Matched active trigger '" + trigger + "' for pet '" + data.id() + "' (Skill: " + ability.skillId() + ")");
                if (!canUse(p, data, ability, false)) continue;
                execute(p, findTarget(p), trigger, ability.skillId());
                markUsed(p, data, ability);
                used = true;
            }
            return used;
        }

        PetSkill selected = selectAbility(activeAbilities, abilityId);
        if (selected == null) {
            if (notify) {
                p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.skill_not_found").replace("<skill>", abilityId)));
            }
            return false;
        }

        debug("[DEBUG] Selected active skill '" + selected.skillId() + "' for trigger '" + trigger + "'");
        if (!canUse(p, data, selected, notify)) return false;
        execute(p, findTarget(p), trigger, selected.skillId());
        markUsed(p, data, selected);
        if (notify)
            p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.skill_cast").replace("<skill>", selected.id())));
        return true;
    }

    private PetSkill selectAbility(List<PetSkill> abilities, String abilityId) {
        if (abilityId == null || abilityId.isBlank()) return abilities.getFirst();
        for (PetSkill ability : abilities) {
            if (ability.id().equalsIgnoreCase(abilityId)) return ability;
        }
        return null;
    }

    private boolean canUse(Player p, PetData data, PetSkill ability, boolean notify) {
        if (!hasMythicMobs) {
            if (notify) p.sendMessage(getComp("pet.command.skill_unavailable"));
            return false;
        }
        if (ability.cooldown() <= 0) return true;

        long readyAt = cooldowns
                .getOrDefault(p.getUniqueId(), Map.of())
                .getOrDefault(getCooldownKey(data, ability), 0L);
        long now = System.currentTimeMillis();
        if (readyAt <= now) return true;
        if (notify) {
            double seconds = (readyAt - now) / 1000.0;
            p.sendMessage(ColorUtils.parseWithPrefix(getMsg("pet.command.skill_cooldown")
                    .replace("<skill>", ability.id())
                    .replace("<seconds>", String.format("%.1f", seconds))));
        }
        return false;
    }

    private void markUsed(Player p, PetData data, PetSkill ability) {
        if (ability.cooldown() <= 0) return;
        double finalCooldown = Math.max(0.1, ability.cooldown() - plugin.getPetManager().getUpgradeBonus(p, data, "skill_cooldown", null));
        cooldowns
                .computeIfAbsent(p.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(getCooldownKey(data, ability), System.currentTimeMillis() + (long) (finalCooldown * 1000));
    }

    private String getCooldownKey(PetData data, PetSkill ability) {
        return data.id() + ":" + ability.id();
    }

    private void execute(Player p, Entity target, String trigger, String skillId) {
        try {
            var skillManager = MythicBukkit.inst().getSkillManager();
            var skill = skillManager.getSkill(skillId).orElse(null);
            if (skill == null) {
                debug("[DEBUG] [SincePet] ❌ MythicMobs Skill '" + skillId + "' NOT FOUND in MythicMobs config! Please check your MythicMobs Skills folder.");
                return;
            }

            Entity triggerEntity = target == null ? p : target;
            Entity petEntity = plugin.getPetManager().getActivePetEntity(p);
            Location originLoc = petEntity != null ? petEntity.getLocation() : p.getLocation();

            debug("");
            debug("============ [SincePet Skill Execution] ============");
            debug(" 🐾 Pet Owner: " + p.getName());
            debug(" ⚔️ MythicMobs Skill: " + skillId);
            debug(" 📌 Trigger Cause: " + trigger);
            debug(" 🎯 Target Entity: " + (triggerEntity != null ? triggerEntity.getType() + " (" + triggerEntity.getName() + ")" : "None"));
            debug(" 📍 Origin Location: " + (petEntity != null ? "Pet's Location" : "Player's Location") + " [X:" + originLoc.getBlockX() + ", Y:" + originLoc.getBlockY() + ", Z:" + originLoc.getBlockZ() + "]");

            boolean success = MythicBukkit.inst().getAPIHelper().castSkill(p, skillId, triggerEntity, originLoc, List.of(triggerEntity), null, 1.0f);

            if (success) {
                debug(" ✅ RESULT: SUCCESS (MythicMobs casted the skill)");
            } else {
                debug(" ⚠️ RESULT: FAILED (MythicMobs refused to cast. Check MythicMobs conditions/targeters!)");
            }
            debug("====================================================");
            debug("");
        } catch (Exception ex) {
            plugin.getLogger().warning("[DEBUG] [SincePet] ❌ FATAL ERROR executing pet ability '" + skillId + "' for " + p.getName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Entity findTarget(Player p) {
        var result = p.getWorld().rayTraceEntities(
                p.getEyeLocation(),
                p.getEyeLocation().getDirection(),
                24,
                0.25,
                entity -> entity instanceof LivingEntity && !entity.getUniqueId().equals(p.getUniqueId())
        );
        return result == null || result.getHitEntity() == null ? p : result.getHitEntity();
    }

    private String getMsg(String path) {
        return plugin.getPetMessagesFile().getString(path);
    }

    private net.kyori.adventure.text.Component getComp(String path) {
        return ColorUtils.parseWithPrefix(getMsg(path));
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(message);
        }
    }
}
