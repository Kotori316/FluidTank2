package com.kotori316.fluidtank.render

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.tank.{ItemBlockTank, PlatformTankAccess, Tier, TileTank, VisualTank}
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.special.SpecialModelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.{ProblemReporter, Unit as UtilUnit}
import net.minecraft.world.item.{ItemDisplayContext, ItemStack}
import net.minecraft.world.level.storage.TagValueInput
import org.joml.Vector3f

import java.util
import java.util.Locale

class RenderItemTank(model: TankModel, renderHelper: FluidRenderHelper) extends SpecialModelRenderer[RenderItemTank.RenderContext] {
  private lazy val tileTank: TileTank = new RenderItemTank.TileTankForRender

  override def submit(patterns: RenderItemTank.RenderContext, displayContext: ItemDisplayContext, poseStack: PoseStack, nodeCollector: SubmitNodeCollector, packedLight: Int, packedOverlay: Int, hasFoil: Boolean, outlineColor: Int): Unit = {
    val textureLocation = ResourceLocation.fromNamespaceAndPath(FluidTankCommon.modId, s"textures/block/${patterns.tier.toString}.png".toLowerCase(Locale.ROOT))
    nodeCollector.submitModel(this.model, UtilUnit.INSTANCE, poseStack, this.model.renderType(textureLocation), packedLight, packedOverlay, outlineColor, null)

    tileTank.tier = patterns.tier
    for (d <- patterns.data if !d.isEmpty) {
      val level = Minecraft.getInstance.level
      tileTank.setLevel(level)
      tileTank.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), d))
      if (tileTank.getTank.hasContent) {
        Minecraft.getInstance.getModelManager.specialBlockModelRenderer.get
          .renderByBlock(PlatformTankAccess.getInstance().getTankBlockMap.get(patterns.tier).get(), ItemDisplayContext.NONE, poseStack, nodeCollector, packedLight, packedOverlay, outlineColor)
      }
    }
  }

  override def getExtents(output: util.Set[Vector3f]): Unit = {}

  override def extractArgument(stack: ItemStack): RenderItemTank.RenderContext = {
    RenderItemTank.RenderContext(
      stack.getItem.asInstanceOf[ItemBlockTank].blockTank.tier,
      Option(stack.get(DataComponents.BLOCK_ENTITY_DATA)).map(_.copyTagWithoutId()),
    )
  }
}

object RenderItemTank {
  case class RenderContext(tier: Tier, data: Option[CompoundTag])

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

    override def getBlockPos: BlockPos = {
      Minecraft.getInstance().player.blockPosition()
    }
  }
}
