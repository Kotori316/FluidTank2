package com.kotori316.fluidtank.neoforge.fluid

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, VanillaFluid, VanillaPotion}
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.transfer.fluid.FluidResource

object NeoForgeConverter {
  def toStack(amount: FluidAmount): FluidStack = {
    amount.content match {
      // Just use Holder.direct here, as constructor only see the value.
      case VanillaFluid(fluid) => new FluidStack(Holder.direct(fluid), amount.amount.asForge, amount.componentPatch.getOrElse(DataComponentPatch.EMPTY))
      case VanillaPotion(_) => FluidStack.EMPTY
    }
  }

  def toAmount(stack: FluidStack): FluidAmount = {
    FluidAmountUtil.from(stack.getFluid, GenericUnit.fromForge(stack.getAmount), Option(stack.getComponentsPatch))
  }

  def toAmount(fluid: FluidResource, amount: Long): FluidAmount = {
    FluidAmountUtil.from(fluid.getFluid, GenericUnit.fromForge(amount), Option(fluid.getComponentsPatch))
  }

  def toVariant(amount: FluidAmount): FluidResource = {
    amount.content match {
      case VanillaFluid(fluid) => FluidResource.of(fluid)
      case VanillaPotion(_) => FluidResource.EMPTY
    }
  }

  def forgeAmount(amount: FluidAmount): Int = amount.amount.asForge

  extension (amount: FluidAmount) {
    def asVariant: FluidResource = NeoForgeConverter.toVariant(amount)
  }
}
