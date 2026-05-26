package net.danh.sincePet.pets;

import org.bukkit.Material;

import java.util.List;

public record PetUpgrade(
        String id,
        String name,
        Material material,
        int slot,
        int maxLevel,
        String statFormula,
        String damageFormula,
        String papi,
        String compare,
        String value,
        String requirementDisplay,
        List<String> commands
) {
}
