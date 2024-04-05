package com.kotori316.fluidtank.tank

import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.{ItemStack, TooltipFlag}

import java.util

class ItemBlockCreativeTank(b: BlockTank) extends ItemBlockTank(b) {
  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: util.List[Component], isAdvanced: TooltipFlag): Unit = {
    tooltip.add(Component.literal("Creative"))
  }
}
