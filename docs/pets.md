# Pet Configuration

Pets are configured in separate files under the `pets/` folder. `pets.yml` is now an order index, so collection paging follows the same order after reloads.

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

## Abilities

Pets can optionally trigger configured MythicMobs skills from the `abilities` section. The skill is cast by the player who has the pet equipped, so the ability behaves like it was transferred to the owner while that pet is active.

```yaml
abilities:
  passive:
    counter_howl:
      enabled: true
      mythicmobs_skill: "ARCANE_HAIL"
      cooldown: 8
      triggers:
        - "OWNER_DAMAGED"
  active:
    rift_bite:
      enabled: true
      mythicmobs_skill: "ARCANE_RIFT"
      cooldown: 12
      triggers:
        - "ACTIVE"
        - "SWAP_HAND"
        - "DOUBLE_SNEAK"
```

`mythicmobs_skill` must match a MythicMobs skill ID. Active abilities can be intentionally triggered with `/pet skill`, `/pet skill <ability>`, or non-command player actions listed in `triggers`. Passive abilities run when one of their configured event triggers happens.

| Trigger | Description |
| --- | --- |
| **System & Pet Mechanics** | |
| `ACTIVE` | The player uses `/pet skill`. Active abilities receive this automatically. |
| `EQUIP` | The pet is summoned or restored as the active pet. |
| `PET_ATTACK` | The pet lands its configured auto attack on a monster. |
| **Movement & Actions** | |
| `MOVE` | The player moves to a different block. |
| `JUMP` | The player jumps. |
| `SNEAK` | The player starts sneaking. |
| `DOUBLE_SNEAK` | The player sneaks twice within 0.5 seconds. |
| `SNEAK_START` / `SNEAK_STOP` | The player sneaks or stops sneaking. |
| `SPRINT_START` / `SPRINT_STOP` | The player starts or stops sprinting. |
| `FLIGHT_START` / `FLIGHT_STOP` | The player starts or stops flying. |
| `GLIDE_START` / `GLIDE_STOP` | The player starts or stops gliding (Elytra). |
| `SWIM_START` / `SWIM_STOP` | The player starts or stops swimming. |
| `SWAP_HAND` | The player presses the swap-hand key (usually `F`). |
| `RIPTIDE` | The player uses a Riptide trident. |
| **Combat & Health** | |
| `OWNER_DAMAGED` | The pet owner takes any damage. |
| `OWNER_ATTACK` | The owner attacks any entity. |
| `OWNER_ATTACK_PLAYER` | The owner attacks a player. |
| `OWNER_ATTACK_MONSTER` | The owner attacks a monster. |
| `OWNER_KILL` | The owner kills any entity. |
| `OWNER_KILL_PLAYER` / `_MONSTER` | The owner kills a player / monster. |
| `OWNER_DEATH` | The owner dies. |
| `OWNER_RESURRECT` | The owner uses a Totem of Undying. |
| `OWNER_HEAL` | The owner regenerates health. |
| `TARGETED` | A mob targets the owner. |
| `SHOOT_BOW` | The owner shoots a bow. |
| `PROJECTILE_LAUNCH` | The owner launches a projectile (snowball, egg, etc). |
| `PROJECTILE_HIT` | The owner's projectile hits something. |
| **Interaction & Items** | |
| `LEFT_CLICK` / `RIGHT_CLICK` | The owner clicks air or block. |
| `INTERACT` / `PHYSICAL_INTERACT` | The owner interacts (e.g. pressure plate). |
| `RIGHT_CLICK_ENTITY` | The owner right clicks an entity. |
| `SWING` | The owner swings their arm. |
| `DROP_ITEM` / `ITEM_PICKUP` | The owner drops or picks up an item. |
| `HOTBAR_CHANGE` | The owner switches hotbar slots. |
| `ITEM_CONSUME` | The owner eats or drinks. |
| `ITEM_BREAK` / `ITEM_DAMAGE` / `ITEM_MEND` | The owner's item breaks, takes durability damage, or mends. |
| `ENCHANT_ITEM` | The owner enchants an item. |
| `INVENTORY_OPEN` / `INVENTORY_CLOSE` | The owner opens or closes an inventory. |
| **Blocks & Environment** | |
| `BLOCK_BREAK` / `BLOCK_PLACE` | The owner breaks or places a block. |
| `HARVEST_BLOCK` | The owner harvests berries/crops. |
| `FERTILIZE_BLOCK` | The owner uses bonemeal. |
| `BLOCK_DAMAGE` | The owner starts mining a block. |
| `BLOCK_IGNITE` | The owner lights a fire. |
| **Entities & World** | |
| `TAME_ENTITY` / `BREED_ENTITY` | The owner tames or breeds an animal. |
| `LEASH_ENTITY` / `UNLEASH_ENTITY` | The owner leashes or unleashes an entity. |
| `SHEAR_ENTITY` | The owner shears a sheep/mooshroom. |
| `BUCKET_FILL` / `BUCKET_EMPTY` / `BUCKET_ENTITY` | The owner uses a bucket (or catches fish/axolotl). |
| `VEHICLE_ENTER` / `VEHICLE_EXIT` | The owner enters or leaves a vehicle/mount. |
| `BED_ENTER` / `BED_LEAVE` | The owner enters or leaves a bed. |
| `PORTAL` / `TELEPORT` / `WORLD_CHANGE` | The owner teleports or changes dimension. |
| `FISH` | The owner fishes. |
| **Misc & Stats** | |
| `JOIN` / `QUIT` | The owner joins or leaves the server. |
| `RESPAWN` | The owner respawns. |
| `GAMEMODE_CHANGE` | The owner changes gamemode. |
| `COMMAND` | The owner runs a command. |
| `CHAT` | The owner sends a chat message. |
| `STATISTIC_INCREMENT` | A player statistic increases. |
| `ADVANCEMENT_DONE` | The owner completes an advancement. |
| `RECIPE_DISCOVER` | The owner discovers a recipe. |
| `FOOD_CHANGE` | The owner's hunger level changes. |
| `EXP_GAIN` / `LEVEL_UP` / `LEVEL_DOWN` | The owner's XP or level changes. |
| `POTION_EFFECT` | The owner receives or loses a potion effect. |

For upgrade compatibility, existing `skills.active-skills`, `skills.passive-skills`, `source`, `ATTACK`, and `DAMAGED` entries are still read, but new configs should use `abilities`, `mythicmobs_skill`, `PET_ATTACK`, and `OWNER_DAMAGED`.

Pet collection lore can show configured abilities with `<skills>`, `<active_skills>`, and `<passive_skills>` in `messages.yml`.

For drag-and-drop installs, the default `gui.yml` already contains friendly display mappings for every bundled pet, stat, ability, MythicMobs skill, and trigger. Keep IDs like `SLOTH`, `HEALTH`, and `DOUBLE_SNEAK` stable in pet files, then customize the player-facing wording under `collection.pet_item.display_values` in [GUI Configuration](gui.md#collection-display-values).

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
