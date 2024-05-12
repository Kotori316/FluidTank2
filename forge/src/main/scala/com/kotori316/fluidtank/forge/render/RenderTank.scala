package com.kotori316.fluidtank.forge.render

import com.kotori316.fluidtank.fluids.{FluidLike, VanillaFluid, VanillaPotion}
import com.kotori316.fluidtank.forge.fluid.ForgeConverter.*
import com.kotori316.fluidtank.forge.render.RenderTank.getVisualTank
import com.kotori316.fluidtank.forge.tank.{TileCreativeTankForge, TileTankForge}
import com.kotori316.fluidtank.render.Box
import com.kotori316.fluidtank.tank.{TileTank, VisualTank}
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions
import net.minecraftforge.fluids.FluidType

import scala.jdk.OptionConverters.RichOptional

@OnlyIn(Dist.CLIENT)
class RenderTank(d: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[TileTank] {

  override def render(te: TileTank, partialTicks: Float, matrix: PoseStack, buffer: MultiBufferSource, light: Int, otherLight: Int): Unit = {
    Minecraft.getInstance.getProfiler.push("RenderTank")
    if (!te.getTank.isEmpty) {
      matrix.pushPose()
      val b = buffer.getBuffer(RenderType.translucent)
      val tank = getVisualTank(te)
      if (tank.box != null) {
        val resource = RenderTank.textureName(te)
        val texture = Minecraft.getInstance.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(resource)
        val color = RenderTank.color(te)

        val fluid = te.getTank.content
        val value = Box.LightValue(light).overrideBlock(JavaHelper.getLightLevel(fluid))
        val alpha = if ((color >> 24 & 0xFF) > 0) color >> 24 & 0xFF else 0xFF
        tank.box.render(b, matrix, texture, alpha, color >> 16 & 0xFF, color >> 8 & 0xFF, color >> 0 & 0xFF)(value)
      }
      matrix.popPose()
    }
    Minecraft.getInstance.getProfiler.pop()
  }
}

object RenderTank {
  private def textureName(tile: TileTank) = {
    val world = getTankWorld(tile)
    val pos = getTankPos(tile)
    val fluid = FluidLike.asFluid(tile.getTank.content.content, Fluids.WATER)
    val attributes = IClientFluidTypeExtensions.of(fluid)
    attributes.getStillTexture(fluid.defaultFluidState(), world, pos)
  }

  private def color(tile: TileTank): Int = {
    val fluidAmount = tile.getTank.content

    fluidAmount.content match {
      case VanillaFluid(fluid) =>
        val attributes = IClientFluidTypeExtensions.of(fluid)
        val normal = attributes.getTintColor
        if (attributes.getClass == classOf[FluidType]) {
          normal
        } else {
          val stackColor = attributes.getTintColor(fluidAmount.toStack)
          if (normal == stackColor) {
            val world = getTankWorld(tile)
            val pos = getTankPos(tile)
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

  private final def getTankWorld(tileTank: TileTank): Level = {
    if (tileTank.hasLevel) tileTank.getLevel else Minecraft.getInstance.level
  }

  private final def getTankPos(tileTank: TileTank): BlockPos = {
    if (tileTank.hasLevel) tileTank.getBlockPos else Minecraft.getInstance.player.getOnPos
  }

  private final def getVisualTank(tileTank: TileTank): VisualTank = {
    tileTank match {
      case forge: TileTankForge => forge.visualTank
      case creativeForge: TileCreativeTankForge => creativeForge.visualTank
      // No need to set renderer for void tank because it never has content.
      case _ => throw new NotImplementedError(s"Unavailable for ${tileTank.getClass}")
    }
  }
}
