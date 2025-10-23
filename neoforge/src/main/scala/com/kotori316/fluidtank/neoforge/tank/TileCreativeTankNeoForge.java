package com.kotori316.fluidtank.neoforge.tank;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidConnection;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.neoforge.message.FluidTankContentMessageNeoForge;
import com.kotori316.fluidtank.neoforge.message.PacketHandler;
import com.kotori316.fluidtank.tank.TileCreativeTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TileCreativeTankNeoForge extends TileCreativeTank {
    public TileCreativeTankNeoForge(BlockPos p, BlockState s) {
        super(p, s);
    }

    @NotNull
    private ResourceHandler<FluidResource> fluidHandler = createHandler();

    @Override
    public void setConnection(FluidConnection c) {
        super.setConnection(c);
        this.invalidateCapabilities();
        this.fluidHandler = createHandler();
    }

    @Override
    public void setTank(Tank<FluidLike> tank) {
        super.setTank(tank);
        if (this.level != null && !this.level.isClientSide()) { // In server side
            PacketHandler.sendToClient(new FluidTankContentMessageNeoForge(this), level);
        } else {
            // In client side
            // If level is null, it is the instance in RenderItemTank
            visualTank().updateContent(tank.capacity(), tank.amount(), tank.content().isGaseous());
        }
    }

    @NotNull
    public ResourceHandler<FluidResource> getCapability(@Nullable Direction ignored) {
        return this.fluidHandler;
    }

    @NotNull
    private ResourceHandler<FluidResource> createHandler() {
        return new ConnectionHandler(this.getConnection());
    }

}
