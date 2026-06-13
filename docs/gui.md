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

`use_pet_texture` is only meaningful for collection pet items. When it is true and the material is `PLAYER_HEAD`, the icon uses the texture from the matching `pets/*.yml` file.

## Border

```yaml
border:
  material: "BLACK_STAINED_GLASS_PANE"
  name: " "
```

The border is used in the bottom row of collection/upgrades menus and as a filler in detail/settings menus.

## Collection Pet Items

`collection.pet_item` controls every pet icon in the collection GUI.

The default `gui.yml` is ready to use after installation. It already includes friendly display names for every bundled pet, stat, ability, MythicMobs skill, and trigger. Server owners only need to edit this section when they want different wording, colors, lore order, or custom model data.

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

| Placeholder | Meaning |
| --- | --- |
| `<pet>` / `<name>` | Pet display name. |
| `<id>` / `<pet_id>` | Pet ID from `pets/*.yml`. |
| `<pet_display>` / `<pet_id_display>` | Friendly pet name from `display_values.pets`. |
| `<level>` | Player's current pet level. |
| `<stat>` | Pet stat from config. |
| `<value>` / `<stat_bonus>` | Current stat bonus from the pet formula. |
| `<formula>` | Raw stat formula. |
| `<inheritance>` | Damage inheritance percent. |
| `<skills>` | All enabled abilities using `collection.pet_item.skills.format`. |
| `<active_skills>` | Enabled active abilities only. |
| `<passive_skills>` | Enabled passive abilities only. |
| `<status>` | Configured status text. |
| `<texture>` | Raw pet head texture from `pets/*.yml`. |

### Collection Display Values

Use `collection.pet_item.display_values` to customize raw values before they are inserted into placeholders.

| Path | Affects |
| --- | --- |
| `display_values.pets` | `<pet_display>` and `<pet_id_display>`. |
| `display_values.stats` | `<stat>` |
| `display_values.skills` | `<skill>` inside the `<skills>`, `<active_skills>`, and `<passive_skills>` summary. |
| `display_values.triggers` | `<triggers>` inside skill summaries. |
| `display_values.skill_types` | `<type>` inside skill summaries. |
| `display_values.mythic_skills` | `<mythic_skill>` inside skill summaries. |

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

The plugin checks exact keys first, then uppercase, then lowercase. This means `SLOTH`, `sloth`, `HEALTH`, and `health` can all be mapped without changing pet IDs or ability IDs.

### Bundled Display Keys

Default `gui.yml` includes mappings for these bundled pets:

| Pet ID | Friendly Display |
| --- | --- |
| `BEAR` | Grizzly Bear |
| `DRAGON` | Ender Dragon |
| `EAGLE` | Sky Eagle |
| `FOX` | Swift Fox |
| `FROG` | Swamp Frog |
| `MONKEY` | Monkey King |
| `PANDA` | Bamboo Panda |
| `PARROT` | Pirate Parrot |
| `PHOENIX` | Phoenix Ember |
| `SLOTH` | Sleepy Sloth |
| `TIGER` | Bengal Tiger |
| `TURTLE` | Ancient Turtle |
| `UNICORN` | Mystic Unicorn |
| `WOLF` | Wolf Spirit |

Default stat display mappings:

| Stat Key | Friendly Display |
| --- | --- |
| `CRITICAL_STRIKE_CHANCE` | Critical Strike Chance |
| `CRITICAL_STRIKE_POWER` | Critical Strike Power |
| `HEALTH` | Health |
| `HEALTH_REGENERATION` | Health Regeneration |
| `MAGIC_DAMAGE` | Magic Damage |
| `MAX_HEALTH` | Max Health |
| `MOVEMENT_SPEED` | Movement Speed |
| `PHYSICAL_DAMAGE` | Physical Damage |

Default trigger display mappings:

| Trigger Key | Friendly Display |
| --- | --- |
| `ACTIVE` | Command |
| `BED_ENTER` | Bed Enter |
| `CHAT` | Chat |
| `DOUBLE_SNEAK` | Double Sneak |
| `FLIGHT_START` | Flight Start |
| `GAMEMODE_CHANGE` | Gamemode Change |
| `GLIDE_START` | Glide Start |
| `HARVEST_BLOCK` | Harvest Block |
| `ITEM_CONSUME` | Item Consume |
| `JUMP` | Jump |
| `MOVE` | Move |
| `OWNER_DAMAGED` | Owner Damaged |
| `OWNER_DAMAGED_BY_ENTITY` | Owner Damaged By Entity |
| `OWNER_RESPAWN` | Owner Respawn |
| `PHYSICAL_INTERACT` | Physical Interact |
| `PROJECTILE_LAUNCH` | Projectile Launch |
| `RIGHT_CLICK_AIR` | Right Click |
| `RIPTIDE` | Riptide |
| `SNEAK` | Sneak |
| `SNEAK_START` | Sneak Start |
| `SPRINT_START` | Sprint Start |
| `SWAP_HAND` | Swap Hand |
| `SWIM_START` | Swim Start |
| `WORLD_CHANGE` | World Change |

Default ability display mappings:

| Ability Key | Friendly Display |
| --- | --- |
| `bamboo_snack` | Bamboo Snack |
| `banana_toss` | Banana Toss |
| `berry_eat` | Berry Eat |
| `breath` | Dragon Breath |
| `counter_howl` | Counter Howl |
| `dash` | Dash |
| `dive` | Dive |
| `dragon_scales` | Dragon Scales |
| `eagle_eye` | Eagle Eye |
| `fireball` | Fireball |
| `hibernation` | Hibernation |
| `horn_pierce` | Horn Pierce |
| `jump_boost` | Jump Boost |
| `magic_aura` | Magic Aura |
| `maul` | Maul |
| `mimic` | Mimic |
| `monkey_agility` | Monkey Agility |
| `rebirth` | Rebirth |
| `rift_bite` | Rift Bite |
| `roar` | Roar |
| `roll` | Roll |
| `screech` | Screech |
| `shell_shield` | Shell Shield |
| `sleepy_heal` | Sleepy Heal |
| `swim_boost` | Swim Boost |
| `takeoff` | Takeoff |
| `tiger_pounce` | Tiger Pounce |
| `water_spout` | Water Spout |
| `yawn` | Yawn |

Default MythicMobs skill display mappings:

| Mythic Skill Key | Friendly Display |
| --- | --- |
| `ARCANE_HAIL` | Arcane Hail |
| `ARCANE_RIFT` | Arcane Rift |
| `BANANA_TOSS` | Banana Toss |
| `BEAR_HIBERNATION` | Bear Hibernation |
| `BEAR_MAUL` | Bear Maul |
| `DRAGON_BREATH` | Dragon Breath |
| `DRAGON_SCALES` | Dragon Scales |
| `EAGLE_DIVE` | Eagle Dive |
| `EAGLE_EYE` | Eagle Eye |
| `FOX_DASH` | Fox Dash |
| `FOX_HEAL` | Fox Heal |
| `FROG_JUMP` | Frog Jump |
| `FROG_SWIM` | Frog Swim |
| `MONKEY_AGILITY` | Monkey Agility |
| `PANDA_EAT` | Panda Eat |
| `PANDA_ROLL` | Panda Roll |
| `PARROT_FLY` | Parrot Fly |
| `PARROT_MIMIC` | Parrot Mimic |
| `PARROT_SCREECH` | Parrot Screech |
| `PHOENIX_FIREBALL` | Phoenix Fireball |
| `PHOENIX_REBIRTH` | Phoenix Rebirth |
| `SLOTH_HEAL` | Sleepy Heal |
| `SLOTH_YAWN` | Yawn |
| `TIGER_POUNCE` | Tiger Pounce |
| `TIGER_ROAR` | Tiger Roar |
| `TURTLE_SHIELD` | Turtle Shield |
| `TURTLE_SPOUT` | Turtle Spout |
| `UNICORN_AURA` | Unicorn Aura |
| `UNICORN_PIERCE` | Unicorn Pierce |

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
