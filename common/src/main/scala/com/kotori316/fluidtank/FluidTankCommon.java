package com.kotori316.fluidtank;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.time.Duration;
import java.util.function.Supplier;

public final class FluidTankCommon {
    public static final String modId = "fluidtank";
    public static final Logger LOGGER = LoggerFactory.getLogger(FluidTankCommon.class);
    public static final Marker INITIALIZATION = MarkerFactory.getMarker("Initialization");
    public static final Marker MARKER_CONNECTION = MarkerFactory.getMarker("Connection");
    public static final Marker MARKER_TANK = MarkerFactory.getMarker("Tank");
    public static final Marker MARKER_FLUID_LIKE = MarkerFactory.getMarker("FluidLike");

    private static final double d = 1 / 16d;
    public static final VoxelShape TANK_SHAPE = Shapes.create(new AABB(2 * d, 0, 2 * d, 14 * d, 1d, 14 * d));

    // No meaning for value, just for existence check
    private static final Cache<String, String> knownKeys = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build();

    /**
     * Log given message once in a minute.
     */
    public static void logOnceInMinute(String key, Supplier<String> message, @Nullable Supplier<? extends Throwable> error) {
        if (knownKeys.getIfPresent(key) == null) {
            knownKeys.put(key, key);
            var msg = "[" + key + "] " + message.get();
            if (error == null) {
                LOGGER.warn(msg);
                DebugLogging.LOGGER().error(msg);
            } else {
                LOGGER.error(msg, error.get());
                DebugLogging.LOGGER().error(msg, error.get());
            }
        }
    }
}
