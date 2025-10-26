package com.kotori316.fluidtank.fabric.tank;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fabric.message.FluidTankContentMessageFabric;
import com.kotori316.fluidtank.fabric.message.PacketHandler;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.tank.Tier;
import com.kotori316.fluidtank.tank.TileTank;
import com.kotori316.fluidtank.tank.VisualTank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class TileTankFabric extends TileTank {
    public TileTankFabric(Tier tier, BlockPos p, BlockState s) {
        super(tier, p, s);
    }

    public TileTankFabric(BlockPos p, BlockState s) {
        super(p, s);
    }

    private final VisualTank visualTank = new VisualTank();

    @Override
    public void setTank(Tank<FluidLike> tank) {
        super.setTank(tank);
        if (this.level != null && !this.level.isClientSide()) {
            // Sync to client
            PacketHandler.sendToClientWorld(new FluidTankContentMessageFabric(this), this.level);
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
}
