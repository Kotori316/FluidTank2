package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.tank.TileTank
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.CameraRenderState
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.{RenderType, SubmitNodeCollector}
import net.minecraft.core.BlockPos
import net.minecraft.util.profiling.Profiler
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

abstract class RenderTank(val context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[TileTank, TankRenderState] {
  override final def submit(renderState: TankRenderState, matrix: PoseStack, nodeCollector: SubmitNodeCollector, cameraRenderState: CameraRenderState): Unit = {
    val profiler = Profiler.get()
    profiler.push("RenderTank")
    if (!renderState.tank.isEmpty) {
      profiler.push("Rendering")
      matrix.pushPose()

      val tank = renderState.visualTank
      if (tank.box != null) {
        val texture = getFluidTexture(renderState.tank, renderState.tileTank)
        val color = getFluidColor(renderState.tank, renderState.tileTank)

        val value = Box.LightValue(renderState.lightCoords).overrideBlock(getLuminance(renderState.tank))
        val alpha = if ((color >> 24 & 0xFF) > 0) color >> 24 & 0xFF else 0xFF
        nodeCollector.submitCustomGeometry(matrix, RenderType.translucentMovingBlock(), (pose, buffer) => {
          tank.box.render(buffer, pose, texture, alpha, color >> 16 & 0xFF, color >> 8 & 0xFF, color >> 0 & 0xFF)(value)
        })
      }
      matrix.popPose()
      profiler.pop()
    }
    profiler.pop()
  }

  override def createRenderState(): TankRenderState = new TankRenderState

  override def extractRenderState(blockEntity: TileTank, renderState: TankRenderState, partialTick: Float, cameraPosition: Vec3, breakProgress: ModelFeatureRenderer.CrumblingOverlay): Unit = {
    super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress)
    renderState.extract(blockEntity)
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
