package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.utils.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetConfig {
    private final SincePet plugin;
    private final Map<String, PetData> pets = new HashMap<>();

    public PetConfig(SincePet plugin) {
        this.plugin = plugin;
        loadPets();
    }

    public void loadPets() {
        pets.clear();
        ConfigUtils config = new ConfigUtils(plugin, "pets.yml");
        ConfigurationSection section = config.getConfig().getConfigurationSection("pets");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = key + ".";

            // General pet metadata.
            String name = section.getString(path + "name");
            String texture = section.getString(path + "texture");
            String stat = section.getString(path + "stat");
            String formula = section.getString(path + "formula");

            // Attack configuration.
            double range = section.getDouble(path + "attack.range", 0);
            double cooldown = section.getDouble(path + "attack.cooldown", 2.0);
            String dmgFormula = section.getString(path + "attack.damage_formula", "0");
            double inheritance = section.getDouble(path + "attack.inheritance", 1.0);
            String attackParticle = section.getString(path + "attack.particle");

            // Ride configuration.
            boolean rideable = section.getBoolean(path + "ride.enabled", false);
            boolean canFly = section.getBoolean(path + "ride.can_fly", false);

            // XP progression formula.
            String maxXpFormula = section.getString(path + "max_xp_formula", "100 * <level>");

            pets.put(key, new PetData(key, name, texture, stat, formula, range, cooldown, dmgFormula, inheritance, attackParticle, rideable, canFly, maxXpFormula, loadUpgrades(section.getConfigurationSection(path + "upgrades"))));
        }
    }

    private List<PetUpgrade> loadUpgrades(ConfigurationSection section) {
        List<PetUpgrade> upgrades = new ArrayList<>();
        if (section == null) return upgrades;
        for (String key : section.getKeys(false)) {
            String path = key + ".";
            upgrades.add(new PetUpgrade(
                    key,
                    section.getString(path + "name", key),
                    parseMaterial(section.getString(path + "material", "NETHER_STAR"), Material.NETHER_STAR),
                    section.getInt(path + "slot", upgrades.size()),
                    section.getInt(path + "max_level", 10),
                    section.getString(path + "stat_bonus_formula", "0"),
                    section.getString(path + "damage_bonus_formula", "0"),
                    section.getString(path + "requirement.papi", ""),
                    section.getString(path + "requirement.compare", ">="),
                    section.getString(path + "requirement.value", "0"),
                    section.getString(path + "requirement.display", ""),
                    section.getStringList(path + "commands")
            ));
        }
        return upgrades;
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public PetData getPet(String id) {
        return pets.get(id);
    }

    public Collection<PetData> getAllPets() {
        return pets.values();
    }

    public Map<String, PetData> getPets() {
        return pets;
    }
}
