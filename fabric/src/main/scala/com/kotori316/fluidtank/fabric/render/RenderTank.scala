package com.kotori316.fluidtank.fabric.render

import com.kotori316.fluidtank.fabric.render.RenderTank.getVisualTank
import com.kotori316.fluidtank.fabric.tank.{TileCreativeTankFabric, TileTankFabric}
import com.kotori316.fluidtank.render.Box
import com.kotori316.fluidtank.tank.{TileTank, VisualTank}
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.api.{EnvType, Environment}
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.core.BlockPos
import net.minecraft.util.profiling.Profiler
import net.minecraft.world.level.Level

@Environment(EnvType.CLIENT)
class RenderTank(d: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[TileTank] {

  override def render(te: TileTank, partialTicks: Float, matrix: PoseStack, buffer: MultiBufferSource, light: Int, otherLight: Int): Unit = {
    val profiler = Profiler.get()
    profiler.push("RenderTank")
    if (!te.getTank.isEmpty) {
      profiler.push("Rendering")
      matrix.pushPose()
      val b = buffer.getBuffer(RenderType.translucent)
      val tank = getVisualTank(te)
      if (tank.box != null) {
        val texture = RenderTank.textureName(te)
        val color = RenderTank.color(te)

        val fluid = te.getTank.content
        val value = Box.LightValue(light).overrideBlock(RenderResourceHelper.getLuminance(fluid))
        val alpha = if ((color >> 24 & 0xFF) > 0) color >> 24 & 0xFF else 0xFF
        tank.box.render(b, matrix, texture, alpha, color >> 16 & 0xFF, color >> 8 & 0xFF, color >> 0 & 0xFF)(value)
      }
      matrix.popPose()
      profiler.pop()
    }
    profiler.pop()
  }
}

object RenderTank {
  private def textureName(tile: TileTank) = {
    RenderResourceHelper.getSprite(tile.getTank.content)
  }

  private def color(tile: TileTank): Int = {
    val fluidAmount = tile.getTank.content
    RenderResourceHelper.getColorWithPos(fluidAmount, getTankWorld(tile), getTankPos(tile))
  }

  private final def getTankWorld(tileTank: TileTank): Level = {
    if (tileTank.hasLevel) tileTank.getLevel else Minecraft.getInstance.level
  }

  private final def getTankPos(tileTank: TileTank): BlockPos = {
    if (tileTank.hasLevel) tileTank.getBlockPos else Minecraft.getInstance.player.getOnPos
  }

  private final def getVisualTank(tileTank: TileTank): VisualTank = {
    tileTank match {
      case fabric: TileTankFabric => fabric.visualTank
      case creativeFabric: TileCreativeTankFabric => creativeFabric.visualTank
      // No need to set renderer for void tank because it never has content.
      case _ => throw new NotImplementedError(s"Unavailable for ${tileTank.getClass}")
    }
  }
}
