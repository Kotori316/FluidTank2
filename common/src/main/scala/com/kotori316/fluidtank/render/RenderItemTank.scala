package com.kotori316.fluidtank.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.world.item.{ItemDisplayContext, ItemStack}

class RenderItemTank extends SpecialModelRenderer[RenderItemTank.RenderContext] {

  override def render(patterns: RenderItemTank.RenderContext, displayContext: ItemDisplayContext, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int, hasFoilType: Boolean): Unit = {


  }

  override def extractArgument(stack: ItemStack): RenderItemTank.RenderContext = {
    RenderItemTank.RenderContext()
  }
}

object RenderItemTank {
  case class RenderContext()
}
