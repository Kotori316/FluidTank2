package com.kotori316.fluidtank.tank

import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.{ItemStack, TooltipFlag}

import java.util.function.Consumer

class ItemBlockVoidTank(b: BlockTank) extends ItemBlockTank(b) {
  //noinspection ScalaDeprecation,deprecation
  override def appendHoverText(stack: ItemStack, context: TooltipContext, tooltipDisplay: TooltipDisplay, tooltip: Consumer[Component], isAdvanced: TooltipFlag): Unit = {
  }
}
