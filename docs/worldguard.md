# WorldGuard Integration

SincePet registers and checks custom WorldGuard flags.

## Pet Flags

| Flag Purpose | Effect                                                              |
|--------------|---------------------------------------------------------------------|
| Pet spawn    | Controls whether pets can be summoned or remain active in a region. |
| Pet ride     | Controls whether players can ride pets in a region.                 |
| Pet fly      | Controls whether flying pets can fly in a region.                   |
| Pet attack   | Controls whether pets can auto attack in a region.                  |
| Pet buff     | Controls whether MythicLib stat bonuses apply in a region.          |

When a player enters or leaves a restricted region, notification messages are read from `messages.yml`.

## Behavior

If pet spawning becomes denied in the current region, the active pet is despawned.

If pet riding becomes denied while the player is mounted, the player is dismounted.

If pet buff becomes denied, MythicLib stat modifiers are removed until the player leaves the denied region.
