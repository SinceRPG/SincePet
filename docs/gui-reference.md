# GUI Reference

This page contains reference lists for placeholders and bundled display mappings used in the GUI configuration.

## Collection Pet Placeholders

| Placeholder                          | Meaning                                                          |
|--------------------------------------|------------------------------------------------------------------|
| `<pet>` / `<name>`                   | Pet display name.                                                |
| `<id>` / `<pet_id>`                  | Pet ID from `pets/*.yml`.                                        |
| `<pet_display>` / `<pet_id_display>` | Friendly pet name from `display_values.pets`.                    |
| `<level>`                            | Player's current pet level.                                      |
| `<upgrading_points>`                 | Available pet upgrading points.                                  |
| `<max_upgrading_points>`             | Total pet upgrading points earned over all levels.               |
| `<stat>`                             | Pet stat from config.                                            |
| `<value>` / `<stat_bonus>`           | Current stat bonus from the pet formula.                         |
| `<formula>`                          | Raw stat formula.                                                |
| `<inheritance>`                      | Damage inheritance percent.                                      |
| `<skills>`                           | All enabled abilities using `collection.pet_item.skills.format`. |
| `<active_skills>`                    | Enabled active abilities only.                                   |
| `<passive_skills>`                   | Enabled passive abilities only.                                  |
| `<status>`                           | Configured status text.                                          |
| `<texture>`                          | Raw pet head texture from `pets/*.yml`.                          |

## Upgrade Placeholders

| Placeholder           | Meaning                                               |
|-----------------------|-------------------------------------------------------|
| `<pet>`               | Pet display name.                                     |
| `<pet_id>`            | Pet ID from `pets.yml`.                               |
| `<upgrade>`           | Upgrade display name.                                 |
| `<upgrade_id>`        | Upgrade ID from `pets.yml`.                           |
| `<upgrading_points>`  | Available pet upgrading points.                       |
| `<max_upgrading_points>`| Total pet upgrading points earned over all levels.    |
| `<level>`             | Current upgrade level.                                |
| `<next_level>`        | Next upgrade level.                                   |
| `<max_level>`         | Max upgrade level.                                    |
| `<papi>`              | Raw PlaceholderAPI requirement string.                |
| `<compare>`           | Raw compare operator.                                 |
| `<value>`             | Raw required value.                                   |
| `<current_value>`     | Player's resolved PlaceholderAPI value.               |
| `<requirement>`       | Friendly `requirement.display` text from `pets.yml`.  |
| `<raw_requirement>`   | Raw condition such as `%playerpoints_points% >= 20`.  |
| `<state>`             | `available`, `locked`, or `maxed`.                    |
| `<stat_bonus>`        | Current stat upgrade bonus.                           |
| `<next_stat_bonus>`   | Next level stat upgrade bonus.                        |
| `<damage_bonus>`      | Current damage upgrade bonus.                         |
| `<next_damage_bonus>` | Next level damage upgrade bonus.                      |
| `<commands>`          | Commands configured for the upgrade, joined by comma. |

## Bundled Display Keys

Default `gui.yml` includes mappings for these bundled pets:

| Pet ID    | Friendly Display |
|-----------|------------------|
| `BEAR`    | Grizzly Bear     |
| `DRAGON`  | Ender Dragon     |
| `EAGLE`   | Sky Eagle        |
| `FOX`     | Swift Fox        |
| `FROG`    | Swamp Frog       |
| `MONKEY`  | Monkey King      |
| `PANDA`   | Bamboo Panda     |
| `PARROT`  | Pirate Parrot    |
| `PHOENIX` | Phoenix Ember    |
| `SLOTH`   | Sleepy Sloth     |
| `TIGER`   | Bengal Tiger     |
| `TURTLE`  | Ancient Turtle   |
| `UNICORN` | Mystic Unicorn   |
| `WOLF`    | Wolf Spirit      |

Default stat display mappings:

| Stat Key                 | Friendly Display       |
|--------------------------|------------------------|
| `CRITICAL_STRIKE_CHANCE` | Critical Strike Chance |
| `CRITICAL_STRIKE_POWER`  | Critical Strike Power  |
| `HEALTH`                 | Health                 |
| `HEALTH_REGENERATION`    | Health Regeneration    |
| `MAGIC_DAMAGE`           | Magic Damage           |
| `MAX_HEALTH`             | Max Health             |
| `MOVEMENT_SPEED`         | Movement Speed         |
| `PHYSICAL_DAMAGE`        | Physical Damage        |

Default trigger display mappings:

| Trigger Key               | Friendly Display        |
|---------------------------|-------------------------|
| `ACTIVE`                  | Command                 |
| `BED_ENTER`               | Bed Enter               |
| `CHAT`                    | Chat                    |
| `DOUBLE_SNEAK`            | Double Sneak            |
| `FLIGHT_START`            | Flight Start            |
| `GAMEMODE_CHANGE`         | Gamemode Change         |
| `GLIDE_START`             | Glide Start             |
| `HARVEST_BLOCK`           | Harvest Block           |
| `ITEM_CONSUME`            | Item Consume            |
| `JUMP`                    | Jump                    |
| `MOVE`                    | Move                    |
| `OWNER_DAMAGED`           | Owner Damaged           |
| `OWNER_DAMAGED_BY_ENTITY` | Owner Damaged By Entity |
| `OWNER_RESPAWN`           | Owner Respawn           |
| `PHYSICAL_INTERACT`       | Physical Interact       |
| `PROJECTILE_LAUNCH`       | Projectile Launch       |
| `RIGHT_CLICK_AIR`         | Right Click             |
| `RIPTIDE`                 | Riptide                 |
| `SNEAK`                   | Sneak                   |
| `SNEAK_START`             | Sneak Start             |
| `SPRINT_START`            | Sprint Start            |
| `SWAP_HAND`               | Swap Hand               |
| `SWIM_START`              | Swim Start              |
| `WORLD_CHANGE`            | World Change            |

Default ability display mappings:

| Ability Key      | Friendly Display |
|------------------|------------------|
| `bamboo_snack`   | Bamboo Snack     |
| `banana_toss`    | Banana Toss      |
| `berry_eat`      | Berry Eat        |
| `breath`         | Dragon Breath    |
| `counter_howl`   | Counter Howl     |
| `dash`           | Dash             |
| `dive`           | Dive             |
| `dragon_scales`  | Dragon Scales    |
| `eagle_eye`      | Eagle Eye        |
| `fireball`       | Fireball         |
| `hibernation`    | Hibernation      |
| `horn_pierce`    | Horn Pierce      |
| `jump_boost`     | Jump Boost       |
| `magic_aura`     | Magic Aura       |
| `maul`           | Maul             |
| `mimic`          | Mimic            |
| `monkey_agility` | Monkey Agility   |
| `rebirth`        | Rebirth          |
| `rift_bite`      | Rift Bite        |
| `roar`           | Roar             |
| `roll`           | Roll             |
| `screech`        | Screech          |
| `shell_shield`   | Shell Shield     |
| `sleepy_heal`    | Sleepy Heal      |
| `swim_boost`     | Swim Boost       |
| `takeoff`        | Takeoff          |
| `tiger_pounce`   | Tiger Pounce     |
| `water_spout`    | Water Spout      |
| `yawn`           | Yawn             |

Default MythicMobs skill display mappings:

| Mythic Skill Key   | Friendly Display |
|--------------------|------------------|
| `ARCANE_HAIL`      | Arcane Hail      |
| `ARCANE_RIFT`      | Arcane Rift      |
| `BANANA_TOSS`      | Banana Toss      |
| `BEAR_HIBERNATION` | Bear Hibernation |
| `BEAR_MAUL`        | Bear Maul        |
| `DRAGON_BREATH`    | Dragon Breath    |
| `DRAGON_SCALES`    | Dragon Scales    |
| `EAGLE_DIVE`       | Eagle Dive       |
| `EAGLE_EYE`        | Eagle Eye        |
| `FOX_DASH`         | Fox Dash         |
| `FOX_HEAL`         | Fox Heal         |
| `FROG_JUMP`        | Frog Jump        |
| `FROG_SWIM`        | Frog Swim        |
| `MONKEY_AGILITY`   | Monkey Agility   |
| `PANDA_EAT`        | Panda Eat        |
| `PANDA_ROLL`       | Panda Roll       |
| `PARROT_FLY`       | Parrot Fly       |
| `PARROT_MIMIC`     | Parrot Mimic     |
| `PARROT_SCREECH`   | Parrot Screech   |
| `PHOENIX_FIREBALL` | Phoenix Fireball |
| `PHOENIX_REBIRTH`  | Phoenix Rebirth  |
| `SLOTH_HEAL`       | Sleepy Heal      |
| `SLOTH_YAWN`       | Yawn             |
| `TIGER_POUNCE`     | Tiger Pounce     |
| `TIGER_ROAR`       | Tiger Roar       |
| `TURTLE_SHIELD`    | Turtle Shield    |
| `TURTLE_SPOUT`     | Turtle Spout     |
| `UNICORN_AURA`     | Unicorn Aura     |
| `UNICORN_PIERCE`   | Unicorn Pierce   |
