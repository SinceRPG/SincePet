# Installation

## Requirements

SincePet targets Paper API `1.21` and Java `21`.

Required server plugins:

- MythicLib
- MythicMobs
- WorldGuard

Optional server plugins:

- PlaceholderAPI, used by upgrade requirements such as `%playerpoints_points%`.
- PlayerPoints or any economy/points plugin that exposes PlaceholderAPI placeholders and has console commands.

## Install Steps

1. Build or download the SincePet jar.
2. Put the jar in your server `plugins` folder.
3. Install the required dependencies.
4. Start the server once.
5. Stop the server.
6. Edit the generated files in `plugins/SincePet/`.
7. Start the server again or run `/sincepet reload`.

## Updating

When a new version adds keys to resource YAML files, Bukkit does not automatically merge them into existing files. Compare the new files in the jar or repository with your server files.

The most important files to merge after this version are:

- `config.yml`
- `gui.yml`
- `messages.yml`
- `pets.yml`
- `pets/`

If you are testing on a fresh server, you can delete the old generated YAML files and let the plugin regenerate them.
