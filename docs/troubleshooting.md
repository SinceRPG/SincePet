# Troubleshooting

## The pet name appears in the wrong place

The pet name uses a separate `TextDisplay`.

Adjust:

```yaml
pet:
  display:
    name_offset_y: 1.25
    ride_name_offset_y: 0.25
```

## The pet visual is on the player's head while riding

Adjust:

```yaml
pet:
  display:
    ride_offset_y: -1.35
```

More negative values move the visual lower under the rider.

## Upgrade GUI shows `%playerpoints_points% >= 20`

Add a friendly display line in `pets.yml`:

```yaml
requirement:
  papi: "%playerpoints_points%"
  compare: ">="
  value: "20"
  display: "You need 20 points"
```

Then use `<requirement>` in `gui.yml`:

```yaml
upgrade_items:
  locked:
    lore:
      - "<red><requirement>"
```

Use `<raw_requirement>` only if you intentionally want to show the raw condition.

## PlaceholderAPI values always resolve to 0

Check that PlaceholderAPI is installed and that the placeholder expansion/plugin is installed.

For PlayerPoints, confirm that `%playerpoints_points%` works with `/papi parse`.

## GUI item does not show

Check its display conditions in `gui.yml`.

Example:

```yaml
display:
  conditions:
    - "active_pet"
```

This item only appears if the player has an active pet.

## Existing YAML files do not contain new keys

Bukkit does not merge new resource keys into existing config files. Compare your generated files with the repository files and copy new sections manually.
