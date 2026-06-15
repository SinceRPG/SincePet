# Pet Experience GUI (MMOItems Integration)

This feature allows players to feed their pets special items from MMOItems to gain experience points or level up directly. These items are configured directly within the active pet's GUI (Detail GUI).

## 1. How It Works (Code Logic)

This feature is implemented in two main parts:

*   **GUI Display (`PetGUI.java`):**
    In the `openDetail(p)` method, the system scans the `pet-experience` section in the `gui.yml` file. Each child node (such as `1:`, `2:`) corresponds to a displayable item. Based on the `slot` position, the item is rendered as a clickable button (e.g., an experience bottle).
    
*   **Item Consumption Logic (`PetListener.java`):**
    The `handleExpItemConsume` function is responsible for processing clicks on these experience slots:
    1.  **Identify the Item Used:** The system first checks if the player is holding the required MMOItem on their cursor.
    2.  **Scan Inventory:** If the cursor is empty, the system automatically searches the player's entire inventory. The verification process uses MythicLib's NBT API (`io.lumine.mythic.lib.api.item.NBTItem`). The item is considered valid if it has `MMOITEMS_ITEM_ID` and `MMOITEMS_ITEM_TYPE` tags matching the ones specified in the configuration.
    3.  **Consume Item:** If the total valid items found are greater than or equal to `quantity`, the corresponding amount is subtracted from the cursor or sequentially removed from the inventory.
    4.  **Add Experience/Level:** 
        *   If the `exp-level` property is present, the pet's level is increased directly (up to the maximum level).
        *   If the `exp-points` property is present, experience points are added. If the experience exceeds the requirement for the next level (based on `maxXpFormula`), the system automatically triggers a recursive level-up until no excess experience remains.

## 2. Configuration & Usage

1.  **Create MMOItems:** You need to have consumable items ready in MMOItems (e.g., TYPE: `CONSUMABLE`, ID: `CONSUMABLE_EXP_POTION_1`).
2.  **Edit `gui.yml`:** In the `gui.yml` file, there is a `pet-experience` section. You just need to modify the `type` and `id` under `consumable-item` to match the items you created in step 1.
3.  **Reload:** Restart or reload the SincePet plugin.
4.  **How players use it:** 
    *   The player summons their pet and opens the Pet GUI (`Active Pet` / `Detail GUI`).
    *   They will see the experience bottles appear in their designated slots (e.g., slot 5, slot 8).
    *   If the player has the corresponding MMOItem in their inventory, they simply **click** on the experience bottle in the GUI. The item will be automatically consumed, and the pet will gain experience or level up.
    *   Alternatively, they can hold the specific MMOItem directly on their cursor and then click on the experience slot in the GUI.

### Example configuration block in `gui.yml`:
```yaml
pet-experience:
  1: 
    material: "EXPERIENCE_BOTTLE"
    slot: 5
    glow: true
    item_flags:
      - "HIDE_ENCHANTS"
    name: "<green>Common Experience"
    lore:
      - "<gray>Feed your pet to gain"
      - "<gray>200 experience points."
      - ""
      - "<gray>Current EXP: <green><current_exp> / <max_exp>"
      - ""
      - "<yellow>Click to use!"
      - "<gray>(Requires 1 MMOItems)"
    consumable-item:
      type: "CONSUMABLE"
      id: "CONSUMABLE_EXP_POTION_1"
      quantity: 1
    exp-points: 200
  2: 
    material: "EXPERIENCE_BOTTLE"
    slot: 8
    glow: true
    item_flags:
      - "HIDE_ENCHANTS"
    name: "<gold>Rare Experience"
    lore:
      - "<gray>Feed your pet to level up"
      - "<gray>2 times."
      - ""
      - "<gray>Current EXP: <green><current_exp> / <max_exp>"
      - ""
      - "<yellow>Click to use!"
      - "<gray>(Requires 2 MMOItems)"
    consumable-item:
      type: "CONSUMABLE"
      id: "RARE_EXP_CANDY"
      quantity: 2
    exp-level: 2
```
