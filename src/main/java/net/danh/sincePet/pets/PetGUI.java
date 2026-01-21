package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import net.danh.sincePet.utils.ColorUtils;
import net.danh.sincePet.utils.ConfigUtils;
import net.danh.sincePet.utils.FormulaUtils;
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

        List<PetData> allPets = new ArrayList<>(plugin.getPetManager().getPetConfig().getAllPets());
        List<PetData> viewablePets = new ArrayList<>();

        for (PetData pet : allPets) {
            if (p.hasPermission("pet." + pet.id().toLowerCase())) {
                viewablePets.add(pet);
            }
        }

        // Border
        ItemStack borderItem = new ItemStack(Material.valueOf(gui.getString("border.material", "BLACK_STAINED_GLASS_PANE")));
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.displayName(ColorUtils.parse(gui.getString("border.name", " ")));
        if (gui.getInt("border.model_data") > 0) {
            int modelId = gui.getInt("border.model_data");
            try {
                CustomModelDataComponent modelComp = borderMeta.getCustomModelDataComponent();
                modelComp.setFloats(List.of((float) modelId));
                borderMeta.setCustomModelDataComponent(modelComp);
            } catch (NoSuchMethodError e) {
                borderMeta.setCustomModelData(modelId);
            }
        }
        borderItem.setItemMeta(borderMeta);
        for (int i = 45; i < 54; i++) inv.setItem(i, borderItem);

        // Pagination Logic
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

            ItemStack icon = new ItemStack(Material.PAPER);
            ItemMeta meta = icon.getItemMeta();

            String nameFmt = messages.getString("pet.gui.item_name", "<name>");
            meta.displayName(ColorUtils.parse(nameFmt.replace("<name>", data.name())));

            double buffValue = 0;
            try {
                String f = data.formula().replace("<level>", String.valueOf(specificLevel));
                buffValue = FormulaUtils.eval(f);
            } catch (Exception ignored) {
            }
            String buffDisplay = (buffValue % 1 == 0) ? String.valueOf((int) buffValue) : String.format("%.2f", buffValue);

            String inheritanceStr = String.valueOf((int) (data.inheritance() * 100));
            String statusLine = messages.getString("pet.gui.status_unlocked");

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

        // Buttons
        int removeSlot = gui.getInt("buttons.remove.slot", 49);
        ItemStack remove = new ItemStack(Material.valueOf(gui.getString("buttons.remove.material", "BARRIER")));
        ItemMeta rmMeta = remove.getItemMeta();
        rmMeta.displayName(ColorUtils.parse(gui.getString("buttons.remove.name")));
        List<Component> rmLore = new ArrayList<>();
        for (String l : gui.getStringList("buttons.remove.lore")) rmLore.add(ColorUtils.parse(l));
        rmMeta.lore(rmLore);
        remove.setItemMeta(rmMeta);
        inv.setItem(removeSlot, remove);

        if (page > 1) {
            int prevSlot = gui.getInt("buttons.previous.slot", 45);
            ItemStack prev = new ItemStack(Material.valueOf(gui.getString("buttons.previous.material", "ARROW")));
            ItemMeta pm = prev.getItemMeta();
            pm.displayName(ColorUtils.parse(gui.getString("buttons.previous.name", "Previous")));
            prev.setItemMeta(pm);
            inv.setItem(prevSlot, prev);
        }

        if (page < totalPages) {
            int nextSlot = gui.getInt("buttons.next.slot", 53);
            ItemStack next = new ItemStack(Material.valueOf(gui.getString("buttons.next.material", "ARROW")));
            ItemMeta nm = next.getItemMeta();
            nm.displayName(ColorUtils.parse(gui.getString("buttons.next.name", "Next")));
            next.setItemMeta(nm);
            inv.setItem(nextSlot, next);
        }

        p.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}