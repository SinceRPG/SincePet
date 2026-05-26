package net.danh.sincePet.pets;

import net.danh.sincePet.SincePet;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;

/**
 * Immutable runtime settings loaded from config.yml for pet display, motion, and combat timings.
 */
public record PetSettings(
        float seatOffset,
        double width,
        double height,
        double gravity,
        double jumpForce,
        double groundSpeed,
        double maxStepHeight,
        double flySpeed,
        double flyAcceleration,
        double flyFriction,
        double flyVerticalSpeed,
        int teleportDuration,
        float followSideOffset,
        float followBackOffset,
        float followVerticalOffset,
        float followLerp,
        float idleLerp,
        float bobAmplitude,
        float idleBobAmplitude,
        float bobSpeed,
        float displayScale,
        float displayOffsetY,
        float rideDisplayOffsetY,
        float nameOffsetY,
        float rideNameOffsetY,
        float viewRange,
        float nameViewRange,
        int targetCheckMillis,
        ItemDisplay.ItemDisplayTransform itemTransform,
        Display.Billboard billboard
) {
    public static PetSettings load(SincePet plugin) {
        var config = plugin.getConfigFile();
        return new PetSettings(
                (float) config.getDouble("pet.physics.seat_offset", 0.7),
                config.getDouble("pet.physics.width", 0.6),
                config.getDouble("pet.physics.height", 0.8),
                config.getDouble("pet.physics.gravity", 0.08),
                config.getDouble("pet.physics.jump_force", 0.6),
                config.getDouble("pet.physics.ground_speed", 0.45),
                config.getDouble("pet.physics.max_step_height", 1.1),
                config.getDouble("pet.physics.fly_speed", 0.8),
                config.getDouble("pet.physics.fly_acceleration", 0.15),
                config.getDouble("pet.physics.fly_friction", 0.85),
                config.getDouble("pet.physics.fly_vertical_speed", 0.4),
                Math.max(0, config.getInt("pet.physics.teleport_duration", 2)),
                (float) config.getDouble("pet.follow.side_offset", 1.0),
                (float) config.getDouble("pet.follow.back_offset", 0.3),
                (float) config.getDouble("pet.follow.vertical_offset", -0.2),
                (float) config.getDouble("pet.follow.move_lerp", 0.15),
                (float) config.getDouble("pet.follow.idle_lerp", 0.1),
                (float) config.getDouble("pet.follow.bob_amplitude", 0.05),
                (float) config.getDouble("pet.follow.idle_bob_amplitude", 0.03),
                (float) config.getDouble("pet.follow.bob_speed", 0.15),
                (float) config.getDouble("pet.display.scale", 1.0),
                (float) config.getDouble("pet.display.offset_y", -0.7),
                (float) config.getDouble("pet.display.ride_offset_y", -1.35),
                (float) config.getDouble("pet.display.name_offset_y", 0.85),
                (float) config.getDouble("pet.display.ride_name_offset_y", 0.25),
                (float) config.getDouble("pet.display.view_range", 0.6),
                (float) config.getDouble("pet.display.name_view_range", 16.0),
                Math.max(50, config.getInt("pet.attack.target_check_millis", 500)),
                parseItemTransform(config.getString("pet.display.item_transform", "FIXED")),
                parseBillboard(config.getString("pet.display.billboard", "FIXED"))
        );
    }

    private static ItemDisplay.ItemDisplayTransform parseItemTransform(String value) {
        try {
            return ItemDisplay.ItemDisplayTransform.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ItemDisplay.ItemDisplayTransform.FIXED;
        }
    }

    private static Display.Billboard parseBillboard(String value) {
        try {
            return Display.Billboard.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Display.Billboard.FIXED;
        }
    }
}
