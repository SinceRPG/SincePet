package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.utils.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

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

        Map<String, PetData.PetStatData> stats = new LinkedHashMap<>();
        ConfigurationSection statsSection = section.getConfigurationSection(path + "stats");
        if (statsSection != null) {
            for (String statKey : statsSection.getKeys(false)) {
                if (statsSection.isConfigurationSection(statKey)) {
                    ConfigurationSection s = statsSection.getConfigurationSection(statKey);
                    double base = s.getDouble("base", 0.0);
                    Double max = s.contains("max-value") ? s.getDouble("max-value") : null;
                    String f = s.getString("formula", "0");
                    stats.put(statKey.toUpperCase(Locale.ROOT), new PetData.PetStatData(base, max, f));
                } else {
                    stats.put(statKey.toUpperCase(Locale.ROOT), new PetData.PetStatData(0.0, null, statsSection.getString(statKey)));
                }
            }
        } else {
            String stat = section.getString(path + "stat");
            String formula = section.getString(path + "formula");
            if (stat != null && formula != null) {
                stats.put(stat.toUpperCase(Locale.ROOT), new PetData.PetStatData(0.0, null, formula));
            }
        }

        double range = section.getDouble(path + "attack.range", 0);
        double cooldown = section.getDouble(path + "attack.cooldown", 2.0);
        String dmgFormula = section.getString(path + "attack.damage_formula", "0");
        double inheritance = section.getDouble(path + "attack.inheritance", 1.0);
        String attackParticle = section.getString(path + "attack.particle");

        boolean rideable = section.getBoolean(path + "ride.enabled", false);
        boolean canFly = section.getBoolean(path + "ride.can_fly", false);
        String maxXpFormula = section.getString(path + "max_xp_formula", "100 * <level>");

        List<PetSkill> loadedSkills = loadSkills(section, path);
        List<String> mmocoreClasses = section.getStringList(path + "mmocore_classes").stream().map(c -> c.toUpperCase(Locale.ROOT)).toList();
        
        ConfigurationSection pointsSec = section.getConfigurationSection(path + "upgrading_points");
        PetData.UpgradingPoints upgradingPoints;
        if (pointsSec != null) {
            upgradingPoints = new PetData.UpgradingPoints(
                pointsSec.getInt("levels_per_point", 5),
                pointsSec.getInt("points_per_interval", 1),
                pointsSec.getInt("max_points", 24)
            );
        } else {
            // Default if missing
            upgradingPoints = new PetData.UpgradingPoints(5, 1, 24);
        }

        pets.put(key, new PetData(key, name, texture, stats, range, cooldown, dmgFormula, inheritance, attackParticle, rideable, canFly, maxXpFormula, loadUpgrades(section.getConfigurationSection(path + "upgrades")), loadedSkills, mmocoreClasses, upgradingPoints));
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
            String name = section.getString(path + "name", "");
            List<String> lore = section.getStringList(path + "lore");
            skills.add(new PetSkill(
                    key,
                    type,
                    name,
                    lore,
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
            
            Map<String, PetData.PetStatData> stats = new LinkedHashMap<>();
            ConfigurationSection statsSec = section.getConfigurationSection(path + "stats");
            if (statsSec != null) {
                for (String statKey : statsSec.getKeys(false)) {
                    if (statsSec.isConfigurationSection(statKey)) {
                        ConfigurationSection s = statsSec.getConfigurationSection(statKey);
                        double base = s.getDouble("base", 0.0);
                        Double max = s.contains("max-value") ? s.getDouble("max-value") : null;
                        String f = s.getString("formula", "0");
                        stats.put(statKey.toUpperCase(Locale.ROOT), new PetData.PetStatData(base, max, f));
                    } else {
                        stats.put(statKey.toUpperCase(Locale.ROOT), new PetData.PetStatData(0.0, null, statsSec.getString(statKey)));
                    }
                }
            } else {
                String legacyFormula = section.getString(path + "stat_bonus_formula");
                if (legacyFormula != null) {
                    stats.put("LEGACY_ALL", new PetData.PetStatData(0.0, null, legacyFormula));
                }
            }
            
            upgrades.add(new PetUpgrade(
                    key,
                    section.getString(path + "name", key),
                    parseMaterial(section.getString(path + "material", "NETHER_STAR"), Material.NETHER_STAR),
                    section.getInt(path + "slot", upgrades.size()),
                    section.getInt(path + "max_level", 10),
                    section.getInt(path + "cost", 1),
                    stats,
                    section.getString(path + "damage_bonus_formula", "0"),
                    section.getString(path + "requirement.papi", section.getString(path + "requirement.upgrading_points", "")),
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
