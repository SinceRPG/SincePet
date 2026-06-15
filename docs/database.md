# Database and Player Data

SincePet stores player pet data in a database table.

## Stored Values

The user table stores:

| Column           | Meaning                             |
|------------------|-------------------------------------|
| `uuid`           | Player UUID.                        |
| `active_pet`     | Active pet ID.                      |
| `pet_levels`     | JSON map of pet levels.             |
| `pet_xp`         | JSON map of pet XP.                 |
| `pet_max_levels` | JSON map of per-pet max levels.     |
| `pet_upgrades`   | JSON map of pet upgrade levels.     |
| `pet_settings`   | JSON map of per-pet setting values. |

## SQLite

SQLite creates `database.db` in the plugin data folder.

Use SQLite for single-server setups or local testing.

## MySQL

Use MySQL for larger servers or network setups where an external database is preferred.

Make sure the configured user has permission to create and alter the configured table.

## Migrations

The plugin creates missing columns automatically:

- `pet_max_levels`
- `pet_upgrades`
- `pet_settings`

If the server logs a warning about a missing column, restart the server once after the plugin has created/migrated the
table.
