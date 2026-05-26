# Upgrade System

Each pet can define upgrade nodes in `pets.yml`.

## Example

```yaml
upgrades:
  ember:
    name: "<red>Ember Core"
    material: "FIRE_CHARGE"
    slot: 10
    max_level: 10
    stat_bonus_formula: "1.2 * <upgrade_level>"
    damage_bonus_formula: "4 * <upgrade_level>"
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

Do not rename upgrade IDs on a live server unless you are okay with old saved upgrade levels no longer matching the renamed upgrade.

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
stat_bonus_formula: "1.2 * <upgrade_level>"
damage_bonus_formula: "4 * <upgrade_level>"
```

| Formula | Used For |
| --- | --- |
| `stat_bonus_formula` | Adds to the pet's MythicLib stat bonus. |
| `damage_bonus_formula` | Adds to the pet's base attack damage. |

Available placeholders:

| Placeholder | Meaning |
| --- | --- |
| `<level>` | Current pet level. |
| `<upgrade_level>` | Current upgrade level. |

## Requirement Logic

```yaml
requirement:
  papi: "%playerpoints_points%"
  compare: ">="
  value: "20"
```

The plugin resolves the PlaceholderAPI text in `papi`, compares it to `value`, and only upgrades if the comparison passes.

Supported comparisons:

| Compare | Meaning |
| --- | --- |
| `>=` | Greater than or equal. |
| `>` | Greater than. |
| `<=` | Less than or equal. |
| `<` | Less than. |
| `=`, `==` | Equal. |
| `!=` | Not equal. |

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

Commands run from console after the upgrade succeeds. Use this to remove points, money, items, or run any other server command.

Available command placeholders:

| Placeholder | Meaning |
| --- | --- |
| `<player>` | Player name. |
| `<uuid>` | Player UUID. |
| `<pet>` | Pet ID. |
| `<upgrade>` | Upgrade ID. |
| `<level>` | New upgrade level after success. |

## Recommended Pattern

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

The requirement checks whether the player has enough points. The command removes the points after the upgrade is applied.
