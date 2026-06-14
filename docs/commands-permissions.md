# Commands and Permissions

## Player Commands

| Command | Description |
| --- | --- |
| `/pet` | Opens the pet collection GUI. |
| `/pet ride` | Rides the active pet if the pet is rideable and the player has permission. |
| `/pet skill [ability]` | Uses the active pet's first command-triggered active ability, or a specific ability ID. |

## Admin Commands

| Command                                             | Description |
|-----------------------------------------------------| --- |
| `/sincepet reload`                                  | Reloads plugin configuration files and refreshes active pets. |
| `/sincepet levelup <target>`                        | Levels up the target player's active pet. |
| `/sincepet max_level <target> <pet/petall> <level>` | Sets the max level for one pet or all pets for the target player. |

## Permissions

| Permission | Description |
| --- | --- |
| `sincepet.admin` | Allows use of `/sincepet` admin commands. |
| `sincepet.pet.hasall` | Allows the player to see, summon, and restore all pets without per-pet nodes. |
| `sincepet.pet.<pet_id>` | Allows the player to see, summon, and restore a specific pet. Example: `sincepet.pet.monkey`. |
| `sincepet.ride` | Allows riding pets through `/pet ride`, the GUI ride button, and right-clicking the active pet. |
| `sincepet.skill` | Allows using active pet abilities through `/pet skill`. |

Pet IDs are read from `pets/*.yml`, and collection listings follow the order in `pets.yml`.
