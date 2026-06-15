# Display Conditions

GUI items can be hidden with display conditions.

```yaml
display:
  conditions:
    - "active_pet"
```

Conditions are checked when rendering the item and again when clicking the slot. Hidden actions cannot be triggered by
clicking an empty slot.

## Conditions

| Condition                 | Meaning                              |
|---------------------------|--------------------------------------|
| `active_pet`              | Player has an active pet.            |
| `no_active_pet`           | Player does not have an active pet.  |
| `rideable`                | Active pet can be ridden.            |
| `has_upgrades`            | Active pet has at least one upgrade. |
| `permission:<permission>` | Player has the permission.           |
| `setting:<setting_id>`    | Active pet setting is enabled.       |

## Negating Conditions

Prefix a condition with `!` to invert it:

```yaml
display:
  conditions:
    - "!active_pet"
```

This item displays only when the player has no active pet.

## Examples

Only show ride when the active pet can be ridden:

```yaml
display:
  conditions:
    - "active_pet"
    - "rideable"
```

Only show an admin button to staff:

```yaml
display:
  conditions:
    - "permission:sincepet.admin"
```

Only show an item when auto attack is enabled:

```yaml
display:
  conditions:
    - "setting:auto_attack"
```
