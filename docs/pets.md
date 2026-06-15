# Pet Configuration

Pets are configured in separate files under the `pets/` folder. `pets.yml` is now an order index, so collection paging
follows the same order after reloads.

## Basic Structure

`pets.yml`:

```yaml
pets:
  - MONKEY
  - WOLF
  - PHOENIX
```

`pets/phoenix.yml`:

```yaml
id: PHOENIX
name: "&cPhoenix Ember"
texture: "base64 texture"
stats:
  MAGIC_DAMAGE:
    base: 0.0
    max-value: 100.0
    formula: "3 + (level * 0.8)"
max_xp_formula: "150 + (level * 75)"
attack:
  range: 5.5
  cooldown: 1.2
  damage_formula: "12 + (level * 3.5)"
  inheritance: 0.55
  particle: "FLAME"
ride:
  enabled: true
  can_fly: true
```

The old monolithic `pets.yml` format with a `pets.<id>` section is still supported for existing servers.

## Pet ID

`id: PHOENIX` is the pet ID. It is used for:

- Permissions: `sincepet.pet.phoenix`
- Admin commands
- Saved player data

Do not rename pet IDs on a live server unless you are okay with old saved levels no longer matching the renamed pet.

## Texture

`texture` is a base64 Minecraft head texture.

If the texture is empty, the GUI falls back to a paper item for collection icons.

## Stat Bonus

```yaml
stats:
  MAGIC_DAMAGE:
    base: 0.0
    max-value: 100.0
    formula: "3 + (level * 0.8)"
  MAX_HEALTH:
    base: 10.0
    max-value: 200.0
    formula: "level * 2"
```

`stats` is a map where the key is a MythicLib stat name. Each stat contains:
- `base`: The flat base value.
- `max-value`: (Optional) The maximum cap this stat can reach, even with upgrades. Omit or leave empty for no limit.
- `formula`: The calculation formula based on pet level.

The formula currently uses `level` as the variable. The Java code internally replaces `<level>` too, so both of these
styles are accepted by the current calculator usage:

```yaml
stats:
  MAGIC_DAMAGE:
    formula: "2 + (level * 1)"
  MAX_HEALTH:
    formula: "2 + (<level> * 1)"
```

Note: The system still supports backwards compatibility if you write `MAGIC_DAMAGE: "2 + (level * 1)"` directly. However, using `base` and `max-value` offers more precise balancing.

## Class Restriction

```yaml
mmocore_classes:
  - "WARRIOR"
  - "ROGUE"
```

The `mmocore_classes` list allows you to optionally restrict this pet to specific MMOCore classes. Only players whose
current MMOCore class matches one of the classes in this list will be allowed to spawn and use the pet. Leave the list
empty or omit it entirely to allow all classes.

## XP Formula

```yaml
max_xp_formula: "150 + (level * 75)"
```

This controls the XP required for the next pet level.

## Upgrading Points

```yaml
upgrading_points:
  levels_per_point: 5       # Pet gains points every 5 levels
  points_per_interval: 1    # Pet gains 1 point
  max_points: 24            # Maximum points this pet can hold
```

This section is entirely optional and gives each pet an independent internal point system. 
As pets level up, they automatically accumulate "Upgrading Points" based on this formula. 
Players can spend these points on upgrades in the pet's skill tree without using PlaceholderAPI economies.

If omitted, it defaults to `levels_per_point: 5`, `points_per_interval: 1`, `max_points: 24`.

## Attack

```yaml
attack:
  range: 5.5
  cooldown: 1.2
  damage_formula: "12 + (level * 3.5)"
  inheritance: 0.55
  particle: "FLAME"
```

| Key              | Description                                                                     |
|------------------|---------------------------------------------------------------------------------|
| `range`          | Search radius for hostile monsters. Set to `0` to disable attacks.              |
| `cooldown`       | Seconds between attacks.                                                        |
| `damage_formula` | Pet base damage formula.                                                        |
| `inheritance`    | Portion of the owner's MythicLib damage inherited by the pet. `0.55` means 55%. |
| `particle`       | The Bukkit particle effect shown when the pet attacks.                          |

## Ride

```yaml
ride:
  enabled: true
  can_fly: true
```

`enabled` controls whether the pet can be ridden. `can_fly` allows flying movement while riding, if WorldGuard also
allows pet flying in the current region.

## Abilities

Pets can optionally trigger configured MythicMobs skills from the `abilities` section. The skill is cast by the player
who has the pet equipped, so the ability behaves like it was transferred to the owner while that pet is active.

Because there are many available triggers, this section has been moved to its own page:

**[Read about Pet Abilities and Triggers here.](pet-abilities.md)**

## Example Pets

Here is a list of the 14 default pet examples included in `pets.yml`:

1. **MONKEY** (`Monkey King`): Primary physical damage dealer.
2. **WOLF** (`Wolf Spirit`): Increases critical strike chance, rapid attacks.
3. **PHOENIX** (`Phoenix Ember`): Magic damage dealer, rideable and can fly.
4. **TURTLE** (`Ancient Turtle`): Tank pet, significantly increases max health.
5. **DRAGON** (`Ender Dragon`): High magic damage and inheritance, powerful flying mount.
6. **TIGER** (`Bengal Tiger`): Very fast attacker with physical damage bonuses.
7. **BEAR** (`Grizzly Bear`): Hybrid tank/damage dealer.
8. **PANDA** (`Bamboo Panda`): Support pet, provides health regeneration.
9. **EAGLE** (`Sky Eagle`): Sniper pet with very long range and critical strike power.
10. **UNICORN** (`Mystic Unicorn`): Fast ground mount that boosts movement speed.
11. **SLOTH** (`Sleepy Sloth`): Defensive pet, triggered by sneaking or sleeping.
12. **FROG** (`Swamp Frog`): Jump and swim boosting pet.
13. **FOX** (`Swift Fox`): Agile pet that dashes on sprint and heals when eating.
14. **PARROT** (`Pirate Parrot`): Flying pet that mimics chat and screeches on command.
