package net.danh.sincePet.pets;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.danh.sincePet.SincePet;
import net.danh.sincePet.data.PlayerDataHandler;
import net.danh.sincePet.utils.Calculator;
import net.danh.sincePet.utils.ColorUtils;
import net.danh.sincePet.utils.ConfigUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * PetGUI handles all the interactive inventory menus for SincePet.
 * This class uses standard Bukkit Inventory APIs to display the main collection,
 * pet detail screens, upgrades, and pet settings.
 */
public class PetGUI implements InventoryHolder {
    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;
    private final SincePet plugin;
    private final int page;
    private final View view;
    private Inventory inv;
    public PetGUI(SincePet plugin, int page) {
        this(plugin, page, View.COLLECTION);
    }

    public PetGUI(SincePet plugin, int page, View view) {
        this.plugin = plugin;
        this.page = page;
        this.view = view;
    }

    public int getPage() {
        return page;
    }

    public View getView() {
        return view;
    }

    /**
     * Opens the designated GUI view for the specified player.
     * Routes to the correct method based on the current View state.
     *
     * @param p The player opening the GUI.
     */
    public void open(Player p) {
        if (view == View.DETAIL) {
            openDetail(p);
            return;
        }
        if (view == View.SETTINGS) {
            openSettings(p);
            return;
        }
        if (view == View.UPGRADES) {
            openUpgrades(p);
            return;
        }
        ConfigUtils gui = plugin.getPetGuiFile();
        ConfigUtils messages = plugin.getPetMessagesFile();

        String titleRaw = messages.getString("pet.gui.title", "Pet Menu <page>").replace("<page>", String.valueOf(page));
        this.inv = Bukkit.createInventory(this, INVENTORY_SIZE, ColorUtils.parse(titleRaw));

        List<PetData> viewablePets = getViewablePets(p);
        setBorder(p, gui);
        setPetIcons(p, gui, messages, viewablePets);
        setNavigationButtons(p, gui, viewablePets.size());

        p.openInventory(inv);
    }

    /**
     * Opens the detailed view for the player's currently active pet.
     * Displays current experience, stats, and provides options to mount or open settings/upgrades.
     *
     * @param p The player.
     */
    private void openDetail(Player p) {
        ConfigUtils gui = plugin.getPetGuiFile();
        ConfigUtils messages = plugin.getPetMessagesFile();
        PetData data = plugin.getPetManager().getActivePetData(p);
        String petName = data == null ? messages.getString("pet.gui.no_active_pet", "No Active Pet") : data.name();
        
        double currentXp = 0;
        double maxReq = 0;
        if (data != null) {
            PlayerDataHandler.PlayerSession session = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
            if (session != null) {
                currentXp = session.getXp(data.id());
                int level = session.getLevel(data.id());
                try {
                    maxReq = Double.parseDouble(Calculator.calculator(data.maxXpFormula().replaceAll("(?i)<?\\blevel\\b>?", String.valueOf(level)), 2));
                } catch (Exception ignored) {}
            }
        }
        Placeholder expPl = new Placeholder("<current_exp>", formatNumber(currentXp));
        Placeholder maxExpPl = new Placeholder("<max_exp>", formatNumber(maxReq));

        this.inv = Bukkit.createInventory(this, 27, ColorUtils.parse(messages.getString("pet.gui.detail_title", "<black><bold>Pet Detail: <name>").replace("<name>", petName)));
        fill(p, inv, gui);
        setButton(p, gui, "detail.buttons.back", 18, inv);
        setButton(p, gui, "detail.buttons.ride", 11, inv);
        setButton(p, gui, "detail.buttons.settings", 13, inv);
        setButton(p, gui, "detail.buttons.upgrades", 15, inv);

        org.bukkit.configuration.ConfigurationSection expSection = gui.getConfig().getConfigurationSection("pet-experience");
        if (expSection != null) {
            for (String key : expSection.getKeys(false)) {
                String path = "pet-experience." + key;
                int slot = expSection.getInt(key + ".slot", -1);
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, createConfiguredItem(p, gui, path, Material.EXPERIENCE_BOTTLE, "XP", List.of(), expPl, maxExpPl));
                }
            }
        }

        p.openInventory(inv);
    }

    private void openSettings(Player p) {
        ConfigUtils gui = plugin.getPetGuiFile();
        ConfigUtils messages = plugin.getPetMessagesFile();
        this.inv = Bukkit.createInventory(this, 27, ColorUtils.parse(messages.getString("pet.gui.settings_title", "<black><bold>Pet Settings")));
        fill(p, inv, gui);
        setButton(p, gui, "detail.buttons.back_detail", 18, inv);
        setSettingButton(p, "show_name", "settings.show_name", 10);
        setSettingButton(p, "auto_attack", "settings.auto_attack", 12);
        setSettingButton(p, "stat_buff", "settings.stat_buff", 14);
        p.openInventory(inv);
    }

    private void openUpgrades(Player p) {
        ConfigUtils gui = plugin.getPetGuiFile();
        ConfigUtils messages = plugin.getPetMessagesFile();
        PetData data = plugin.getPetManager().getActivePetData(p);
        this.inv = Bukkit.createInventory(this, 54, ColorUtils.parse(messages.getString("pet.gui.upgrades_title", "<black><bold>Pet Upgrades")));
        setBorder(p, gui);
        setButton(p, gui, "detail.buttons.back_detail", 49, inv);
        if (data != null) {
            for (PetUpgrade upgrade : data.upgrades()) {
                int level = plugin.getPetManager().getUpgradeLevel(p, data, upgrade);
                inv.setItem(Math.max(0, Math.min(44, upgrade.slot())), createUpgradeIcon(p, gui, data, upgrade, level));
            }
        }
        p.openInventory(inv);
    }

    private List<PetData> getViewablePets(Player p) {
        List<PetData> viewablePets = new ArrayList<>();
        for (PetData pet : plugin.getPetManager().getPetConfig().getAllPets()) {
            if (plugin.getPetManager().canAccessPet(p, pet)) {
                viewablePets.add(pet);
            }
        }
        return viewablePets;
    }

    private void setBorder(Player p, ConfigUtils gui) {
        ItemStack borderItem = createConfiguredItem(p, gui, "border", Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 45; i < INVENTORY_SIZE; i++) inv.setItem(i, borderItem);
    }

    private void setPetIcons(Player p, ConfigUtils gui, ConfigUtils messages, List<PetData> viewablePets) {
        int totalPets = viewablePets.size();
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalPets);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            PetData data = viewablePets.get(i);
            int level = plugin.getPetManager().getPetLevel(p, data.id());
            inv.setItem(slot++, createPetIcon(p, gui, messages, data, level));
        }
    }

    /**
     * Constructs the visual ItemStack for a pet in the main collection menu.
     * Applies dynamic placeholders such as current level, stats from the new Map structure,
     * and custom item textures.
     *
     * @param p        The player.
     * @param gui      GUI configuration.
     * @param messages Messages configuration.
     * @param data     The pet data to format.
     * @param level    The pet's current level.
     * @return Formatted ItemStack representing the pet.
     */
    private ItemStack createPetIcon(Player p, ConfigUtils gui, ConfigUtils messages, PetData data, int level) {
        List<String> statsLore = new ArrayList<>();
        String statFormat = gui.getString("collection.pet_item.stats_format", " <gray>Stat Bonus: <green>+<value> <stat>");
        for (Map.Entry<String, PetData.PetStatData> entry : data.stats().entrySet()) {
            double statVal = entry.getValue().base();
            try {
                statVal += Double.parseDouble(Calculator.calculator(entry.getValue().formula().replaceAll("(?i)<?\\blevel\\b>?", String.valueOf(level)), 2));
            } catch (Exception ignored) {}
            if (entry.getValue().maxValue() != null && statVal > entry.getValue().maxValue()) {
                statVal = entry.getValue().maxValue();
            }
            String valDisplay = (statVal % 1 == 0) ? String.valueOf((int) statVal) : String.format("%.2f", statVal);
            statsLore.add(statFormat.replace("<stat>", getDisplayValue(gui, "stats", entry.getKey())).replace("<value>", valDisplay));
        }
        String statsDisplay = statsLore.isEmpty() ? "" : String.join("\n", statsLore);

        String inheritanceStr = String.valueOf((int) (data.inheritance() * 100));
        String statusLine = getConfigString(gui, messages, "collection.pet_item.status.unlocked", "pet.gui.status_unlocked", "<green>Unlocked");
        String itemPath = gui.getConfig().isConfigurationSection("collection.items." + data.id()) ? "collection.items." + data.id() : "collection.pet_item";
        Material fallbackMaterial = data.texture() == null || data.texture().isBlank() ? Material.PAPER : Material.PLAYER_HEAD;
        List<String> fallbackLore = messages.getStringList("pet.gui.item_lore");
        
        double currentXp = 0;
        double maxReq = 0;
        PlayerDataHandler.PlayerSession session = plugin.getPlayerDataHandler().getSession(p.getUniqueId());
        if (session != null) {
            currentXp = session.getXp(data.id());
            try {
                maxReq = Double.parseDouble(Calculator.calculator(data.maxXpFormula().replaceAll("(?i)<?\\blevel\\b>?", String.valueOf(level)), 2));
            } catch (Exception ignored) {}
        }

        List<Placeholder> placeholdersList = new ArrayList<>(Arrays.asList(
                new Placeholder("<pet>", data.name()),
                new Placeholder("<name>", data.name()),
                new Placeholder("<current_exp>", formatNumber(currentXp)),
                new Placeholder("<max_exp>", formatNumber(maxReq)),
                new Placeholder("<pet_display>", getDisplayValue(gui, "pets", data.id())),
                new Placeholder("<pet_id_display>", getDisplayValue(gui, "pets", data.id())),
                new Placeholder("<id>", data.id()),
                new Placeholder("<pet_id>", data.id()),
                new Placeholder("<level>", String.valueOf(level)),
                new Placeholder("<upgrading_points>", String.valueOf(plugin.getPetManager().getAvailableUpgradePoints(p, data))),
                new Placeholder("<max_upgrading_points>", String.valueOf(plugin.getPetManager().getTotalEarnedUpgradePoints(p, data))),
                new Placeholder("<stats>", statsDisplay),
                new Placeholder("<formula>", ""), // Kept for backwards compat but empty
                new Placeholder("<inheritance>", inheritanceStr),
                new Placeholder("<skills>", getSkillSummary(gui, data, "all")),
                new Placeholder("<active_skills>", getSkillSummary(gui, data, "active")),
                new Placeholder("<passive_skills>", getSkillSummary(gui, data, "passive")),
                new Placeholder("<status>", statusLine),
                new Placeholder("<texture>", data.texture() == null ? "" : data.texture())
        ));
        placeholdersList.addAll(getSkillPlaceholders(gui, data));
        return createConfiguredItem(gui, itemPath, fallbackMaterial, messages.getString("pet.gui.item_name", "<name>"), fallbackLore, data.texture(), placeholdersList.toArray(new Placeholder[0]));
    }

    private void setNavigationButtons(Player p, ConfigUtils gui, int totalPets) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPets / ITEMS_PER_PAGE));
        setButton(p, gui, "buttons.detail", 48, inv);
        setButton(p, gui, "buttons.remove", 49, inv);
        if (page > 1) setButton(p, gui, "buttons.previous", 45, inv);
        if (page < totalPages) setButton(p, gui, "buttons.next", 53, inv);
    }

    private void setSettingButton(Player p, String settingId, String configPath, int defaultSlot) {
        ConfigUtils gui = plugin.getPetGuiFile();
        ConfigUtils messages = plugin.getPetMessagesFile();
        if (!shouldDisplay(p, gui, configPath)) return;
        PetData data = plugin.getPetManager().getActivePetData(p);
        boolean enabled = data != null && plugin.getPetManager().getPetSetting(p, data.id(), settingId, true);
        int slot = gui.getInt(configPath + ".slot", defaultSlot);
        ItemStack item = createConfiguredItem(p, gui, configPath, enabled ? Material.LIME_DYE : Material.GRAY_DYE, settingId, List.of(),
                new Placeholder("<state>", enabled ? messages.getString("pet.gui.enabled", "<green>ON") : messages.getString("pet.gui.disabled", "<red>OFF")));
        inv.setItem(slot, item);
    }

    /**
     * Constructs the upgrade icon based on the player's current upgrade level.
     * It checks if the upgrade is available, locked due to requirements, or maxed.
     *
     * @param p       The player viewing the upgrade.
     * @param gui     GUI configuration.
     * @param data    The pet data being upgraded.
     * @param upgrade The specific upgrade node.
     * @param level   The current upgrade level for this node.
     * @return Formatted ItemStack for the upgrade menu.
     */
    private ItemStack createUpgradeIcon(Player p, ConfigUtils gui, PetData data, PetUpgrade upgrade, int level) {
        boolean maxed = level >= upgrade.maxLevel();
        boolean requirementMet = plugin.getPetManager().checkUpgradeRequirement(p, data, upgrade);
        String state = maxed ? "maxed" : requirementMet ? "available" : "locked";
        String overridePath = "upgrade_items.items." + upgrade.id();
        String path = gui.getConfig().isConfigurationSection(overridePath) ? overridePath : "upgrade_items." + state;
        int nextLevel = Math.min(level + 1, upgrade.maxLevel());

        List<String> statsLore = new ArrayList<>();
        String statFormat = gui.getString(path + ".stats_format", "<gray><stat> Bonus: <green>+<stat_bonus> <dark_gray>-> <green>+<next_stat_bonus>");
        for (String statKey : upgrade.stats().keySet()) {
            double currentBonus = plugin.getPetManager().getUpgradeStatBonus(p, data, upgrade, level, statKey);
            double nextBonus = plugin.getPetManager().getUpgradeStatBonus(p, data, upgrade, nextLevel, statKey);
            
            String currDisp = formatNumber(currentBonus);
            String nextDisp = formatNumber(nextBonus);
            
            String statName = "LEGACY_ALL".equals(statKey) ? "All Stats" : getDisplayValue(gui, "stats", statKey);
            statsLore.add(statFormat
                .replace("<stat>", statName)
                .replace("<stat_bonus>", currDisp)
                .replace("<next_stat_bonus>", nextDisp));
        }
        String upgradeStatsDisplay = statsLore.isEmpty() ? "" : String.join("\n", statsLore);

        List<Placeholder> placeholdersList = new ArrayList<>(Arrays.asList(
                new Placeholder("<pet>", data.name()),
                new Placeholder("<pet_id>", data.id()),
                new Placeholder("<upgrade>", upgrade.name()),
                new Placeholder("<upgrade_id>", upgrade.id()),
                new Placeholder("<upgrading_points>", String.valueOf(plugin.getPetManager().getAvailableUpgradePoints(p, data))),
                new Placeholder("<max_upgrading_points>", String.valueOf(plugin.getPetManager().getTotalEarnedUpgradePoints(p, data))),
                new Placeholder("<level>", String.valueOf(level)),
                new Placeholder("<next_level>", String.valueOf(nextLevel)),
                new Placeholder("<max_level>", String.valueOf(upgrade.maxLevel())),
                new Placeholder("<papi>", upgrade.papi()),
                new Placeholder("<compare>", upgrade.compare()),
                new Placeholder("<value>", upgrade.value()),
                new Placeholder("<current_value>", plugin.getPetManager().getResolvedUpgradeRequirement(p, data, upgrade)),
                new Placeholder("<requirement>", getRequirementDisplay(p, data, upgrade)),
                new Placeholder("<raw_requirement>", upgrade.papi() + " " + upgrade.compare() + " " + upgrade.value()),
                new Placeholder("<state>", state),
                new Placeholder("<upgrade_stats>", upgradeStatsDisplay),
                new Placeholder("<damage_bonus>", formatNumber(plugin.getPetManager().getUpgradeDamageBonus(p, data, upgrade, level))),
                new Placeholder("<next_damage_bonus>", formatNumber(plugin.getPetManager().getUpgradeDamageBonus(p, data, upgrade, nextLevel))),
                new Placeholder("<commands>", String.join(", ", upgrade.commands()))
        ));
        placeholdersList.addAll(getSkillPlaceholders(gui, data));
        return createConfiguredItem(p, gui, path, upgrade.material(), upgrade.name(), List.of(), placeholdersList.toArray(new Placeholder[0]));
    }

    private void fill(Player p, Inventory inventory, ConfigUtils gui) {
        ItemStack borderItem = createConfiguredItem(p, gui, "border", Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, borderItem);
    }

    private void setButton(Player p, ConfigUtils config, String path, int defaultSlot, Inventory inv) {
        if (!shouldDisplay(p, config, path)) return;
        int slot = config.getInt(path + ".slot", defaultSlot);
        inv.setItem(slot, createConfiguredItem(p, config, path, Material.ARROW, "Button", List.of()));
    }

    private Material parseMaterial(String name, Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private void applyModelData(ItemMeta meta, int modelId) {
        if (modelId <= 0) return;
        try {
            CustomModelDataComponent modelComp = meta.getCustomModelDataComponent();
            modelComp.setFloats(List.of((float) modelId));
            meta.setCustomModelDataComponent(modelComp);
        } catch (NoSuchMethodError e) {
            meta.setCustomModelData(modelId);
        }
    }

    private ItemStack createConfiguredItem(Player p, ConfigUtils config, String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore, Placeholder... placeholders) {
        return createConfiguredItem(config, path, fallbackMaterial, fallbackName, fallbackLore, "", placeholders);
    }

    private ItemStack createConfiguredItem(ConfigUtils config, String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore, String fallbackSkullTexture, Placeholder... placeholders) {
        Material material = parseMaterial(config.getString(path + ".material", fallbackMaterial.name()), fallbackMaterial);
        int amount = Math.max(1, Math.min(99, config.getInt(path + ".amount", 1)));
        ItemStack item = new ItemStack(material, amount);

        String texture = replacePlaceholders(config.getString(path + ".skull_texture", ""), placeholders);
        boolean useFallbackTexture = config.getConfig().contains(path + ".use_pet_texture")
                ? config.getConfig().getBoolean(path + ".use_pet_texture")
                : fallbackSkullTexture != null && !fallbackSkullTexture.isBlank();
        if (texture.isBlank() && useFallbackTexture) {
            texture = fallbackSkullTexture == null ? "" : fallbackSkullTexture;
        }
        if (material == Material.PLAYER_HEAD && !texture.isBlank()) {
            item = createSkull(texture);
            item.setAmount(amount);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = replacePlaceholders(config.getString(path + ".name", fallbackName), placeholders);
        meta.displayName(ColorUtils.parse(name));

        List<String> loreRaw = config.getStringList(path + ".lore");
        if (loreRaw.isEmpty()) loreRaw = fallbackLore;
        if (!loreRaw.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreRaw) {
                String replaced = replacePlaceholders(line, false, placeholders);
                if (replaced.matches(".*<skill\\.(active|passive)\\.[^>]+>.*") ||
                        replaced.matches(".*<skill\\.description\\.(active|passive)\\.[^>]+>.*") ||
                        replaced.matches(".*<skill\\.trigger\\.(active|passive)\\.[^>]+>.*")) {
                    continue;
                }
                for (String splitLine : replaced.split("\\n")) {
                    lore.add(ColorUtils.parse(splitLine));
                }
            }
            meta.lore(lore);
        }

        applyModelData(meta, config.getInt(path + ".model_data", config.getInt(path + ".custom_model_data", 0)));
        meta.setUnbreakable(config.getBoolean(path + ".unbreakable"));
        for (String flag : config.getStringList(path + ".item_flags")) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (config.getBoolean(path + ".hide_attributes")) meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (config.getBoolean(path + ".hide_enchants")) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        if (config.getBoolean(path + ".hide_unbreakable")) meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        if (meta instanceof Damageable damageable) {
            int damage = config.getInt(path + ".damage", -1);
            if (damage >= 0) damageable.setDamage(damage);
        }

        for (String enchantLine : config.getStringList(path + ".enchants")) {
            applyEnchant(meta, enchantLine);
        }
        if (config.getBoolean(path + ".glow")) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        String pdcKey = replacePlaceholders(config.getString(path + ".data.key", ""), placeholders);
        String pdcValue = replacePlaceholders(config.getString(path + ".data.value", ""), placeholders);
        if (!pdcKey.isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(pdcKey.contains(":") ? pdcKey : "sincepet:" + pdcKey);
            if (key != null) meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, pdcValue);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSkull(String texture) {
        ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) icon.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", texture));
        skullMeta.setPlayerProfile(profile);
        icon.setItemMeta(skullMeta);
        return icon;
    }

    private void applyEnchant(ItemMeta meta, String enchantLine) {
        String[] parts = enchantLine.split(":", 2);
        String name = parts[0].toLowerCase(Locale.ROOT);
        int level = 1;
        if (parts.length > 1) {
            try {
                level = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        NamespacedKey key = NamespacedKey.fromString(name.contains(":") ? name : "minecraft:" + name);
        Enchantment enchantment = key == null ? null : Registry.ENCHANTMENT.get(key);
        if (enchantment != null) meta.addEnchant(enchantment, level, true);
    }

    public boolean isActionVisible(Player p, String path) {
        return shouldDisplay(p, plugin.getPetGuiFile(), path);
    }

    private boolean shouldDisplay(Player p, ConfigUtils config, String path) {
        boolean activePet = plugin.getPetManager().getActivePetData(p) != null;
        String condition = config.getString(path + ".display.condition", "always");
        if (!condition.isBlank() && !condition.equalsIgnoreCase("always")) {
            if (!evaluateCondition(p, condition, activePet)) return false;
        }
        for (String entry : config.getStringList(path + ".display.conditions")) {
            if (!evaluateCondition(p, entry, activePet)) return false;
        }
        return true;
    }

    private boolean evaluateCondition(Player p, String condition, boolean activePet) {
        String normalized = condition.toLowerCase(Locale.ROOT);
        boolean negate = normalized.startsWith("!");
        if (negate) normalized = normalized.substring(1);
        boolean result;
        if (normalized.equals("active_pet")) result = activePet;
        else if (normalized.equals("no_active_pet")) result = !activePet;
        else if (normalized.equals("rideable")) {
            PetData data = plugin.getPetManager().getActivePetData(p);
            result = data != null && data.rideable();
        } else if (normalized.equals("has_upgrades")) {
            PetData data = plugin.getPetManager().getActivePetData(p);
            result = data != null && !data.upgrades().isEmpty();
        } else if (normalized.startsWith("permission:")) {
            result = p.hasPermission(condition.substring(condition.indexOf(':') + 1));
        } else if (normalized.startsWith("setting:")) {
            PetData data = plugin.getPetManager().getActivePetData(p);
            String setting = condition.substring(condition.indexOf(':') + 1);
            result = data != null && plugin.getPetManager().getPetSetting(p, data.id(), setting, true);
        } else {
            result = true;
        }
        return negate ? !result : result;
    }

    private String replacePlaceholders(String text, Placeholder... placeholders) {
        return replacePlaceholders(text, true, placeholders);
    }

    private String replacePlaceholders(String text, boolean cleanUnmatched, Placeholder... placeholders) {
        String output = text == null ? "" : text;
        for (Placeholder placeholder : placeholders) output = output.replace(placeholder.key(), placeholder.value());
        if (cleanUnmatched) {
            output = output.replaceAll("<skill\\.(active|passive)\\.[^>]+>", "");
            output = output.replaceAll("<skill\\.description\\.(active|passive)\\.[^>]+>", "");
            output = output.replaceAll("<skill\\.trigger\\.(active|passive)\\.[^>]+>", "");
        }
        return output;
    }

    private List<Placeholder> getSkillPlaceholders(ConfigUtils gui, PetData data) {
        List<Placeholder> list = new ArrayList<>();
        if (data == null) return list;

        int passiveCount = 1;
        int activeCount = 1;

        for (PetSkill skill : data.skills()) {
            if (!skill.enabled()) continue;
            String type = skill.type().toLowerCase(Locale.ROOT);
            String id = skill.id();

            int index = type.equals("passive") ? passiveCount++ : activeCount++;

            String skillName = skill.name() != null && !skill.name().isBlank() ? skill.name() : getDisplayValue(gui, "skills", skill.skillId());
            if (skillName == null || skillName.isBlank()) skillName = skill.skillId();

            list.add(new Placeholder("<skill." + type + "." + id + ">", skillName));
            list.add(new Placeholder("<skill." + type + "." + index + ">", skillName));

            if (skill.lore() != null && !skill.lore().isEmpty() && !String.join("", skill.lore()).isBlank()) {
                String desc = String.join("\n", skill.lore()) + "\n";
                list.add(new Placeholder("<skill.description." + type + "." + id + ">", desc));
                list.add(new Placeholder("<skill.description." + type + "." + index + ">", desc));
            }

            String triggers = skill.triggers().stream()
                    .map(trigger -> getDisplayValue(gui, "triggers", trigger))
                    .collect(java.util.stream.Collectors.joining("/"));
            list.add(new Placeholder("<skill.trigger." + type + "." + id + ">", triggers));
            list.add(new Placeholder("<skill.trigger." + type + "." + index + ">", triggers));
        }
        return list;
    }

    private String formatNumber(double value) {
        return value % 1 == 0 ? String.valueOf((int) value) : String.format("%.2f", value);
    }

    private String getSkillSummary(ConfigUtils gui, PetData data, String type) {
        String format = gui.getString("collection.pet_item.skills.format", "<skill> (<triggers>)");
        String separator = gui.getString("collection.pet_item.skills.separator", ", ");
        String triggerSeparator = gui.getString("collection.pet_item.skills.trigger_separator", "/");
        String empty = gui.getString("collection.pet_item.skills.empty", "-");
        List<String> skills = data.skills().stream()
                .filter(PetSkill::enabled)
                .filter(skill -> "all".equals(type) || skill.type().equalsIgnoreCase(type))
                .map(skill -> format
                        .replace("<id>", skill.id())
                        .replace("<skill>", getDisplayValue(gui, "skills", skill.id()))
                        .replace("<type>", getDisplayValue(gui, "skill_types", skill.type()))
                        .replace("<mythic_skill>", getDisplayValue(gui, "mythic_skills", skill.skillId()))
                        .replace("<cooldown>", formatNumber(skill.cooldown()))
                        .replace("<triggers>", skill.triggers().stream()
                                .map(trigger -> getDisplayValue(gui, "triggers", trigger))
                                .collect(java.util.stream.Collectors.joining(triggerSeparator))))
                .toList();
        if (skills.isEmpty()) return empty;
        return String.join(separator, skills);
    }

    private String getDisplayValue(ConfigUtils gui, String group, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) return "";
        String basePath = "collection.pet_item.display_values." + group + ".";
        String value = gui.getString(basePath + rawValue, "");
        if (!value.isBlank()) return value;
        value = gui.getString(basePath + rawValue.toUpperCase(Locale.ROOT), "");
        if (!value.isBlank()) return value;
        value = gui.getString(basePath + rawValue.toLowerCase(Locale.ROOT), "");
        return value.isBlank() ? rawValue : value;
    }

    private String getConfigString(ConfigUtils primary, ConfigUtils fallbackConfig, String primaryPath, String fallbackPath, String fallbackValue) {
        String value = primary.getString(primaryPath, "");
        if (!value.isBlank()) return value;
        return fallbackConfig.getString(fallbackPath, fallbackValue);
    }

    private String getRequirementDisplay(Player p, PetData data, PetUpgrade upgrade) {
        String display = upgrade.requirementDisplay();
        if (display == null || display.isBlank()) display = "<papi> <compare> <value>";
        return plugin.getPetManager().resolvePlaceholders(p, data, display)
                .replace("<papi>", upgrade.papi())
                .replace("<compare>", upgrade.compare())
                .replace("<value>", upgrade.value())
                .replace("<current_value>", plugin.getPetManager().getResolvedUpgradeRequirement(p, data, upgrade));
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public enum View {
        COLLECTION,
        DETAIL,
        SETTINGS,
        UPGRADES
    }

    private record Placeholder(String key, String value) {
    }
}
