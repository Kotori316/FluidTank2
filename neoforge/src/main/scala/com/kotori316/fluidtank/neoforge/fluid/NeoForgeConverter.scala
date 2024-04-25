package com.kotori316.fluidtank.neoforge.fluid

import com.kotori316.fluidtank.contents.GenericUnit
import com.kotori316.fluidtank.fluids.{FluidAmount, FluidAmountUtil, VanillaFluid, VanillaPotion}
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.neoforged.neoforge.fluids.FluidStack

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

  implicit final class FluidAmount2FluidStack(private val a: FluidAmount) extends AnyVal {
    def toStack: FluidStack = NeoForgeConverter.toStack(a)
  }

  implicit final class FluidStack2FluidAmount(private val stack: FluidStack) extends AnyVal {
    def toAmount: FluidAmount = NeoForgeConverter.toAmount(stack)
  }
}
