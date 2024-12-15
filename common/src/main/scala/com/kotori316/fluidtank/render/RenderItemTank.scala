package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.tank.{ItemBlockTank, PlatformTankAccess, Tier, TileTank, VisualTank}
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.{ItemDisplayContext, ItemStack}

class RenderItemTank(model: TankModel, renderHelper: FluidRenderHelper) extends SpecialModelRenderer[RenderItemTank.RenderContext] {
  private lazy val tileTank: TileTank = new RenderItemTank.TileTankForRender

  override def render(patterns: RenderItemTank.RenderContext, displayContext: ItemDisplayContext, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int, hasFoilType: Boolean): Unit = {
    Lighting.setupFor3DItems()
    val buffer = bufferSource.getBuffer(this.model.renderType(ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, s"textures/block/fluid_source.png")))
    this.model.renderToBuffer(poseStack, buffer, packedLight, packedOverlay)

    tileTank.tier = patterns.item.blockTank.tier
    for (d <- patterns.data if !d.isEmpty) {
      val level = Minecraft.getInstance.level
      tileTank.setLevel(level)
      tileTank.loadAdditional(d.copyTag(), level.registryAccess())
      if (tileTank.getTank.hasContent) {
        Minecraft.getInstance.getBlockEntityRenderDispatcher.render(
          tileTank, 0, poseStack, bufferSource
        )
      }
    }
  }

  override def extractArgument(stack: ItemStack): RenderItemTank.RenderContext = {
    RenderItemTank.RenderContext(
      stack.getItem.asInstanceOf[ItemBlockTank],
      Option(stack.get(DataComponents.BLOCK_ENTITY_DATA)),
    )
  }
}

object RenderItemTank {
  case class RenderContext(item: ItemBlockTank, data: Option[CustomData])

  private class TileTankForRender extends TileTank(
    BlockPos.ZERO,
    PlatformTankAccess.getInstance().getTankBlockMap.get(Tier.WOOD).get().defaultBlockState(),
  ) {
    override val getVisualTank: VisualTank = new VisualTank

    override def setTank(tank: Tank[FluidLike]): Unit = {
      super.setTank(tank)
      // In client side
      // If level is null, it is the instance in RenderItemTank
      getVisualTank.updateContent(tank.capacity, tank.amount, tank.content.isGaseous)
    }
  }
}
