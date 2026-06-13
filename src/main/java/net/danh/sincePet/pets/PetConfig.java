package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.utils.ConfigUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PetConfig {
    private final SincePet plugin;
    private final Map<String, PetData> pets = new LinkedHashMap<>();

    public PetConfig(SincePet plugin) {
        this.plugin = plugin;
        loadPets();
    }

    public void loadPets() {
        pets.clear();
        ConfigUtils config = new ConfigUtils(plugin, "pets.yml");
        if (loadPetFolder(config)) return;

        ConfigurationSection section = config.getConfig().getConfigurationSection("pets");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            loadPet(key, section, key + ".");
        }
    }

    private boolean loadPetFolder(ConfigUtils indexConfig) {
        File folder = new File(plugin.getDataFolder(), "pets");
        if (!folder.exists() && !indexConfig.getConfig().getStringList("pets").isEmpty()) {
            folder.mkdirs();
            for (String id : indexConfig.getConfig().getStringList("pets")) {
                String resource = "pets/" + id.toLowerCase(Locale.ROOT) + ".yml";
                if (plugin.getResource(resource) != null) plugin.saveResource(resource, false);
            }
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null || files.length == 0) return false;

        Map<String, File> byId = new LinkedHashMap<>();
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toUpperCase(Locale.ROOT);
            byId.put(id, file);
        }

        for (String configuredId : indexConfig.getConfig().getStringList("pets")) {
            File file = byId.remove(configuredId.toUpperCase(Locale.ROOT));
            if (file != null) loadPetFile(file);
        }

        byId.values().stream()
                .sorted(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(this::loadPetFile);
        return true;
    }

    private void loadPetFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String fallbackId = file.getName().substring(0, file.getName().length() - 4).toUpperCase(Locale.ROOT);
        String id = config.getString("id", fallbackId);
        loadPet(id, config, "");
    }

    private void loadPet(String key, ConfigurationSection section, String path) {
        String name = section.getString(path + "name");
        String texture = section.getString(path + "texture");
        String stat = section.getString(path + "stat");
        String formula = section.getString(path + "formula");

        double range = section.getDouble(path + "attack.range", 0);
        double cooldown = section.getDouble(path + "attack.cooldown", 2.0);
        String dmgFormula = section.getString(path + "attack.damage_formula", "0");
        double inheritance = section.getDouble(path + "attack.inheritance", 1.0);
        String attackParticle = section.getString(path + "attack.particle");

        boolean rideable = section.getBoolean(path + "ride.enabled", false);
        boolean canFly = section.getBoolean(path + "ride.can_fly", false);
        String maxXpFormula = section.getString(path + "max_xp_formula", "100 * <level>");

        List<PetSkill> loadedSkills = loadSkills(section, path);
        pets.put(key, new PetData(key, name, texture, stat, formula, range, cooldown, dmgFormula, inheritance, attackParticle, rideable, canFly, maxXpFormula, loadUpgrades(section.getConfigurationSection(path + "upgrades")), loadedSkills));
        debug("[DEBUG] Loaded pet '" + key + "' with " + loadedSkills.size() + " skills.");
    }

    private List<PetSkill> loadSkills(ConfigurationSection petSection, String petPath) {
        List<PetSkill> skills = new ArrayList<>();
        ConfigurationSection section = petSection.getConfigurationSection(petPath + "abilities");
        if (section == null) section = petSection.getConfigurationSection(petPath + "skills");
        if (section == null) return skills;
        loadSkillGroup(section.getConfigurationSection("passive"), "passive", skills);
        loadSkillGroup(section.getConfigurationSection("active"), "active", skills);
        loadSkillGroup(section.getConfigurationSection("passive-skills"), "passive", skills);
        loadSkillGroup(section.getConfigurationSection("active-skills"), "active", skills);
        return skills;
    }

    private void loadSkillGroup(ConfigurationSection section, String type, List<PetSkill> skills) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path = key + ".";
            String skillId = parseSkillId(section.getString(path + "mythicmobs_skill", section.getString(path + "source", "")));
            skills.add(new PetSkill(
                    key,
                    type,
                    section.getBoolean(path + "enabled", true),
                    skillId,
                    parseTriggers(section, path + "triggers", type),
                    Math.max(0, section.getDouble(path + "cooldown", 0))
            ));
        }
    }

    private String parseSkillId(String source) {
        if (source == null) return "";
        String value = source.trim();
        int delimiter = value.indexOf(':');
        if (delimiter >= 0) value = value.substring(delimiter + 1).trim();
        return value;
    }

    private List<String> parseTriggers(ConfigurationSection section, String path, String type) {
        List<String> triggers = section.getStringList(path);
        if (triggers.isEmpty()) {
            String single = section.getString(path, "");
            if (!single.isBlank()) triggers = List.of(single);
        }
        if (triggers.isEmpty() && "active".equals(type)) triggers = List.of("ACTIVE");
        return triggers.stream()
                .map(trigger -> trigger.toUpperCase(Locale.ROOT).trim())
                .filter(trigger -> !trigger.isBlank())
                .toList();
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

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(message);
        }
    }
}
