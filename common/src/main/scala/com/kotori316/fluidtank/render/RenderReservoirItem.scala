package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.reservoir.ItemReservoir
import com.kotori316.fluidtank.tank.Tier
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.{ARGB, Mth}
import net.minecraft.world.item.{ItemDisplayContext, ItemStack}
import org.joml.Vector3f

import java.util.Locale

final class RenderReservoirItem(protected val model: ReservoirModel, renderHelper: FluidRenderHelper) extends SpecialModelRenderer[RenderReservoirItem.RenderContext] {

  override def render(patterns: RenderReservoirItem.RenderContext, displayContext: ItemDisplayContext, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int, hasFoilType: Boolean): Unit = {
    poseStack.pushPose()
    poseStack.scale(1.0F, 1.0F, 1.0F)
    poseStack.translate(0, 0, 0.5f)

    val vertexConsumer = ItemRenderer.getFoilBuffer(bufferSource,
      this.model.renderType(RenderReservoirItem.textureNameMap(patterns.tier)),
      true, patterns.hasFoil)
    // RenderSystem.enableCull()
    this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, ARGB.color(255, -1))

    val tank = patterns.tank
    if (tank.hasContent) {
      val ratio = Mth.clamp(tank.content.amount.asForgeDouble / tank.capacity.asForgeDouble, 0.1d, 1d)
      val (minY, maxY) = if (tank.content.isGaseous) {
        (1d - ratio, 1d)
      } else {
        (0d, ratio)
      }

      val box = Box(0.5, minY, 0.5d / 16d,
        0.5, maxY, 0.5d / 16d,
        11.9d / 16d, maxY - minY, 0.99d / 16d, firstSide = false, endSide = false)
      val texture = getFluidTexture(tank)
      val color = getFluidColor(tank)
      val alpha = if ((color >> 24 & 0xFF) > 0) color >> 24 & 0xFF else 0xFF
      box.render(
        buffer = bufferSource.getBuffer(RenderType.cutout()), // TODO check rendering
        matrix = poseStack, sprite = texture,
        alpha, color >> 16 & 0xFF, color >> 8 & 0xFF, color >> 0 & 0xFF
      )
    }

    poseStack.popPose()
  }

  override def getExtents(output: java.util.Set[Vector3f]): Unit = {
    // TODO What should I do here?
  }

  override def extractArgument(stack: ItemStack): RenderReservoirItem.RenderContext = {
    val reservoir = stack.getItem.asInstanceOf[ItemReservoir]
    val tank = reservoir.getTank(stack)
    RenderReservoirItem.RenderContext(reservoir.tier, stack.hasFoil, tank)
  }

  def getFluidTexture(tank: Tank[FluidLike]): TextureAtlasSprite = renderHelper.getFluidTexture(tank)

  def getFluidColor(tank: Tank[FluidLike]): Int = renderHelper.getFluidColor(tank)
}

object RenderReservoirItem {
  private final val textureNameMap: Map[Tier, ResourceLocation] = Tier.values().map(t =>
      (t, ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, s"textures/item/reservoir_${t.name().toLowerCase(Locale.ROOT)}.png")))
    .toMap

  case class RenderContext(tier: Tier, hasFoil: Boolean, tank: Tank[FluidLike])
}
