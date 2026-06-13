package net.danh.sincePet.pets;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.danh.sincePet.SincePet;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PetGUI implements InventoryHolder {
    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    public enum View {
        COLLECTION,
        DETAIL,
        SETTINGS,
        UPGRADES
    }

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
        setPetIcons(p, messages, viewablePets);
        setNavigationButtons(p, gui, viewablePets.size());

        p.openInventory(inv);
    }

    private void openDetail(Player p) {
        ConfigUtils gui = plugin.getPetGuiFile();
        ConfigUtils messages = plugin.getPetMessagesFile();
        PetData data = plugin.getPetManager().getActivePetData(p);
        String petName = data == null ? messages.getString("pet.gui.no_active_pet", "No Active Pet") : data.name();
        this.inv = Bukkit.createInventory(this, 27, ColorUtils.parse(messages.getString("pet.gui.detail_title", "<black><bold>Pet Detail: <name>").replace("<name>", petName)));
        fill(p, inv, gui);
        setButton(p, gui, "detail.buttons.back", 18, inv);
        setButton(p, gui, "detail.buttons.ride", 11, inv);
        setButton(p, gui, "detail.buttons.settings", 13, inv);
        setButton(p, gui, "detail.buttons.upgrades", 15, inv);
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

    private void setPetIcons(Player p, ConfigUtils messages, List<PetData> viewablePets) {
        int totalPets = viewablePets.size();
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalPets);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            PetData data = viewablePets.get(i);
            int level = plugin.getPetManager().getPetLevel(p, data.id());
            inv.setItem(slot++, createPetIcon(messages, data, level));
        }
    }

    private ItemStack createPetIcon(ConfigUtils messages, PetData data, int level) {
        ItemStack icon = createIconBase(data);
        ItemMeta meta = icon.getItemMeta();
        String nameFmt = messages.getString("pet.gui.item_name", "<name>");
        meta.displayName(ColorUtils.parse(nameFmt.replace("<name>", data.name())));

        double buffValue = 0;
        try {
            String formula = data.formula().replace("<level>", String.valueOf(level));
            buffValue = Double.parseDouble(Calculator.calculator(formula, 2));
        } catch (NumberFormatException ignored) {
        }

        String buffDisplay = (buffValue % 1 == 0) ? String.valueOf((int) buffValue) : String.format("%.2f", buffValue);
        String inheritanceStr = String.valueOf((int) (data.inheritance() * 100));
        String statusLine = messages.getString("pet.gui.status_unlocked", "Unlocked");

        List<Component> lore = new ArrayList<>();
        for (String line : messages.getStringList("pet.gui.item_lore")) {
            lore.add(ColorUtils.parse(line
                    .replace("<id>", data.id())
                    .replace("<level>", String.valueOf(level))
                    .replace("<stat>", data.stat())
                    .replace("<value>", buffDisplay)
                    .replace("<formula>", data.formula())
                    .replace("<inheritance>", inheritanceStr)
                    .replace("<skills>", getSkillSummary(data, "all"))
                    .replace("<active_skills>", getSkillSummary(data, "active"))
                    .replace("<passive_skills>", getSkillSummary(data, "passive"))
                    .replace("<status>", statusLine)));
        }
        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createIconBase(PetData data) {
        if (data.texture() == null || data.texture().isEmpty()) {
            return new ItemStack(Material.PAPER);
        }

        ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) icon.getItemMeta();
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        profile.setProperty(new ProfileProperty("textures", data.texture()));
        skullMeta.setPlayerProfile(profile);
        icon.setItemMeta(skullMeta);
        return icon;
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

    private ItemStack createUpgradeIcon(Player p, ConfigUtils gui, PetData data, PetUpgrade upgrade, int level) {
        boolean maxed = level >= upgrade.maxLevel();
        boolean requirementMet = plugin.getPetManager().checkUpgradeRequirement(p, upgrade);
        String state = maxed ? "maxed" : requirementMet ? "available" : "locked";
        String overridePath = "upgrade_items.items." + upgrade.id();
        String path = gui.getConfig().isConfigurationSection(overridePath) ? overridePath : "upgrade_items." + state;
        int nextLevel = Math.min(level + 1, upgrade.maxLevel());
        return createConfiguredItem(p, gui, path, upgrade.material(), upgrade.name(), List.of(),
                new Placeholder("<pet>", data.name()),
                new Placeholder("<pet_id>", data.id()),
                new Placeholder("<upgrade>", upgrade.name()),
                new Placeholder("<upgrade_id>", upgrade.id()),
                new Placeholder("<level>", String.valueOf(level)),
                new Placeholder("<next_level>", String.valueOf(nextLevel)),
                new Placeholder("<max_level>", String.valueOf(upgrade.maxLevel())),
                new Placeholder("<papi>", upgrade.papi()),
                new Placeholder("<compare>", upgrade.compare()),
                new Placeholder("<value>", upgrade.value()),
                new Placeholder("<current_value>", plugin.getPetManager().getResolvedUpgradeRequirement(p, upgrade)),
                new Placeholder("<requirement>", getRequirementDisplay(p, upgrade)),
                new Placeholder("<raw_requirement>", upgrade.papi() + " " + upgrade.compare() + " " + upgrade.value()),
                new Placeholder("<state>", state),
                new Placeholder("<stat_bonus>", formatNumber(plugin.getPetManager().getUpgradeStatBonus(p, data, upgrade, level))),
                new Placeholder("<next_stat_bonus>", formatNumber(plugin.getPetManager().getUpgradeStatBonus(p, data, upgrade, nextLevel))),
                new Placeholder("<damage_bonus>", formatNumber(plugin.getPetManager().getUpgradeDamageBonus(p, data, upgrade, level))),
                new Placeholder("<next_damage_bonus>", formatNumber(plugin.getPetManager().getUpgradeDamageBonus(p, data, upgrade, nextLevel))),
                new Placeholder("<commands>", String.join(", ", upgrade.commands())));
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
        Material material = parseMaterial(config.getString(path + ".material", fallbackMaterial.name()), fallbackMaterial);
        int amount = Math.max(1, Math.min(99, config.getInt(path + ".amount", 1)));
        ItemStack item = new ItemStack(material, amount);

        String texture = config.getString(path + ".skull_texture", "");
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
            for (String line : loreRaw) lore.add(ColorUtils.parse(replacePlaceholders(line, placeholders)));
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

        String pdcKey = config.getString(path + ".data.key", "");
        String pdcValue = config.getString(path + ".data.value", "");
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
        String output = text == null ? "" : text;
        for (Placeholder placeholder : placeholders) output = output.replace(placeholder.key(), placeholder.value());
        return output;
    }

    private String formatNumber(double value) {
        return value % 1 == 0 ? String.valueOf((int) value) : String.format("%.2f", value);
    }

    private String getSkillSummary(PetData data, String type) {
        List<String> skills = data.skills().stream()
                .filter(PetSkill::enabled)
                .filter(skill -> "all".equals(type) || skill.type().equalsIgnoreCase(type))
                .map(skill -> skill.id() + " (" + String.join("/", skill.triggers()) + ")")
                .toList();
        if (skills.isEmpty()) return "-";
        return String.join(", ", skills);
    }

    private String getRequirementDisplay(Player p, PetUpgrade upgrade) {
        String display = upgrade.requirementDisplay();
        if (display == null || display.isBlank()) display = "<papi> <compare> <value>";
        return plugin.getPetManager().resolvePlaceholders(p, display)
                .replace("<papi>", upgrade.papi())
                .replace("<compare>", upgrade.compare())
                .replace("<value>", upgrade.value())
                .replace("<current_value>", plugin.getPetManager().getResolvedUpgradeRequirement(p, upgrade));
    }

    private record Placeholder(String key, String value) {
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
