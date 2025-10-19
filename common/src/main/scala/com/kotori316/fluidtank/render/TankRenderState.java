package com.kotori316.fluidtank.render;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.tank.TileTank;
import com.kotori316.fluidtank.tank.VisualTank;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class TankRenderState extends BlockEntityRenderState {
    TileTank tileTank;
    Tank<FluidLike> tank;
    VisualTank visualTank;

    void extract(TileTank tank) {
        this.tileTank = tank;
        this.tank = tank.getTank();
        this.visualTank = tank.getVisualTank();
    }
}
