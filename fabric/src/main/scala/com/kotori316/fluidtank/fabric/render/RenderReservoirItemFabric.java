package com.kotori316.fluidtank.fabric.render;

import com.kotori316.fluidtank.contents.Tank;
import com.kotori316.fluidtank.fluids.FluidLike;
import com.kotori316.fluidtank.render.RenderReservoirItem;
import com.kotori316.fluidtank.render.ReservoirModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Objects;

public final class RenderReservoirItemFabric extends RenderReservoirItem {

    public RenderReservoirItemFabric(ReservoirModel model) {
        super(model);
    }

    @Override
    public TextureAtlasSprite getFluidTexture(Tank<FluidLike> tank) {
        return RenderResourceHelper.getSprite(tank.content());
    }

    @Override
    public int getFluidColor(Tank<FluidLike> tank) {
        return RenderResourceHelper.getColorWithPos(tank.content(), Minecraft.getInstance().level, Objects.requireNonNull(Minecraft.getInstance().player).getOnPos());
    }
}
