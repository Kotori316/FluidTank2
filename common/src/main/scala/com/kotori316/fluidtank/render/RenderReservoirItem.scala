package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.reservoir.ItemReservoir
import com.kotori316.fluidtank.tank.Tier
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.{BlockEntityWithoutLevelRenderer, MultiBufferSource, RenderType}
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.{ARGB, Mth}
import net.minecraft.world.item.{ItemDisplayContext, ItemStack}

import java.util.Locale

abstract class RenderReservoirItem extends BlockEntityWithoutLevelRenderer(Minecraft.getInstance.getBlockEntityRenderDispatcher, Minecraft.getInstance.getEntityModels) {
  private var model: ReservoirModel = scala.compiletime.uninitialized

  override def renderByItem(stack: ItemStack, displayContext: ItemDisplayContext, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    poseStack.pushPose()
    poseStack.scale(1.0F, 1.0F, 1.0F)
    poseStack.translate(0, 0, 0.5f)
    val reservoir = stack.getItem.asInstanceOf[ItemReservoir]
    val vertexConsumer = ItemRenderer.getFoilBuffer(buffer,
      this.model.renderType(RenderReservoirItem.textureNameMap(reservoir.tier)),
      true, stack.hasFoil)
    RenderSystem.enableCull()
    this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, ARGB.color(255, -1))

    val tank = reservoir.getTank(stack)
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
        buffer = buffer.getBuffer(RenderType.translucent),
        matrix = poseStack, sprite = texture,
        alpha, color >> 16 & 0xFF, color >> 8 & 0xFF, color >> 0 & 0xFF
      )
    }

    poseStack.popPose()
  }

  override def onResourceManagerReload(resourceManager: ResourceManager): Unit = {
    this.model = new ReservoirModel(Minecraft.getInstance.getEntityModels.bakeLayer(ReservoirModel.LOCATION))
  }

  def getFluidTexture(tank: Tank[FluidLike]): TextureAtlasSprite

  def getFluidColor(tank: Tank[FluidLike]): Int
}

object RenderReservoirItem {
  private final val textureNameMap: Map[Tier, ResourceLocation] = Tier.values().map(t =>
      (t, ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, s"textures/item/reservoir_${t.name().toLowerCase(Locale.ROOT)}.png")))
    .toMap
}
