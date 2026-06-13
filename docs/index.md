# Welcome

SincePet is a Paper/Folia-ready pet plugin with configurable pet visuals, riding, combat, stat bonuses, pet levels, upgrade levels, per-pet settings, GUI menus, PlaceholderAPI requirements, MythicLib stat integration, MythicMobs EXP drops, and WorldGuard region control.

This wiki is written for server owners who want to configure the plugin without reading Java code. Start with Installation if this is a new setup, or jump straight to GUI and Upgrades if you are customizing menus.

<div class="cards">
  <a class="card" href="installation.html"><strong>Install</strong><span>Server requirements, first start, and updating existing files.</span></a>
  <a class="card" href="configuration.html"><strong>Configure</strong><span>Display offsets, riding physics, database, follow behavior, and combat.</span></a>
  <a class="card" href="pets.html"><strong>Create Pets</strong><span>Textures, stats, XP formulas, attacks, riding, and permissions.</span></a>
  <a class="card" href="upgrades.html"><strong>Build Upgrades</strong><span>Level upgrades, PlaceholderAPI checks, friendly requirement text, and commands.</span></a>
  <a class="card" href="gui.html"><strong>Customize GUI</strong><span>Every item meta field, pet display value, placeholder, and per-pet/per-upgrade override.</span></a>
  <a class="card" href="troubleshooting.html"><strong>Fix Issues</strong><span>Name offsets, riding visuals, PlaceholderAPI values, and missing config keys.</span></a>
</div>

## Main Sections

| Page | What it covers |
| --- | --- |
| [Installation](installation.md) | Requirements, first install, updates, and generated files. |
| [Commands and Permissions](commands-permissions.md) | Player commands, admin commands, and permission nodes. |
| [Configuration Reference](configuration.md) | `config.yml` display, follow, riding, database, and combat keys. |
| [Pet Configuration](pets.md) | Pet IDs, textures, stats, XP, attacks, and riding. |
| [Upgrade System](upgrades.md) | Upgrade levels, PAPI checks, friendly requirement display, and commands. |
| [GUI Configuration](gui.md) | Item meta, pet display values, placeholders, and per-pet/per-upgrade overrides. |
| [Display Conditions](display-conditions.md) | GUI condition syntax like `active_pet`, `rideable`, and permissions. |
| [Troubleshooting](troubleshooting.md) | Common visual, config, and PlaceholderAPI problems. |

## Important Files

| File | Purpose |
| --- | --- |
| `config.yml` | Core plugin settings, database, display offsets, riding physics, follow behavior, combat throttle. |
| `pets.yml` + `pets/*.yml` | Pet order plus individual pet definitions, textures, stats, attack behavior, riding support, upgrade definitions, and abilities. |
| `gui.yml` | Every GUI item, GUI item meta, pet display value, upgrade display template, and display condition. |
| `messages.yml` | Player-facing messages, menu titles, and status labels. |
| `paper-plugin.yml` | Plugin metadata and dependency declarations. |

## Color Formats

Most text supports both legacy color codes and MiniMessage:

```yaml
name: "&eMonkey King"
name: "<gold><bold>Monkey King"
name: "<gradient:#ff5500:#ffaa00>Ember Core</gradient>"
```

Prefer MiniMessage for new configuration because it is more readable and supports gradients.

## Quick Upgrade Example

This checks whether the player has 20 points, shows a friendly GUI line, and removes 20 points after a successful upgrade.

```yaml
requirement:
  papi: "%playerpoints_points%"
  compare: ">="
  value: "20"
  display: "You need 20 points"
commands:
  - "points take <player> 20"
```

In `gui.yml`, use `<requirement>` to show the friendly line. Use `<raw_requirement>` only when you intentionally want to show `%playerpoints_points% >= 20`.
