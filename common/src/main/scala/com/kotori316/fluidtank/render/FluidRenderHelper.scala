package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import net.minecraft.client.renderer.texture.TextureAtlasSprite

trait FluidRenderHelper {
  def getFluidTexture(tank: Tank[FluidLike]): TextureAtlasSprite

  def getFluidColor(tank: Tank[FluidLike]): Int
}
