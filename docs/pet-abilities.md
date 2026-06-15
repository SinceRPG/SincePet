# Pet Abilities

Pets can optionally trigger configured MythicMobs skills from the `abilities` section. The skill is cast by the player
who has the pet equipped, so the ability behaves like it was transferred to the owner while that pet is active.

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

`mythicmobs_skill` must match a MythicMobs skill ID. Active abilities can be intentionally triggered with `/pet skill`,
`/pet skill <ability>`, or non-command player actions listed in `triggers`. Passive abilities run when one of their
configured event triggers happens.

## Triggers

| Trigger                                          | Description                                                                |
|--------------------------------------------------|----------------------------------------------------------------------------|
| **System & Pet Mechanics**                       |                                                                            |
| `ACTIVE`                                         | The player uses `/pet skill`. Active abilities receive this automatically. |
| `EQUIP`                                          | The pet is summoned or restored as the active pet.                         |
| `PET_ATTACK`                                     | The pet lands its configured auto attack on a monster.                     |
| **Movement & Actions**                           |                                                                            |
| `MOVE`                                           | The player moves to a different block.                                     |
| `JUMP`                                           | The player jumps.                                                          |
| `SNEAK`                                          | The player starts sneaking.                                                |
| `DOUBLE_SNEAK`                                   | The player sneaks twice within 0.5 seconds.                                |
| `SNEAK_START` / `SNEAK_STOP`                     | The player sneaks or stops sneaking.                                       |
| `SPRINT_START` / `SPRINT_STOP`                   | The player starts or stops sprinting.                                      |
| `FLIGHT_START` / `FLIGHT_STOP`                   | The player starts or stops flying.                                         |
| `GLIDE_START` / `GLIDE_STOP`                     | The player starts or stops gliding (Elytra).                               |
| `SWIM_START` / `SWIM_STOP`                       | The player starts or stops swimming.                                       |
| `SWAP_HAND`                                      | The player presses the swap-hand key (usually `F`).                        |
| `RIPTIDE`                                        | The player uses a Riptide trident.                                         |
| **Combat & Health**                              |                                                                            |
| `OWNER_DAMAGED`                                  | The pet owner takes any damage.                                            |
| `OWNER_ATTACK`                                   | The owner attacks any entity.                                              |
| `OWNER_ATTACK_PLAYER`                            | The owner attacks a player.                                                |
| `OWNER_ATTACK_MONSTER`                           | The owner attacks a monster.                                               |
| `OWNER_KILL`                                     | The owner kills any entity.                                                |
| `OWNER_KILL_PLAYER` / `_MONSTER`                 | The owner kills a player / monster.                                        |
| `OWNER_DEATH`                                    | The owner dies.                                                            |
| `OWNER_RESURRECT`                                | The owner uses a Totem of Undying.                                         |
| `OWNER_HEAL`                                     | The owner regenerates health.                                              |
| `TARGETED`                                       | A mob targets the owner.                                                   |
| `SHOOT_BOW`                                      | The owner shoots a bow.                                                    |
| `PROJECTILE_LAUNCH`                              | The owner launches a projectile (snowball, egg, etc).                      |
| `PROJECTILE_HIT`                                 | The owner's projectile hits something.                                     |
| **Interaction & Items**                          |                                                                            |
| `LEFT_CLICK` / `RIGHT_CLICK`                     | The owner clicks air or block.                                             |
| `INTERACT` / `PHYSICAL_INTERACT`                 | The owner interacts (e.g. pressure plate).                                 |
| `RIGHT_CLICK_ENTITY`                             | The owner right clicks an entity.                                          |
| `SWING`                                          | The owner swings their arm.                                                |
| `DROP_ITEM` / `ITEM_PICKUP`                      | The owner drops or picks up an item.                                       |
| `HOTBAR_CHANGE`                                  | The owner switches hotbar slots.                                           |
| `ITEM_CONSUME`                                   | The owner eats or drinks.                                                  |
| `ITEM_BREAK` / `ITEM_DAMAGE` / `ITEM_MEND`       | The owner's item breaks, takes durability damage, or mends.                |
| `ENCHANT_ITEM`                                   | The owner enchants an item.                                                |
| `INVENTORY_OPEN` / `INVENTORY_CLOSE`             | The owner opens or closes an inventory.                                    |
| **Blocks & Environment**                         |                                                                            |
| `BLOCK_BREAK` / `BLOCK_PLACE`                    | The owner breaks or places a block.                                        |
| `HARVEST_BLOCK`                                  | The owner harvests berries/crops.                                          |
| `FERTILIZE_BLOCK`                                | The owner uses bonemeal.                                                   |
| `BLOCK_DAMAGE`                                   | The owner starts mining a block.                                           |
| `BLOCK_IGNITE`                                   | The owner lights a fire.                                                   |
| **Entities & World**                             |                                                                            |
| `TAME_ENTITY` / `BREED_ENTITY`                   | The owner tames or breeds an animal.                                       |
| `LEASH_ENTITY` / `UNLEASH_ENTITY`                | The owner leashes or unleashes an entity.                                  |
| `SHEAR_ENTITY`                                   | The owner shears a sheep/mooshroom.                                        |
| `BUCKET_FILL` / `BUCKET_EMPTY` / `BUCKET_ENTITY` | The owner uses a bucket (or catches fish/axolotl).                         |
| `VEHICLE_ENTER` / `VEHICLE_EXIT`                 | The owner enters or leaves a vehicle/mount.                                |
| `BED_ENTER` / `BED_LEAVE`                        | The owner enters or leaves a bed.                                          |
| `PORTAL` / `TELEPORT` / `WORLD_CHANGE`           | The owner teleports or changes dimension.                                  |
| `FISH`                                           | The owner fishes.                                                          |
| **Misc & Stats**                                 |                                                                            |
| `JOIN` / `QUIT`                                  | The owner joins or leaves the server.                                      |
| `RESPAWN`                                        | The owner respawns.                                                        |
| `GAMEMODE_CHANGE`                                | The owner changes gamemode.                                                |
| `COMMAND`                                        | The owner runs a command.                                                  |
| `CHAT`                                           | The owner sends a chat message.                                            |
| `STATISTIC_INCREMENT`                            | A player statistic increases.                                              |
| `ADVANCEMENT_DONE`                               | The owner completes an advancement.                                        |
| `RECIPE_DISCOVER`                                | The owner discovers a recipe.                                              |
| `FOOD_CHANGE`                                    | The owner's hunger level changes.                                          |
| `EXP_GAIN` / `LEVEL_UP` / `LEVEL_DOWN`           | The owner's XP or level changes.                                           |
| `POTION_EFFECT`                                  | The owner receives or loses a potion effect.                               |

For upgrade compatibility, existing `skills.active-skills`, `skills.passive-skills`, `source`, `ATTACK`, and `DAMAGED`
entries are still read, but new configs should use `abilities`, `mythicmobs_skill`, `PET_ATTACK`, and `OWNER_DAMAGED`.

Pet collection lore can show configured abilities with `<skills>`, `<active_skills>`, and `<passive_skills>` in
`messages.yml`.

For drag-and-drop installs, the default `gui.yml` already contains friendly display mappings for every bundled pet,
stat, ability, MythicMobs skill, and trigger. Keep IDs like `SLOTH`, `HEALTH`, and `DOUBLE_SNEAK` stable in pet files,
then customize the player-facing wording under `collection.pet_item.display_values`
in [GUI Configuration](gui.md#collection-display-values).
