# Messages

`messages.yml` contains player-facing messages and GUI titles.

## Prefix

```yaml
prefix: "<gray>[<gradient:#00FFAE:#00E0FF><bold>SincePet</bold></gradient><gray>] "
```

Messages sent through `ColorUtils.parseWithPrefix` use this prefix.

## Admin Messages

```yaml
admin:
  reload: "<green>Successfully reloaded configuration files!"
```

## Command Messages

```yaml
pet:
  command:
    spawn: "<green>Successfully summoned <name><green>!"
    despawn: "<yellow>Pet despawned and secured."
```

Common placeholders:

| Placeholder | Meaning |
| --- | --- |
| `<name>` | Pet display name. |
| `<level>` | Pet level. |
| `<target>` | Target player. |
| `<pet>` | Pet ID or display label. |

## Display Name Format

```yaml
pet:
  display:
    name_format: "<name> <gray>[Lv.<level>]"
```

This controls the separate `TextDisplay` above the pet.

## GUI Titles

```yaml
pet:
  gui:
    title: "<black><bold>Pet Collection <gray>(Page <page>)"
    detail_title: "<black><bold>Pet Detail: <name>"
    settings_title: "<black><bold>Pet Settings"
    upgrades_title: "<black><bold>Pet Upgrades"
```

GUI item lore is configured in `gui.yml`, not `messages.yml`.
