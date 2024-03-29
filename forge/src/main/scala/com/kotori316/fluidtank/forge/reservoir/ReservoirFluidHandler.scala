package com.kotori316.fluidtank.forge.reservoir

import com.kotori316.fluidtank.contents.Tank
import com.kotori316.fluidtank.fluids.FluidLike
import com.kotori316.fluidtank.forge.fluid.TankFluidHandler
import com.kotori316.fluidtank.reservoir.ItemReservoir
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.capabilities.{Capability, ForgeCapabilities, ICapabilityProvider}
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.fluids.capability.IFluidHandlerItem

class ReservoirFluidHandler(reservoir: ItemReservoir, stack: ItemStack) extends TankFluidHandler with ICapabilityProvider {
  private val handler = LazyOptional.of[IFluidHandlerItem](() => this)

  override def getContainer: ItemStack = stack

  override def getCapability[T](capability: Capability[T], arg: Direction): LazyOptional[T] =
    ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(capability, this.handler)

  override def getTank: Tank[FluidLike] = reservoir.getTank(stack)

  override def saveTank(newTank: Tank[FluidLike]): Unit = reservoir.saveTank(stack, newTank)
}
