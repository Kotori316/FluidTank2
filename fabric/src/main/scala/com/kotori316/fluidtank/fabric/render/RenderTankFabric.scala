package com.kotori316.fluidtank.fabric.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.render.RenderTank
import com.kotori316.fluidtank.tank.TileTank
import net.fabricmc.api.{EnvType, Environment}
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite

@Environment(EnvType.CLIENT)
class RenderTankFabric(d: BlockEntityRendererProvider.Context) extends RenderTank(d) {

  override def getFluidTexture(tank: Tank[FluidLike], blockEntity: TileTank): TextureAtlasSprite = FluidRenderHelperFabric.getSprite(tank.content)

  override def getFluidColor(tank: Tank[FluidLike], blockEntity: TileTank): Int = {
    val level = getTankWorld(blockEntity)
    val pos = getTankPos(blockEntity)
    level match {
      case view: BlockAndTintGetter => FluidRenderHelperFabric.getColorWithPos(tank.content, view, pos)
      case _ => 0 // Unreachable
    }
  }

  override def getLuminance(tank: Tank[FluidLike]): Int = FluidRenderHelperFabric.getLuminance(tank.content)
}
