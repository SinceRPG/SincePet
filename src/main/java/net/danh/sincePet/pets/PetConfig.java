package net.danh.sincePet.data;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.pets.PetData;
import net.danh.sincePet.utils.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
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

            // Thông tin chung
            String name = section.getString(path + "name");
            String texture = section.getString(path + "texture");
            String stat = section.getString(path + "stat");
            String formula = section.getString(path + "formula");

            // Đọc từ mục 'attack'
            double range = section.getDouble(path + "attack.range", 0);
            double cooldown = section.getDouble(path + "attack.cooldown", 2.0);
            String dmgFormula = section.getString(path + "attack.damage_formula", "0");
            double inheritance = section.getDouble(path + "attack.inheritance", 1.0);

            // Đọc từ mục 'ride'
            boolean rideable = section.getBoolean(path + "ride.enabled", false);
            boolean canFly = section.getBoolean(path + "ride.can_fly", false);

            // XP (Mới thêm)
            String maxXpFormula = section.getString(path + "max_xp_formula", "100 * <level>");

            pets.put(key, new PetData(key, name, texture, stat, formula, range, cooldown, dmgFormula, inheritance, rideable, canFly, maxXpFormula));
        }
    }

    public PetData getPet(String id) {
        return pets.get(id);
    }

    public Collection<PetData> getAllPets() {
        return pets.values();
    }
}