package com.kotori316.fluidtank;

import com.google.common.collect.Iterables;
import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Stream;

public class BeforeMC {

    @BeforeAll
    public static void initMC() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        unfreezeRegistry();
        PlatformConfigAccess.setInstance(ConfigData::FOR_TEST);
        setPotionComponent();
    }

    private static void unfreezeRegistry() {
        try {
            final var frozenField = MappedRegistry.class.getDeclaredField("frozen");
            final var mapField = MappedRegistry.class.getDeclaredField("unregisteredIntrusiveHolders");
            frozenField.setAccessible(true);
            mapField.setAccessible(true);

            var registries = Iterables.concat(List.of(BuiltInRegistries.REGISTRY), BuiltInRegistries.REGISTRY);
            for (Registry<?> registry : registries) {
                if (registry instanceof MappedRegistry<?>) {
                    frozenField.setBoolean(registry, false);
                    mapField.set(registry, new IdentityHashMap<>());
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setPotionComponent() {
        Stream.of(
            Items.POTION,
            Items.SPLASH_POTION,
            Items.LINGERING_POTION,
            Items.GLASS_BOTTLE,
            Items.BUCKET,
            Items.WATER_BUCKET,
            Items.LAVA_BUCKET,
            Items.MILK_BUCKET,
            Items.STONE,
            Items.AIR
        ).forEach(BeforeMC::setItemComponent);
    }

    @SuppressWarnings("deprecation")
    protected static void setItemComponent(Item item) {
        item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
    }
}
