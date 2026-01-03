package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.reservoir.ItemReservoir
import com.kotori316.fluidtank.tank.Tier
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.client.resources.model.MaterialSet
import net.minecraft.resources.Identifier
import net.minecraft.util.{Mth, Unit as UtilUnit}
import net.minecraft.world.item.{ItemDisplayContext, ItemStack}
import org.joml.{Vector3f, Vector3fc}

import java.util.Locale

final class RenderReservoirItem(protected val model: ReservoirModel, protected val materialSet: MaterialSet, renderHelper: FluidRenderHelper) extends SpecialModelRenderer[RenderReservoirItem.RenderContext] {

  override def submit(patterns: RenderReservoirItem.RenderContext, displayContext: ItemDisplayContext, poseStack: PoseStack, nodeCollector: SubmitNodeCollector, packedLight: Int, packedOverlay: Int, hasFoilType: Boolean, outlineColor: Int): Unit = {
    poseStack.pushPose()
    poseStack.scale(1.0F, 1.0F, 1.0F)
    poseStack.translate(0, 0, 0.5f)
    // RenderSystem.enableCull()
    nodeCollector.submitModel(this.model, UtilUnit.INSTANCE, poseStack, this.model.renderType(RenderReservoirItem.textureNameMap(patterns.tier)), packedLight, packedOverlay, outlineColor, null)

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
      val texture = renderHelper.getFluidTexture(tank, materialSet)
      val color = renderHelper.getFluidColor(tank)
      val alpha = if ((color >> 24 & 0xFF) > 0) color >> 24 & 0xFF else 0xFF
      nodeCollector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (pose, buffer) => {
        box.render(
          buffer = buffer,
          matrix = pose, sprite = texture,
          alpha, color >> 16 & 0xFF, color >> 8 & 0xFF, color >> 0 & 0xFF
        )
      })
    }

    poseStack.popPose()
  }

  override def getExtents(output: java.util.function.Consumer[Vector3fc]): Unit = {
    val pose = new PoseStack()
    pose.translate(0.5F, 0.0F, 0.5F)
    pose.scale(-1.0F, -1.0F, 1.0F)
    this.model.root.getExtentsForGui(pose, output)
  }

  override def extractArgument(stack: ItemStack): RenderReservoirItem.RenderContext = {
    val reservoir = stack.getItem.asInstanceOf[ItemReservoir]
    val tank = reservoir.getTank(stack)
    RenderReservoirItem.RenderContext(reservoir.tier, stack.hasFoil, tank)
  }
}

object RenderReservoirItem {
  private final val textureNameMap: Map[Tier, Identifier] = Tier.values().map(t =>
      (t, Identifier.fromNamespaceAndPath(FluidTankCommon.modId, s"textures/item/reservoir_${t.name().toLowerCase(Locale.ROOT)}.png")))
    .toMap

  case class RenderContext(tier: Tier, hasFoil: Boolean, tank: Tank[FluidLike])
}
