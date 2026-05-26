# MythicMobs and EXP Drops

SincePet hooks into MythicMobs to support pet EXP drops.

## Pet EXP

Pet EXP is applied to the player's active pet. When enough EXP is collected, the pet levels up according to `max_xp_formula` in `pets.yml`.

If the pet is already at its max level, the plugin shows the max-level action bar message from `messages.yml`.

## MythicLib

MythicLib is used for:

- Pet stat modifiers
- Pet attack damage calculation
- Player damage inheritance
- Elemental damage inheritance

Make sure stat names in `pets.yml` match MythicLib stat names.
