package com.kotori316.fluidtank.tank;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface PlatformTileAccess {
    @NotNull
    static PlatformTileAccess getInstance() {
        return PlatformTileAccessHolder.access;
    }

    static void setInstance(PlatformTileAccess instance) {
        PlatformTileAccessHolder.access = instance;
    }

    BlockEntityType<? extends TileTank> getNormalType();

    BlockEntityType<? extends TileTank> getCreativeType();

    BlockEntityType<? extends TileTank> getVoidType();

    static boolean isTankType(BlockEntityType<?> entityType) {
        var i = getInstance();
        return entityType == i.getNormalType() || entityType == i.getCreativeType() || entityType == i.getVoidType();
    }

    LootItemFunctionType getTankLoot();
}

class PlatformTileAccessHolder {
    @NotNull
    static PlatformTileAccess access = new Default();

    @ApiStatus.Internal
    private static class Default implements PlatformTileAccess {

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
        public LootItemFunctionType getTankLoot() {
            return null;
        }
    }
}
