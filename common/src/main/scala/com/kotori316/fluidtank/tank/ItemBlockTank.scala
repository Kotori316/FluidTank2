package com.kotori316.fluidtank.tank

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmountUtil, PlatformFluidAccess}
import net.minecraft.network.chat.Component
import net.minecraft.world.item.{BlockItem, Item, ItemStack, Rarity, TooltipFlag}
import net.minecraft.world.level.Level

import java.util

class ItemBlockTank(val blockTank: BlockTank) extends BlockItem(blockTank, new Item.Properties()) {
  override def toString: String = blockTank.tier.getBlockName

  override def getRarity(stack: ItemStack): Rarity =
    if (BlockItem.getBlockEntityData(stack) != null) Rarity.RARE
    else Rarity.COMMON

  override def appendHoverText(stack: ItemStack, level: Level, tooltip: util.List[Component], isAdvanced: TooltipFlag): Unit = {
    super.appendHoverText(stack, level, tooltip, isAdvanced)
    val nbt = BlockItem.getBlockEntityData(stack)
    if (nbt != null) {
      val tankTag = nbt.getCompound(TileTank.KEY_TANK)
      val access = FluidAmountUtil.access
      val fluid = access.read(tankTag.getCompound(access.KEY_CONTENT))
      val capacity = GenericUnit.fromByteArray(tankTag.getByteArray(access.KEY_AMOUNT_GENERIC))
      tooltip.add(Component.translatable("fluidtank.waila.short",
        PlatformFluidAccess.getInstance().getDisplayName(fluid), fluid.amount.asDisplay, capacity.asDisplay))
    } else {
      tooltip.add(Component.translatable("fluidtank.waila.capacity", GenericUnit.asForgeFromBigInt(blockTank.tier.getCapacity)))
    }
  }
}
