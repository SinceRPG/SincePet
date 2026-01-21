package net.danh.sincePet.data;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.pets.PetData;
import net.danh.sincePet.utils.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PetConfig {
    private final Map<String, PetData> loadedPets = new HashMap<>();
    private final ConfigUtils petFile;

    public PetConfig(SincePet plugin) {
        this.petFile = new ConfigUtils(plugin, "pets.yml");
        loadPets();
    }

    public void loadPets() {
        loadedPets.clear();
        petFile.reload();
        ConfigurationSection sec = petFile.getConfig().getConfigurationSection("pets");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                String p = "pets." + key + ".";
                String n = petFile.getString(p + "name");
                String t = petFile.getString(p + "texture");
                String s = petFile.getString(p + "stat");
                String f = petFile.getString(p + "formula");

                double r = petFile.getConfig().getDouble(p + "attack.range", 0);
                double c = petFile.getConfig().getDouble(p + "attack.cooldown", 2.0);
                String df = petFile.getString(p + "attack.damage_formula", "0");
                double inh = petFile.getConfig().getDouble(p + "attack.inheritance", 1.0);

                boolean rideable = petFile.getConfig().getBoolean(p + "ride.enabled", true);
                boolean canFly = petFile.getConfig().getBoolean(p + "ride.can_fly", true);

                loadedPets.put(key, new PetData(key, n, t, s, f, r, c, df, inh, rideable, canFly));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PetData getPet(String id) {
        return loadedPets.get(id);
    }

    public Collection<PetData> getAllPets() {
        return loadedPets.values();
    }
}