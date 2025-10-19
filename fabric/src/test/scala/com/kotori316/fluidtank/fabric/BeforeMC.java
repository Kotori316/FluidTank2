package com.kotori316.fluidtank.fabric;

import com.kotori316.fluidtank.config.ConfigData;
import com.kotori316.fluidtank.config.PlatformConfigAccess;
import com.kotori316.fluidtank.fabric.tank.TileTankFabric;
import com.kotori316.fluidtank.reservoir.ItemReservoir;
import com.kotori316.fluidtank.tank.BlockTank;
import com.kotori316.fluidtank.tank.PlatformTankAccess;
import com.kotori316.fluidtank.tank.TankLootFunction;
import com.kotori316.fluidtank.tank.Tier;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class BeforeMC {
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @BeforeAll
    public static void setup() {
        if (!initialized.getAndSet(true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            PlatformConfigAccess.setInstance(ConfigData::FOR_TEST);
            PlatformTankAccess.setInstance(new TestPlatformTankAccessFabric());
        }
    }

    public static void assertEqualHelper(Object expected, Object actual) {
        Assertions.assertEquals(expected, actual, "Expected: %s, Actual: %s".formatted(expected, actual));
    }

    public static void assertEqualStack(ItemStack expected, ItemStack actual) {
        Assertions.assertTrue(ItemStack.matches(expected, actual),
            "Expected: %s(%s), Actual: %s(%s)".formatted(expected, expected.getComponents(), actual, actual.getComponents()));
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
        public LootItemFunctionType<TankLootFunction> getTankLoot() {
            return null;
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
