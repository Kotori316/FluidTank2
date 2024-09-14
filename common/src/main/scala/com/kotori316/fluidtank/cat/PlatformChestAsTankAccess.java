package com.kotori316.fluidtank.cat;

import com.kotori316.fluidtank.contents.GenericAmount;
import com.kotori316.fluidtank.fluids.FluidLike;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public interface PlatformChestAsTankAccess {
    @NotNull
    static PlatformChestAsTankAccess getInstance() {
        return PlatformChestAsTankAccessHolder.access;
    }

    static void setInstance(@NotNull PlatformChestAsTankAccess access) {
        PlatformChestAsTankAccessHolder.access = access;
    }

    Supplier<? extends BlockChestAsTank> getCATBlock();

    @Nullable
    BlockEntity createCATEntity(BlockPos pos, BlockState state);

    @NotNull
    List<GenericAmount<FluidLike>> getCATFluids(Level level, BlockPos pos);
}

class PlatformChestAsTankAccessHolder {
    static PlatformChestAsTankAccess access = new Default();

    @ApiStatus.Internal
    private static final class Default implements PlatformChestAsTankAccess {

        @Override
        public Supplier<? extends BlockChestAsTank> getCATBlock() {
            return null;
        }

        @Override
        public @Nullable BlockEntity createCATEntity(BlockPos pos, BlockState state) {
            return null;
        }

        @Override
        public List<GenericAmount<FluidLike>> getCATFluids(Level level, BlockPos pos) {
            return List.of();
        }
    }
}
