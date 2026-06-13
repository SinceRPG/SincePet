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

## Border

```yaml
border:
  material: "BLACK_STAINED_GLASS_PANE"
  name: " "
```

The border is used in the bottom row of collection/upgrades menus and as a filler in detail/settings menus.

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

| Path | Purpose |
| --- | --- |
| `buttons.detail` | Opens the active pet detail menu. |
| `buttons.remove` | Despawns the active pet. |
| `buttons.previous` | Previous collection page. |
| `buttons.next` | Next collection page. |

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
          - "permission:pets.ride"
```

Default detail buttons:

| Path | Purpose |
| --- | --- |
| `detail.buttons.back` | Back to collection. |
| `detail.buttons.back_detail` | Back to detail menu. |
| `detail.buttons.ride` | Ride active pet when the player has `pets.ride`. |
| `detail.buttons.settings` | Open pet settings. |
| `detail.buttons.upgrades` | Open pet upgrades. |

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

| Path | Setting |
| --- | --- |
| `settings.show_name` | Toggle pet name display. |
| `settings.auto_attack` | Toggle pet auto attack. |
| `settings.stat_buff` | Toggle pet stat buff. |

## Upgrade Item Templates

Upgrade GUI items are selected by state:

```yaml
upgrade_items:
  available:
  locked:
  maxed:
```

| State | Meaning |
| --- | --- |
| `available` | Player meets the requirement and can upgrade. |
| `locked` | Player does not meet the requirement. |
| `maxed` | Upgrade is already at max level. |

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

| Placeholder | Meaning |
| --- | --- |
| `<pet>` | Pet display name. |
| `<pet_id>` | Pet ID from `pets.yml`. |
| `<upgrade>` | Upgrade display name. |
| `<upgrade_id>` | Upgrade ID from `pets.yml`. |
| `<level>` | Current upgrade level. |
| `<next_level>` | Next upgrade level. |
| `<max_level>` | Max upgrade level. |
| `<papi>` | Raw PlaceholderAPI requirement string. |
| `<compare>` | Raw compare operator. |
| `<value>` | Raw required value. |
| `<current_value>` | Player's resolved PlaceholderAPI value. |
| `<requirement>` | Friendly `requirement.display` text from `pets.yml`. |
| `<raw_requirement>` | Raw condition such as `%playerpoints_points% >= 20`. |
| `<state>` | `available`, `locked`, or `maxed`. |
| `<stat_bonus>` | Current stat upgrade bonus. |
| `<next_stat_bonus>` | Next level stat upgrade bonus. |
| `<damage_bonus>` | Current damage upgrade bonus. |
| `<next_damage_bonus>` | Next level damage upgrade bonus. |
| `<commands>` | Commands configured for the upgrade, joined by comma. |

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
