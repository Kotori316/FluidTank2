package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.fabric.tank.TileTankFabric;
import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.Tier;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class BeforeMC {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @BeforeAll
    public static void setup() {
        if (!initialized.getAndSet(true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            PlatformConfigAccess.setInstance(ConfigData::FOR_TEST);
            PlatformTankAccess.setInstance(new TestPlatformTankAccessFabric());
            setPotionComponent();
        }
    }

    public static void assertEqualHelper(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, "Expected: %s, Actual: %s".formatted(expected, actual));
    }

    public static void assertEqualStack(ItemStack expected, ItemStack actual) {
        Assertions.assertTrue(ItemStack.matches(expected, actual),
            "Expected: %s(%s), Actual: %s(%s)".formatted(expected, expected.getComponents(), actual, actual.getComponents()));
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
                Map.entry(Items.AIR, createComponentMap(1))
            ),
            FluidTank.TANK_MAP.values().stream().map(i -> Map.entry(i.asItem(), createComponentMap(64)))
        ).forEach(e -> setItemComponent(e.getKey(), e.getValue()));
    }

    private static DataComponentMap createComponentMap(int stackTo) {
        return DataComponentMap.builder()
            .set(DataComponents.MAX_STACK_SIZE, stackTo)
            .build();
    }

    @SuppressWarnings("deprecation")
    protected static void setItemComponent(Item item, DataComponentMap components) {
        item.builtInRegistryHolder().bindComponents(components);
    }

    private static class TestPlatformTankAccessFabric implements PlatformTankAccess {
        private BlockEntityType<TileTankFabric> mock() {
            return FabricBlockEntityTypeBuilder.create(TileTankFabric::new).build();
        }

        @Override
        public BlockEntityType<TileTankFabric> getNormalType() {
            return mock();
        }

        @Override
        public BlockEntityType<TileTankFabric> getCreativeType() {
            return mock();
        }

        @Override
        public BlockEntityType<TileTankFabric> getVoidType() {
            return mock();
        }

        @Override
        public Map<Tier, Supplier<? extends BlockTank>> getTankBlockMap() {
            return Map.of();
        }

        @Override
        public Map<Tier, ? extends Supplier<? extends ItemReservoir>> getReservoirMap() {
            return Map.of();
        }
    }
}
