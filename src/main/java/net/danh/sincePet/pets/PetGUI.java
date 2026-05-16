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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PetGUI implements InventoryHolder {
    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;

    private final SincePet plugin;
    private final int page;
    private Inventory inv;

    public PetGUI(SincePet plugin, int page) {
        this.plugin = plugin;
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    public void open(Player p) {
        ConfigUtils gui = plugin.getPetGuiFile();
        ConfigUtils messages = plugin.getPetMessagesFile();

        String titleRaw = messages.getString("pet.gui.title", "Pet Menu <page>").replace("<page>", String.valueOf(page));
        this.inv = Bukkit.createInventory(this, INVENTORY_SIZE, ColorUtils.parse(titleRaw));

        List<PetData> viewablePets = getViewablePets(p);
        setBorder(gui);
        setPetIcons(p, messages, viewablePets);
        setNavigationButtons(gui, viewablePets.size());

        p.openInventory(inv);
    }

    private List<PetData> getViewablePets(Player p) {
        List<PetData> viewablePets = new ArrayList<>();
        boolean hasAllPerm = p.hasPermission("pet.hasall");
        for (PetData pet : plugin.getPetManager().getPetConfig().getAllPets()) {
            if (hasAllPerm || p.hasPermission("pet." + pet.id().toLowerCase())) {
                viewablePets.add(pet);
            }
        }
        return viewablePets;
    }

    private void setBorder(ConfigUtils gui) {
        ItemStack borderItem = new ItemStack(parseMaterial(gui.getString("border.material", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE));
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.displayName(ColorUtils.parse(gui.getString("border.name", " ")));
        applyModelData(borderMeta, gui.getInt("border.model_data"));
        borderItem.setItemMeta(borderMeta);
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

    private void setNavigationButtons(ConfigUtils gui, int totalPets) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPets / ITEMS_PER_PAGE));
        setButton(gui, "buttons.remove", 49, inv);
        if (page > 1) setButton(gui, "buttons.previous", 45, inv);
        if (page < totalPages) setButton(gui, "buttons.next", 53, inv);
    }

    private void setButton(ConfigUtils config, String path, int defaultSlot, Inventory inv) {
        int slot = config.getInt(path + ".slot", defaultSlot);
        ItemStack item = new ItemStack(parseMaterial(config.getString(path + ".material", "ARROW"), Material.ARROW));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ColorUtils.parse(config.getString(path + ".name", "Button")));

        List<String> loreList = config.getStringList(path + ".lore");
        if (!loreList.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreList) lore.add(ColorUtils.parse(line));
            meta.lore(lore);
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
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

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
