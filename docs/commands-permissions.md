# Commands and Permissions

## Player Commands

| Command | Description |
| --- | --- |
| `/pet` | Opens the pet collection GUI. |
| `/pet ride` | Rides the active pet if the pet is rideable and the player has permission. |

## Admin Commands

| Command | Description |
| --- | --- |
| `/sincepet reload` | Reloads plugin configuration files and refreshes active pets. |
| `/sincepet levelup <target>` | Levels up the target player's active pet. |
| `/sincepet max_level <target> <pet|petall> <level>` | Sets the max level for one pet or all pets for the target player. |

## Permissions

| Permission | Description |
| --- | --- |
| `sincepet.admin` | Allows use of `/sincepet` admin commands. |
| `pet.hasall` | Allows the player to see all pets in the pet collection GUI. |
| `pet.<pet_id>` | Allows the player to see and summon a specific pet. Example: `pet.MONKEY`. |
| `pets.ride` | Allows `/pet ride`. |

Pet IDs are read from `pets.yml`.
