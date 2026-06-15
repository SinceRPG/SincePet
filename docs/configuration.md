# Configuration Reference

This page documents `config.yml`.

## Auto Save

```yaml
auto-save: 300
```

Player pet data is saved every `300` seconds. Set this to `0` to disable auto-save, but this is not recommended.

## Database

```yaml
database:
  type: "SQLITE"
  table_users: "sincepet_users"
```

Supported database types:

- `SQLITE`: local file database, easiest setup.
- `MYSQL`: remote MySQL database.

MySQL options:

```yaml
database:
  type: "MYSQL"
  host: "localhost"
  port: "3306"
  database: "minecraft"
  username: "root"
  password: ""
```

Pool options:

```yaml
database:
  pool:
    maximum_size: 10
    connection_timeout_ms: 5000
```

## Pet Level Cap

```yaml
pet:
  default_max_level: 100
```

This is the default maximum level for pets unless a player-specific max level is changed with admin commands.

## Display

```yaml
pet:
  display:
    item_transform: "FIXED"
    billboard: "FIXED"
    scale: 1.0
    offset_y: -0.7
    ride_offset_y: -1.35
    name_offset_y: 1.25
    ride_name_offset_y: 0.25
    view_range: 0.8
    name_view_range: 16.0
```

Display keys:

| Key                  | Description                                                              |
|----------------------|--------------------------------------------------------------------------|
| `item_transform`     | ItemDisplay transform mode. `FIXED` is recommended for player head pets. |
| `billboard`          | ItemDisplay billboard mode.                                              |
| `scale`              | Pet visual scale.                                                        |
| `offset_y`           | Visual Y offset while following. More negative means lower.              |
| `ride_offset_y`      | Visual Y offset while riding. More negative means lower under the rider. |
| `name_offset_y`      | TextDisplay name height while following.                                 |
| `ride_name_offset_y` | TextDisplay name height while riding.                                    |
| `view_range`         | Pet display view range.                                                  |
| `name_view_range`    | Name display view range.                                                 |

The name is a separate `TextDisplay`, not the custom name of the `ItemDisplay`. This prevents the name from being forced
into the wrong position when changing pet visual offsets.

## Follow Behavior

```yaml
pet:
  follow:
    side_offset: 1.0
    back_offset: 0.3
    vertical_offset: -0.2
    move_lerp: 0.15
    idle_lerp: 0.1
    bob_amplitude: 0.05
    idle_bob_amplitude: 0.03
    bob_speed: 0.15
```

| Key                  | Description                                                   |
|----------------------|---------------------------------------------------------------|
| `side_offset`        | Pet position to the side of the player.                       |
| `back_offset`        | Pet distance behind the player.                               |
| `vertical_offset`    | Pet follow height.                                            |
| `move_lerp`          | Follow smoothing while moving. Higher values catch up faster. |
| `idle_lerp`          | Rotation smoothing while idle.                                |
| `bob_amplitude`      | Floating movement amount while moving.                        |
| `idle_bob_amplitude` | Floating movement amount while idle.                          |
| `bob_speed`          | Floating animation speed.                                     |

## Riding Physics

```yaml
pet:
  physics:
    seat_offset: 0.7
    width: 0.6
    height: 0.8
    gravity: 0.08
    jump_force: 0.6
    ground_speed: 0.45
    max_step_height: 1.1
    fly_speed: 0.8
    fly_acceleration: 0.15
    fly_friction: 0.85
    fly_vertical_speed: 0.4
    teleport_duration: 2
```

`seat_offset` controls the entity origin used for movement and collision. `ride_offset_y` controls only how the pet
visual is drawn under the rider.

## Combat

```yaml
pet:
  attack:
    target_check_millis: 500
```

This throttles nearby target scans. Lower values make pets react faster but cost more server performance.
