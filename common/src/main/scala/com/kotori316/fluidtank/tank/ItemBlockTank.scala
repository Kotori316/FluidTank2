package com.kotori316.fluidtank.tank

import com.kotori316.fluidtank.FluidTankCommon
import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, PlatformFluidAccess}
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.{Identifier, ResourceKey}
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.{BlockItem, Item, ItemStack, TooltipFlag}

import java.util.function.Consumer

class ItemBlockTank(val blockTank: BlockTank) extends BlockItem(blockTank, new Item.Properties()
  .useBlockDescriptionPrefix()
  .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(FluidTankCommon.modId, blockTank.tier.getBlockName)))
) {
  override def toString: String = blockTank.tier.getBlockName

  //noinspection ScalaDeprecation,deprecation
  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltipDisplay: TooltipDisplay, tooltip: Consumer[Component], isAdvanced: TooltipFlag): Unit = {
    super.appendHoverText(stack, context, tooltipDisplay, tooltip, isAdvanced)
    val component = stack.get(DataComponents.BLOCK_ENTITY_DATA)
    if (component != null) {
      val nbt = component.copyTagWithoutId()
      val tankTag = nbt.getCompoundOrEmpty(TileTank.KEY_TANK)
      val access = FluidAmountUtil.access
      val fluid = access.read(tankTag.getCompoundOrEmpty(access.KEY_CONTENT))
      val capacity = GenericUnit.fromByteArray(tankTag.getByteArray(access.KEY_AMOUNT_GENERIC).orElse(Array()))
      tooltip.accept(Component.translatable("fluidtank.waila.short",
        PlatformFluidAccess.getInstance().getDisplayName(fluid), fluid.amount.asDisplay, capacity.asDisplay))
    } else {
      tooltip.accept(Component.translatable("fluidtank.waila.capacity", GenericUnit.asForgeFromBigInt(blockTank.tier.getCapacity)))
    }
  }
}
