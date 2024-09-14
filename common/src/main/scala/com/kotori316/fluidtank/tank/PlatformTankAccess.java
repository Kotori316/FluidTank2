package com.kotori316.fluidtank.tank;

import com.kotori316.fluidtank.reservoir.ItemReservoir;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;

public interface PlatformTankAccess {
    @NotNull
    static PlatformTankAccess getInstance() {
        return PlatformTankAccessHolder.access;
    }

    static void setInstance(PlatformTankAccess instance) {
        PlatformTankAccessHolder.access = instance;
    }

    BlockEntityType<? extends TileTank> getNormalType();

    BlockEntityType<? extends TileTank> getCreativeType();

    BlockEntityType<? extends TileTank> getVoidType();

    static boolean isTankType(BlockEntityType<?> entityType) {
        var i = getInstance();
        return entityType == i.getNormalType() || entityType == i.getCreativeType() || entityType == i.getVoidType();
    }

    LootItemFunctionType<TankLootFunction> getTankLoot();

    Map<Tier, Supplier<? extends BlockTank>> getTankBlockMap();

    Map<Tier, ? extends Supplier<? extends ItemReservoir>> getReservoirMap();
}

class PlatformTankAccessHolder {
    @NotNull
    static PlatformTankAccess access = new Default();

    @ApiStatus.Internal
    private static class Default implements PlatformTankAccess {

        @Override
        public BlockEntityType<? extends TileTank> getNormalType() {
            return null;
        }

        @Override
        public BlockEntityType<? extends TileTank> getCreativeType() {
            return null;
        }

        @Override
        public BlockEntityType<? extends TileTank> getVoidType() {
            return null;
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
