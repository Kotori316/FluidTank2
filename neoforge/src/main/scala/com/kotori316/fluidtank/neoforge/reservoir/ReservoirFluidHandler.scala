package com.kotori316.fluidtank.neoforge.reservoir

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.neoforge.fluid.TankFluidHandler
import com.kotori316.fluidtank.reservoir.ItemReservoir
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem

class ReservoirFluidHandler(reservoir: ItemReservoir, stack: ItemStack) extends TankFluidHandler {

  override def getContainer: ItemStack = stack

  def getCapability(ignored: Void): IFluidHandlerItem = this

  override def getTank: Tank[FluidLike] = reservoir.getTank(stack)

  override def saveTank(newTank: Tank[FluidLike]): Unit = reservoir.saveTank(stack, newTank)
}
