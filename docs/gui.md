# GUI Configuration

`gui.yml` controls GUI items, item meta, display conditions, and upgrade item templates.

## Item Meta Supported Everywhere

Most GUI items support this format:

```yaml
slot: 13
material: "COMPARATOR"
amount: 1
name: "<aqua><b>Pet Settings"
lore:
  - "<gray>Open pet settings"
model_data: 1001
custom_model_data: 1001
skull_texture: "base64 texture"
use_pet_texture: true
glow: true
enchants:
  - "sharpness:1"
item_flags:
  - "HIDE_ATTRIBUTES"
  - "HIDE_ENCHANTS"
hide_attributes: true
hide_enchants: true
hide_unbreakable: true
unbreakable: true
damage: 0
data:
  key: "sincepet:example"
  value: "example-value"
display:
  conditions:
    - "active_pet"
```

`use_pet_texture` is only meaningful for collection pet items. When it is true and the material is `PLAYER_HEAD`, the
icon uses the texture from the matching `pets/*.yml` file.

## Border

```yaml
border:
  material: "BLACK_STAINED_GLASS_PANE"
  name: " "
```

The border is used in the bottom row of collection/upgrades menus and as a filler in detail/settings menus.

## Collection Pet Items

`collection.pet_item` controls every pet icon in the collection GUI.

The default `gui.yml` is ready to use after installation. It already includes friendly display names for every bundled
pet, stat, ability, MythicMobs skill, and trigger. Server owners only need to edit this section when they want different
wording, colors, lore order, or custom model data.

```yaml
collection:
  pet_item:
    material: "PLAYER_HEAD"
    use_pet_texture: true
    name: "<name>"
    lore:
      - "<gray>Level: <white><level>"
      - "<gray>Stat Bonus: <green>+<value> <stat>"
      - "<gray>Abilities: <aqua><skills>"
      - "<yellow>Click to summon!"
    skills:
      format: "<skill> (<triggers>)"
      separator: ", "
      trigger_separator: "/"
      empty: "-"
    display_values:
      pets:
        SLOTH: "Sleepy Sloth"
      stats:
        HEALTH: "Health"
      triggers:
        SNEAK: "Sneak"
        DOUBLE_SNEAK: "Double Sneak"
      skills:
        sleepy_heal: "Sleepy Heal"
```

Use `collection.items.<pet_id>` to override one pet.

```yaml
collection:
  items:
    SLOTH:
      material: "PLAYER_HEAD"
      use_pet_texture: true
      name: "<green><b>Custom Sloth"
      lore:
        - "<gray>ID: <pet_id>"
        - "<yellow>Click to summon!"
```

### Collection Pet Placeholders

[See GUI Reference for the list of Collection Pet Placeholders.](gui-reference.md#collection-pet-placeholders)

### Collection Display Values

Use `collection.pet_item.display_values` to customize raw values before they are inserted into placeholders.

| Path                           | Affects                                                                             |
|--------------------------------|-------------------------------------------------------------------------------------|
| `display_values.pets`          | `<pet_display>` and `<pet_id_display>`.                                             |
| `display_values.stats`         | `<stat>`                                                                            |
| `display_values.skills`        | `<skill>` inside the `<skills>`, `<active_skills>`, and `<passive_skills>` summary. |
| `display_values.triggers`      | `<triggers>` inside skill summaries.                                                |
| `display_values.skill_types`   | `<type>` inside skill summaries.                                                    |
| `display_values.mythic_skills` | `<mythic_skill>` inside skill summaries.                                            |

Example:

```yaml
collection:
  pet_item:
    display_values:
      pets:
        SLOTH: "<green>Sleepy Sloth"
      stats:
        HEALTH: "<green>Health"
      triggers:
        SNEAK: "<yellow>Sneak"
      skills:
        sleepy_heal: "<aqua>Sleepy Heal"
```

The plugin checks exact keys first, then uppercase, then lowercase. This means `SLOTH`, `sloth`, `HEALTH`, and `health`
can all be mapped without changing pet IDs or ability IDs.

### Bundled Display Keys

[See GUI Reference for the list of default bundled mappings.](gui-reference.md#bundled-display-keys)

## Collection Buttons

```yaml
buttons:
  detail:
    slot: 48
    material: "BOOK"
    name: "<aqua><b>Active Pet"
    display:
      conditions:
        - "active_pet"
```

Default collection buttons:

| Path               | Purpose                           |
|--------------------|-----------------------------------|
| `buttons.detail`   | Opens the active pet detail menu. |
| `buttons.remove`   | Despawns the active pet.          |
| `buttons.previous` | Previous collection page.         |
| `buttons.next`     | Next collection page.             |

## Detail Menu Buttons

```yaml
detail:
  buttons:
    ride:
      slot: 11
      material: "SADDLE"
      name: "<green><b>Ride Pet"
      display:
        conditions:
          - "active_pet"
          - "rideable"
          - "permission:sincepet.ride"
```

Default detail buttons:

| Path                         | Purpose                                              |
|------------------------------|------------------------------------------------------|
| `detail.buttons.back`        | Back to collection.                                  |
| `detail.buttons.back_detail` | Back to detail menu.                                 |
| `detail.buttons.ride`        | Ride active pet when the player has `sincepet.ride`. |
| `detail.buttons.settings`    | Open pet settings.                                   |
| `detail.buttons.upgrades`    | Open pet upgrades.                                   |

## Settings Buttons

```yaml
settings:
  show_name:
    slot: 10
    material: "NAME_TAG"
    name: "<aqua>Name Display: <state>"
```

`<state>` is replaced with the configured enabled/disabled text from `messages.yml`.

Default setting buttons:

| Path                   | Setting                  |
|------------------------|--------------------------|
| `settings.show_name`   | Toggle pet name display. |
| `settings.auto_attack` | Toggle pet auto attack.  |
| `settings.stat_buff`   | Toggle pet stat buff.    |

## Upgrade Item Templates

Upgrade GUI items are selected by state:

```yaml
upgrade_items:
  available:
  locked:
  maxed:
```

| State       | Meaning                                       |
|-------------|-----------------------------------------------|
| `available` | Player meets the requirement and can upgrade. |
| `locked`    | Player does not meet the requirement.         |
| `maxed`     | Upgrade is already at max level.              |

Example:

```yaml
upgrade_items:
  locked:
    name: "<red><upgrade>"
    lore:
      - "<gray>Level: <white><level>/<max_level>"
      - "<red><requirement>"
      - "<gray>Your value: <red><current_value>"
```

### Upgrade Placeholders

[See GUI Reference for the list of Upgrade Placeholders.](gui-reference.md#upgrade-placeholders)

## Per-Upgrade Override

Use `upgrade_items.items.<upgrade_id>` to override one upgrade.

```yaml
upgrade_items:
  items:
    ember:
      material: "FIRE_CHARGE"
      name: "<gradient:red:gold>Custom Ember</gradient>"
      model_data: 1001
      lore:
        - "<gray>Level: <level>/<max_level>"
        - "<gray><requirement>"
```

The key `ember` must match the upgrade ID in `pets.yml`.
