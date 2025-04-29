package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.tank.TileTank
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.core.BlockPos
import net.minecraft.util.profiling.Profiler
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

abstract class RenderTank(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[TileTank] {
  override final def render(blockEntity: TileTank, partialTick: Float, matrix: PoseStack, buffer: MultiBufferSource, packedLight: Int, packedOverlay: Int, vec3: Vec3): Unit = {
    val profiler = Profiler.get()
    profiler.push("RenderTank")
    if (!blockEntity.getTank.isEmpty) {
      profiler.push("Rendering")
      matrix.pushPose()
      val b = buffer.getBuffer(RenderType.translucent)
      val tank = blockEntity.getVisualTank
      if (tank.box != null) {
        val texture = getFluidTexture(blockEntity.getTank, blockEntity)
        val color = getFluidColor(blockEntity.getTank, blockEntity)

        val value = Box.LightValue(packedLight).overrideBlock(getLuminance(blockEntity.getTank))
        val alpha = if ((color >> 24 & 0xFF) > 0) color >> 24 & 0xFF else 0xFF
        tank.box.render(b, matrix, texture, alpha, color >> 16 & 0xFF, color >> 8 & 0xFF, color >> 0 & 0xFF)(value)
      }
      matrix.popPose()
      profiler.pop()
    }
    profiler.pop()
  }

  def getFluidTexture(tank: Tank[FluidLike], blockEntity: TileTank): TextureAtlasSprite

  def getFluidColor(tank: Tank[FluidLike], blockEntity: TileTank): Int

  def getLuminance(tank: Tank[FluidLike]): Int

  protected final def getTankWorld(tileTank: TileTank): Level = {
    if (tileTank.hasLevel) tileTank.getLevel else Minecraft.getInstance.level
  }

  protected final def getTankPos(tileTank: TileTank): BlockPos = {
    if (tileTank.hasLevel) tileTank.getBlockPos else Minecraft.getInstance.player.getOnPos
  }
}
