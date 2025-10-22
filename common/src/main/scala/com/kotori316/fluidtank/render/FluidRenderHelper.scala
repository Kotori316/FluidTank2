package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.resources.model.MaterialSet

trait FluidRenderHelper {
  def getFluidTexture(tank: Tank[FluidLike], materialSet: MaterialSet): TextureAtlasSprite

  def getFluidColor(tank: Tank[FluidLike]): Int
}
