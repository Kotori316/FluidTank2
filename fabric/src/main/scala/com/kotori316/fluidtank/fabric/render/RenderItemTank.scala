package com.kotori316.fluidtank.fabric.render

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.fabric.FluidTank
import com.kotori316.fluidtank.fabric.tank.TileTankFabric
import com.kotori316.fluidtank.tank.{ItemBlockTank, Tier}
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.{ItemDisplayContext, ItemStack}

class RenderItemTank extends BuiltinItemRendererRegistry.DynamicItemRenderer {

  lazy val tileTank = new TileTankFabric(BlockPos.ZERO, FluidTank.TANK_MAP.get(Tier.WOOD).defaultBlockState())
  private final val modelWrapper = new TankModelWrapper(null)

  override def render(stack: ItemStack, cameraType: ItemDisplayContext, matrixStack: PoseStack,
                      renderTypeBuffer: MultiBufferSource, light: Int, otherLight: Int): Unit = {
    stack.getItem match {
      case tankItem: ItemBlockTank =>

        val state = tankItem.blockTank.defaultBlockState()
        val model = Minecraft.getInstance.getBlockRenderer.getBlockModel(state)
        RenderSystem.enableCull()
        renderItemModel(Minecraft.getInstance().getItemRenderer, model, stack, light, otherLight, matrixStack, renderTypeBuffer)

        tileTank.tier = tankItem.blockTank.tier
        val data = stack.get(DataComponents.BLOCK_ENTITY_DATA)
        if (data != null && !data.isEmpty) {
          tileTank.load(data.copyTag(), Minecraft.getInstance.level.registryAccess())
          if (tileTank.getTank.hasContent) {
            Minecraft.getInstance.getBlockEntityRenderDispatcher.renderItem(
              tileTank, matrixStack, renderTypeBuffer, light, otherLight
            )
          }
          // RenderHelper.disableStandardItemLighting()
        }
      case _ => FluidTankCommon.LOGGER.info("RenderItemTank is called for " + stack.getItem)
    }
  }

  // copy of ItemRenderer#func_229114_a_()
  def renderItemModel(renderer: ItemRenderer, model: BakedModel, stack: ItemStack, light: Int, otherLight: Int, matrixStack: PoseStack, renderTypeBuffer: MultiBufferSource): Unit = {
    val tankModelWrapper = modelWrapper
    tankModelWrapper.setModel(model)
    matrixStack.pushPose()
    matrixStack.translate(0.5D, 0.5D, 0.5D)
    renderer.render(stack, ItemDisplayContext.NONE, false, matrixStack, renderTypeBuffer, light, otherLight, tankModelWrapper)
    matrixStack.popPose()
  }
}

object RenderItemTank {
  val INSTANCE = new RenderItemTank
}
