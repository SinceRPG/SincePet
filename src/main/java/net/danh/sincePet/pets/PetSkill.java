package net.danh.sincePet.pets;

import java.util.List;

public record PetSkill(String id, String type, String name, List<String> lore, boolean enabled, String skillId,
                       List<String> triggers, double cooldown) {
    public boolean matchesTrigger(String trigger) {
        if (!enabled || trigger == null) return false;
        for (String configured : triggers) {
            if (configured.equalsIgnoreCase(trigger)) return true;
            if (configured.equalsIgnoreCase("DAMAGED") && trigger.equalsIgnoreCase("OWNER_DAMAGED")) return true;
            if (configured.equalsIgnoreCase("ATTACK") && trigger.equalsIgnoreCase("PET_ATTACK")) return true;
        }
        return false;
    }
}
