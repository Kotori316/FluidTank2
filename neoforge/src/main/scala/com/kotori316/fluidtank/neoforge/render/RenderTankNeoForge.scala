package com.kotori316.fluidtank.neoforge.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.{FluidLike, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.neoforge.fluid.NeoForgeConverter.*
import com.kotori316.fluidtank.render.{RenderItemCodecs, RenderTank}
import com.kotori316.fluidtank.tank.TileTank
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.component.DataComponents
import net.minecraft.world.level.material.Fluids
import net.neoforged.api.distmarker.{Dist, OnlyIn}
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions
import net.neoforged.neoforge.fluids.FluidType

import scala.jdk.OptionConverters.RichOptional

@OnlyIn(Dist.CLIENT)
class RenderTankNeoForge(d: BlockEntityRendererProvider.Context) extends RenderTank(d) {

  override def getFluidTexture(tank: Tank[FluidLike], blockEntity: TileTank): TextureAtlasSprite = {
    val world = getTankWorld(blockEntity)
    val pos = getTankPos(blockEntity)
    val fluid = FluidLike.asFluid(tank.content.content, Fluids.WATER)
    val attributes = IClientFluidTypeExtensions.of(fluid)
    val resource = attributes.getStillTexture(fluid.defaultFluidState(), world, pos)
    Minecraft.getInstance.getTextureAtlas(RenderItemCodecs.atlas()).apply(resource)
  }

  override def getFluidColor(tank: Tank[FluidLike], blockEntity: TileTank): Int = {
    val fluidAmount = tank.content

    fluidAmount.content match {
      case VanillaFluid(fluid) =>
        val attributes = IClientFluidTypeExtensions.of(fluid)
        val normal = attributes.getTintColor
        if (attributes.getClass == classOf[FluidType]) {
          normal
        } else {
          val stackColor = attributes.getTintColor(fluidAmount.toStack)
          if (normal == stackColor) {
            val world = getTankWorld(blockEntity)
            val pos = getTankPos(blockEntity)
            val worldColor = attributes.getTintColor(fluid.defaultFluidState, world, pos)
            worldColor
          } else {
            stackColor
          }
        }
      case VanillaPotion(_) =>
        fluidAmount.componentPatch
          .flatMap(_.get(DataComponents.POTION_CONTENTS).toScala)
          .map(_.getColor)
          .getOrElse(16253176)
    }
  }

  override def getLuminance(tank: Tank[FluidLike]): Int = JavaHelper.getLightLevel(tank.content)
}
