package com.kotori316.fluidtank.forge.tank;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidConnection;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.forge.message.FluidTankContentMessageForge;
import com.kotori316.fluidtank.forge.message.PacketHandler;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import com.kotori316.fluidtank.tank.VisualTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TileTankForge extends TileTank {
    public TileTankForge(Tier tier, BlockPos p, BlockState s) {
        super(tier, p, s);
    }

    public TileTankForge(BlockPos p, BlockState s) {
        super(p, s);
    }

    private LazyOptional<IFluidHandler> fluidHandler = createHandler();
    private final VisualTank visualTank = new VisualTank();

    @Override
    public void setConnection(FluidConnection c) {
        super.setConnection(c);
        this.fluidHandler.invalidate();
        this.fluidHandler = createHandler();
    }

    @Override
    public void setTank(Tank<FluidLike> tank) {
        super.setTank(tank);
        if (this.level != null && !this.level.isClientSide) { // In server side
            PacketHandler.sendToClient(new FluidTankContentMessageForge(this), level);
        } else {
            // In client side
            // If level is null, it is the instance in RenderItemTank
            visualTank.updateContent(tank.capacity(), tank.amount(), tank.content().isGaseous());
        }
    }

    @Override
    public VisualTank getVisualTank() {
        return visualTank;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!remove && cap == ForgeCapabilities.FLUID_HANDLER) {
            return this.fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @NotNull
    private LazyOptional<IFluidHandler> createHandler() {
        return LazyOptional.of(() -> new ConnectionHandler(this.getConnection()));
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.fluidHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        // Basically, not called for block entity
        super.reviveCaps();
        this.fluidHandler = createHandler();
    }
}
