# Upgrade System

Each pet can define upgrade nodes in its `pets/*.yml` file.

## Example

```yaml
upgrades:
  ember:
    name: "<red>Ember Core"
    material: "FIRE_CHARGE"
    slot: 10
    max_level: 10
    stats:
      MAGIC_DAMAGE:
        base: 0.0
        max-value: 10.0
        formula: "1.2 * <upgrade_level>"
    damage_bonus_formula: "4 * <upgrade_level>"
    skill_cooldown_formula: "0.1 * <upgrade_level>"
    attack_speed_formula: "0.2 * <upgrade_level>"
    requirement:
      papi: "%playerpoints_points%"
      compare: ">="
      value: "20"
      display: "You need 20 points"
    commands:
      - "points take <player> 20"
```

## Upgrade ID

`ember` is the upgrade ID. It is used for:

- Saved player upgrade levels
- GUI per-upgrade override keys
- Command placeholders

Do not rename upgrade IDs on a live server unless you are okay with old saved upgrade levels no longer matching the
renamed upgrade.

## GUI Material and Slot

```yaml
material: "FIRE_CHARGE"
slot: 10
```

`material` is used as the fallback item for the upgrade GUI. `slot` is the slot in the upgrade GUI.

You can override the visual completely in `gui.yml` under:

```yaml
upgrade_items:
  items:
    ember:
      material: "FIRE_CHARGE"
      name: "<gold>Custom Ember"
      lore:
        - "<gray>Level: <level>/<max_level>"
```

## Max Level

```yaml
max_level: 10
```

The player cannot upgrade this node above `10`.

## Bonus Formulas

```yaml
stats:
  MAGIC_DAMAGE:
    base: 0.0
    max-value: 10.0
    formula: "1.2 * <upgrade_level>"
damage_bonus_formula: "4 * <upgrade_level>"
skill_cooldown_formula: "0.1 * <upgrade_level>"
attack_speed_formula: "0.2 * <upgrade_level>"
```

| Formula                  | Used For                                      |
|--------------------------|-----------------------------------------------|
| `stats`                  | Map of MythicLib stat bonuses.                |
| `damage_bonus_formula`   | Adds to the pet's base attack damage.         |
| `skill_cooldown_formula` | Reduces the cooldown of active/passive skills.|
| `attack_speed_formula`   | Reduces the delay between auto-attacks.       |

Available placeholders:

| Placeholder       | Meaning                |
|-------------------|------------------------|
| `<level>`         | Current pet level.     |
| `<upgrade_level>` | Current upgrade level. |

## Requirement Logic

```yaml
requirement:
  papi: "%playerpoints_points%"
  compare: ">="
  value: "20"
```

The plugin resolves the PlaceholderAPI text in `papi`, compares it to `value`, and only upgrades if the comparison
passes.

Supported comparisons:

| Compare   | Meaning                |
|-----------|------------------------|
| `>=`      | Greater than or equal. |
| `>`       | Greater than.          |
| `<=`      | Less than or equal.    |
| `<`       | Less than.             |
| `=`, `==` | Equal.                 |
| `!=`      | Not equal.             |

## Requirement Display

```yaml
requirement:
  display: "You need 20 points"
```

`display` controls what the GUI shows. It does not affect the upgrade logic.

This prevents ugly GUI lines like:

```text
%playerpoints_points% >= 20
```

Use `<requirement>` in `gui.yml` to show the friendly display text:

```yaml
upgrade_items:
  locked:
    lore:
      - "<red><requirement>"
```

You can still show the raw condition with `<raw_requirement>` if you want it for debugging.

## Commands After Upgrade

```yaml
commands:
  - "points take <player> 20"
```

Commands run from console after the upgrade succeeds. Use this to remove points, money, items, or run any other server
command.

Available command placeholders:

| Placeholder | Meaning                          |
|-------------|----------------------------------|
| `<player>`  | Player name.                     |
| `<uuid>`    | Player UUID.                     |
| `<pet>`     | Pet ID.                          |
| `<upgrade>` | Upgrade ID.                      |
| `<level>`   | New upgrade level after success. |

## Recommended Patterns

### Using Economy or Player Points
For a points upgrade:

```yaml
requirement:
  papi: "%playerpoints_points%"
  compare: ">="
  value: "20"
  display: "You need 20 points"
commands:
  - "points take <player> 20"
```

The requirement checks whether the player has enough points. The command removes the points after the upgrade is
applied.

### Using Internal Pet Upgrading Points (New in Version X)
SincePet now supports an internal points system where pets earn upgrading points simply by leveling up. 
This is completely native and does not require PlaceholderAPI.

1. Configure how fast a specific pet earns points in its `pets/*.yml` file:
```yaml
upgrading_points:
  levels_per_point: 5       # Grants points every 5 levels
  points_per_interval: 1    # Grants 1 point
  max_points: 24            # Maximum points this pet can hold
```

2. Use `upgrading_points` in your upgrade requirement:
```yaml
requirement:
  upgrading_points: "<current.pet.upgrading.points>"
  compare: ">="
  value: "1"
  display: "You need 1 point"
# Notice we do NOT need any commands to take points!
```

When players upgrade a skill branch using `upgrading_points`, the plugin automatically tracks that the point has been used. The `<current.pet.upgrading.points>` placeholder will automatically decrease, allowing a completely cost-free economy setup specific to each pet's progression!
