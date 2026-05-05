package com.kotori316.fluidtank.neoforge.test;

import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.neoforge.FluidTank;
import com.kotori316.testutil.MCTestInitializer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Stream;

public abstract class BeforeMC {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeforeMC.class);

    @BeforeAll
    public static void initialize() {
        MCTestInitializer.setUp(BeforeMC::setup);
    }

    private static void setup() {
        PlatformConfigAccess.setInstance(ConfigData::FOR_TEST);
        setPotionComponent();
    }

    private static void setPotionComponent() {
        Stream.concat(
            Stream.of(
                Map.entry(Items.POTION, createComponentMap(1)),
                Map.entry(Items.SPLASH_POTION, createComponentMap(1)),
                Map.entry(Items.LINGERING_POTION, createComponentMap(1)),
                Map.entry(Items.GLASS_BOTTLE, createComponentMap(16)),
                Map.entry(Items.BUCKET, createComponentMap(16)),
                Map.entry(Items.WATER_BUCKET, createComponentMap(1)),
                Map.entry(Items.LAVA_BUCKET, createComponentMap(1)),
                Map.entry(Items.MILK_BUCKET, createComponentMap(1)),
                Map.entry(Items.STONE, createComponentMap(64)),
                Map.entry(Items.APPLE, createComponentMap(64)),
                Map.entry(Items.AIR, createComponentMap(1))
            ),
            FluidTank.TANK_MAP.values().stream().map(i -> Map.entry(i.asItem(), createComponentMap(64)))
        ).forEach(e -> setItemComponent(e.getKey(), e.getValue()));

        Stream.of(
            Fluids.EMPTY,
            Fluids.WATER,
            Fluids.LAVA
        ).forEach(e -> setFluidComponent(e, DataComponentMap.EMPTY));
    }

    private static DataComponentMap createComponentMap(int stackTo) {
        return DataComponentMap.builder()
            .set(DataComponents.MAX_STACK_SIZE, stackTo)
            .build();
    }

    @SuppressWarnings("deprecation")
    protected static void setItemComponent(Item item, DataComponentMap components) {
        item.builtInRegistryHolder().bindComponents(components);
        LOGGER.info("Set item component for {}, {}", item, components);
    }

    @SuppressWarnings("deprecation")
    protected static void setFluidComponent(Fluid fluid, DataComponentMap components) {
        fluid.builtInRegistryHolder().bindComponents(components);
        LOGGER.info("Set fluid component for {}, {}", fluid, components);
    }
}
