package com.kotori316.fluidtank.neoforge.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.render.RenderTank
import com.kotori316.fluidtank.tank.TileTank
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite

class RenderTankNeoForge(d: BlockEntityRendererProvider.Context) extends RenderTank(d) {

  override def getFluidTexture(tank: Tank[FluidLike], blockEntity: TileTank): TextureAtlasSprite = {
    FluidRenderHelperNeoForge.getFluidTexture(tank, null)
  }

  override def getFluidColor(tank: Tank[FluidLike], blockEntity: TileTank): Int = {
    FluidRenderHelperNeoForge.getFluidColor(tank)
  }

  override def getLuminance(tank: Tank[FluidLike]): Int = JavaHelper.getLightLevel(tank.content)
}
