package com.kotori316.fluidtank.neoforge.reservoir

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.neoforge.fluid.TankFluidHandler
import com.kotori316.fluidtank.reservoir.ItemReservoir
import net.neoforged.neoforge.transfer.access.ItemAccess
import net.neoforged.neoforge.transfer.item.ItemResource

class ReservoirFluidHandler(reservoir: ItemReservoir, access: ItemAccess) extends TankFluidHandler(access) {

  override def getTank: Tank[FluidLike] = reservoir.getTank(this.context.getResource.toStack)

  override def saveTank(newTank: Tank[FluidLike]): ItemResource = {
    val stack = this.context.getResource.toStack
    reservoir.saveTank(stack, newTank)
    ItemResource.of(stack)
  }
}
