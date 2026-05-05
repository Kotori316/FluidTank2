package com.kotori316.fluidtank.neoforge.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.{FluidLike, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter
import com.kotori316.fluidtank.render.RenderTank
import com.kotori316.fluidtank.tank.TileTank
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.component.{DataComponentMap, DataComponents}
import net.minecraft.world.level.material.Fluids
import net.neoforged.neoforge.client.fluid.FluidTintSources

class RenderTankNeoForge(d: BlockEntityRendererProvider.Context) extends RenderTank(d) {

  override def getFluidTexture(tank: Tank[FluidLike], blockEntity: TileTank): TextureAtlasSprite = {
    val fluid = FluidLike.asFluid(tank.content.content, Fluids.WATER)
    val fluidState = fluid.defaultFluidState
    Minecraft.getInstance.getModelManager.getFluidStateModelSet.get(fluidState).stillMaterial().sprite()
  }

  override def getFluidColor(tank: Tank[FluidLike], blockEntity: TileTank): Int = {
    val fluidAmount = tank.content

    fluidAmount.content match {
      case VanillaFluid(fluid) =>
        val fluidState = fluid.defaultFluidState
        val model = Minecraft.getInstance.getModelManager.getFluidStateModelSet.get(fluidState)
        val tintSource = FluidTintSources.of(model.tintSource())
        tintSource.colorAsStack(NeoForgeConverter.toStack(fluidAmount))
      case VanillaPotion(_) =>
        fluidAmount.componentPatch
          .flatMap(p => Option(p.get(DataComponentMap.EMPTY, DataComponents.POTION_CONTENTS)))
          .map(_.getColor)
          .getOrElse(16253176)
    }
  }

  override def getLuminance(tank: Tank[FluidLike]): Int = JavaHelper.getLightLevel(tank.content)
}
