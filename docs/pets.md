# Pet Configuration

Pets are configured in `pets.yml` under the `pets` section.

## Basic Structure

```yaml
pets:
  PHOENIX:
    name: "&cPhoenix Ember"
    texture: "base64 texture"
    stat: "MAGIC_DAMAGE"
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

## Pet ID

`PHOENIX` is the pet ID. It is used for:

- Permissions: `pet.PHOENIX`
- Admin commands
- Saved player data

Do not rename pet IDs on a live server unless you are okay with old saved levels no longer matching the renamed pet.

## Texture

`texture` is a base64 Minecraft head texture.

If the texture is empty, the GUI falls back to a paper item for collection icons.

## Stat Bonus

```yaml
stat: "MAGIC_DAMAGE"
formula: "3 + (level * 0.8)"
```

`stat` must be a MythicLib stat name. `formula` calculates the flat stat value granted by the pet.

The formula currently uses `level` as the variable. The Java code internally replaces `<level>` too, so both of these styles are accepted by the current calculator usage:

```yaml
formula: "2 + (level * 1)"
formula: "2 + (<level> * 1)"
```

## XP Formula

```yaml
max_xp_formula: "150 + (level * 75)"
```

This controls the XP required for the next pet level.

## Attack

```yaml
attack:
  range: 5.5
  cooldown: 1.2
  damage_formula: "12 + (level * 3.5)"
  inheritance: 0.55
  particle: "FLAME"
```

| Key | Description |
| --- | --- |
| `range` | Search radius for hostile monsters. Set to `0` to disable attacks. |
| `cooldown` | Seconds between attacks. |
| `damage_formula` | Pet base damage formula. |
| `inheritance` | Portion of the owner's MythicLib damage inherited by the pet. `0.55` means 55%. |
| `particle` | The Bukkit particle effect shown when the pet attacks. |

## Ride

```yaml
ride:
  enabled: true
  can_fly: true
```

`enabled` controls whether the pet can be ridden. `can_fly` allows flying movement while riding, if WorldGuard also allows pet flying in the current region.

## Example Pets

Here is a list of the 10 default pet examples included in `pets.yml`:

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
