package net.danh.sincePet.pets;

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
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PetGUI implements InventoryHolder {
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
        this.inv = Bukkit.createInventory(this, 54, ColorUtils.parse(titleRaw));

        // Get viewable pets for this player
        List<PetData> allPets = new ArrayList<>(plugin.getPetManager().getPetConfig().getAllPets());
        List<PetData> viewablePets = new ArrayList<>();
        for (PetData pet : allPets) {
            if (p.hasPermission("pet." + pet.id().toLowerCase())) {
                viewablePets.add(pet);
            }
        }

        // --- BORDER LOGIC (45-53) ---
        ItemStack borderItem = new ItemStack(Material.valueOf(gui.getString("border.material", "BLACK_STAINED_GLASS_PANE")));
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.displayName(ColorUtils.parse(gui.getString("border.name", " ")));
        if (gui.getInt("border.model_data") > 0) {
            int modelId = gui.getInt("border.model_data");
            try {
                // Paper 1.20.5+ API
                CustomModelDataComponent modelComp = borderMeta.getCustomModelDataComponent();
                modelComp.setFloats(List.of((float) modelId));
                borderMeta.setCustomModelDataComponent(modelComp);
            } catch (NoSuchMethodError e) {
                // Fallback for older API
                borderMeta.setCustomModelData(modelId);
            }
        }
        borderItem.setItemMeta(borderMeta);
        for (int i = 45; i < 54; i++) inv.setItem(i, borderItem);

        // --- PAGINATION LOGIC ---
        int itemsPerPage = 45;
        int totalPets = viewablePets.size();
        int totalPages = (int) Math.ceil((double) totalPets / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalPets);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            PetData data = viewablePets.get(i);
            int specificLevel = plugin.getPetManager().getPetLevel(p, data.id());

            ItemStack icon = new ItemStack(Material.PAPER); // Default fallback
            try {
                // Create skull item logic could go here if needed, but PAPER is fine for now if configured so
                // Or retrieve skull texture if defined in PetData
                // For now, adhering to user structure which uses PAPER + ModelData usually
            } catch (Exception ignored) {
            }

            ItemMeta meta = icon.getItemMeta();
            String nameFmt = messages.getString("pet.gui.item_name", "<name>");
            meta.displayName(ColorUtils.parse(nameFmt.replace("<name>", data.name())));

            // Model Data for Pet Icon
            // (Thêm logic này nếu bạn muốn Icon Pet có texture riêng trong GUI)
            // meta.setCustomModelData(...)

            double buffValue = 0;
            try {
                String f = data.formula().replace("<level>", String.valueOf(specificLevel));
                buffValue = Double.parseDouble(Calculator.calculator(f, 2));
            } catch (Exception ignored) {
            }

            String buffDisplay = (buffValue % 1 == 0) ? String.valueOf((int) buffValue) : String.format("%.2f", buffValue);
            String inheritanceStr = String.valueOf((int) (data.inheritance() * 100));
            String statusLine = messages.getString("pet.gui.status_unlocked", "Unlocked");

            List<String> loreRaw = messages.getStringList("pet.gui.item_lore");
            List<Component> lore = new ArrayList<>();
            for (String line : loreRaw) {
                lore.add(ColorUtils.parse(line
                        .replace("<id>", data.id())
                        .replace("<level>", String.valueOf(specificLevel))
                        .replace("<stat>", data.stat())
                        .replace("<value>", buffDisplay)
                        .replace("<formula>", data.formula())
                        .replace("<inheritance>", inheritanceStr)
                        .replace("<status>", statusLine)));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
        }

        // --- BUTTONS ---
        setButton(gui, "buttons.remove", 49, inv);

        if (page > 1) {
            setButton(gui, "buttons.previous", 45, inv);
        }

        if (page < totalPages) {
            setButton(gui, "buttons.next", 53, inv);
        }

        p.openInventory(inv);
    }

    private void setButton(ConfigUtils config, String path, int defaultSlot, Inventory inv) {
        int slot = config.getInt(path + ".slot", defaultSlot);
        ItemStack item = new ItemStack(Material.valueOf(config.getString(path + ".material", "ARROW")));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ColorUtils.parse(config.getString(path + ".name", "Button")));

        List<String> loreList = config.getStringList(path + ".lore");
        if (!loreList.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String l : loreList) lore.add(ColorUtils.parse(l));
            meta.lore(lore);
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}